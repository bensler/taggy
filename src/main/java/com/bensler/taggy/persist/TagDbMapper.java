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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    final Map<Integer, Set<EntityReference<Blob>>> blobs = new HashMap<>();
    final List<Tag> tags = new ArrayList<>();

    try {
      try (
        PreparedStatement stmt = prepareStmt(con, "SELECT tp.tag_id, tp.name, tp.value FROM tag_property tp", ids, "tp.tag_id IN (%s)");
        ResultSet result = stmt.executeQuery()
      ) {
        while (result.next()) {
          properties.computeIfAbsent(result.getInt(1), tagId -> new HashMap<>()).put(TagProperty.valueOf(result.getString(2)), result.getString(3));
        }
      }
      try (
        PreparedStatement stmt = prepareStmt(con, "SELECT btx.tag_id, btx.blob_id FROM blob_tag_xref btx", ids, "btx.tag_id IN (%s)");
        ResultSet result = stmt.executeQuery()
      ) {
        while (result.next()) {
          blobs.computeIfAbsent(result.getInt(1), tagId -> new HashSet<>()).add(new EntityReference<>(Blob.class, result.getInt(2)));
        }
      }
      try (
        PreparedStatement stmt = prepareStmt(con, "SELECT t.id, t.name, t.parent_id FROM tag t", ids, "t.id IN (%s)");
        ResultSet result = stmt.executeQuery()
      ) {
        while (result.next()) {
          final Integer tagId = result.getInt(1);
          final Integer parentId = (Integer)result.getObject(3);

          tags.add(new Tag(
            tagId, ((parentId != null) ? new EntityReference<>(Tag.class, parentId) : null),
            result.getString(2),
            properties.computeIfAbsent(tagId, lTagId -> Map.of()),
            blobs.computeIfAbsent(tagId, lTagId -> Set.of())
          ));
        }
        return tags;
      }
    } catch(SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  private PreparedStatement prepareStmt(Connection con, String sql, Collection<Integer> ids, String whereClause) throws SQLException {
    final List<Integer> idList = List.copyOf(ids);
    final PreparedStatement stmt = con.prepareStatement(sql + (idList.isEmpty() ? "" : " WHERE " + whereClause.formatted(
      IntStream.range(0, idList.size()).mapToObj(id -> "?").collect(Collectors.joining(","))
    )));

    try {
      for (int i = 0; i < idList.size(); i++) {
        stmt.setInt(i + 1, idList.get(i));
      }
    } catch (SQLException sqle) {
      throw new RuntimeException(sqle);
    }
    return stmt;
  }

  @Override
  public void remove(Connection con, Integer id) throws SQLException {
    try (PreparedStatement stmt = con.prepareStatement("DELETE FROM tag WHERE id=?")) {
      stmt.setInt(1, id);
      stmt.execute();
    }
  }


  @Override
  public void update(Connection con, Tag tag) throws SQLException {
    final Integer tagId = tag.getId();

    try (PreparedStatement stmt = con.prepareStatement("UPDATE tag SET (name,parent_id)=(?,?) WHERE id=?")) {
      stmt.setString(1, tag.getName());
      stmt.setInt(2, Tag.getProperty(tag.getParent(), Tag::getId));
      stmt.setInt(3, tagId);
      stmt.execute();
    }
    try (PreparedStatement stmt = con.prepareStatement("DELETE FROM tag_property WHERE tag_id=?")) {
      stmt.setInt(1, tagId);
    }
    insertProperties(con, tag, tagId);
    try (PreparedStatement stmt = con.prepareStatement("DELETE FROM blob_tag_xref WHERE tag_id=?")) {
      stmt.setInt(1, tagId);
    }
    insertBlobs(con, tagId, tag.getBlobs());
  }

  private void insertProperties(Connection con, Tag tag, Integer tagId) throws SQLException {
    try (PreparedStatement stmt = con.prepareStatement("INSERT INTO tag_property (tag_id,name,value) VALUES (?,?,?)")) {
      for (TagProperty property : tag.getPropertyKeys()) {
        stmt.setInt(1, tagId);
        stmt.setString(2, property.name());
        stmt.setString(3, tag.getProperty(property));
        stmt.addBatch();
      }
      stmt.executeBatch();
    }
  }

  private void insertBlobs(Connection con, Integer tagId, Collection<Blob> blobs) throws SQLException {
    try (PreparedStatement stmt = con.prepareStatement("INSERT INTO blob_tag_xref (blob_id,tag_id) VALUES (?,?)")) {
      for (Blob blob : blobs) {
        stmt.setInt(1, blob.getId());
        stmt.setInt(2, tagId);
        stmt.addBatch();
      }
      stmt.executeBatch();
    }
  }

  @Override
  public Integer insert(Connection con, Tag tag) throws SQLException {
    final Integer newId;

    try (PreparedStatement stmt = con.prepareStatement("INSERT INTO tag (name,parent_id) VALUES (?,?)")) {
      final Integer parentId = Tag.getProperty(tag.getParent(), Tag::getId);

      stmt.setString(1, tag.getName());
      if (parentId != null) {
        stmt.setInt(2, Tag.getProperty(tag.getParent(), Tag::getId));
      }
      stmt.execute();
      try (ResultSet ids = stmt.getGeneratedKeys()) {
        ids.next();
        newId = ids.getInt(1);
      }
    }
    insertProperties(con, tag, newId);
    insertBlobs(con, newId, tag.getBlobs());
    return newId;
  }

}
