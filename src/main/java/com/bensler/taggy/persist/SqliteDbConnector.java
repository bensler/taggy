package com.bensler.taggy.persist;

import java.io.File;

import org.hibernate.community.dialect.SQLiteDialect;

public class SqliteDbConnector extends DbConnector {

  public SqliteDbConnector(File dataDir, String dbFile) {
    super(
      "jdbc:sqlite:" + new File(dataDir, dbFile).toPath(),
      org.sqlite.JDBC.class,
      "db/migration/sqlite",
      new Class<?>[] {
        Tag.class,
        Blob.class
      },
      hbnConfig -> hbnConfig.setProperty("hibernate.dialect", SQLiteDialect.class.getName())
    );
  }

}
