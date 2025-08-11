package com.bensler.taggy.persist;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public interface DbMapper<E extends Entity<E>> {

  List<E> loadAll();

  List<E> loadAll(Collection<Integer> ids);

  void remove(Integer id) throws SQLException;

  void update(E entity) throws SQLException;

  Integer insert(E entity) throws SQLException;

}
