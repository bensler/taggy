package db.migration.sqlite;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.bensler.taggy.Main;
import com.bensler.taggy.ui.BlobController;

public class V008__CreateThumbnails extends BaseJavaMigration {

  private BlobController blobCtrl;
  private File dataDir;

  public V008__CreateThumbnails() { }

  @Override
  public void migrate(Context context) throws Exception {
    final Connection connection = context.getConnection();
    final int workerCount = 4;
    final Semaphore semaphore = new Semaphore(workerCount);
    final long startMillis = System.currentTimeMillis();

    dataDir = Main.getDataDir();
    blobCtrl = new BlobController(dataDir, new int[] {1, 1});
    try (
      PreparedStatement updateStatement = connection.prepareStatement("UPDATE blob SET thumbnail_sha = ? WHERE id = ?");
      Statement statement = connection.createStatement();
      ResultSet result = statement.executeQuery("SELECT b.id, b.sha256sum FROM blob b")
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

  class Worker implements Runnable {

    private final Source source;
    private final Semaphore semaphore;

    Worker(int workerId, Source pSource, Semaphore pSemaphore) {
      source = pSource;
      try {
        (semaphore = pSemaphore).acquire();
        new Thread(this, "worker-" + workerId).start();
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
          System.out.println(Thread.currentThread().getName() + " # id:" + workPackage.id + " failure");
          e.printStackTrace();
        }
      }
      semaphore.release();
    }

    private void doWork(WorkPackage workPackage) throws SQLException, IOException {
      try (FileInputStream fis = new FileInputStream(blobCtrl.getFile(workPackage.shaHash))) {
        source.workDone(workPackage.id, blobCtrl.storeBlob(scaleImage(workPackage, fis), false));
      }
    }

    private File scaleImage(WorkPackage workPackage, FileInputStream fis) throws IOException {
      final BufferedImage srcImg = ImageIO.read(fis);
      int width = srcImg.getWidth();
      int height = srcImg.getHeight();

      if (width > height) {
        width = BlobController.THUMBNAIL_SIZE;
        height = -1;
      } else {
        width = -1;
        height = BlobController.THUMBNAIL_SIZE;
      }

      final String threadName = Thread.currentThread().getName();
      final File outputFile = new File(dataDir, threadName + "-" + workPackage.id);
      final Image scaledImg = srcImg.getScaledInstance(width, height, Image.SCALE_SMOOTH);
      final BufferedImage bufferedImg = new BufferedImage(scaledImg.getWidth(null), scaledImg.getHeight(null), BufferedImage.TYPE_INT_RGB);

      bufferedImg.getGraphics().drawImage(scaledImg, 0, 0 , null);
      ImageIO.write(bufferedImg, "jpg", outputFile);
      System.out.println(threadName + " processed " + workPackage.id);
      return outputFile;
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
