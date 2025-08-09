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

public class BlobDbMapper implements DbMapper<Blob> {

  public BlobDbMapper() { }

  @Override
  public List<Blob> loadAll(Connection con) {
    return loadAllBlobs(con, Set.of());
  }

  @Override
  public List<Blob> loadAll(Connection con, Collection<Integer> ids) {
    return ids.isEmpty() ? List.of() : loadAllBlobs(con, ids);
  }

  private List<Blob> loadAllBlobs(Connection con, Collection<Integer> ids) {
    final Map<Integer, Map<String, String>> properties = new HashMap<>();
    final Map<Integer, Set<EntityReference<Tag>>> tags = new HashMap<>();
    final List<Blob> blobs = new ArrayList<>();

    try {
      try (
        PreparedStatement stmt = prepareStmt(con, "SELECT bp.blob_id, bp.name, bp.value FROM blob_property bp", ids, "bp.blob_id IN (%s)");
        ResultSet result = stmt.executeQuery()
      ) {
        while (result.next()) {
          properties.computeIfAbsent(result.getInt(1), tagId -> new HashMap<>()).put(result.getString(2), result.getString(3));
        }
      }
      try (
        PreparedStatement stmt = prepareStmt(con, "SELECT btx.blob_id, btx.tag_id FROM blob_tag_xref btx", ids, "btx.blob_id IN (%s)");
        ResultSet result = stmt.executeQuery()
      ) {
        while (result.next()) {
          tags.computeIfAbsent(result.getInt(1), tagId -> new HashSet<>()).add(new EntityReference<>(Tag.class, result.getInt(2)));
        }
      }
      try (
        PreparedStatement stmt = prepareStmt(con, "SELECT b.id, b.sha256sum, b.thumbnail_sha, b.type FROM blob b", ids, "b.id IN (%s)");
        ResultSet result = stmt.executeQuery()
      ) {
        while (result.next()) {
          final Integer blobId = result.getInt(1);

          blobs.add(new Blob(
            blobId, result.getString(2), result.getString(3), result.getString(4),
            properties.computeIfAbsent(blobId, lBlobId -> Map.of()),
            tags.computeIfAbsent(blobId, lBlobId -> Set.of())
          ));
        }
        return blobs;
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
    try (PreparedStatement stmt = con.prepareStatement("DELETE FROM blob WHERE id=?")) {
      stmt.setInt(1, id);
      stmt.execute();
    }
  }

}
