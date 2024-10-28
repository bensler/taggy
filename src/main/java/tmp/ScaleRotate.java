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
    new ScaleRotate()
    .scaleRotate();
//    .scaleRotate_1();
  }

  private void scaleRotate() throws IOException, ImageReadException {
    final File destDir = new File(System.getProperty("user.dir"));
    final File src = new File("/home/thomas/.taggy/data/blobs-1-1/0/d/0d6089f3ce9b99296ad41caf5a55347cfcd6bbdf465bace289abc63f94025c3a");
    final BufferedImage srcImg = ImageIO.read(src);

    ImageIO.write(srcImg, "jpg", new File(destDir, "srcImg.jpg"));

    int width = srcImg.getWidth();
    int height = srcImg.getHeight();
    final double scaleFactor;

    if (width > height) {
      scaleFactor = 150.0 / width;
    } else {
      scaleFactor = 150.0 / height;
    }

    AffineTransform affTrans = AffineTransform.getQuadrantRotateInstance(1);
    affTrans.concatenate(AffineTransform.getScaleInstance(scaleFactor, scaleFactor));
    AffineTransformOp transformOp = new AffineTransformOp(affTrans, null);

    Point2D p1 = transformOp.getPoint2D(new Point(0, 0), null);
    Point2D p2 = transformOp.getPoint2D(new Point(width, height), null);

    affTrans.concatenate(AffineTransform.getTranslateInstance(
        -1 * Math.min(p1.getX(), p2.getX()), -1 * Math.min(p1.getY(), p2.getY())
    ));
    transformOp = new AffineTransformOp(affTrans, null);
    BufferedImage destImage = transformOp.createCompatibleDestImage(srcImg, null);
//    transformOp.filter(srcImg, destImage);
//    destImage.getSubimage(
//      (int)Math.min(p1.getX(), p2.getX()),
//      (int)Math.min(p1.getY(), p2.getY()),
//      (int)Math.abs(p1.getX() - p2.getX()),
//      (int)Math.abs(p1.getY() - p2.getY())
//    );
//    ImageIO.write(destImage, "jpg", new File(destDir, "scaledImg.jpg"));
    System.out.println(".");
  }

  private void scaleRotate_1() throws IOException, ImageReadException {
    final File destDir = new File(System.getProperty("user.dir"));
    final File src = new File("/home/thomas/.taggy/data/blobs-1-1/0/d/0d6089f3ce9b99296ad41caf5a55347cfcd6bbdf465bace289abc63f94025c3a");
    final BufferedImage srcImg = ImageIO.read(src);

    ImageIO.write(srcImg, "jpg", new File(destDir, "srcImg.jpg"));

    int width = srcImg.getWidth();
    int height = srcImg.getHeight();
    final double scaleFactor;

    if (width > height) {
      scaleFactor = 150.0 / width;
    } else {
      scaleFactor = 150.0 / height;
    }

    AffineTransform affTrans = AffineTransform.getTranslateInstance((height * scaleFactor) * .9, 0);
    affTrans.concatenate(AffineTransform.getQuadrantRotateInstance(1));
    affTrans.concatenate(AffineTransform.getScaleInstance(scaleFactor, scaleFactor));
    AffineTransformOp transformOp = new AffineTransformOp(affTrans, null);

    BufferedImage destImage = transformOp.createCompatibleDestImage(srcImg, null);
    transformOp.filter(srcImg, destImage);
    System.out.println(transformOp.getPoint2D(new Point(0, 0), null));
    System.out.println(transformOp.getPoint2D(new Point(width, height), null));

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
