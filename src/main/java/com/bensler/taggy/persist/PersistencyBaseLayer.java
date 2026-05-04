package com.bensler.taggy.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

public class PersistencyBaseLayer {

  interface SetPrepStmtValueWrapper<BASE_TYPE> {

    void setValue(PreparedStatement stmt, int index, BASE_TYPE value) throws SQLException;

  }

  public static final SetPrepStmtValueWrapper<Integer> INTEGER_PROPERTY_PERSISTER = (stmt, index, value) -> stmt.setInt(index, value);
  public static final SetPrepStmtValueWrapper<String>  STRING_PROPERTY_PERSISTER  = (stmt, index, value) -> stmt.setString(index, value);

  public static class PropertyTable<BASE_TYPE> {

    private final String tableName_;
    private final String insertStmt_;
    private final SetPrepStmtValueWrapper<BASE_TYPE> valuePersister_;

    PropertyTable(String tableName, SetPrepStmtValueWrapper<BASE_TYPE> valuePersister) {
      tableName_ = tableName;
      insertStmt_ = "INSERT INTO %s (entity_id,entity_property_id,value) VALUES (?,?,?)".formatted(tableName_);
      valuePersister_ = valuePersister;
    }

    public String getTableName() {
      return tableName_;
    }

    public PreparedStatement prepareInsertStmt(Connection con) {
      try {
        return con.prepareStatement(insertStmt_);
      } catch (SQLException sqle) {
        throw new RuntimeException(sqle);
      }
    }

    public void populateInsertStmt(PreparedStatement stmt, Integer entityId, Integer propertyId, BASE_TYPE value) throws SQLException {
      stmt.setInt(1, entityId);
      stmt.setInt(2, propertyId);
      valuePersister_.setValue(stmt, 3, value);
      stmt.addBatch();
    }
  }

  public static final PropertyTable<String>  PROPERTY_TABLE_STRING  = new PropertyTable<>("property_string",  STRING_PROPERTY_PERSISTER);
  public static final PropertyTable<String>  PROPERTY_TABLE_BLOB    = new PropertyTable<>("property_blob",    STRING_PROPERTY_PERSISTER);
  public static final PropertyTable<Integer> PROPERTY_TABLE_INTEGER = new PropertyTable<>("property_integer", INTEGER_PROPERTY_PERSISTER);
  public static final PropertyTable<Integer> PROPERTY_TABLE_ENTITY  = new PropertyTable<>("property_entity",  INTEGER_PROPERTY_PERSISTER);

  public static final List<String> PROPERTY_TABLE_NAMES = Stream.concat(
    Stream.of(
      PROPERTY_TABLE_STRING,
      PROPERTY_TABLE_BLOB,
      PROPERTY_TABLE_INTEGER,
      PROPERTY_TABLE_ENTITY
    ).map(PropertyTable::getTableName),
    Stream.of("property_optional")
  ).toList();

  public static class PropertyTableEntry<BASE_TYPE> {

    private final Integer propertyId_;
    private final PropertyTable<BASE_TYPE> table_;
    private final BASE_TYPE value_;

    PropertyTableEntry(Integer propertyId, PropertyTable<BASE_TYPE> table, BASE_TYPE value) {
      propertyId_ = propertyId;
      table_ = table;
      value_ = value;
    }

    public void setValues(PreparedStatement stmt, Integer entityId) throws SQLException {
      table_.populateInsertStmt(stmt, entityId, propertyId_, value_);
    }
  }

  private final Connection con_;

  public PersistencyBaseLayer(Connection con) {
    con_ = con;
  }

  public  Integer createEntity(EntityType<?> type) throws SQLException {
    try (PreparedStatement stmt = con_.prepareStatement("INSERT INTO entity (name) VALUES (?)")) {
      stmt.setString(1, type.getClassName());
      stmt.execute();
      try (ResultSet ids = stmt.getGeneratedKeys()) {
        ids.next();
        return ids.getInt(1);
      }
    }
  }

  public void storeOptionalProperties(Integer entityId, Map<String, String> properties) throws SQLException {
    if (!properties.isEmpty()) {
      try (PreparedStatement stmt = con_.prepareStatement("INSERT INTO property_optional (entity_id,name,value) VALUES (?,?,?)")) {
        for (Entry<String, String> property : properties.entrySet()) {
          stmt.setInt(1, entityId);
          stmt.setString(2, property.getKey());
          stmt.setString(3, property.getValue());
          stmt.addBatch();
        }
        stmt.executeBatch();
      }
    }
  }

  public void dropProperties(Integer entityId) throws SQLException {
    for (String tableName : PROPERTY_TABLE_NAMES) {
      try (PreparedStatement stmt = con_.prepareStatement("DELETE FROM %s WHERE entity_id=?".formatted(tableName))) {
        stmt.setInt(1, entityId);
        stmt.execute();
      }
    }
  }

  public void storeProperties(Integer entityId, Collection<PropertyTableEntry<?>> values) throws SQLException {
    final Map<PropertyTable<?>,PreparedStatement> prepStmts = new HashMap<>();

    for (PropertyTableEntry<?> value : values) {
      final PreparedStatement stmt = prepStmts.computeIfAbsent(value.table_, table -> table.prepareInsertStmt(con_));

      value.setValues(stmt, entityId);
    }
    for (PreparedStatement stmt : prepStmts.values()) {
      stmt.executeBatch();
      stmt.close();
    }
  }

}
