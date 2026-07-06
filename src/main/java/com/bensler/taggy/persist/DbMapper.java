package com.bensler.taggy.persist;

import java.sql.SQLException;
import java.util.List;

import com.bensler.decaf.util.entity.Entity;

public interface DbMapper<E extends Entity<E>> {

  public enum Scope {
    PROPERTIES   (true,  false),
    RELATIONSHIPS(false, true),
    FULL         (true,  true);

    public final boolean properties_;
    public final boolean relationships_;

    Scope(boolean properties, boolean relationships) {
      properties_ = properties;
      relationships_ = relationships;
    }
  }

  Class<E> getEntityClass();

  void remove(Integer id) throws SQLException;

  void update(E entity, Scope scope) throws SQLException;

  Integer insert(E entity) throws SQLException;

  List<E> loadAllEntities(List<Integer> ids);

  default E loadOne(Integer id) {
    return loadAllEntities(List.of(id)).get(0);
  }

}
