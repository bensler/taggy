package com.bensler.taggy.persist;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Consumer;

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
    // first start on an non existing data dir
    final File dbFile = new File(new LinkedList<>(Arrays.asList(dbUrl.split(":"))).getLast());
    if (!dbFile.exists()) {
      dbFile.getParentFile().mkdirs();
    }
    migrationPath = pMigrationPath;
    final Configuration hbnConfig = new Configuration();

    Optional.ofNullable(configManipulator).ifPresent(pConfigManipulator -> pConfigManipulator.accept(hbnConfig));
    Arrays.stream(entityClasses)
      .map(clazz -> clazz.getName().replace('.', '/') + ".hbm.xml")
      .forEach(hbnConfig::addResource);
    hbnConfig
      .setProperty("hibernate.transform_hbm_xml.enabled",     Boolean.TRUE.toString())
      .setProperty("hibernate.connection.driver_class",       driverClass.getName())
      .setProperty("hibernate.connection.url",                dbUrl)
      .setProperty("hibernate.show_sql",                      "true")
      .setProperty("hibernate.connection.pool_size",          Integer.toString(1));
    sessionFactory = hbnConfig.buildSessionFactory();
  }

  public synchronized Session getSession() {
    if (session == null) {
      session = sessionFactory.openSession();
    }
    return session;
  }

  public MigrateResult performFlywayMigration() {
    return Flyway.configure().locations(migrationPath).dataSource(dbUrl, "", "").load().migrate();
  }

}
