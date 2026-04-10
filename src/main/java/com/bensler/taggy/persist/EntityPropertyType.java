package com.bensler.taggy.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum EntityPropertyType {

  STRING,
  INTEGER,
  ENTITY,
  BLOB;

  public static void persist(Connection con) throws SQLException {
    final Set<EntityPropertyType> dbValues = new HashSet<>();
    final List<EntityPropertyType> values = List.of(EntityPropertyType.values());

    try (
      PreparedStatement stmt = con.prepareStatement("SELECT name FROM entity_property_type");
      ResultSet result = stmt.executeQuery();
    ) {
      while (result.next()) {
        dbValues.add(valueOf(result.getString(1)));
      }
    }
    if (dbValues.size() < values.size()) {
      try (
        PreparedStatement stmt = con.prepareStatement("INSERT INTO entity_property_type (name) VALUES (?)");
      ) {
        for (EntityPropertyType type : values) {
          if (!dbValues.contains(type)) {
            stmt.setString(1, type.name());
            stmt.addBatch();
          }
        }
        stmt.executeBatch();
      }
    }
  }

}
