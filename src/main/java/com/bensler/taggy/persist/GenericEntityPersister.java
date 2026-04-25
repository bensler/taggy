package com.bensler.taggy.persist;

import java.sql.Connection;
import java.sql.SQLException;

public class GenericEntityPersister {

  public GenericEntityPersister() {
    // TODO
  }

  public void persist(Connection con, PersistedEntity entity) throws SQLException {
    entity.persist(new PersistencyBaseLayer(con));
  }

}
