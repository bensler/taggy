package com.bensler.taggy.imprt;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.decaf.util.function.ForEachMapperAdapter.forEachMapper;
import static com.bensler.taggy.App.getApp;
import static com.bensler.taggy.ui.Icons.IMAGES_48;
import static com.bensler.taggy.ui.Icons.PLUS_30;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.imaging.ImageReadException;

import com.bensler.decaf.swing.action.ActionAppearance;
import com.bensler.decaf.swing.action.FilteredAction;
import com.bensler.decaf.swing.action.UiAction;
import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.decaf.util.prefs.DelegatingPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersister;
import com.bensler.taggy.imprt.FileToImport.ImportObstacle;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;

public class ImportController {

  public  static final String TYPE_BIN_PREFIX = "bin.";
  public  static final String TYPE_IMG_PREFIX = TYPE_BIN_PREFIX + "img.";
  private static final String TYPE_JPG = TYPE_IMG_PREFIX + "JPG";
  private static final String TYPE_PNG = TYPE_IMG_PREFIX + "PNG";
  private static final String TYPE_TIF = TYPE_IMG_PREFIX + "TIF";

  private static final Map<String, String> EXTENSIONS_TO_TYPE_MAP = Map.of(
    "JPG",  TYPE_JPG,
    "JPEG", TYPE_JPG,
    "PNG",  TYPE_PNG,
    "TIF",  TYPE_TIF,
    "TIFF", TYPE_TIF
  );

  private static final String IMPORT_DIR = "import";

  public static final OverlayIcon ICON_LARGE = new OverlayIcon(IMAGES_48, new Overlay(PLUS_30, SE));

  public static final ActionAppearance IMPORT_ACTION_APPEARANCE =  new ActionAppearance(
    null, ICON_LARGE, null, "Scan for new Images to import."
  );

  private final UiAction actionImport_;
  private final Map<File, String> fileShaMap_;
  private File importDir_;

  public ImportController(File dataDir) {
    importDir_ = new File(dataDir, IMPORT_DIR);
    importDir_.mkdirs();
    actionImport_ = new UiAction(
      IMPORT_ACTION_APPEARANCE, FilteredAction.many(Void.class, FilteredAction.allwaysOnFilter(), entities -> showImportDialog())
    );
    fileShaMap_ = new HashMap<>();
  }

  public UiAction getImportAction() {
    return actionImport_;
  }

  private void showImportDialog() {
    new ImportDialog(getApp()).setVisible(true);
  }

  File getImportDir() {
    return importDir_;
  }

  List<FileToImport> getFilesToImport() {
    final Path basePath = importDir_.toPath();
    final List<FileToImport> result = getFilesToImport(importDir_)
      .map(file -> new FileToImport(basePath, file))
      .map(forEachMapper(file -> getType(file.getFileType()).ifPresentOrElse(
        file::setType, () -> file.setImportObstacle(ImportObstacle.UNSUPPORTED_TYPE, null)
      )))
      .map(forEachMapper(this::reuseSha)).toList();

    synchronized (fileShaMap_) {
      fileShaMap_.clear();
      fileShaMap_.putAll(result.stream().filter(file -> file.hasObstacle(ImportObstacle.DUPLICATE_CHECK_MISSING))
        .collect(Collectors.toMap(FileToImport::getFile, FileToImport::getShaSum)));
    }
    return result;
  }

  private void reuseSha(FileToImport file) {
    if (file.hasObstacle(ImportObstacle.SHA_MISSING)) {
      synchronized (fileShaMap_) {
        final String sha;

        if ((sha = fileShaMap_.get(file.getFile())) != null) {
          file.setShaSum(sha);
        }
      }
    }
  }

  void putShaSum(File file, String shaSum) {
    synchronized (fileShaMap_) {
      fileShaMap_.put(file, shaSum);
    }
  }

  Stream<File> getFilesToImport(File dir) {
    return Arrays.stream(dir.listFiles((FileFilter)null))
        .flatMap(file -> file.isFile() ? Stream.of(file) : getFilesToImport(file));
  }

  Optional<String> getType(String fileExtension) {
    return Optional.ofNullable(EXTENSIONS_TO_TYPE_MAP.get(fileExtension.toUpperCase()));
  }

  FileToImport importFile(FileToImport file, Tag initialTag) {
    final String type = file.getType();

    try {
      final Blob blob = getApp().getBlobCtrl().importFile(file.getFile(), type, initialTag);

      return new FileToImport(file, blob.getSha256sum(), ImportObstacle.DUPLICATE, "just imported", type, blob);
    } catch (IOException | ImageReadException e) {
      e.printStackTrace();
      return new FileToImport(file, file.getShaSum(), ImportObstacle.IMPORT_ERROR, e.getMessage(), file.getType(), null);
    }
  }

  void setImportDir(File newImportDir) {
    importDir_ = newImportDir;
  }

  private File setPrefImportDir(String prefValue) {
    final File file = new File(prefValue);

    if (
      file.exists() && file.isDirectory()
      && file.getParentFile() instanceof File parent && parent.exists()
    ) {
      return file;
    }
    return importDir_;
  }

  PrefPersister getPrefPersister(PrefKey baseKey) {
    return new DelegatingPrefPersister(new PrefKey(baseKey, "sourceFolder"),
      () -> Optional.of(importDir_.getAbsolutePath()),
      value -> importDir_ = setPrefImportDir(value)
    );
  }

}
