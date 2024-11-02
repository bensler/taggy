package com.bensler.taggy;

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
import java.util.Arrays;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;

import com.bensler.taggy.ui.BlobController;

public class Thumbnailer {

  public static final AffineTransform NOOP_AFFINE_TRANSFORM = AffineTransform.getScaleInstance(1.0, 1.0);

  private final static Map<Integer, AffineTransform> ROTATION_TRANSFORMATIONS = Map.of(
    ORIENTATION_VALUE_ROTATE_90_CW,  AffineTransform.getQuadrantRotateInstance(1),
    ORIENTATION_VALUE_ROTATE_270_CW, AffineTransform.getQuadrantRotateInstance(3)
  );

  private final File tmpDir_;

  public Thumbnailer(File tmpDir) {
    tmpDir_ = new File(tmpDir, "thumbnailer");
    tmpDir_.mkdirs();
    Arrays.stream(tmpDir_.listFiles()).forEach(File::delete);
  }

  public File scaleImage(File sourceFile) throws IOException {
    try (FileInputStream fis = new FileInputStream(sourceFile)) {
      return writeImgToFile(scaleImageFromStream(fis));
    }
  }

  public BufferedImage scaleImageFromStream(InputStream fis) throws IOException {
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

    final Image scaledImg = srcImg.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    final BufferedImage bufferedImg = new BufferedImage(scaledImg.getWidth(null), scaledImg.getHeight(null), BufferedImage.TYPE_INT_RGB);

    bufferedImg.getGraphics().drawImage(scaledImg, 0, 0 , null);
    return bufferedImg;
  }

  private File writeImgToFile(BufferedImage img) throws IOException {
    final File outputFile = new File(tmpDir_, "%s-%s".formatted(
      ProcessHandle.current().pid(), Thread.currentThread().getName()
    ));

    outputFile.delete(); // just in case it already exists
    ImageIO.write(img, "jpg", outputFile);
    return outputFile;
  }

  private AffineTransform chooseRotationTransform(File srcFile) throws ImageReadException, IOException {
    final TiffField tiffRotationValue = findTiffRotationValue(srcFile);

    if (tiffRotationValue != null) {
      return ROTATION_TRANSFORMATIONS.getOrDefault(tiffRotationValue.getIntValue(), NOOP_AFFINE_TRANSFORM);
    }
    return NOOP_AFFINE_TRANSFORM;
  }

  private TiffField findTiffRotationValue(File srcFile) throws ImageReadException, IOException {
    return (Imaging.getMetadata(srcFile) instanceof JpegImageMetadata jpgMeta)
      ? jpgMeta.findEXIFValue(TiffTagConstants.TIFF_TAG_ORIENTATION)
      : null;
  }

  public File scaleRotateImage(File srcFile) throws IOException, ImageReadException {
    final BufferedImage scaledImg = scaleImageFromStream(new FileInputStream(srcFile));
    final AffineTransform transRotate = chooseRotationTransform(srcFile);
    final AffineTransform transTranslate = compensateForRotation(scaledImg, transRotate);
    final AffineTransformOp rotateTranslateOp;

    transTranslate.concatenate(transRotate);
    rotateTranslateOp = new AffineTransformOp(transTranslate, new RenderingHints(
      RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC
    ));

    final Rectangle2D rotatedBounds = rotateTranslateOp.getBounds2D(scaledImg);

    return writeImgToFile(rotateTranslateOp.filter(
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
