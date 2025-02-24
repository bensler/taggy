package com.bensler.taggy.imprt;

import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.ORIENTATION_VALUE_ROTATE_270_CW;
import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.ORIENTATION_VALUE_ROTATE_90_CW;

import java.awt.Image;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;

import com.bensler.decaf.util.TimerTrap;

public class Thumbnailer {

  public static final String WORKING_SUBDIR = "thumbnailer";

  public static final AffineTransform NOOP_AFFINE_TRANSFORM = AffineTransform.getScaleInstance(1.0, 1.0);

  private final static Map<Integer, AffineTransform> ROTATION_TRANSFORMATIONS = Map.of(
    ORIENTATION_VALUE_ROTATE_90_CW,  AffineTransform.getQuadrantRotateInstance(1),
    ORIENTATION_VALUE_ROTATE_270_CW, AffineTransform.getQuadrantRotateInstance(3)
  );

  private final File tmpDir_;

  /** Length of the largest side of a thumnail img. */
  public static final int THUMBNAIL_SIZE = 150;

  public Thumbnailer(File tmpDir) {
    tmpDir_ = new File(tmpDir, WORKING_SUBDIR);
    tmpDir_.mkdirs();
    Arrays.stream(tmpDir_.listFiles()).forEach(File::delete);
  }

  public BufferedImage scaleImageFromStream(InputStream fis, Map<String, String> metaDataSink) throws IOException {
    final BufferedImage srcImg = ImageIO.read(fis);
    int width = srcImg.getWidth();
    int height = srcImg.getHeight();

    metaDataSink.put(ImportController.PROPERTY_SIZE_WIDTH,  String.valueOf(width));
    metaDataSink.put(ImportController.PROPERTY_SIZE_HEIGHT, String.valueOf(height));
    if ((width > THUMBNAIL_SIZE) || (height > THUMBNAIL_SIZE)) {
      if (width > height) {
        width = THUMBNAIL_SIZE;
        height = -1;
      } else {
        width = -1;
        height = THUMBNAIL_SIZE;
      }

      final Image scaledImg = srcImg.getScaledInstance(width, height, Image.SCALE_SMOOTH);
      final BufferedImage bufferedImg = new BufferedImage(scaledImg.getWidth(null), scaledImg.getHeight(null), BufferedImage.TYPE_INT_RGB);

      bufferedImg.getGraphics().drawImage(scaledImg, 0, 0 , null);
      return bufferedImg;
    } else {
      return srcImg;
    }
  }

  private File writeImgToFile(BufferedImage img) throws IOException {
    final File outputFile = new File(tmpDir_, "%s-%s".formatted(
      ProcessHandle.current().pid(), Thread.currentThread().getName()
    ));

    outputFile.delete(); // just in case it already exists
    ImageIO.write(img, "jpg", outputFile);
    return outputFile;
  }

  public AffineTransform chooseRotationTransform(File srcFile) throws ImageReadException, IOException {
    final TiffField tiffRotationValue = findTiffRotationValue(srcFile);

    if (tiffRotationValue != null) {
      return ROTATION_TRANSFORMATIONS.getOrDefault(tiffRotationValue.getIntValue(), NOOP_AFFINE_TRANSFORM);
    }
    return NOOP_AFFINE_TRANSFORM;
  }

  private final static List<Long> statistics = new ArrayList<>(); // TODO rm

  private TiffField findTiffRotationValue(File srcFile) throws ImageReadException, IOException {
    try (var timer = new TimerTrap(statistics::add)) {
      return (Imaging.getMetadata(srcFile) instanceof JpegImageMetadata jpgMeta)
        ? jpgMeta.findEXIFValue(TiffTagConstants.TIFF_TAG_ORIENTATION)
        : null;
    }
  }

  public BufferedImage loadRotated(File srcFile) throws IOException, ImageReadException {
    final BufferedImage srcImg = ImageIO.read(srcFile);
    final AffineTransform transRotate = chooseRotationTransform(srcFile);
    final AffineTransform transTranslate = compensateForRotation(srcImg, transRotate);
    final AffineTransformOp rotateTranslateOp;

    transTranslate.concatenate(transRotate);
    rotateTranslateOp = new AffineTransformOp(transTranslate, new RenderingHints(
      RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC
    ));

    return rotateTranslateOp.filter(
      srcImg, rotateTranslateOp.createCompatibleDestImage(srcImg, null)
    );
  }

  public File scaleRotateImage(File srcFile, Map<String, String> metaDataSink) throws IOException, ImageReadException {
    final BufferedImage scaledImg = scaleImageFromStream(new FileInputStream(srcFile), metaDataSink);
    final AffineTransform transRotate = chooseRotationTransform(srcFile);
    final AffineTransform transTranslate = compensateForRotation(scaledImg, transRotate);
    final AffineTransformOp rotateTranslateOp;

    transTranslate.concatenate(transRotate);
    rotateTranslateOp = new AffineTransformOp(transTranslate, new RenderingHints(
      RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC
    ));

    final Rectangle2D rotatedBounds = rotateTranslateOp.getBounds2D(scaledImg);

    return writeImgToFile(rotateTranslateOp.filter( // no alpha as jpg does not support it ------------------------------vvv
      scaledImg, new BufferedImage((int)rotatedBounds.getWidth(), (int)rotatedBounds.getHeight(), BufferedImage.TYPE_INT_RGB)
    ));
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

}
