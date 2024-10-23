package db.migration.sqlite;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V003__MigrationSha256Sum extends BaseJavaMigration {

  public V003__MigrationSha256Sum() { }

  @Override
  public void migrate(Context context) throws Exception {
    final Connection connection = context.getConnection();
    final int workerCount = 4;
    final Semaphore semaphore = new Semaphore(workerCount);
    final long startMillis = System.currentTimeMillis();

    try (
      PreparedStatement updateStatement = connection.prepareStatement("UPDATE blob SET sha256sum = ? WHERE id = ?");
      Statement statement = connection.createStatement();
      ResultSet result = statement.executeQuery("SELECT b.id, b.filename FROM blob b WHERE b.sha256sum IS NULL")
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

    synchronized void workDone(int id, String sha256Sum) throws SQLException {
      updateStatement.setInt(2, id);
      updateStatement.setString(1, sha256Sum);
      updateStatement.addBatch();
    }

  }

  static class Worker implements Runnable {

    private final Source source;
    private final Semaphore semaphore;
    private final byte[] buffer;
    private final MessageDigest digest;

    Worker(int workerId, Source pSource, Semaphore pSemaphore) {
      source = pSource;
      buffer = new byte[1_000_000];
      try {
        digest = MessageDigest.getInstance("SHA-256");
        (semaphore = pSemaphore).acquire();
        new Thread(this, "worker-" + workerId).start();
      } catch (NoSuchAlgorithmException | InterruptedException e) {
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
          e.printStackTrace();
        }
      }
      semaphore.release();
    }

    private void doWork(WorkPackage workPackage) throws SQLException, IOException {
      try (FileInputStream fis = new FileInputStream(workPackage.filename)) {
        int bytesRead;

        while ((bytesRead = fis.read(buffer)) > -1) {
          digest.update(buffer, 0, bytesRead);
        }
      }
      final byte[] shaSum = digest.digest();
      final BigInteger shaSumBigInteger = new BigInteger(1, shaSum);
      final String shaSumHex = String.format("%0" + (shaSum.length << 1) + "x", shaSumBigInteger).toLowerCase();

      System.out.println(Thread.currentThread().getName() + " # id:" + workPackage.id + " # file:" + workPackage.filename + " # sha256sum:" + shaSumHex);
      source.workDone(workPackage.id, shaSumHex);
    }

  }

  static class WorkPackage {

    final int id;
    final String filename;

    WorkPackage(int pId, String pFilename) {
      id = pId;
      filename = pFilename;
    }

  }

}
