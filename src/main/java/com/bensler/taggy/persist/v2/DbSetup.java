package com.bensler.taggy.persist.v2;

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
import java.util.Set;
import java.util.stream.Collectors;

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

  public static final EntityRelationshipType RELATION_TAG_IMAGE = new EntityRelationshipType("tag-image");

  private final Map<Class<?>, EntityType<?>> entityTypes_;
  private final Map<String, EntityPropertyType<?, ?>> propertyTypes_;
  private final Map<String, EntityRelationshipType> relationshipTypes_;
  private final Map<BoundEntityProperty, Integer> propertyIds_;

  public DbSetup(Connection con) throws SQLException {
    propertyTypes_ = setupEntityPropertyTypes(con);
    relationshipTypes_ = setupEntityRelationshipTypes(con, List.of(RELATION_TAG_IMAGE));
    entityTypes_ = new HashMap<>();
    propertyIds_ = new HashMap<>();
  }

  public void registerEntityTypes(Connection con, List<EntityType<?>> entityTypes) throws SQLException {
    final Map<Class<?>, EntityType<?>> entityTypesByName = new HashMap<>();

    for (EntityType<?> entityType : entityTypes) {
      final Class<?> entityClass = entityType.getEntityClass();

      if (entityTypes_.containsKey(entityClass)) {
        throw new IllegalArgumentException();
      } else {
        entityTypesByName.put(entityClass, entityType);
      }
    }
    entityTypes_.putAll(entityTypesByName);
    propertyIds_.putAll(setupPropertyIds(con, entityTypesByName.values()));
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

  private Map<BoundEntityProperty, Integer> setupPropertyIds(Connection con, Collection<EntityType<?>> entityTypes) throws SQLException {
    final Map<BoundEntityProperty, Integer> propIdCollector = new HashMap<>();
    final Map<String, EntityType<?>> typesByName = setupEntityIds(con, entityTypes);
    final Map<String, Collection<BoundEntityProperty>> propsToInsert = entityTypes.stream().collect(toMap(EntityType::getClassName, EntityType::getProperties));

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
    if (propsToInsert.values().stream().flatMap(Collection::stream).findAny().isPresent()) {
      try (
        PreparedStatement stmt = con.prepareStatement(
          "INSERT INTO entity_property (entity_type_name, name, entity_property_type_name) VALUES (?, ?, ?) RETURNING id", Statement.RETURN_GENERATED_KEYS
        );
      ) {
        for (String typeName : propsToInsert.keySet()) {
          for (BoundEntityProperty prop : propsToInsert.get(typeName)) {
            final ResultSet generatedKeys;

            stmt.setString(1, typeName);
            stmt.setString(2, prop.getName());
            stmt.setString(3, prop.getTypeName());
            // executeBatch() returning a collection of generated IDs does not work with Sqlite
            generatedKeys = stmt.executeQuery();
            generatedKeys.next();
            if (propIdCollector.containsKey(prop)) {
              throw new IllegalStateException("Duplicate use of property \"%s:%s\" in EntityType \"%s\" ".formatted(prop.getName(), prop.getTypeName(), typeName));
            } else {
              propIdCollector.put(prop, generatedKeys.getInt(1));
            }
          }
        }
      }
    }
    return propIdCollector;
  }

  private Map<String, EntityType<?>> setupEntityIds(Connection con, Collection<EntityType<?>> entityTypes) throws SQLException {
    final Map<String, EntityType<?>> typesByName = entityTypes.stream().collect(toMap(EntityType::getClassName, identity()));

    entityTypes = new ArrayList<>(entityTypes);
    try (
      PreparedStatement stmt = con.prepareStatement("SELECT name FROM entity_type");
      ResultSet result = stmt.executeQuery();
    ) {
      while (result.next()) {
        final String dbTypeName = result.getString(1);
        final EntityType<?> type = typesByName.get(dbTypeName);

        if (type != null) {
          entityTypes.remove(type);
        }
      }
    }
    if (!entityTypes.isEmpty()) {
      try (
        PreparedStatement stmt = con.prepareStatement("INSERT INTO entity_type (name) VALUES (?)");
      ) {
        for (EntityType<?> type : entityTypes) {
          stmt.setString(1, type.getClassName());
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

  public Integer getPropertyKey(EntityProperty<?> property) {
    if (propertyIds_.containsKey(property)) {
      return propertyIds_.get(property);
    } else {
      throw new IllegalArgumentException();
    }
  }

}
