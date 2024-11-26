package com.bensler.taggy;

import java.io.File;
import java.util.Arrays;

import javax.swing.UIManager;

import com.bensler.taggy.persist.DbConnector;
import com.bensler.taggy.persist.SqliteDbConnector;
import com.bensler.taggy.ui.BlobController;
import com.bensler.taggy.ui.ImportController;
import com.bensler.taggy.ui.MainFrame;
import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.jgoodies.looks.plastic.theme.DesertYellow;

public class Main {

  public static void main(String[] args) throws Exception {
    new Main().run();
  }

  private static final int[] FOLDER_PATTERN = new int[] {1, 1};

  public static int[] getFolderPattern() {
    return Arrays.copyOf(FOLDER_PATTERN, FOLDER_PATTERN.length);
  }

  public static File getBaseDir() {
    return new File(System.getProperty("user.home"), ".taggy");
  }

  public static File getDataDir() {
    return new File(getBaseDir(), "data");
  }

  private final BlobController blobController_;
  private final Thumbnailer thumbnailer_;
  private final DbConnector db_;
  private final ImportController importCtrl_;

  private Main() throws Exception {
    Plastic3DLookAndFeel.setCurrentTheme(new DesertYellow());
    UIManager.setLookAndFeel(new Plastic3DLookAndFeel());

    final File dataDir = getDataDir();
    db_ = new SqliteDbConnector(dataDir, "taggy.sqlite.db");
    db_.performFlywayMigration();
    blobController_ = new BlobController(dataDir, FOLDER_PATTERN);
    importCtrl_ = new ImportController(getBaseDir());
    thumbnailer_ = new Thumbnailer(dataDir);
  }

  public void run() {
    new MainFrame(blobController_, importCtrl_, db_.getSession(), thumbnailer_).show();
  }

}
