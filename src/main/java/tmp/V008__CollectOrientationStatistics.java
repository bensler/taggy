package tmp;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.bensler.taggy.App;
import com.bensler.taggy.ui.BlobController;

public class V008__CollectOrientationStatistics extends BaseJavaMigration {

  private final Map<String, Integer> stats;

  public V008__CollectOrientationStatistics() {
    stats = new TreeMap<>();
  }

  @Override
  public void migrate(Context context) throws Exception {
    final File dataDir = App.getDataDir();
    final BlobController blobCtrl = new BlobController(dataDir, new int[] {1, 1});
    final Connection connection = context.getConnection();

    try (
      Statement statement = connection.createStatement();
      ResultSet result = statement.executeQuery("SELECT b.sha256sum FROM blob b")
    ) {
      while (result.next()) {
        analyze(blobCtrl.getFile(result.getString(1)));
      }
    };
    System.out.println();
  }

  /**
   * no-meta=216,
   * no-meta-jpeg=2,
   * orientation:null=127
   * orientation:1=1776 (ORIENTATION_VALUE_HORIZONTAL_NORMAL) blobs-1-1/3/6/3607751fd6f152a4c882a83f08dae180676c75b6f46db30aa1fda905e6fa3228
   * orientation:6=453  (ORIENTATION_VALUE_ROTATE_90_CW)      blobs-1-1/0/d/0d6089f3ce9b99296ad41caf5a55347cfcd6bbdf465bace289abc63f94025c3a
   * orientation:8=270  (ORIENTATION_VALUE_ROTATE_270_CW)     blobs-1-1/8/3/837afb74f4ea7efd718d2b59c6ddb51a56cb26ca242b1130d0355c032e5dc0b0
   */
  private void analyze(File file) throws Exception {
    try {
      final ImageMetadata metaData = Imaging.getMetadata(file);

      if (metaData == null) {
        addStat("no-meta");
      } else {
        if (metaData instanceof JpegImageMetadata jpgMeta) {
          final TiffField exifValue = jpgMeta.findEXIFValue(TiffTagConstants.TIFF_TAG_ORIENTATION);

          if (exifValue != null) {
            addStat("orientation:" + exifValue.getValueDescription());
          } else {
            addStat("orientation:null");
          }
        } else {
          addStat("no-meta-jpeg");
        }
      }
    } catch (Exception e) {
      System.out.println("exc: " + file.getAbsolutePath());
      e.printStackTrace();
      throw e;
    }
  }

  private void addStat(String key) {
    final Integer count = stats.computeIfAbsent(key, aKey -> 0);

    stats.put(key, count + 1);
  }

}
