package com.bensler.taggy.ui;

import static com.bensler.taggy.App.getApp;
import static com.bensler.taggy.imprt.ImportController.TYPE_BIN_PREFIX;
import static com.bensler.taggy.imprt.ImportController.TYPE_IMG_PREFIX;
import static com.bensler.taggy.persist.TagProperty.REPRESENTED_DATE;
import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.ORIENTATION_VALUE_ROTATE_180;
import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.ORIENTATION_VALUE_ROTATE_270_CW;
import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.ORIENTATION_VALUE_ROTATE_90_CW;

import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoAscii;

import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.App;
import com.bensler.taggy.imprt.Thumbnailer;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.DbMapper.Scope;
import com.bensler.taggy.persist.Image;
import com.bensler.taggy.persist.Photo;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.persist.Thumbnail;
import com.bensler.taggy.persist.v2.ThumbnailDbMapper;
import com.bensler.taggy.persist.v2.V2PhotoDbMapper;


public class BlobController {

  public static final String SIZE_PREFIX = TYPE_IMG_PREFIX + "size.";
  public static final String PROPERTY_SIZE_WIDTH  = SIZE_PREFIX + "width";
  public static final String PROPERTY_SIZE_HEIGHT = SIZE_PREFIX + "height";
  public static final String PROPERTY_ORIENTATION = TYPE_IMG_PREFIX + "orientation";
  public static final String PROPERTY_ORIENTATION_VALUE_90_CW  = "rotate90cw";
  public static final String PROPERTY_ORIENTATION_VALUE_180_CW = "rotate180cw";
  public static final String PROPERTY_ORIENTATION_VALUE_270_CW = "rotate270cw";
  public static final String DATE_PREFIX = TYPE_BIN_PREFIX + "date.";
  public static final String PROPERTY_DATE_EPOCH_SECONDS = DATE_PREFIX + "epochSeconds";
  public static final String PROPERTY_DATE_YMD = DATE_PREFIX + "ymd";
  public static final String PROPERTY_FILENAME = TYPE_BIN_PREFIX + "filename";

  public enum Orientation {
    ROTATE_000_CW(AffineTransform.getQuadrantRotateInstance(0), null),
    ROTATE_090_CW(AffineTransform.getQuadrantRotateInstance(1), PROPERTY_ORIENTATION_VALUE_90_CW),
    ROTATE_180_CW(AffineTransform.getQuadrantRotateInstance(2), PROPERTY_ORIENTATION_VALUE_180_CW),
    ROTATE_270_CW(AffineTransform.getQuadrantRotateInstance(3), PROPERTY_ORIENTATION_VALUE_270_CW);

    public final static List<Orientation> ORIENTATIONS = List.of(
      ROTATE_000_CW, ROTATE_090_CW, ROTATE_180_CW, ROTATE_270_CW
    );

    public final AffineTransform transform_;
    public final Optional<String> metaDataValue_;

    Orientation(AffineTransform transform, String metaDataValue) {
      transform_ = transform;
      metaDataValue_ = Optional.ofNullable(metaDataValue);
    }

    public void putMetaData(BiConsumer<String, String> putOperation) {
      putOperation.accept(PROPERTY_ORIENTATION, metaDataValue_.orElse(null));
    }

    public Orientation turnClockwise() {
      return getNext(1);
    }

    private Orientation getNext(int plusOrMinusOne) {
      final int size = ORIENTATIONS.size();

      return ORIENTATIONS.get((ORIENTATIONS.indexOf(this) + plusOrMinusOne + size) % size);
    }

    public Orientation turnCounterclockwise() {
      return getNext(-1);
    }
  }

  private final static Map<Integer, Orientation> ROTATION_TRANSFORMATIONS = new HashMap<>(Map.of(
    ORIENTATION_VALUE_ROTATE_90_CW,  Orientation.ROTATE_090_CW,
    ORIENTATION_VALUE_ROTATE_180,    Orientation.ROTATE_180_CW,
    ORIENTATION_VALUE_ROTATE_270_CW, Orientation.ROTATE_270_CW
  ));

  private static final Map<String, Orientation> ORIENTATIONS_BY_STR = Map.of(
    PROPERTY_ORIENTATION_VALUE_90_CW,  Orientation.ROTATE_090_CW,
    PROPERTY_ORIENTATION_VALUE_180_CW, Orientation.ROTATE_180_CW,
    PROPERTY_ORIENTATION_VALUE_270_CW, Orientation.ROTATE_270_CW
  );

  private final static DateTimeFormatter META_DATA_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
    .withZone(ZoneOffset.UTC);

  public static final List<TagInfoAscii> DATE_TAGS = List.of(
    TiffTagConstants.TIFF_TAG_DATE_TIME,
    ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL,
    ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED
  );

  public static final String BLOB_FOLDER_BASE_NAME = "blobs";
  public static final String DIGEST_TYPE_SHA_256 = "SHA-256";

  private final V2PhotoDbMapper dbMapper_;
  private final List<Fragment> pathFragments_;
  private final File blobBasePath_;
  private final MessageDigest digest_;
  private final byte[] buffer_;

  public BlobController(V2PhotoDbMapper dbMapper, File blobBasePath, int[] folderPattern) throws NoSuchAlgorithmException {
    dbMapper_ = dbMapper;
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

  /** @param direction change orientation property relativly in respect to {@link Orientation#ORIENTATIONS}.
   */
  public void rotateBlob(Photo blob, int direction) throws IOException, SQLException {
    Optional.ofNullable(blob.getProperty(PROPERTY_ORIENTATION))
    .flatMap(propertyValue -> Optional.ofNullable(ORIENTATIONS_BY_STR.get(propertyValue)))
    .orElse(Orientation.ROTATE_000_CW)
    .getNext(direction).putMetaData(blob::addProperty);

    final Thumbnail oldThumb = blob.getThumbnail();
    final Thumbnail newThumb;
    final String newThumbSha = storeBlob(getApp().getThumbnailer().createThumbnail(this, loadRotated(blob), Orientation.ROTATE_000_CW), false);
    final ThumbnailDbMapper thumbnailDbMapper = dbMapper_.getThumbnailDbMapper();

    thumbnailDbMapper.remove(oldThumb.getId());
    getFile(oldThumb.getSha256sum()).delete();
    newThumb = thumbnailDbMapper.loadOne(thumbnailDbMapper.insert(new Thumbnail(null, newThumbSha)));
    getApp().storeEntity(new Photo(
      blob.getId(), blob.getSha256sum(), newThumb, blob.getType(), blob.getMetaData(), blob.getTagRefs()
    ), Scope.PROPERTIES);
  }

  /** @return the sourceFiles sha256sum */
  public String storeBlob(File sourceFile, boolean keepSource) throws IOException {
    final String sourceFileHash  = hashFile(sourceFile);
    final File targetFile = getFile(sourceFileHash);

    if (keepSource) {
      Files.copy(sourceFile.toPath(), targetFile.toPath());
    } else {
      Files.move(sourceFile.toPath(), targetFile.toPath());
    }
    return sourceFileHash;
  }

  public void deleteBlob(Photo blob) {
    final App app = getApp();
    final DbAccess db = app.getDbAccess();
    final Set<Tag> tags;
    final String blobSha256sum = blob.getSha256sum();
    final Image<?> thumbnail = blob.getThumbnail();
    final String thumbnailSha = thumbnail.getSha256sum();

    try {
      db.runInTxn(_ -> db.deleteNoTxn(blob));
      tags = db.refreshAllRefs(blob.getTagRefs());
      Optional.ofNullable(blobSha256sum).ifPresent(this::deleteFile);
      Optional.ofNullable(thumbnailSha).ifPresent(this::deleteFile);
      app.entityRemoved(blob);
      app.entitiesChanged(tags);
    } catch (Exception e) {
      e.printStackTrace(); // TODO
    }
  }

  public void deleteFile(String shasum) {
    try {
      Files.deleteIfExists(getFile(shasum).toPath());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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

  public BufferedImage loadRotated(Photo blob) throws IOException {
    return rotate(
      ImageIO.read(getFile(blob.getSha256sum())),
      findOrientation(blob.getProperty(PROPERTY_ORIENTATION)),
      (transform, srcImg) -> transform.createCompatibleDestImage(srcImg, null)
    );
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

  public Photo importFile(File file, String type, Tag initialTag) throws IOException, ImageReadException, InvocationTargetException, InterruptedException {
    final App app = getApp();
    final ThumbnailDbMapper thumbnailDbMapper = dbMapper_.getThumbnailDbMapper();
    final Map<String, String> metaData = new HashMap<>();
    final File thumbnailFile = createThumbnail(app.getThumbnailer(), file, metaData);
    final String fileSha = storeBlob(file, true); // TODO rm file again if exc later in this method
    final String thumbSha = storeBlob(thumbnailFile, false);// TODO rm file again if exc later in this method
    final Set<Tag> tags = new HashSet<>();
    final AtomicReference<Photo> newPhoto = new AtomicReference<>();

    Optional.ofNullable(metaData.get(PROPERTY_DATE_YMD))
      .map(app.getTagCtrl()::getDateTag)
      .ifPresent(tags::add);
    Optional.ofNullable(initialTag).ifPresent(tags::add);
    metaData.put(PROPERTY_FILENAME, file.getName());
    SwingUtilities.invokeAndWait(() -> {
      final Integer thumbId = thumbnailDbMapper.insert(new Thumbnail(null, thumbSha));
      final Thumbnail thumbnail = thumbnailDbMapper.loadAllEntities(List.of(thumbId)).get(0);

      newPhoto.set(app.storeEntity(new Photo(null, fileSha, thumbnail, type, metaData, EntityReference.createCollection(tags, new HashSet<>())), Scope.FULL));
      app.entitiesChanged(app.getDbAccess().refreshAll(tags));
    });
    return newPhoto.get();
  }

  private Optional<JpegImageMetadata> getMetaData(File srcFile) throws ImageReadException, IOException {
    return (Imaging.getMetadata(srcFile) instanceof JpegImageMetadata jpgMeta)
      ? Optional.of(jpgMeta) : Optional.empty();
  }

  public BufferedImage readImageMetadata(File srcFile, Map<String, String> metaDataSink) throws ImageReadException, IOException {
    final Optional<JpegImageMetadata> srcMetaData = getMetaData(srcFile);
    final BufferedImage srcImg = ImageIO.read(new FileInputStream(srcFile));

    metaDataSink.put(PROPERTY_SIZE_WIDTH,  String.valueOf(srcImg.getWidth()));
    metaDataSink.put(PROPERTY_SIZE_HEIGHT, String.valueOf(srcImg.getHeight()));
    srcMetaData.ifPresent(metaData -> findDate(metaData, metaDataSink));
    findOrientation(srcMetaData).ifPresent(value -> value.putMetaData(metaDataSink::put));
    return srcImg;
  }

  public File createThumbnail(Thumbnailer thumbnailer, File srcFile, Map<String, String> metaDataSink) throws IOException, ImageReadException {
    return thumbnailer.createThumbnail(
      this, readImageMetadata(srcFile, metaDataSink), findOrientation(metaDataSink.get(PROPERTY_ORIENTATION))
    );
  }

  public BufferedImage rotate(
    BufferedImage srcImg, Orientation orientation,
    BiFunction<AffineTransformOp, BufferedImage, BufferedImage> targetImgSource
  ) {
    final AffineTransform transRotate = orientation.transform_;
    final AffineTransform transTranslate = compensateForRotation(srcImg, transRotate);
    final AffineTransformOp rotateTranslateOp;

    transTranslate.concatenate(transRotate);
    rotateTranslateOp = new AffineTransformOp(transTranslate, new RenderingHints(
      RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC
    ));

    return rotateTranslateOp.filter(srcImg, targetImgSource.apply(rotateTranslateOp, srcImg));
  }

  private Orientation findOrientation(String orientationStr) {
    return ORIENTATIONS_BY_STR.getOrDefault(Optional.ofNullable(orientationStr).orElse(""), Orientation.ROTATE_000_CW);
  }

  private Optional<Orientation> findOrientation(Optional<JpegImageMetadata> optMeta) {
    try {
      return optMeta.flatMap(jpgMeta -> Optional.ofNullable(jpgMeta.findEXIFValue(TiffTagConstants.TIFF_TAG_ORIENTATION)))
        .map(this::getTiffIntValue)
        .filter(ROTATION_TRANSFORMATIONS::containsKey)
        .map(ROTATION_TRANSFORMATIONS::get);
    } catch (RuntimeException rte) { // never trust untrusted date strings
      rte.printStackTrace();
      return Optional.empty();
    }
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
    try {
      DATE_TAGS.stream()
        .flatMap(tag -> Optional.ofNullable(jpgMeta.findEXIFValue(tag)).stream())
        .findFirst()
        .map(this::getTiffStringValue).map(META_DATA_DATE_FORMAT::parse).map(LocalDateTime::from).ifPresent(instant -> {
          metaDataSink.put(PROPERTY_DATE_EPOCH_SECONDS, String.valueOf(instant.toEpochSecond(ZoneOffset.UTC)));
          metaDataSink.put(PROPERTY_DATE_YMD, TagsUiController.YYYY_MM_DD.format(instant));
        });
    } catch (RuntimeException rte) { // never trust untrusted date strings
      rte.printStackTrace();
    }
  }

  public void setTags(Photo blob, Set<Tag> newTags) {
    final App app = getApp();
    final DbAccess db = app.getDbAccess();
    final Set<Tag> oldTags = blob.getTags();
    final List<Tag> affectedTags = Stream.of(oldTags, newTags).flatMap(Set::stream)
      .filter(tag -> oldTags.contains(tag) ^ newTags.contains(tag)).toList();
    final EntityReference<Photo> blobRef = new EntityReference<>(blob);
    final Photo newBlob;
    final Set<Tag> updatedTags;

    db.runInTxn(_ -> dbMapper_.setTags(blobRef, newTags));
    newBlob = db.refresh(blobRef);
    updatedTags = db.refreshAll(affectedTags);
    app.entityChanged(newBlob);
    app.entitiesChanged(updatedTags);
  }

  public void addTags(List<Photo> blobs, Set<Tag> tags) {
    blobs.forEach(blob -> setTags(blob, Stream.concat(tags.stream(), blob.getTags().stream()).collect(Collectors.toSet())));
  }

  public List<Photo> findOrphanBlobs() {
    final DbAccess dbAccess = getApp().getDbAccess();

    try {
      return dbAccess.resolveAll(dbMapper_.findOrphanBlobs().stream().map(id -> new EntityReference<>(Photo.class, id)).toList(), new ArrayList<Photo>());
    } catch (SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  public boolean doesBlobExist(String shaHash) throws SQLException {
    return dbMapper_.doesBlobExist(shaHash);
  }

  public String getTagString(Photo blob) {
    final Set<Tag> tags = blob.getTags();
    final String dateStr = tags.stream()
      .flatMap(tag -> tag.containsProperty(REPRESENTED_DATE).stream())
      .findFirst()
      .map(str -> str.concat("_")).orElse("");
    final String tagsStr = tags.stream()
      .filter(tag -> tag.containsProperty(REPRESENTED_DATE).isEmpty())
      .map(Tag::getName)
      .collect(Collectors.joining("-"));
    final String typeStr = Arrays.asList(blob.getType().split("\\.")).getLast().toLowerCase();
    final String fileName = dateStr + tagsStr;

    return (fileName.isEmpty() ? "image" : fileName) + "." + typeStr.toLowerCase();
  }

  public void export(Photo blob, File file) {
    try {
      Files.copy(getFile(blob.getSha256sum()).toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ioe) {
      // TODO Auto-generated catch block
      throw new RuntimeException(ioe);
    }
  }

}
