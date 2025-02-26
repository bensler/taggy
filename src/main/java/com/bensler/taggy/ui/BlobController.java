package com.bensler.taggy.ui;

import static com.bensler.taggy.imprt.ImportController.TYPE_BIN_PREFIX;
import static com.bensler.taggy.imprt.ImportController.TYPE_IMG_PREFIX;
import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.ORIENTATION_VALUE_ROTATE_270_CW;
import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.ORIENTATION_VALUE_ROTATE_90_CW;

import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoAscii;

import com.bensler.taggy.App;
import com.bensler.taggy.imprt.Thumbnailer;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.DbAccess;

public class BlobController {

  public static final String SIZE_PREFIX = TYPE_IMG_PREFIX + "size.";
  public static final String PROPERTY_SIZE_WIDTH  = SIZE_PREFIX + "width";
  public static final String PROPERTY_SIZE_HEIGHT = SIZE_PREFIX + "height";
  public static final String PROPERTY_ORIENTATION = TYPE_IMG_PREFIX + "orientation";
  public static final String PROPERTY_ORIENTATION_VALUE_90_CW  = "rotate90cw";
  public static final String PROPERTY_ORIENTATION_VALUE_270_CW = "rotate270cw";
  public static final String DATE_PREFIX = TYPE_BIN_PREFIX + "date.";
  public static final String PROPERTY_DATE_YEAR  = DATE_PREFIX + "year";
  public static final String PROPERTY_DATE_MONTH = DATE_PREFIX + "month";
  public static final String PROPERTY_DATE_DAY   = DATE_PREFIX + "day";

  public enum Orientation {
    ROTATE_000_CW(AffineTransform.getQuadrantRotateInstance(0), null),
    ROTATE_090_CW(AffineTransform.getQuadrantRotateInstance(1), PROPERTY_ORIENTATION_VALUE_90_CW),
    ROTATE_270_CW(AffineTransform.getQuadrantRotateInstance(3), PROPERTY_ORIENTATION_VALUE_270_CW);

    public final AffineTransform transform_;
    public final Optional<String> metaDataValue_;

    Orientation(AffineTransform transform, String metaDataValue) {
      transform_ = transform;
      metaDataValue_ = Optional.ofNullable(metaDataValue);
    }

    public void putMetaData(Map<String, String> metaDataSink) {
      metaDataValue_.ifPresent(value -> metaDataSink.put(PROPERTY_ORIENTATION, value));
    }
  }

  public final static Map<Integer, Orientation> ROTATION_TRANSFORMATIONS = Map.of(
    ORIENTATION_VALUE_ROTATE_90_CW,  Orientation.ROTATE_090_CW,
    ORIENTATION_VALUE_ROTATE_270_CW, Orientation.ROTATE_270_CW
  );

  public static final String BLOB_FOLDER_BASE_NAME = "blobs";
  public static final String DIGEST_TYPE_SHA_256 = "SHA-256";

  private final List<Fragment> pathFragments_;
  private final File blobBasePath_;
  private final MessageDigest digest_;
  private final byte[] buffer_;

  public BlobController(File blobBasePath, int[] folderPattern) throws NoSuchAlgorithmException {
    buffer_ = new byte[1_000_000];
    digest_ = MessageDigest.getInstance(DIGEST_TYPE_SHA_256);
    final int digestStringLength = digest_.getDigestLength() * 2;

    IntStream.of(folderPattern).forEach(anInt -> {
      if (anInt < 1) {
        throw new IllegalArgumentException("All folderPattern elements must be larger than 1. \"%s\" violates that.".formatted(anInt));
      }
    });
    final int folderPatternSum = IntStream.of(folderPattern).sum();
    if (folderPatternSum > digestStringLength) {
      throw new IllegalArgumentException("Sum of folderPattern array is %s but must be <=%s.".formatted(folderPatternSum, digestStringLength));
    }
    if (!blobBasePath.exists()) {
      throw new IllegalArgumentException("Folder \"%s\" does not exist.".formatted(blobBasePath));
    }
    if (!blobBasePath.isDirectory()) {
      throw new IllegalArgumentException("\"%s\" must be a folder.".formatted(blobBasePath));
    }

    final List<Fragment> fragments = new ArrayList<>();
    int start = 0;
    for (int patternComponent : folderPattern) {
      final int end = start + patternComponent;

      fragments.add(new Fragment(start, end));
      start = end;
    }
    pathFragments_ = List.copyOf(fragments);
    final String patternSuffix = IntStream.of(folderPattern).mapToObj(String::valueOf).collect(Collectors.joining("-"));
    blobBasePath_ = new File(blobBasePath, String.join("-", BLOB_FOLDER_BASE_NAME, patternSuffix));
    blobBasePath.mkdirs();
  }

  /** @return the sourceFiles sha256sum */
  public String storeBlob(File sourceFile, boolean keepSource) throws IOException {
    final String sourceHash  = hashFile(sourceFile);

    storeBlob(sourceFile, sourceHash, keepSource);
    return sourceHash;
  }

  public void deleteBlob(Blob blob) {
    final App app = App.getApp();
    final DbAccess dbAccess = app.getDbAccess();

    Optional.ofNullable(blob.getSha256sum()).ifPresent(this::deleteFile);
    Optional.ofNullable(blob.getThumbnailSha()).ifPresent(this::deleteFile);
    dbAccess.remove(blob);
    blob.getTags().stream().forEach(dbAccess::refresh);
    app.entityRemoved(blob);
  }

  private void deleteFile(String shasum) {
    try {
      Files.deleteIfExists(getFile(shasum).toPath());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void storeBlob(File sourceFile, String sourceFileHash, boolean keepSource) throws IOException {
    final File targetFile = getFile(sourceFileHash);

    if (keepSource) {
      Files.copy(sourceFile.toPath(), targetFile.toPath());
    } else {
      Files.move(sourceFile.toPath(), targetFile.toPath());
    }
  }

  public String hashFile(File sourceFile) throws IOException {
    final byte[] shaSum;

    synchronized (digest_) {
      try (FileInputStream fileIs = new FileInputStream(sourceFile)) {
        int bytesRead;

        while ((bytesRead = fileIs.read(buffer_)) > -1) {
          digest_.update(buffer_, 0, bytesRead);
        }
      }
      shaSum = digest_.digest();
    }

    final BigInteger shaSumBigInteger = new BigInteger(1, shaSum);
    // shaSum hex String
    return String.format("%0" + (shaSum.length << 1) + "x", shaSumBigInteger).toLowerCase();
  }

  public File getFile(String hash) {
    final List<String> pathFragments = fragmentString(hash);

    File targetDir = blobBasePath_;
    for (String pathFragment : pathFragments) {
      targetDir = new File(targetDir, pathFragment);
    }
    targetDir.mkdirs();
    return new File(targetDir, hash);
  }

  private List<String> fragmentString(String hash) {
    return pathFragments_.stream().map(fragment -> fragment.apply(hash)).toList();
  }

  static class Fragment {

    final int start_, endExcl_;

    Fragment(int start, int endExcl) {
      start_ = start;
      endExcl_ = endExcl;
    }

    String apply(String source) {
      return source.substring(start_, endExcl_);
    }

  }

  public BufferedImage loadRotated(Blob blob) throws IOException {
    return rotate(
      ImageIO.read( getFile(blob.getSha256sum())), findOrientation(blob),
      (transform, srcImg) -> transform.createCompatibleDestImage(srcImg, null)
    );
  }

  private static final Map<String, Orientation> ORIENTATIONS_BY_STR = new HashMap<>(Map.of(
    PROPERTY_ORIENTATION_VALUE_90_CW, Orientation.ROTATE_090_CW,
    PROPERTY_ORIENTATION_VALUE_270_CW, Orientation.ROTATE_270_CW
  ));

  private Optional<Orientation> findOrientation(Blob blob) {
    return Optional.ofNullable(ORIENTATIONS_BY_STR.get(blob.getProperty(PROPERTY_ORIENTATION)));
  }

  private AffineTransform compensateForRotation(BufferedImage scaledImg, AffineTransform transRotate) {
    final AffineTransformOp rotateOp = new AffineTransformOp(transRotate, null);
    final Point2D cornerTopLeft     = rotateOp.getPoint2D(new Point(0, 0), null);
    final Point2D cornerBottomRight = rotateOp.getPoint2D(
      new Point(scaledImg.getWidth(), scaledImg.getHeight()), null
    );

    return AffineTransform.getTranslateInstance(
      -1 * Math.min(cornerTopLeft.getX(), cornerBottomRight.getX()),
      -1 * Math.min(cornerTopLeft.getY(), cornerBottomRight.getY())
    );
  }

  public Blob importFile(File file, String type) throws IOException, ImageReadException {
      final App app = App.getApp();
      final Map<String, String> metaData = new HashMap<>();
      final File thumbnail = importFile(file, metaData);
      final String fileSha = storeBlob(file, true);
      final String thumbSha = storeBlob(thumbnail, false);

      return app.storeEntity(new Blob(file.getName(), fileSha, thumbSha, type, metaData));
  }

  private Optional<JpegImageMetadata> getMetaData(File srcFile) throws ImageReadException, IOException {
    return (Imaging.getMetadata(srcFile) instanceof JpegImageMetadata jpgMeta)
      ? Optional.of(jpgMeta) : Optional.empty();
  }

  private File importFile(File srcFile, Map<String, String> metaDataSink) throws IOException, ImageReadException {
    final Thumbnailer thumbnailer = App.getApp().getThumbnailer();
    final Optional<JpegImageMetadata> srcMetaData = getMetaData(srcFile);
    final BufferedImage srcImg = ImageIO.read(new FileInputStream(srcFile));
    final Optional<Orientation> orientation = findOrientation(srcMetaData);

    metaDataSink.put(PROPERTY_SIZE_WIDTH,  String.valueOf(srcImg.getWidth()));
    metaDataSink.put(PROPERTY_SIZE_HEIGHT, String.valueOf(srcImg.getHeight()));
    srcMetaData.ifPresent(metaData -> findDate(metaData, metaDataSink));
    orientation.ifPresent(value -> value.putMetaData(metaDataSink));

    return thumbnailer.writeImgToFile(rotate(
      thumbnailer.scaleImage(srcImg), orientation,
      (transform, img) -> {
        final Rectangle2D rotatedBounds = transform.getBounds2D(img);
        // no alpha as jpg does not support it ------------------------------------------------------------------------vvv
        return new BufferedImage((int)rotatedBounds.getWidth(), (int)rotatedBounds.getHeight(), BufferedImage.TYPE_INT_RGB);
      }
    ));
  }

  private BufferedImage rotate(
    BufferedImage srcImg, Optional<Orientation> orientation,
    BiFunction<AffineTransformOp, BufferedImage, BufferedImage> targetImgSource
  ) {
    final AffineTransform transRotate = orientation.orElse(Orientation.ROTATE_000_CW).transform_;
    final AffineTransform transTranslate = compensateForRotation(srcImg, transRotate);
    final AffineTransformOp rotateTranslateOp;

    transTranslate.concatenate(transRotate);
    rotateTranslateOp = new AffineTransformOp(transTranslate, new RenderingHints(
      RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC
    ));

    return rotateTranslateOp.filter(srcImg, targetImgSource.apply(rotateTranslateOp, srcImg));
  }

  private Optional<Orientation> findOrientation(Optional<JpegImageMetadata> optMeta) {
    return optMeta.flatMap(jpgMeta -> Optional.ofNullable(jpgMeta.findEXIFValue(TiffTagConstants.TIFF_TAG_ORIENTATION)))
      .map(this::getTiffIntValue)
      .filter(ROTATION_TRANSFORMATIONS::containsKey)
      .map(ROTATION_TRANSFORMATIONS::get);
  }

  private int getTiffIntValue(TiffField tiffField) {
    try {
      return tiffField.getIntValue();
    } catch (ImageReadException ire) {
      throw new RuntimeException(ire);
    }
  }

  private String getTiffStringValue(TiffField tiffField) {
    try {
      return tiffField.getStringValue();
    } catch (ImageReadException ire) {
      throw new RuntimeException(ire);
    }
  }

  private void findDate(JpegImageMetadata jpgMeta, Map<String, String> metaDataSink) {
    DATE_TAGS.stream()
      .flatMap(tag -> Optional.ofNullable(jpgMeta.findEXIFValue(tag)).stream())
      .findFirst()
      .map(this::getTiffStringValue).map(dateParser::parse).map(LocalDate::from).ifPresent(instant -> {
        metaDataSink.put(PROPERTY_DATE_YEAR,  String.valueOf(instant.get(ChronoField.YEAR)));
        metaDataSink.put(PROPERTY_DATE_MONTH, String.valueOf(instant.get(ChronoField.MONTH_OF_YEAR)));
        metaDataSink.put(PROPERTY_DATE_DAY,   String.valueOf(instant.get(ChronoField.DAY_OF_MONTH)));
      });
  }

  public static final List<TagInfoAscii> DATE_TAGS = List.of(
    TiffTagConstants.TIFF_TAG_DATE_TIME,
    ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL,
    ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED
  );

  public final static DateTimeFormatter dateParser = DateTimeFormatter
    .ofPattern("yyyy:MM:dd HH:mm:ss")
    .withZone(TimeZone.getDefault().toZoneId());

}
