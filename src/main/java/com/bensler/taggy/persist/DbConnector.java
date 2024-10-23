package com.bensler.taggy.persist;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class DbConnector {

  protected final String dbUrl;
  protected final String migrationPath;
  protected final SessionFactory sessionFactory;

  protected       Session session;

  protected DbConnector(
    String pDbUrl, Class<?> driverClass, String pMigrationPath,
    Class<?>[] entityClasses, Consumer<Configuration> configManipulator
  ) {
    dbUrl = pDbUrl;
    migrationPath = pMigrationPath;
    final Configuration hbnConfig = new Configuration();

    Optional.ofNullable(configManipulator).ifPresent(pConfigManipulator -> pConfigManipulator.accept(hbnConfig));
    Arrays.stream(entityClasses)
      .map(clazz -> clazz.getName().replace('.', '/') + ".hbm.xml")
      .forEach((String resourceName) -> hbnConfig.addResource(resourceName));
    hbnConfig
      .setProperty("hibernate.transform_hbm_xml.enabled",     Boolean.TRUE.toString())
      .setProperty("hibernate.connection.driver_class",       driverClass.getName())
      .setProperty("hibernate.connection.url",                dbUrl)
      .setProperty("hibernate.show_sql",                      "true")
      .setProperty("hibernate.connection.pool_size",          Integer.valueOf(1).toString());
    sessionFactory = hbnConfig.buildSessionFactory();
  }

  public synchronized Session getSession() {
    if (session == null) {
      session = sessionFactory.openSession();
    }
    return session;
  }

  public MigrateResult performFlywayMigration() {
    new DataSource() {

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void setLoginTimeout(int seconds) throws SQLException {
        // TODO Auto-generated method stub

      }

      @Override
      public void setLogWriter(PrintWriter out) throws SQLException {
        // TODO Auto-generated method stub

      }

      @Override
      public int getLoginTimeout() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
      }

      @Override
      public PrintWriter getLogWriter() throws SQLException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Connection getConnection(String username, String password)
          throws SQLException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Connection getConnection() throws SQLException {
        // TODO Auto-generated method stub
        return null;
      }
    };
    return Flyway.configure().locations(migrationPath)
        .dataSource(dbUrl, "", "").load().migrate();
  }

}
