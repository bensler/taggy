package com.bensler.taggy.persist;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AbstractDbMapper<E extends Entity<E>> implements DbMapper<E> {

  protected final DbAccess db_;

  protected AbstractDbMapper(DbAccess db) {
    db_ = db;
  }

  protected PreparedStatement prepareStmt(String sql, Collection<Integer> ids, String whereClause) throws SQLException {
    final List<Integer> idList = List.copyOf(ids);
    final PreparedStatement stmt = db_.session_.prepareStatement(sql + (idList.isEmpty() ? "" : " WHERE " + whereClause.formatted(
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

}
