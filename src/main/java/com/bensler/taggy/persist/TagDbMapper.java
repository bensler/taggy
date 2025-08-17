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

public class TagDbMapper extends AbstractDbMapper<Tag> {

  public static class TagHeadData {

    final Tag subject_;
    final Tag parent_;
    final String name_;

    public TagHeadData(Tag subject, Tag parent, String name) {
      subject_ = subject;
      parent_ = parent;
      name_ = name;
    }
  }

  TagDbMapper(DbAccess db) {
    super(db);
  }

  @Override
  public List<Tag> loadAll() {
    return loadAllTags(Set.of());
  }

  @Override
  public List<Tag> loadAll(Collection<Integer> ids) {
    return ids.isEmpty() ? List.of() : loadAllTags(ids);
  }

  private List<Tag> loadAllTags(Collection<Integer> ids) {
    final Map<Integer, Map<TagProperty, String>> properties = new HashMap<>();
    final Map<Integer, Set<EntityReference<Blob>>> blobs = new HashMap<>();
    final List<Tag> tags = new ArrayList<>();

    try {
      try (
        PreparedStatement stmt = prepareStmt("SELECT tp.tag_id, tp.name, tp.value FROM tag_property tp", ids, "tp.tag_id IN (%s)");
        ResultSet result = stmt.executeQuery()
      ) {
        while (result.next()) {
          properties.computeIfAbsent(result.getInt(1), tagId -> new HashMap<>()).put(TagProperty.valueOf(result.getString(2)), result.getString(3));
        }
      }
      try (
        PreparedStatement stmt = prepareStmt("SELECT btx.tag_id, btx.blob_id FROM blob_tag_xref btx", ids, "btx.tag_id IN (%s)");
        ResultSet result = stmt.executeQuery()
      ) {
        while (result.next()) {
          blobs.computeIfAbsent(result.getInt(1), tagId -> new HashSet<>()).add(new EntityReference<>(Blob.class, result.getInt(2)));
        }
      }
      try (
        PreparedStatement stmt = prepareStmt("SELECT t.id, t.name, t.parent_id FROM tag t", ids, "t.id IN (%s)");
        ResultSet result = stmt.executeQuery()
      ) {
        while (result.next()) {
          final Integer tagId = result.getInt(1);
          final Integer parentId = (Integer)result.getObject(3);
          final Tag tag = new Tag(
            tagId, ((parentId != null) ? new EntityReference<>(Tag.class, parentId) : null),
            result.getString(2),
            properties.computeIfAbsent(tagId, lTagId -> Map.of()),
            blobs.computeIfAbsent(tagId, lTagId -> Set.of())
          );

          db_.addToCache(tag);
          tags.add(tag);
        }
        return tags;
      }
    } catch(SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  @Override
  public void remove(Integer id) throws SQLException {
    try (PreparedStatement stmt = db_.session_.prepareStatement("DELETE FROM tag WHERE id=?")) {
      stmt.setInt(1, id);
      stmt.execute();
    }
  }

  private void setParentId(Tag parentTag, PreparedStatement stmt, int index) throws SQLException {
    final Integer parentId = Tag.getProperty(parentTag, Tag::getId);

    if (parentId != null) {
      stmt.setInt(index, parentId);
    }
  }

  public Tag updateHeadData(TagHeadData tagHeadData) throws SQLException {
    updateHeadData(tagHeadData.subject_.getId(), tagHeadData.parent_, tagHeadData.name_);
    return db_.refresh(tagHeadData.subject_);
  }

  private void updateHeadData(Integer tagId, Tag parent, String name) throws SQLException {
    try (PreparedStatement stmt = db_.session_.prepareStatement("UPDATE tag SET (name,parent_id)=(?,?) WHERE id=?")) {
      stmt.setString(1, name);
      setParentId(parent, stmt, 2);
      stmt.setInt(3, tagId);
      stmt.execute();
    }
  }

  @Override
  public void update(Tag tag) throws SQLException {
    final Connection con = db_.session_;
    final Integer tagId = tag.getId();

    updateHeadData(tagId, tag.getParent(), tag.getName());
    try (PreparedStatement stmt = con.prepareStatement("DELETE FROM tag_property WHERE tag_id=?")) {
      stmt.setInt(1, tagId);
      stmt.execute();
    }
    insertProperties(tag, tagId);
    try (PreparedStatement stmt = con.prepareStatement("DELETE FROM blob_tag_xref WHERE tag_id=?")) {
      stmt.setInt(1, tagId);
      stmt.execute();
    }
    insertBlobs(tagId, tag.getBlobs());
  }

  private void insertProperties(Tag tag, Integer tagId) throws SQLException {
    final Set<TagProperty> propertyKeys = tag.getPropertyKeys();

    if (!propertyKeys.isEmpty()) {
      try (PreparedStatement stmt = db_.session_.prepareStatement("INSERT INTO tag_property (tag_id,name,value) VALUES (?,?,?)")) {
        for (TagProperty property : propertyKeys) {
          stmt.setInt(1, tagId);
          stmt.setString(2, property.name());
          stmt.setString(3, tag.getProperty(property));
          stmt.addBatch();
        }
        stmt.executeBatch();
      }
    }
  }

  private void insertBlobs(Integer tagId, Collection<Blob> blobs) throws SQLException {
    if (!blobs.isEmpty()) {
      try (PreparedStatement stmt = db_.session_.prepareStatement("INSERT INTO blob_tag_xref (blob_id,tag_id) VALUES (?,?)")) {
        for (Blob blob : blobs) {
          stmt.setInt(1, blob.getId());
          stmt.setInt(2, tagId);
          stmt.addBatch();
        }
        stmt.executeBatch();
      }
    }
  }

  @Override
  public Integer insert(Tag tag) throws SQLException {
    final Integer newId;

    try (PreparedStatement stmt = db_.session_.prepareStatement("INSERT INTO tag (name,parent_id) VALUES (?,?)")) {
      stmt.setString(1, tag.getName());
      setParentId(tag.getParent(), stmt, 2);
      stmt.execute();
      try (ResultSet ids = stmt.getGeneratedKeys()) {
        ids.next();
        newId = ids.getInt(1);
      }
    }
    insertProperties(tag, newId);
    insertBlobs(newId, tag.getBlobs());
    return newId;
  }

}
