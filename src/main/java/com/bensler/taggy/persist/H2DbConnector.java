package com.bensler.taggy.persist;

import java.io.File;

public class H2DbConnector extends DbConnector {

  public H2DbConnector(File dataDir, String dbFile) {
    super(
      "jdbc:h2:file:" + new File(dataDir, dbFile).toPath(),
      org.h2.Driver.class,
      "db/migration/h2",
      new Class<?>[] {
        Tag.class,
        Blob.class
      },
      hbnConfig -> {
        hbnConfig.setProperty("hibernate.connection.DATABASE_TO_UPPER",  Boolean.FALSE.toString());
      }
    );
  }

}
