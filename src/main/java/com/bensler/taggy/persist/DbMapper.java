package com.bensler.taggy.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.bensler.decaf.util.entity.Entity;

public abstract class DbMapper<E extends Entity<E>> {

  protected final Class<E> entityClass_;
  protected final Connection con_;

  protected DbMapper(Class<E> entityClass, Connection con) {
    entityClass_= entityClass;
    con_ = con;
  }

  public Class<E> getEntityClass() {
    return entityClass_;
  }

  public abstract void remove(Integer id) throws SQLException;

  public abstract void update(E entity) throws SQLException;

  public abstract Integer insert(E entity) throws SQLException;

  public abstract List<E> loadAllEntities(List<Integer> ids);

  protected PreparedStatement prepareStmt(String sql, List<Integer> ids, String whereClause) throws SQLException {
    final PreparedStatement stmt = con_.prepareStatement(sql + (ids.isEmpty() ? "" : " WHERE " + whereClause.formatted(
      IntStream.range(0, ids.size()).mapToObj(id -> "?").collect(Collectors.joining(","))
    )));

    for (int i = 0; i < ids.size(); i++) {
      stmt.setInt(i + 1, ids.get(i));
    }
    return stmt;
  }


}
