package com.bensler.taggy.persist.v2;

import static com.bensler.taggy.persist.v2.EntityPropertyType.ENTITY;
import static com.bensler.taggy.persist.v2.EntityPropertyType.STRING;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Image;
import com.bensler.taggy.persist.Tag;

public class DbSetup {

  public static final List<EntityPropertyType<?, ?>> KNOWN_PROPERTY_TYPES = List.of(
    EntityPropertyType.STRING,
    EntityPropertyType.STRINGS,
    EntityPropertyType.INTEGER,
    EntityPropertyType.INTEGERS,
    EntityPropertyType.ENTITY,
    EntityPropertyType.ENTITIES,
    EntityPropertyType.BLOB
  );

  public static final EntityProperty<String> P_TAG__NAME = new EntityProperty<>("name", STRING);
  public static final EntityProperty<EntityReference<?>> P_TAG__PARENT = new EntityProperty<>("parent", ENTITY);
  public static final EntityType<Tag> E_TAG = new EntityType<>(
    Tag.class,
    P_TAG__NAME,
    P_TAG__PARENT
  );
  public static final EntityProperty<String> P_BLOB__FILE = new EntityProperty<>("file", STRING);
  public static final EntityProperty<String> P_BLOB__TYPE = new EntityProperty<>("type", STRING);
  public static final EntityType<Blob> E_BLOB = new EntityType<>(
    Blob.class,
    P_BLOB__FILE,
    P_BLOB__TYPE
  );
  public static final EntityProperty<EntityReference<?>> P_IMAGE__FULL_SCALE_IMAGE = new EntityProperty<>("fullScaleImage", ENTITY);
  public static final EntityType<Image> E_IMAGE = new EntityType<>(
    Image.class, E_BLOB,
    P_IMAGE__FULL_SCALE_IMAGE
  );
  public static final EntityRelationshipType RELATION_TAG_IMAGE = new EntityRelationshipType("tag-image");

  private final Map<Class<?>, EntityType<?>> entityTypes_;
  private final Map<String, EntityPropertyType<?, ?>> propertyTypes_;
  private final Map<String, EntityRelationshipType> relationshipTypes_;
  private final Map<EntityProperty<?>, Integer> propertyIds_;

  public DbSetup(Connection con) throws SQLException {
    propertyTypes_ = setupEntityPropertyTypes(con);
    relationshipTypes_ = setupEntityRelationshipTypes(con, List.of(RELATION_TAG_IMAGE));
    entityTypes_ = List.of(E_TAG, E_BLOB, E_IMAGE).stream().collect(Collectors.toMap(EntityType::getEntityClass, identity()));
    propertyIds_ = setupPropertyIds(con, entityTypes_.values());
  }

  public PersistedEntity createPersistedEntity(EntityType<?> type) {
    return new PersistedEntity(this, type, Optional.empty());
  }

  public PersistedEntity createPersistedEntity(EntityType<?> type, Integer id) {
    return new PersistedEntity(this, type, Optional.of(id));
  }

  private Map<String, EntityRelationshipType> setupEntityRelationshipTypes(Connection con, List<EntityRelationshipType> relationshipTypes) throws SQLException {
    final Map<String, EntityRelationshipType> typesByName = relationshipTypes.stream().collect(Collectors.toMap(EntityRelationshipType::getName, identity()));
    final Set<String> dbValues = new HashSet<>();

    try (
      PreparedStatement stmt = con.prepareStatement("SELECT name FROM entity_relationship_type");
      ResultSet result = stmt.executeQuery();
    ) {
      while (result.next()) {
        dbValues.add(result.getString(1));
      }
    }
    if (dbValues.size() < relationshipTypes.size()) {
      try (
        PreparedStatement stmt = con.prepareStatement("INSERT INTO entity_relationship_type (name) VALUES (?)");
      ) {
        for (String typeName : typesByName.keySet()) {
          if (!dbValues.contains(typeName)) {
            stmt.setString(1, typeName);
            stmt.addBatch();
          }
        }
        stmt.executeBatch();
      }
    }
    return typesByName;
  }

  private Map<EntityProperty<?>, Integer> setupPropertyIds(Connection con, Collection<EntityType<?>> collection) throws SQLException {
    final Map<EntityProperty<?>, Integer> propIdCollector = new HashMap<>();
    final Map<String, EntityType<?>> typesByName = setupEntityIds(con, collection);
    final Map<String, Set<EntityProperty<?>>> propsToInsert = collection.stream().collect(toMap(EntityType::getClassName, EntityType::getProperties));

    try (
      PreparedStatement stmt = con.prepareStatement("SELECT id, entity_type_name, name, entity_property_type_name FROM entity_property");
      ResultSet result = stmt.executeQuery();
    ) {
      while (result.next()) {
        final Integer typeId = result.getInt(1);
        final String typeName = result.getString(2);
        final EntityType<?> type = typesByName.get(typeName);

        if (type != null) {
          type.getProperty(result.getString(3), propertyTypes_.get(result.getString(4)))
          .ifPresent(property -> {
            propsToInsert.get(typeName).remove(property);
            propIdCollector.put(property, typeId);
          });
        }
      }
    }
    if (propsToInsert.values().stream().flatMap(Set::stream).findAny().isPresent()) {
      try (
        PreparedStatement stmt = con.prepareStatement(
          "INSERT INTO entity_property (entity_type_name, name, entity_property_type_name) VALUES (?, ?, ?) RETURNING id", Statement.RETURN_GENERATED_KEYS
        );
      ) {
        for (String typeName : propsToInsert.keySet()) {
          for (EntityProperty<?> prop : propsToInsert.get(typeName)) {
            final ResultSet generatedKeys;

            stmt.setString(1, typeName);
            stmt.setString(2, prop.getName());
            stmt.setString(3, prop.getType().getName());
            // executeBatch() returning a collection of generated IDs does not work with Sqlite
            generatedKeys = stmt.executeQuery();
            generatedKeys.next();
            if (propIdCollector.containsKey(prop)) {
              throw new IllegalStateException("Duplicate use of property \"%s:%s\" in EntityType \"%s\" ".formatted(prop.getName(), prop.getType().getName(), typeName));
            } else {
              propIdCollector.put(prop, generatedKeys.getInt(1));
            }
          }
        }
      }
    }
    return propIdCollector;
  }

  private Map<String, EntityType<?>> setupEntityIds(Connection con, Collection<EntityType<?>> collection) throws SQLException {
    final Map<String, EntityType<?>> typesByName = collection.stream().collect(toMap(EntityType::getClassName, identity()));

    collection = new ArrayList<>(collection);
    try (
      PreparedStatement stmt = con.prepareStatement("SELECT name, parent_name FROM entity_type");
      ResultSet result = stmt.executeQuery();
    ) {
      while (result.next()) {
        final String dbTypeName = result.getString(1);
        final EntityType<?> type = typesByName.get(dbTypeName);

        if (type != null) {
          if (!type.getParentClassName().equals(Optional.ofNullable(result.getString(2)))) {
            throw new IllegalStateException("Parent type mismatch in type \"%s\"".formatted(dbTypeName));
          }
          collection.remove(type);
        }
      }
    }
    if (!collection.isEmpty()) {
      try (
        PreparedStatement stmt = con.prepareStatement("INSERT INTO entity_type (name, parent_name) VALUES (?, ?)");
      ) {
        for (EntityType<?> type : collection) {
          stmt.setString(1, type.getClassName());
          stmt.setString(2, type.getParentClassName().orElse(null));
          stmt.addBatch();
        }
        stmt.executeBatch();
      }
    }
    return typesByName;
  }

  private Map<String, EntityPropertyType<?, ?>> setupEntityPropertyTypes(Connection con) throws SQLException {
    final Map<String, EntityPropertyType<?, ?>> types = KNOWN_PROPERTY_TYPES.stream().collect(Collectors.toMap(EntityPropertyType::getName, identity()));
    final Set<String> dbValues = new HashSet<>();

    try (
      PreparedStatement stmt = con.prepareStatement("SELECT name FROM entity_property_type");
      ResultSet result = stmt.executeQuery();
    ) {
      while (result.next()) {
        dbValues.add(result.getString(1));
      }
    }
    if (dbValues.size() < types.size()) {
      try (
        PreparedStatement stmt = con.prepareStatement("INSERT INTO entity_property_type (name) VALUES (?)");
      ) {
        for (String typeName : types.keySet()) {
          if (!dbValues.contains(typeName)) {
            stmt.setString(1, typeName);
            stmt.addBatch();
          }
        }
        stmt.executeBatch();
      }
    }
    return types;
  }

}
