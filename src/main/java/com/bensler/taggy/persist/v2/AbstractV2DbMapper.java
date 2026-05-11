package com.bensler.taggy.persist.v2;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.bensler.decaf.util.entity.Entity;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.DbMapper;

public abstract class AbstractV2DbMapper<E extends Entity<E>> implements DbMapper<E> {

  protected final Class<E> entityClass_;
  protected final DbAccess db_;

  protected AbstractV2DbMapper(Class<E> entityClass, DbAccess db) {
    entityClass_= entityClass;
    db_ = db;
  }

  @Override
  public final Class<E> getEntityClass() {
    return entityClass_;
  }

  protected void removeEntity(String tableName, String idColName, Integer id) throws SQLException {
    try (PreparedStatement stmt = db_.prepareStatement("DELETE FROM %s WHERE %s=?".formatted(tableName, idColName))) {
      stmt.setInt(1, id);
      stmt.execute();
    }
  }

}
