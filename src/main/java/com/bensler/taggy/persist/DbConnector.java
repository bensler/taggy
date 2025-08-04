package com.bensler.taggy.persist;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
//import org.hibernate.Session;
//import org.hibernate.SessionFactory;
//import org.hibernate.cfg.Configuration;

public class DbConnector {

  protected final String dbUrl;
  protected final String migrationPath;
  protected final Connection session;

  protected DbConnector(String pDbUrl, String pMigrationPath) throws SQLException {
    session = DriverManager.getConnection(dbUrl = pDbUrl);
    // first start on an non existing data dir
    final File dbFile = new File(new LinkedList<>(Arrays.asList(dbUrl.split(":"))).getLast());
    if (!dbFile.exists()) {
      dbFile.getParentFile().mkdirs();
    }
    migrationPath = pMigrationPath;
  }

  public synchronized Connection getSession() {
    return session;
  }

  public MigrateResult performFlywayMigration() {
    return Flyway.configure().locations(migrationPath).dataSource(dbUrl, "", "").load().migrate();
  }

}
