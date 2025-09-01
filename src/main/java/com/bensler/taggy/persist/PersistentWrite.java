package com.bensler.taggy.persist;

import java.sql.SQLException;

public interface PersistentWrite {

  void runInTxn() throws SQLException;

}
