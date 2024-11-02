package tmp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ImageReadException;

import com.bensler.taggy.Thumbnailer;

public class ScaleRotate {

  private static void mv(File file, File targetFolder, String name) throws IOException {
    Files.move(file.toPath(),  Path.of(targetFolder.getPath(), name), StandardCopyOption.REPLACE_EXISTING );
  }

  public static void main(String[] args) throws IOException, ImageReadException {
    final File srcFolder = new File(System.getProperty("user.dir"));
    final File srcFile = new File(srcFolder, "f800b347ecf9a12ffe0991dbca14582452c54456f8f86a128b8168cb9b451350");
    final Thumbnailer thumbnailer_ = new Thumbnailer(srcFolder);
    int number = 0;

   ImageIO.write(ImageIO.read(srcFile), "jpg", new File(srcFolder, ++number + "-src.jpg"));
    mv(
      thumbnailer_.scaleRotateImage(srcFile),
      srcFolder, number + "-scaleRotated.jpg"
    );
  }


}
