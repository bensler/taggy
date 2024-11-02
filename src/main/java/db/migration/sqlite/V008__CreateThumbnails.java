package db.migration.sqlite;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

import org.apache.commons.imaging.ImageReadException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.bensler.taggy.Main;
import com.bensler.taggy.Thumbnailer;
import com.bensler.taggy.ui.BlobController;

public class V008__CreateThumbnails extends BaseJavaMigration {

  private final File dataDir_;
  private final BlobController blobCtrl_;
  private final Thumbnailer thumbnailer_;

  public V008__CreateThumbnails() throws NoSuchAlgorithmException {
    dataDir_ = Main.getDataDir();
    blobCtrl_ = new BlobController(dataDir_, new int[] {1, 1});
    thumbnailer_ = new Thumbnailer(dataDir_);
  }

  @Override
  public void migrate(Context context) throws SQLException, InterruptedException {
    final Connection connection = context.getConnection();
    final int workerCount = 4;
    final Semaphore semaphore = new Semaphore(workerCount);
    final long startMillis = System.currentTimeMillis();

    try (
      PreparedStatement updateStatement = connection.prepareStatement("UPDATE blob SET thumbnail_sha = ? WHERE id = ?");
      Statement statement = connection.createStatement();
      ResultSet result = statement.executeQuery("SELECT b.id, b.sha256sum FROM blob b ORDER BY b.id ASC")
    ) {
      final Source source = new Source(result, updateStatement);
      IntStream.range(0, workerCount).forEach(anInt -> new Worker(anInt, source, semaphore));
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

    synchronized WorkPackage requestWork() {
      try {
        if (result.next()) {
          return new WorkPackage(result.getInt(1), result.getString(2));
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

    Worker(int workerId, Source pSource, Semaphore pSemaphore) {
      source = pSource;
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
      WorkPackage workPackage;

      while ((workPackage = source.requestWork()) != null) {
        try {
          doWork(workPackage);
        } catch (Exception e) {
          System.out.println("%s # %s failure".formatted(workerName, workPackage.id));
          e.printStackTrace();
        }
      }
      semaphore.release();
    }

    private void doWork(WorkPackage workPackage) throws SQLException, IOException, ImageReadException {
      final File thunbnail;

      thunbnail = thumbnailer_.scaleRotateImage(blobCtrl_.getFile(workPackage.shaHash));
      source.workDone(workPackage.id, blobCtrl_.storeBlob(thunbnail, false));
      System.out.println("%s processed %d".formatted(workerName, workPackage.id));
    }

  }

  static class WorkPackage {

    final int id;
    final String shaHash;

    WorkPackage(int pId, String pShaHash) {
      id = pId;
      shaHash = pShaHash;
    }

  }

}
