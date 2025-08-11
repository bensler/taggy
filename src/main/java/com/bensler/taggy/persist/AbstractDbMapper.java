package com.bensler.taggy.persist;

public abstract class AbstractDbMapper<E extends Entity<E>> implements DbMapper<E> {

  protected final DbAccess db_;

  protected AbstractDbMapper(DbAccess db) {
    db_ = db;
  }

}
