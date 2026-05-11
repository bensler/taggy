package com.bensler.taggy.persist;

import java.sql.SQLException;
import java.util.List;

import com.bensler.decaf.util.entity.Entity;

public interface DbMapper<E extends Entity<E>> {

  Class<E> getEntityClass();

  void remove(Integer id) throws SQLException;

  void update(E entity) throws SQLException;

  Integer insert(E entity) throws SQLException;

  List<E> loadAllEntities(List<Integer> ids);

}
