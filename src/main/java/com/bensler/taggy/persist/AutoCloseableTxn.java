package com.bensler.taggy.persist;

import java.sql.Connection;
import java.sql.SQLException;

import com.bensler.decaf.util.io.AutoCloseableAdapter;

public class AutoCloseableTxn extends AutoCloseableAdapter<Connection> {

  public AutoCloseableTxn(Connection con) {
    super(con, aCon -> {
      try {
        aCon.commit();
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    });
  }

}