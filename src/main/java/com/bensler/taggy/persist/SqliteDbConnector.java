package com.bensler.taggy.persist;

import java.io.File;
import java.sql.SQLException;

public class SqliteDbConnector extends DbConnector {

  public SqliteDbConnector(File dataDir, String dbFile) throws SQLException {
    super(
      "jdbc:sqlite:" + new File(dataDir, dbFile).toPath(),
      "db/migration/sqlite"
    );
  }

}
