package db.migration.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V007__FillTypeColumnInBlob extends BaseJavaMigration {

  public V007__FillTypeColumnInBlob() { }

  @Override
  public void migrate(Context context) throws Exception {
    final Connection connection = context.getConnection();
    final long startMillis = System.currentTimeMillis();

    try (
      PreparedStatement updateStmt = connection.prepareStatement("UPDATE blob SET type = ? WHERE id = ?");
      Statement statement = connection.createStatement();
      ResultSet result = statement.executeQuery("SELECT b.id, b.filename FROM blob b")
    ) {
      while (result.next()) {
        final int id = result.getInt(1);
        final String fileName = result.getString(2);
        final String[] fileNameParts = fileName.split("\\.");
        final String extension = fileNameParts[fileNameParts.length - 1].toUpperCase();

        updateStmt.setString(1, extension);
        updateStmt.setInt(2, id);
        updateStmt.addBatch();
      }
      updateStmt.executeBatch();
    };
    System.out.println("Done " + (System.currentTimeMillis() - startMillis) + "ms");
    System.out.println();
  }

}
