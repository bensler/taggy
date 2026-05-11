package com.bensler.taggy.persist.v1;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.bensler.decaf.util.entity.Entity;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.DbMapper;

public abstract class AbstractV1DbMapper<E extends Entity<E>> implements DbMapper<E> {

  protected final Class<E> entityClass_;
  protected final DbAccess db_;

  protected AbstractV1DbMapper(Class<E> entityClass, DbAccess db) {
    entityClass_= entityClass;
    db_ = db;
  }

  @Override
  public final Class<E> getEntityClass() {
    return entityClass_;
  }

  protected PreparedStatement prepareStmt(String sql, List<Integer> ids, String whereClause) throws SQLException {
    final PreparedStatement stmt = db_.prepareStatement(sql + (ids.isEmpty() ? "" : " WHERE " + whereClause.formatted(
      IntStream.range(0, ids.size()).mapToObj(id -> "?").collect(Collectors.joining(","))
    )));

    for (int i = 0; i < ids.size(); i++) {
      stmt.setInt(i + 1, ids.get(i));
    }
    return stmt;
  }

}
