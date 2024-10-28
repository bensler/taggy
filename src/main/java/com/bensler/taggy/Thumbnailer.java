package com.bensler.taggy;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import com.bensler.taggy.ui.BlobController;

public class Thumbnailer {

  private final File tmpDir_;

  public Thumbnailer(File tmpDir) {
    tmpDir_ = new File(tmpDir, "thumbnailer");
    tmpDir_.mkdirs();
    Arrays.stream(tmpDir_.listFiles()).forEach(File::delete);
  }

  public File scaleImage(File sourceFile) throws IOException {
    try (FileInputStream fis = new FileInputStream(sourceFile)) {
      return scaleImage(fis);
    }
  }

  public File scaleImage(FileInputStream fis) throws IOException {
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

    final File outputFile = new File(tmpDir_, "%s-%s".formatted(ProcessHandle.current().pid(), Thread.currentThread().getName()));
    final Image scaledImg = srcImg.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    final BufferedImage bufferedImg = new BufferedImage(scaledImg.getWidth(null), scaledImg.getHeight(null), BufferedImage.TYPE_INT_RGB);

    bufferedImg.getGraphics().drawImage(scaledImg, 0, 0 , null);
    outputFile.delete();
    ImageIO.write(bufferedImg, "jpg", outputFile);
    return outputFile;
  }

}
