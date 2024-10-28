package tmp;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ScaleTest {

  private ScaleTest() {}

  public static void main(String[] args) throws IOException {
    final File dir = new File(System.getProperty("user.dir"));
    final BufferedImage srcImg = ImageIO.read(new File(dir, "sample_big"));
    int width = srcImg.getWidth();
    int height = srcImg.getHeight();

    if (width > height) {
      width = 150;
      height = -1;
    } else {
      width = -1;
      height = 150;
    }

    final Image scaledImg = srcImg.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    final BufferedImage bufferedImg = new BufferedImage(scaledImg.getWidth(null), scaledImg.getHeight(null), BufferedImage.TYPE_INT_RGB);
    bufferedImg.getGraphics().drawImage(scaledImg, 0, 0 , null);

    ImageIO.write(bufferedImg, "jpg", new File(dir, "sample_scaled"));
    System.out.println(".");
  }

}
