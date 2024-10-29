package tmp;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;

public class ScaleRotate {

  public static void main(String[] args) throws IOException, ImageReadException {
    final ScaleRotate instance = new ScaleRotate();
    final File srcFolder = new File("/home/thomas/.taggy/data/blobs-1-1");

    instance.scaleRotate(new File(srcFolder, "0/d/0d6089f3ce9b99296ad41caf5a55347cfcd6bbdf465bace289abc63f94025c3a"), null);
  }

  private void scaleRotate(File srcFile, String id) throws IOException, ImageReadException {
    final File destDir = new File(System.getProperty("user.dir"));
    final BufferedImage srcImg = ImageIO.read(srcFile);
    final int srcWidth = srcImg.getWidth();
    final int srcHeight = srcImg.getHeight();
    final double scaleFactor = 150.0 / ((srcWidth > srcHeight) ? srcWidth : srcHeight);

    ImageIO.write(srcImg, "jpg", new File(destDir, "srcImg.jpg"));

    AffineTransform transRotate = AffineTransform.getQuadrantRotateInstance(1);
    AffineTransform transScale = AffineTransform.getScaleInstance(scaleFactor, scaleFactor);
    AffineTransform affTrans = new AffineTransform(transRotate);
    affTrans.concatenate(transScale);
    AffineTransformOp transformOp = new AffineTransformOp(affTrans, null);

    Point2D cornerTopLeft     = transformOp.getPoint2D(new Point(0, 0), null);
    Point2D cornerBottomRight = transformOp.getPoint2D(new Point(srcWidth, srcHeight), null);

    AffineTransform transTranslate = AffineTransform.getTranslateInstance(
      -1 * Math.min(cornerTopLeft.getX(), cornerBottomRight.getX()),
      -1 * Math.min(cornerTopLeft.getY(), cornerBottomRight.getY())
    );

    transTranslate.concatenate(transRotate);
    transTranslate.concatenate(transScale);
    transformOp = new AffineTransformOp(transTranslate, null);
    BufferedImage destImage = transformOp.createCompatibleDestImage(srcImg, null);
    transformOp.filter(srcImg, destImage);
    ImageIO.write(destImage, "jpg", new File(destDir, "scaledImg.jpg"));
    System.out.println(".");
  }

  private AffineTransform chooseTransformation(File srcFile) throws ImageReadException, IOException {
    final ImageMetadata metaData = Imaging.getMetadata(srcFile);
    final TiffField exifValue;

    if (
        (metaData == null)
        && (metaData instanceof JpegImageMetadata jpgMeta)
        && ((exifValue = jpgMeta.findEXIFValue(TiffTagConstants.TIFF_TAG_ORIENTATION)) != null)
        && (exifValue != null)
      ) {
      if (String.valueOf(TiffTagConstants.ORIENTATION_VALUE_ROTATE_90_CW).equals(String.valueOf(exifValue))) {
        return AffineTransform.getRotateInstance(0.5);
//        return AffineTransform.getQuadrantRotateInstance(1);
      }
      if (String.valueOf(TiffTagConstants.ORIENTATION_VALUE_ROTATE_270_CW).equals(String.valueOf(exifValue))) {
        return AffineTransform.getRotateInstance(-0.5);
//        return AffineTransform.getQuadrantRotateInstance(3);
      }
    }
    return AffineTransform.getScaleInstance(1.0, 1.0);
  }

}
