package com.bensler.taggy.persist;

import static com.bensler.taggy.persist.EntityPropertyType.ENTITY;
import static com.bensler.taggy.persist.EntityPropertyType.STRING;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class DbSetup {

  public static final EntityProperty P_TAG__NAME = new EntityProperty("name", STRING);
  public static final EntityProperty P_TAG__PARENT = new EntityProperty("parent", ENTITY);
  public static final EntityType<Tag> E_TAG = new EntityType<>(
    Tag.class,
    P_TAG__NAME,
    P_TAG__PARENT
  );
  public static final EntityProperty P_BLOB__FILE = new EntityProperty("file", STRING);
  public static final EntityProperty P_BLOB__TYPE = new EntityProperty("type", STRING);
  public static final EntityType<Blob> E_BLOB = new EntityType<>(
    Blob.class,
    P_BLOB__FILE,
    P_BLOB__TYPE
  );
  public static final EntityProperty P_IMAGE__FULL_SCALE_IMAGE = new EntityProperty("fullScaleImage", ENTITY);
  public static final EntityType<Image> E_IMAGE = new EntityType<>(
    Image.class, E_BLOB,
    P_IMAGE__FULL_SCALE_IMAGE
  );

  private final Map<EntityProperty, Integer> propertyIds_;

  public DbSetup(DbAccess db) throws SQLException {
    propertyIds_ = setupPropertyIds(db, List.of(E_TAG, E_BLOB, E_IMAGE));
  }

  private Map<EntityProperty, Integer> setupPropertyIds(DbAccess db, List<EntityType<?>> types) throws SQLException {
    final Map<EntityProperty, Integer> propIdCollector = new HashMap<>();
    final Map<String, EntityType<?>> typesByName = setupEntityIds(db, types);
    final Map<String, Set<EntityProperty>> propsToInsert = types.stream().collect(toMap(EntityType::getClassName, EntityType::getProperties));

    try (
      PreparedStatement stmt = db.session_.prepareStatement("SELECT id, entity_type_name, name, entity_property_type_name FROM entity_property");
      ResultSet result = stmt.executeQuery();
    ) {
      while (result.next()) {
        final Integer typeId = result.getInt(1);
        final String typeName = result.getString(2);
        final EntityType<?> type = typesByName.get(typeName);

        if (type != null) {
          type.getProperty(result.getString(3), EntityPropertyType.valueOf(result.getString(4)))
          .ifPresent(property -> {
            propsToInsert.get(typeName).remove(property);
            propIdCollector.put(property, typeId);
          });
        }
      }
    }
    if (propsToInsert.values().stream().flatMap(Set::stream).findAny().isPresent()) {
      try (
        PreparedStatement stmt = db.session_.prepareStatement(
          "INSERT INTO entity_property (entity_type_name, name, entity_property_type_name) VALUES (?, ?, ?) RETURNING id", Statement.RETURN_GENERATED_KEYS
        );
      ) {
        for (String typeName : propsToInsert.keySet()) {
          for (EntityProperty prop : propsToInsert.get(typeName)) {
            final ResultSet generatedKeys;

            stmt.setString(1, typeName);
            stmt.setString(2, prop.getName());
            stmt.setString(3, prop.getType().name());
            // executeBatch() returning a collection of generated IDs does not work with Sqlite
            generatedKeys = stmt.executeQuery();
            generatedKeys.next();
            if (propIdCollector.containsKey(prop)) {
              throw new IllegalStateException("Duplicate use of property \"%s:%s\" in EntityType \"%s\" ".formatted(prop.getName(), prop.getType().name(), typeName));
            } else {
              propIdCollector.put(prop, generatedKeys.getInt(1));
            }
          }
        }
      }
    }
    return propIdCollector;
  }

  private Map<String, EntityType<?>> setupEntityIds(DbAccess db, List<EntityType<?>> types) throws SQLException {
    final Map<String, EntityType<?>> typesByName = types.stream().collect(toMap(EntityType::getClassName, identity()));

    types = new ArrayList<>(types);
    try (
      PreparedStatement stmt = db.session_.prepareStatement("SELECT name, parent_name FROM entity_type");
      ResultSet result = stmt.executeQuery();
    ) {
      while (result.next()) {
        final String dbTypeName = result.getString(1);
        final EntityType<?> type = typesByName.get(dbTypeName);

        if (type != null) {
          if (!type.getParentClassName().equals(Optional.ofNullable(result.getString(2)))) {
            throw new IllegalStateException("Parent type mismatch in type \"%s\"".formatted(dbTypeName));
          }
          types.remove(type);
        }
      }
    }
    if (!types.isEmpty()) {
      try (
        PreparedStatement stmt = db.session_.prepareStatement("INSERT INTO entity_type (name, parent_name) VALUES (?, ?)");
      ) {
        for (EntityType<?> type : types) {
          stmt.setString(1, type.getClassName());
          stmt.setString(2, type.getParentClassName().orElse(null));
          stmt.addBatch();
        }
        stmt.executeBatch();
      }
    }
    return typesByName;
  }

}
