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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

import org.apache.commons.imaging.ImageReadException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.bensler.taggy.App;
import com.bensler.taggy.ui.BlobController;

public class V012__ExtractMetadataToBlobPropertyTable extends BaseJavaMigration {

  private final File dataDir_;
  private final BlobController blobCtrl_;

  public V012__ExtractMetadataToBlobPropertyTable() throws NoSuchAlgorithmException {
    dataDir_ = App.getDataDir();
    blobCtrl_ = new BlobController(dataDir_, new int[] {1, 1});
  }

  @Override
  public void migrate(Context context) throws SQLException, InterruptedException {
    final Connection connection = context.getConnection();
    final int workerCount = 4;
    final Semaphore semaphore = new Semaphore(workerCount);
    final long startMillis = System.currentTimeMillis();

    try (
      Statement statement = connection.createStatement();
      ResultSet result = statement.executeQuery("SELECT b.id, b.sha256sum FROM blob b ORDER BY b.id ASC")
    ) {
      final Source source = new Source(result, connection);

      IntStream.range(0, workerCount).forEach(anInt -> new Worker(anInt, source, semaphore));
      semaphore.acquire(workerCount);
    };
    System.out.println("Done " + (System.currentTimeMillis() - startMillis) + "ms");
    System.out.println();
  }

  static class Source {

    private final ResultSet result_;
    private final Connection connection_;

    Source(ResultSet pResult, Connection connection)  {
      result_ = pResult;
      connection_ = connection;
    }

    synchronized WorkPackage requestWork() {
      try {
        if (result_.next()) {
          return new WorkPackage(result_.getInt(1), result_.getString(2), connection_);
        }
      } catch (SQLException sqle) { /* no more rows available*/ }
      return null;
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
      final Map<String, String> metaData = new HashMap<>();

      blobCtrl_.readImageMetadata(blobCtrl_.getFile(workPackage.shaHash), metaData);
      workPackage.workDone(workPackage.id, metaData);
      System.out.println("%s processed %d".formatted(workerName, workPackage.id));
    }

  }

  static class WorkPackage {

    final int id;
    final String shaHash;
    private final PreparedStatement insertStatement_;
    private final PreparedStatement deleteStatement_;

    WorkPackage(int pId, String pShaHash, Connection connection) throws SQLException {
      id = pId;
      shaHash = pShaHash;
      insertStatement_ = connection.prepareStatement("INSERT INTO blob_property (blob_id, name, value) VALUES (?, ?, ?)");
      deleteStatement_ = connection.prepareStatement("DELETE FROM blob_property WHERE blob_id = ?");
    }

    void workDone(int id, Map<String, String> metaData) throws SQLException {
      try (deleteStatement_; insertStatement_) {
        deleteStatement_.setInt(1, id);
        deleteStatement_.execute();
        for (Entry<String, String> entry : metaData.entrySet()) {
          insertStatement_.setInt(1, id);
          insertStatement_.setString(2, entry.getKey());
          insertStatement_.setString(3, entry.getValue());
          insertStatement_.addBatch();
        };
        insertStatement_.executeBatch();
      }
    }

  }

}
