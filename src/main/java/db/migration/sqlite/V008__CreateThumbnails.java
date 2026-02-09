package db.migration.sqlite;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

import org.apache.commons.imaging.ImageReadException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.App;
import com.bensler.taggy.imprt.Thumbnailer;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.ui.BlobController;

public class V008__CreateThumbnails extends BaseJavaMigration {

  private final File dataDir_;
  private final BlobController blobCtrl_;
  private final Thumbnailer thumbnailer_;

  public V008__CreateThumbnails() throws NoSuchAlgorithmException {
    dataDir_ = App.getDataDir();
    blobCtrl_ = new BlobController(dataDir_, new int[] {1, 1});
    thumbnailer_ = new Thumbnailer(dataDir_);
  }

  @Override
  public void migrate(Context context) throws SQLException, InterruptedException {
    final Connection connection = context.getConnection();
    final DbAccess db = new DbAccess(connection);
    final int workerCount = 4;
    final Semaphore semaphore = new Semaphore(workerCount);
    final long startMillis = System.currentTimeMillis();

    try (
      PreparedStatement updateStatement = connection.prepareStatement("UPDATE blob SET thumbnail_sha = ? WHERE id = ?");
      Statement statement = connection.createStatement();
      ResultSet result = statement.executeQuery(
        "SELECT b.id FROM blob b WHERE (b.thumbnail_sha IS NULL) OR (b.thumbnail_sha = 'deleted') ORDER BY b.id ASC"
      )
    ) {
      final Source source = new Source(result, updateStatement);

      IntStream.range(0, workerCount).forEach(anInt -> new Worker(anInt, db, source, semaphore));
      semaphore.acquire(workerCount);
      updateStatement.executeBatch();
    };
    System.out.println("Done " + (System.currentTimeMillis() - startMillis) + "ms");
    System.out.println();
  }

  static class Source {

    private final ResultSet result;

    private final PreparedStatement updateStatement;

    Source(ResultSet pResult, PreparedStatement pUpdateStatement) {
      result = pResult;
      updateStatement = pUpdateStatement;
    }

    synchronized Integer requestNextBlobId() {
      try {
        if (result.next()) {
          return result.getInt(1);
        }
      } catch (SQLException sqle) { /* no more rows available*/ }
      return null;
    }

    synchronized void workDone(int id, String thumbHash) throws SQLException {
      updateStatement.setInt(2, id);
      updateStatement.setString(1, thumbHash);
      updateStatement.addBatch();
    }

  }

  class Worker implements Runnable {

    private final Source source;
    private final Semaphore semaphore;
    private final String workerName;
    private final DbAccess db_;

    Worker(int workerId, DbAccess db, Source pSource, Semaphore pSemaphore) {
      source = pSource;
      db_ = db;
      try {
        (semaphore = pSemaphore).acquire();
        workerName = "worker-" + workerId;
        new Thread(this).start();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void run() {
      Integer blobId;

      while ((blobId = source.requestNextBlobId()) != null) {
        try {
          doWork(blobId);
        } catch (Exception e) {
          System.out.println("%s # %s failure".formatted(workerName, blobId));
          e.printStackTrace();
        }
      }
      semaphore.release();
    }

    private void doWork(int blobId) throws SQLException, IOException, ImageReadException {
      final Blob blob = db_.resolve(new EntityReference<>(Blob.class, blobId));
      final File file = blobCtrl_.getFile(blob.getSha256sum());
      final File thumbnail = blobCtrl_.createThumbnail(thumbnailer_, file, new HashMap<>());

      source.workDone(blobId, blobCtrl_.storeBlob(thumbnail, false));
      System.out.println("%s processed %d".formatted(workerName, blobId));
    }

  }

}
