package com.bensler.taggy.imprt;

import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.ORIENTATION_VALUE_ROTATE_270_CW;
import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.ORIENTATION_VALUE_ROTATE_90_CW;

import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

public class Thumbnailer {

  public static final String WORKING_SUBDIR = "thumbnailer";

  public enum Orientation {
    ROTATE_000_CW(AffineTransform.getQuadrantRotateInstance(0), null),
    ROTATE_090_CW(AffineTransform.getQuadrantRotateInstance(1), ImportController.PROPERTY_ORIENTATION_VALUE_90_CW),
    ROTATE_270_CW(AffineTransform.getQuadrantRotateInstance(3), ImportController.PROPERTY_ORIENTATION_VALUE_270_CW);

    public final AffineTransform transform_;
    public final Optional<String> metaDataValue_;

    Orientation(AffineTransform transform, String metaDataValue) {
      transform_ = transform;
      metaDataValue_ = Optional.ofNullable(metaDataValue);
    }

    public void putMetaData(Map<String, String> metaDataSink) {
      metaDataValue_.ifPresent(value -> metaDataSink.put(ImportController.PROPERTY_ORIENTATION, value));
    }

  }

  public final static Map<Integer, Orientation> ROTATION_TRANSFORMATIONS = Map.of(
    ORIENTATION_VALUE_ROTATE_90_CW,  Orientation.ROTATE_090_CW,
    ORIENTATION_VALUE_ROTATE_270_CW, Orientation.ROTATE_270_CW
  );

  private final File tmpDir_;

  /** Length of the largest side of a thumnail img. */
  public static final int THUMBNAIL_SIZE = 150;

  public Thumbnailer(File tmpDir) {
    tmpDir_ = new File(tmpDir, WORKING_SUBDIR);
    tmpDir_.mkdirs();
    Arrays.stream(tmpDir_.listFiles()).forEach(File::delete);
  }

  public BufferedImage scaleImage(final BufferedImage srcImg) {
    int width = srcImg.getWidth();
    int height = srcImg.getHeight();

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

  public File writeImgToFile(BufferedImage img) throws IOException {
    final File outputFile = new File(tmpDir_, "%s-%s".formatted(
      ProcessHandle.current().pid(), Thread.currentThread().getName()
    ));

    outputFile.delete(); // just in case it already exists
    ImageIO.write(img, "jpg", outputFile);
    return outputFile;
  }

}
