package com.bensler.taggy.persist;

import java.sql.Connection;
import java.sql.SQLException;

public interface PersistentWrite {

  void runInTxn(Connection con) throws SQLException;

}
