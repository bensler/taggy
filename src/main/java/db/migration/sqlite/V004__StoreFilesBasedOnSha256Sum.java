package db.migration.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.bensler.taggy.App;
import com.bensler.taggy.ui.BlobController;

public class V004__StoreFilesBasedOnSha256Sum extends BaseJavaMigration {

  public V004__StoreFilesBasedOnSha256Sum() { }

  @Override
  public void migrate(Context context) throws Exception {
    final Connection connection = context.getConnection();
    final long startMillis = System.currentTimeMillis();
    final BlobController blobCtrl = new BlobController(App.getDataDir(), new int[] {1, 1});

    try (
      Statement statement = connection.createStatement();
      ResultSet result = statement.executeQuery("SELECT b.filename FROM blob b")
    ) {
      while (result.next()) {
        blobCtrl.storeBlob(new File(result.getString(1)), true);
        System.out.print(".");
      }
    };
    System.out.println("Done " + (System.currentTimeMillis() - startMillis) + "ms");
    System.out.println();
  }

}
