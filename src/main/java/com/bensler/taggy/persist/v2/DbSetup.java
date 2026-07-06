package com.bensler.taggy.persist.v2;

import static java.util.function.Function.identity;

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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.bensler.decaf.util.entity.Entity;
import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.persist.DbAccess;

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

  public static final String SUBSELECT_PROPERTY_BLOB = """
    SELECT
     'property_blob'        AS table_name,
      pb.entity_id          AS entity_id,
      pb.entity_property_id AS property_id,
      NULL                  AS value_name,
      pb.value              AS value_string,
      NULL                  AS value_integer
    FROM property_blob pb""";
  public static final String SUBSELECT_PROPERTY_ENTITY = """
    SELECT
     'property_entity'      AS table_name,
      pe.entity_id          AS entity_id,
      pe.entity_property_id AS property_id,
      NULL                  AS value_name,
      NULL                  AS value_string,
      pe.value              AS value_integer
    FROM property_entity pe""";
  public static final String SUBSELECT_PROPERTY_INTEGER = """
    SELECT
     'property_integer'     AS table_name,
      pi.entity_id          AS entity_id,
      pi.entity_property_id AS property_id,
      NULL                  AS value_name,
      NULL                  AS value_string,
      pi.value              AS value_integer
    FROM property_integer pi""";
  public static final String SUBSELECT_PROPERTY_STRING = """
    SELECT
     'property_string'      AS table_name,
      ps.entity_id          AS entity_id,
      ps.entity_property_id AS property_id,
      NULL                  AS value_name,
      ps.value              AS value_string,
      NULL                  AS value_integer
    FROM property_string ps""";
  public static final String SUBSELECT_PROPERTY_OPTIONAL = """
    SELECT
     'property_optional'    AS table_name,
      po.entity_id          AS entity_id,
      NULL                  AS property_id,
      po.name               AS value_name,
      po.value              AS value_string,
      NULL                  AS value_integer
    FROM property_optional po""";
  public static final List<String> PROPERTY_SUBSELECTS = List.of(
    SUBSELECT_PROPERTY_BLOB, SUBSELECT_PROPERTY_ENTITY, SUBSELECT_PROPERTY_INTEGER, SUBSELECT_PROPERTY_STRING, SUBSELECT_PROPERTY_OPTIONAL
  );

  public static final String SUBSELECT_RELATION_FROM = """
    SELECT
     'FROM'             AS direction,
      type_id           AS relationship_type_id,
      target_entity_id  AS entity_id,
      source_entity_id  AS other_entity_id
    FROM entity_relationship""";
  public static final String SUBSELECT_RELATION_TO = """
    SELECT
     'TO'               AS direction,
      type_id           AS relationship_type_id,
      source_entity_id  AS entity_id,
      target_entity_id  AS other_entity_id
    FROM entity_relationship""";
  public static final List<String> RELATION_SUBSELECTS = List.of(
    SUBSELECT_RELATION_FROM, SUBSELECT_RELATION_TO
  );

  public static final String WHERE_CLAUSE_TYPE = " WHERE entity_id IN (SELECT id FROM entity WHERE entity_type_id=?)";
  public static final String PLACEHOLDER_ENTITY_IDS  = "$entity_ids";
  public static final String WHERE_CLAUSE_IDS  = " WHERE entity_id IN (%s)".formatted(PLACEHOLDER_ENTITY_IDS);

  public static final String PLACEHOLDER_SUBSELECT = "$subselect";
  public static final String LOAD_ENTITY_PROPERTY_SQL = """
    SELECT
      table_name, entity_id, property_id, value_name, value_string, value_integer
    FROM (%s)""".formatted(PLACEHOLDER_SUBSELECT);
  public static final String LOAD_RELATION_PROPERTY_SQL = """
    SELECT
      direction, relationship_type_id, entity_id, other_entity_id, e.entity_type_id as other_entity_type
    FROM (%s)
    JOIN entity e ON other_entity_id = e.id""".formatted(PLACEHOLDER_SUBSELECT);

  public enum Direction {
    FROM, TO
  }

  /** TODO class equal to PersistedEntity? */
  static class LoadEntityCollector {

    private final Integer entityId_;
    private final Map<BoundEntityProperty, List<Object>> properties_;
    private final Map<String, String> optionalProperties_;
    private final Map<Direction, Map<EntityRelationshipType<?, ?>, List<Integer>>> relationsships;

    LoadEntityCollector(Integer entityId) {
      entityId_ = entityId;
      properties_ = new HashMap<>();
      optionalProperties_ = new HashMap<>();
      relationsships = new HashMap<>();
    }

    public Integer getEntityId() {
      return entityId_;
    }

    public void addValue(BoundEntityProperty property, Object value) {
      properties_.computeIfAbsent(property, lProperty -> new ArrayList<Object>()).add(value);
    }

    public void addOptionalValue(String valueName, String strValue) {
      optionalProperties_.put(valueName, strValue);
    }

    public void addRelationship(Direction direction, EntityRelationshipType<?, ?> relationType, Integer otherEntityId) {
      relationsships.computeIfAbsent(direction, _ -> new HashMap<>())
        .computeIfAbsent(relationType, _ -> new ArrayList<>())
        .add(otherEntityId);
    }

    public <T> Optional<T> getValue(EntityProperty<T> type) {
      return properties_.entrySet().stream()
        .filter(entry -> entry.getKey().is(type))
        .map(entry -> type.loadValue(entry.getValue()))
        .findFirst(); // TODO make properties optional <-> not optional
    }

    public Map<String, String> getOptionalProperties() {
      return Map.copyOf(optionalProperties_);
    }

    public <E extends Entity<E>> Set<EntityReference<E>> getRelationships(Direction direction, EntityRelationshipType<?, ?> relationshipType, Class<E> entityClass) {
      return relationsships.computeIfAbsent(direction, _ -> Map.of())
        .getOrDefault(relationshipType, List.of())
        .stream()
        .map(id -> new EntityReference<>(entityClass, id))
        .collect(Collectors.toSet());
    }

  }

  private final Map<String, EntityPropertyType<?, ?>> propertyTypes_;
  private final Map<BoundEntityProperty, Integer> propertyIds_;
  private final Map<Integer, EntityType<?>> idToEntityType_;
  private final Map<EntityType<?>, Integer> entityTypeToId_;
  private final Map<Integer, EntityRelationshipType<?, ?>> relationshipTypeIds_;

  public DbSetup(Connection con) throws SQLException {
    propertyTypes_ = setupEntityPropertyTypes(con);
    propertyIds_ = new HashMap<>();
    idToEntityType_ = new HashMap<>();
    entityTypeToId_ = new HashMap<>();
    relationshipTypeIds_ = new HashMap<>();
  }

  private PreparedStatement prepareStatement(DbAccess db, String query, List<String> subSelects, String subselectWhereClause) throws SQLException {
    return db.prepareStatement(query.replace(PLACEHOLDER_SUBSELECT,
      subSelects.stream().map(subselect -> subselect.concat(subselectWhereClause)).collect(Collectors.joining(" UNION "))
    ));
  }

  private PreparedStatement prepareStatement(DbAccess db, String query, List<String> subSelects, EntityType<?> entityType) throws SQLException {
    final PreparedStatement stmt = prepareStatement(db, query, subSelects, WHERE_CLAUSE_TYPE);
    final Integer entityTypeId = entityTypeToId_.get(entityType);

    IntStream.range(0, subSelects.size()).forEach(subQueryIdx -> {
      try {
        stmt.setInt(subQueryIdx + 1, entityTypeId);
      } catch (SQLException sqle) {
        throw new RuntimeException(sqle);
      }
    });
    return stmt;
  }

  private PreparedStatement prepareStatement(DbAccess db, String query, List<String> subSelects, List<Integer> ids) throws SQLException {
    final String idQuestionMarks = IntStream.range(0, ids.size()).mapToObj(_ -> "?").collect(Collectors.joining(","));
    final String whereClause = WHERE_CLAUSE_IDS.replace(PLACEHOLDER_ENTITY_IDS, idQuestionMarks);
    final PreparedStatement stmt = prepareStatement(db, query, subSelects, whereClause);
    final int idCount = ids.size();

    IntStream.range(0, subSelects.size()).forEach(subQueryIdx ->
      IntStream.range(0, idCount).forEach(idIdx -> {
        try {
          stmt.setInt((subQueryIdx * idCount) + idIdx + 1, ids.get(idIdx));
        } catch (SQLException sqle) {
          throw new RuntimeException(sqle);
        }
      })
    );
    return stmt;
  }

  public Collection<LoadEntityCollector> loadAllEntities(DbAccess db, EntityType<?> entityType, List<Integer> ids) throws SQLException {
    final Map<Integer, BoundEntityProperty> propertiesById = entityType.getProperties().stream().collect(Collectors.toMap(propertyIds_::get, identity()));
    final Map<Integer, LoadEntityCollector> collectors = new HashMap<>();

    try (PreparedStatement stmt = ids.isEmpty()
      ? prepareStatement(db, LOAD_ENTITY_PROPERTY_SQL, PROPERTY_SUBSELECTS, entityType)
      : prepareStatement(db, LOAD_ENTITY_PROPERTY_SQL, PROPERTY_SUBSELECTS, ids)
    ) {
      final ResultSet result = stmt.executeQuery();

      while (result.next()) {
        final LoadEntityCollector collector = collectors.computeIfAbsent(result.getInt("entity_id"), LoadEntityCollector::new);
        final Optional<String> optStrValue = Optional.ofNullable(result.getString("value_string"));
        final Optional<Object> optIntValue = Optional.ofNullable(result.getObject("value_integer"));
        final Optional<Object> optValue = optIntValue.or(() -> optStrValue);
        final Optional<Object> optPropertyId = Optional.ofNullable(result.getObject("property_id"));
        final Optional<String> optValueName = Optional.ofNullable(result.getString("value_name"));

        optPropertyId.map(propertiesById::get).ifPresent(property -> optValue.ifPresent(value -> collector.addValue(property, value)));
        optValueName.ifPresent(valueName -> optStrValue.ifPresent(strValue -> collector.addOptionalValue(valueName, strValue)));
      }
    }
    try (PreparedStatement stmt = ids.isEmpty()
      ? prepareStatement(db, LOAD_RELATION_PROPERTY_SQL, RELATION_SUBSELECTS, entityType)
      : prepareStatement(db, LOAD_RELATION_PROPERTY_SQL, RELATION_SUBSELECTS, ids)
    ) {
      final ResultSet result = stmt.executeQuery();

      while (result.next()) {
        final LoadEntityCollector collector = collectors.computeIfAbsent(result.getInt("entity_id"), LoadEntityCollector::new);
        final Direction direction = Direction.valueOf(result.getString("direction"));
        final Integer relationshipTypeId = (Integer)result.getObject("relationship_type_id");
        final Integer otherEntityId = (Integer)result.getObject("other_entity_id");
        final String otherTypeName = result.getString("other_entity_type"); // TODO keep type -> typecheck later

        collector.addRelationship(direction, relationshipTypeIds_.get(relationshipTypeId), otherEntityId);
      }
    }
    return collectors.values();
  }

  public void registerEntityTypes(Connection con, List<EntityType<?>> entityTypes) throws SQLException {
    final Map<String, Integer> nameIdMap = new HashMap<>();
    final List<EntityType<?>> toInsert = new ArrayList<>();

    try (
      PreparedStatement stmt = con.prepareStatement("SELECT id, name FROM entity_type");
      ResultSet result = stmt.executeQuery();
    ) {
      while (result.next()) {
        nameIdMap.put(result.getString(2),result.getInt(1));
      }
    }

    for (EntityType<?> entityType : entityTypes) {
      final String name = entityType.getClassName();

      if (entityTypeToId_.containsKey(entityType)) {
        throw new IllegalArgumentException();
      }
      if (nameIdMap.containsKey(name)) {
        final Integer id = nameIdMap.get(name);

        entityTypeToId_.put(entityType, id);
        idToEntityType_.put(id, entityType);
      } else {
        toInsert.add(entityType);
      }
    }
    if (!toInsert.isEmpty()) {
      try (PreparedStatement stmt = con.prepareStatement("INSERT INTO entity_type (name) VALUES (?) RETURNING id")) {
        for (EntityType<?> entityType : toInsert) {
          final ResultSet generatedKeys;
          final Integer id;

          stmt.setString(1, entityType.getClassName());
          // executeBatch() returning a collection of generated IDs does not work with Sqlite
          generatedKeys = stmt.executeQuery();
          generatedKeys.next();
          id = generatedKeys.getInt(1);
          entityTypeToId_.put(entityType, id);
          idToEntityType_.put(id, entityType);
        }
      }
    }
    for(EntityType<?> entityType : entityTypes) {
      setupPropertyIds(con, entityType);
    }
  }

  public void registerRelationshipTypes(Connection con, List<EntityRelationshipType<?, ?>> relationshipTypes) throws SQLException {
    final Map<Integer, EntityRelationshipType<?, ?>> relIdCollector = new HashMap<>();
    final Map<String, EntityRelationshipType<?, ?>> relTypesByName = new HashMap<>(relationshipTypes.stream().collect(Collectors.toMap(EntityRelationshipType::getName, identity())));

    try (
      PreparedStatement stmt = con.prepareStatement("SELECT name, id FROM entity_relationship_type");
      ResultSet result = stmt.executeQuery();
    ) {
      while (result.next()) {
        final EntityRelationshipType<?, ?> knownRelationship = relTypesByName.remove(result.getString(1));

        if (knownRelationship != null) {
          relIdCollector.put(result.getInt(2), knownRelationship);
        }
      }
    }
    if (!relTypesByName.isEmpty()) {
      try (
        PreparedStatement stmt = con.prepareStatement("INSERT INTO entity_relationship_type (name, source_entity_type_id, target_entity_type_id) VALUES (?,?,?) RETURNING id");
      ) {
        for (EntityRelationshipType<?, ?> relationsship : relTypesByName.values()) {
          final ResultSet generatedKeys;

          stmt.setString(1, relationsship.getName());
          stmt.setInt(2, entityTypeToId_.get(relationsship.getSourceType()));
          stmt.setInt(3, entityTypeToId_.get(relationsship.getTargetType()));
          // executeBatch() returning a collection of generated IDs does not work with Sqlite
          generatedKeys = stmt.executeQuery();
          generatedKeys.next();
          relIdCollector.put(generatedKeys.getInt(1), relationsship);
        }
      }
    }
    relationshipTypeIds_.putAll(relIdCollector);
  }

  private void setupPropertyIds(Connection con, EntityType<?> entityType) throws SQLException {
    final List<BoundEntityProperty> propsToInsert = entityType.getProperties();
    final Integer typeId = entityTypeToId_.get(entityType);

    try (
      PreparedStatement stmt = con.prepareStatement("SELECT id, name, entity_property_type_name FROM entity_property WHERE entity_type_id=?");
    ) {
      final ResultSet result;

      stmt.setInt(1, typeId);
      result = stmt.executeQuery();
      while (result.next()) {
        final Integer id = result.getInt(1);

        entityType.getProperty(result.getString(2), propertyTypes_.get(result.getString(3)))
        .ifPresent(property -> {
          propsToInsert.remove(property);
          propertyIds_.put(property, id);
        });
      }
    }
    if (propsToInsert.stream().findAny().isPresent()) {
      try (PreparedStatement stmt = con.prepareStatement(
        "INSERT INTO entity_property (entity_type_id, name, entity_property_type_name) VALUES (?, ?, ?) RETURNING id", Statement.RETURN_GENERATED_KEYS
      )) {
        for (BoundEntityProperty prop : propsToInsert) {
          final ResultSet generatedKeys;

          stmt.setInt(1, typeId);
          stmt.setString(2, prop.getName());
          stmt.setString(3, prop.getTypeName());
          // executeBatch() returning a collection of generated IDs does not work with Sqlite
          generatedKeys = stmt.executeQuery();
          generatedKeys.next();
          if (propertyIds_.containsKey(prop)) {
            throw new IllegalStateException("Duplicate use of property \"%s:%s\" in EntityType \"%s\" ".formatted(
              prop.getName(), prop.getTypeName(), idToEntityType_.get(typeId).getClassName()
            ));
          } else {
            propertyIds_.put(prop, generatedKeys.getInt(1));
          }
        }
      }
    }
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

  public Integer getPropertyKey(BoundEntityProperty property) {
    if (propertyIds_.containsKey(property)) {
      return propertyIds_.get(property);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public Integer getRelationshipKey(EntityRelationshipType<?, ?> relationship) {
    return relationshipTypeIds_.entrySet().stream()
      .filter(entry -> entry.getValue().equals(relationship))
      .map(Entry::getKey)
      .findAny()
      .orElseThrow(() -> new IllegalArgumentException());
  }

  public PersistedEntity createPersistedEntity(EntityType<?> entityType, Integer id) {
    return new PersistedEntity(entityType, entityTypeToId_.get(entityType), Optional.ofNullable(id));
  }

  public Integer getEntityTypeId(EntityType<?> entityType) {
    return entityTypeToId_.get(entityType);
  }

}
