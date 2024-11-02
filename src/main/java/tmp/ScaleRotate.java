package tmp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.imaging.ImageReadException;

import com.bensler.taggy.Thumbnailer;

public class ScaleRotate {

  private static void mv(File file, File targetFolder, String name) throws IOException {
    Files.move(file.toPath(),  Path.of(targetFolder.getPath(), name) );
  }

  public static void main(String[] args) throws IOException, ImageReadException {
    final File srcFolder = new File(System.getProperty("user.dir"));
    final Thumbnailer thumbnailer_ = new Thumbnailer(srcFolder);
    int number = 0;

    mv(
      thumbnailer_.scaleRotateImage(new File(srcFolder, "837afb74f4ea7efd718d2b59c6ddb51a56cb26ca242b1130d0355c032e5dc0b0")),
      srcFolder, ++number + "-scaleRotated.jpg"
    );
    mv(
      thumbnailer_.scaleRotateImage(new File(srcFolder, "0d6089f3ce9b99296ad41caf5a55347cfcd6bbdf465bace289abc63f94025c3a")),
      srcFolder, ++number + "-scaleRotated.jpg"
    );
    mv(
      thumbnailer_.scaleRotateImage(new File(srcFolder, "3607751fd6f152a4c882a83f08dae180676c75b6f46db30aa1fda905e6fa3228")),
      srcFolder, ++number + "-scaleRotated.jpg"
    );
  }


}
