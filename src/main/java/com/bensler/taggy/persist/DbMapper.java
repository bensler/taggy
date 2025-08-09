package com.bensler.taggy.persist;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public interface DbMapper<E extends Entity<E>> {

  List<E> loadAll(Connection con);

  List<E> loadAll(Connection con, Collection<Integer> ids);

  void remove(Connection con, Integer id) throws SQLException;

}
