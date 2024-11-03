package com.bensler.taggy;

import java.io.File;
import java.util.Arrays;

import javax.swing.UIManager;

import org.hibernate.Session;

import com.bensler.taggy.persist.SqliteDbConnector;
import com.bensler.taggy.ui.BlobController;
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

  public static File getDataDir() {
    return new File(new File(System.getProperty("user.home"), ".taggy"), "data");
  }

  private final BlobController blobController_;
  private final Thumbnailer thumbnailer_;
  private final Session session_;

  private Main() throws Exception {
    final SqliteDbConnector dbConnector;

    Plastic3DLookAndFeel.setCurrentTheme(new DesertYellow());
    UIManager.setLookAndFeel(new Plastic3DLookAndFeel());

    final File dataDir = getDataDir();
    dbConnector = new SqliteDbConnector(dataDir, "taggy.sqlite.db");
    dbConnector.performFlywayMigration();
    blobController_ = new BlobController(dataDir, FOLDER_PATTERN);
    thumbnailer_ = new Thumbnailer(dataDir);
    session_ = dbConnector.getSession();
  }

  public void run() {
    new MainFrame(blobController_, session_, thumbnailer_).show();
  }

}
