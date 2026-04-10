package com.bensler.taggy.persist;

import static java.lang.Boolean.TRUE;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

public class DbConnector {

  protected final String dbUrl_;
  protected final String migrationPath_;
  protected final Connection connection_;

  protected DbConnector(String dbUrl, String migrationPath) throws SQLException {
    final Properties props = new Properties();

    props.put("foreign_keys", TRUE.toString());
    connection_ = DriverManager.getConnection(dbUrl_ = dbUrl, props);
    // first start on an non existing data dir
    final File dbFile = new File(new LinkedList<>(Arrays.asList(dbUrl_.split(":"))).getLast());
    if (!dbFile.exists()) {
      dbFile.getParentFile().mkdirs();
    }
    migrationPath_ = migrationPath;
  }

  public synchronized Connection getConnection() {
    return connection_;
  }

  public MigrateResult performFlywayMigration() {
    return Flyway.configure().locations(migrationPath_).dataSource(dbUrl_, "", "").load().migrate();
  }

}
