package com.bensler.taggy.imprt;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

import com.bensler.taggy.App;
import com.bensler.taggy.ui.BlobController.Orientation;

public class Thumbnailer {

  public static final String WORKING_SUBDIR = "thumbnailer";

  private final File tmpDir_;

  /** Length of the largest side of a thumnail img. */
  public static final int THUMBNAIL_SIZE = 150;

  public Thumbnailer(File tmpDir) {
    tmpDir_ = new File(tmpDir, WORKING_SUBDIR);
    tmpDir_.mkdirs();
    Arrays.stream(tmpDir_.listFiles()).forEach(File::delete);
  }

  private BufferedImage scaleImage(final BufferedImage srcImg) {
    int width = srcImg.getWidth();
    int height = srcImg.getHeight();

    if ((width > THUMBNAIL_SIZE) || (height > THUMBNAIL_SIZE)) {
      if (width > height) {
        height = Math.round((((float)THUMBNAIL_SIZE) / width) * height);
        width = THUMBNAIL_SIZE;
      } else {
        width = Math.round((((float)THUMBNAIL_SIZE) / height) * width);
        height = THUMBNAIL_SIZE;
      }

      return Scalr.resize(srcImg, Method.ULTRA_QUALITY, Mode.FIT_EXACT, width, height, new BufferedImageOp[0]);
    } else {
      return srcImg;
    }
  }

  private  File writeImgToFile(BufferedImage img) throws IOException {
    final File outputFile = new File(tmpDir_, "%s-%s".formatted(
      ProcessHandle.current().pid(), Thread.currentThread().getName()
    ));

    outputFile.delete(); // just in case it already exists
    ImageIO.write(img, "jpg", outputFile);
    return outputFile;
  }

  public File createThumbnail(BufferedImage srcImg, Orientation orientation) throws IOException {
    return writeImgToFile(App.getApp().getBlobCtrl().rotate(
      scaleImage(srcImg), orientation,
      (transform, img) -> {
        final Rectangle2D rotatedBounds = transform.getBounds2D(img);
        // no alpha as jpg does not support it ------------------------------------------------------------------------vvv
        return new BufferedImage((int)rotatedBounds.getWidth(), (int)rotatedBounds.getHeight(), BufferedImage.TYPE_INT_RGB);
      }
    ));
  }

}
