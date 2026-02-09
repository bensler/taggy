package db.migration.sqlite;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.bensler.taggy.App;
import com.bensler.taggy.ui.BlobController;

public class V014__ReCreateThumbnails extends BaseJavaMigration {

  private final File dataDir_;
  private final BlobController blobCtrl_;

  public V014__ReCreateThumbnails() throws NoSuchAlgorithmException {
    dataDir_ = App.getDataDir();
    blobCtrl_ = new BlobController(dataDir_, new int[] {1, 1});
  }

  @Override
  public void migrate(Context context) throws Exception {
    final Connection connection = context.getConnection();

    dropExistingThumbs(connection);
    new V008__CreateThumbnails().migrate(context);
  }

  private void dropExistingThumbs(Connection connection) throws SQLException {
    try (
      Statement statement = connection.createStatement();
      ResultSet result = statement.executeQuery("SELECT DISTINCT b.thumbnail_sha FROM blob b")
    ) {
      while (result.next()) {
        blobCtrl_.deleteFile(result.getString(1));
      }
    }
    try (
      Statement statement = connection.createStatement();
    ) {
      final int resultCount = statement.executeUpdate("UPDATE blob SET thumbnail_sha = 'deleted'");

      System.out.println("%s files deleted".formatted(resultCount));
    }
  }

}
