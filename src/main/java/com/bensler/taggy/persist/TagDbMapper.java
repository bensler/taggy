package com.bensler.taggy.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TagDbMapper implements DbMapper<Tag> {

  public TagDbMapper() { }

  @Override
  public List<Tag> loadAll(Connection con) {
    return loadAllTags(con, Set.of());
  }

  @Override
  public List<Tag> loadAll(Connection con, Collection<Integer> ids) {
    return ids.isEmpty() ? List.of() : loadAllTags(con, ids);
  }

  private List<Tag> loadAllTags(Connection con, Collection<Integer> ids) {
    final Map<Integer, Map<TagProperty, String>> properties = new HashMap<>();
    final Map<Integer, Set<String>> blobs = new HashMap<>();
    final List<Tag> tags = new ArrayList<>();

    try {
      try (
        PreparedStatement stmt = prepareStmt(con, "SELECT tp.tag_id, tp.name, tp.value FROM tag_property tp", ids, "tp.tag_id IN (?)");
        ResultSet result = stmt.executeQuery()
      ) {
        while (result.next()) {
          properties.computeIfAbsent(result.getInt(1), tagId -> new HashMap<>()).put(TagProperty.valueOf(result.getString(2)), result.getString(3));
        }
      }
      try (
        PreparedStatement stmt = prepareStmt(con, "SELECT btx.tag_id, btx.blob_id FROM blob_tag_xref btx", ids, "btx.tag_id IN (?)");
        ResultSet result = stmt.executeQuery()
      ) {
        while (result.next()) {
          blobs.computeIfAbsent(result.getInt(1), tagId -> new HashSet<>()).add(result.getString(2));
        }
      }
      try (
        PreparedStatement stmt = prepareStmt(con, "SELECT t.id, t.name, t.parent_id FROM tag t", ids, "t.id IN (?)");
        ResultSet result = stmt.executeQuery()
      ) {
        while (result.next()) {
          final Integer tagId = result.getInt(1);

          tags.add(new Tag(
            tagId, new EntityReference<>(Tag.class, result.getInt(2)),
            result.getString(2),
            properties.computeIfAbsent(tagId, lTagId -> new HashMap<>()),
            Set.of()
          ));
        }
        return tags;
      }
    } catch(SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  private PreparedStatement prepareStmt(Connection con, String sql, Collection<Integer> ids, String whereClause) throws SQLException {


    return con.prepareStatement(sql);
  }

}
