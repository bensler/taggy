package com.bensler.taggy.ui;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;

import org.apache.commons.imaging.ImageReadException;

import com.bensler.decaf.swing.action.ActionAppearance;
import com.bensler.decaf.swing.action.EntityAction;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;

public class ImportController {

  private static final List<String> KNOWN_FILEEXTENSIONS = List.of("JPG", "JPEG", "PNG");
  private static final String IMPORT_DIR = "import";

  public static final ActionAppearance IMPORT_ACTION_APPEARANCE =  new ActionAppearance(
    null, new ImageIcon(ImportController.class.getResource("vacuum.png")), null, "Scan for new Images to import."
  );

  private final App app_;
  private final File importDir_;
  private final EntityAction<Object> actionImport_;

  public ImportController(App app, File dataDir) {
    app_ = app;
    importDir_ = new File(dataDir, IMPORT_DIR);
    importDir_.mkdirs();
    actionImport_ = new EntityAction<>(
      IMPORT_ACTION_APPEARANCE, null,
      (source, entities) -> doImport()
    );
  }

  public EntityAction<?> getImportAction() {
    return actionImport_;
  }

  private void doImport() {
    new ImportDialog(app_.getMainFrame().getFrame(), this).setVisible(true);
  }

  public List<File> getFilesToImport() {
    return Arrays.stream(importDir_.listFiles((FileFilter)null))
    .filter(File::isFile)
    .filter(this:: hasKnownFileExtesion)
    .toList();
  }

  boolean hasKnownFileExtesion(File file) {
    return true; // TODO
  }

  public void importFile(File file) {
    try {
      final BlobController blobCtrl = app_.getBlobCtrl();
      final File thumbnail = app_.getThumbnailer().scaleRotateImage(file);
      final String fileSha = blobCtrl.storeBlob(file, false);
      final String thumbSha = blobCtrl.storeBlob(thumbnail, false);

      app_.getDbAccess().storeObject(new Blob(file.getName(), fileSha, thumbSha));
    } catch (ImageReadException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
