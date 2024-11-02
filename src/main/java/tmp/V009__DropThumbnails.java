package tmp;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.bensler.taggy.Main;
import com.bensler.taggy.ui.BlobController;

public class V009__DropThumbnails extends BaseJavaMigration {


  @Override
  public void migrate(Context context) throws Exception {
    final File dataDir = Main.getDataDir();
    final BlobController blobCtrl = new BlobController(dataDir, new int[] {1, 1});
    final Connection connection = context.getConnection();

    try (
      Statement srcStatement = connection.createStatement();
      PreparedStatement updateStmt = connection.prepareStatement("UPDATE blob SET thumbnail_sha = NULL WHERE id = ?");
      ResultSet srcResult = srcStatement.executeQuery("SELECT b.id, b.thumbnail_sha FROM blob b WHERE b.thumbnail_sha IS NOT NULL")
    ) {
      while (srcResult.next()) {
        blobCtrl.getFile(srcResult.getString(2)).delete();
        updateStmt.setInt(1, srcResult.getInt(1));
        updateStmt.addBatch();
      }
      updateStmt.executeBatch();
    };
    System.out.println();
  }

}
