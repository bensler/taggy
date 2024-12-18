package com.bensler.taggy;

import java.io.File;
import java.util.Arrays;

import javax.swing.UIManager;

import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.Prefs;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.DbConnector;
import com.bensler.taggy.persist.SqliteDbConnector;
import com.bensler.taggy.ui.BlobController;
import com.bensler.taggy.ui.ImportController;
import com.bensler.taggy.ui.MainFrame;
import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.jgoodies.looks.plastic.theme.DesertYellow;

public class App {

  public static final PrefKey PREFS_APP_ROOT = new PrefKey(PrefKey.ROOT, "taggy");

  private static final int[] FOLDER_PATTERN = new int[] {1, 1};

  private static App app_;

  public static void main(String[] args) throws Exception {
    (app_ = new App()).run();
  }

  public static int[] getFolderPattern() {
    return Arrays.copyOf(FOLDER_PATTERN, FOLDER_PATTERN.length);
  }

  public static File getBaseDir() {
    return new File(System.getProperty("user.home"), ".taggy");
  }

  public static File getDataDir() {
    return new File(getBaseDir(), "data");
  }

  public static App getApp() {
    return app_;
  }

  private final DbConnector db_;
  private final Prefs prefs_;
  private final BlobController blobCtrl_;
  private final DbAccess dbAccess_;
  private final ImportController importCtrl_;
  private final Thumbnailer thumbnailer_;
  private final MainFrame mainFrame_;

  private App() throws Exception {
    Plastic3DLookAndFeel.setCurrentTheme(new DesertYellow());
    UIManager.setLookAndFeel(new Plastic3DLookAndFeel());

    final File dataDir = getDataDir();
    db_ = new SqliteDbConnector(dataDir, "taggy.sqlite.db");
    db_.performFlywayMigration();
    dbAccess_ = new DbAccess(db_.getSession());
    prefs_ = new Prefs(new File(getBaseDir(), "prefs.xml"));
    blobCtrl_ = new BlobController(dataDir, FOLDER_PATTERN);
    importCtrl_ = new ImportController(this, getBaseDir());
    thumbnailer_ = new Thumbnailer(dataDir);
    mainFrame_ = new MainFrame(this);
  }

  public BlobController getBlobCtrl() {
    return blobCtrl_;
  }

  public DbAccess getDbAccess() {
    return dbAccess_;
  }

  public Thumbnailer getThumbnailer() {
    return thumbnailer_;
  }

  public ImportController getImportCtrl() {
    return importCtrl_;
  }

  public MainFrame getMainFrame() {
    return mainFrame_;
  }

  public Prefs getPrefs() {
    return prefs_;
  }

  public void run() {
    mainFrame_.show();
  }

}
