package com.bensler.taggy.persist;

import java.io.File;
import java.sql.SQLException;

public class SqliteDbConnector extends DbConnector {

  private static File mkDataDir(File dataDir, String dbFile) {
    dataDir.mkdirs();
    return new File(dataDir, dbFile);
  }

  public SqliteDbConnector(File dataDir, String dbFile) throws SQLException {
    super(
      "jdbc:sqlite:" + mkDataDir(dataDir, dbFile).toPath(),
      "db/migration/sqlite"
    );
  }

}
