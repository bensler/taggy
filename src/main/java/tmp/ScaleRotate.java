package tmp;

import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.ORIENTATION_VALUE_ROTATE_270_CW;
import static org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.ORIENTATION_VALUE_ROTATE_90_CW;

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

  public static final AffineTransform NOOP_AFFINE_TRANFORM = AffineTransform.getScaleInstance(1.0, 1.0);

  public static void main(String[] args) throws IOException, ImageReadException {
    final ScaleRotate instance = new ScaleRotate();
    final File srcFolder = new File(System.getProperty("user.dir"));

    instance.scaleRotate(new File(srcFolder, "8d351dc2e9374997bf71c84d04c6cd0ce7d21454a3cbc147a7ae8cc8a1f46f96.jpg"), "1");
    instance.scaleRotate(new File(srcFolder, "837afb74f4ea7efd718d2b59c6ddb51a56cb26ca242b1130d0355c032e5dc0b0.jpg"), "2");
  }

  private void scaleRotate(File srcFile, String id) throws IOException, ImageReadException {
    final File destDir = new File(System.getProperty("user.dir"));
    final BufferedImage srcImg = ImageIO.read(srcFile);
    final int srcWidth = srcImg.getWidth();
    final int srcHeight = srcImg.getHeight();
    final double scaleFactor = 150.0 / ((srcWidth > srcHeight) ? srcWidth : srcHeight);

    ImageIO.write(srcImg, "jpg", new File(destDir, "srcImg-%s.jpg".formatted(id)));

    AffineTransform transRotate = chooseRotation(srcFile);
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
    ImageIO.write(destImage, "jpg", new File(destDir, "scaledImg-%s.jpg".formatted(id)));
    System.out.println(".");
  }

  private AffineTransform chooseRotation(File srcFile) throws ImageReadException, IOException {
    final ImageMetadata metaData = Imaging.getMetadata(srcFile);
    final TiffField orientationExifValue;

    if (
        (metaData != null)
        && (metaData instanceof JpegImageMetadata jpgMeta)
        && ((orientationExifValue = jpgMeta.findEXIFValue(TiffTagConstants.TIFF_TAG_ORIENTATION)) != null)
      ) {
      final int orientationIntValue =  orientationExifValue.getIntValue();

      if (orientationIntValue == ORIENTATION_VALUE_ROTATE_90_CW) {
        return AffineTransform.getQuadrantRotateInstance(1);
      }
      if (orientationIntValue == ORIENTATION_VALUE_ROTATE_270_CW) {
        return AffineTransform.getQuadrantRotateInstance(3);
      }
    }
    return NOOP_AFFINE_TRANFORM;
  }

}
