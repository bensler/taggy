package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.decaf.util.function.ForEachMapperAdapter.forEachMapper;
import static com.bensler.taggy.ui.MainFrame.ICON_IMAGES_48;
import static com.bensler.taggy.ui.MainFrame.ICON_PLUS_30;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.imaging.ImageReadException;

import com.bensler.decaf.swing.action.ActionAppearance;
import com.bensler.decaf.swing.action.EntityAction;
import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;

public class ImportController {

  private static final String TYPE_JPG = "JPG";
  private static final String TYPE_PNG = "PNG";
  private static final String TYPE_TIF = "TIF";

  private static final Map<String, String> EXTENSIONS_TO_TYPE_MAP = Map.of(
    "JPG",  TYPE_JPG,
    "JPEG", TYPE_JPG,
    "PNG",  TYPE_PNG,
    "TIF",  TYPE_TIF,
    "TIFF", TYPE_TIF
  );

  private static final String IMPORT_DIR = "import";

  public static final ActionAppearance IMPORT_ACTION_APPEARANCE =  new ActionAppearance(
    null, new OverlayIcon(ICON_IMAGES_48, new Overlay(ICON_PLUS_30, SE)), null, "Scan for new Images to import."
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
    new ImportDialog(app_).setVisible(true);
  }

  public List<FileToImport> getFilesToImport() {
    return Arrays.stream(importDir_.listFiles((FileFilter)null))
    .filter(File::isFile)
    .map(FileToImport::new)
    .map(forEachMapper(file -> getType(file.getFile()).ifPresentOrElse(file::setType, () -> file.setImportObstacle("Unsupported Type"))))
    .toList();
  }

  Optional<String> getType(File file) {
    final String[] fileNameParts = file.getName().split("\\.");

    return (fileNameParts.length > 0)
      ? Optional.ofNullable(EXTENSIONS_TO_TYPE_MAP.get(fileNameParts[fileNameParts.length - 1].toUpperCase()))
      : Optional.empty();
  }

  public FileToImport importFile(FileToImport fileToImport) {
    final File file = fileToImport.getFile();
    final BlobController blobCtrl = app_.getBlobCtrl();

    try {
      final File thumbnail = app_.getThumbnailer().scaleRotateImage(file);
      final String fileSha = blobCtrl.storeBlob(file, true);
      final String thumbSha = blobCtrl.storeBlob(thumbnail, false);

      app_.storeEntity(new Blob(file.getName(), fileSha, thumbSha, fileToImport.getType()));
      return new FileToImport(file, fileSha, "Duplicate (just imported)", fileToImport.getType());
    } catch (IOException | ImageReadException e) {
      e.printStackTrace();
      return new FileToImport(file, fileToImport.getShaSum(), "Import Error (%s)".formatted(e.getMessage()), fileToImport.getType());
    }
  }

}
