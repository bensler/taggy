package com.bensler.taggy;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.UIManager;

import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.Prefs;
import com.bensler.taggy.imprt.ImportController;
import com.bensler.taggy.imprt.Thumbnailer;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.DbConnector;
import com.bensler.taggy.persist.Entity;
import com.bensler.taggy.persist.SqliteDbConnector;
import com.bensler.taggy.ui.BlobController;
import com.bensler.taggy.ui.MainFrame;
import com.bensler.taggy.ui.TagController;
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
  private final TagController tagCtrl_;
  private final DbAccess dbAccess_;
  private final ImportController importCtrl_;
  private final Thumbnailer thumbnailer_;
  private final MainFrame mainFrame_;

  private final Map<EntityChangeListener, Object> entityChangeListeners_;

  private App() throws Exception {
    Plastic3DLookAndFeel.setCurrentTheme(new DesertYellow());
    UIManager.setLookAndFeel(new Plastic3DLookAndFeel());

    final File dataDir = getDataDir();
    entityChangeListeners_ = new WeakHashMap<>();
    db_ = new SqliteDbConnector(dataDir, "taggy.sqlite.db");
    db_.performFlywayMigration();
    dbAccess_ = new DbAccess(db_.getSession());
    prefs_ = new Prefs(new File(getBaseDir(), "prefs.xml"));
    blobCtrl_ = new BlobController(dataDir, FOLDER_PATTERN);
    tagCtrl_ = new TagController(this);
    importCtrl_ = new ImportController(this, getBaseDir());
    thumbnailer_ = new Thumbnailer(dataDir);
    mainFrame_ = new MainFrame(this);
  }

  public BlobController getBlobCtrl() {
    return blobCtrl_;
  }

  public TagController getTagCtrl() {
    return tagCtrl_;
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

  public void addEntityChangeListener(EntityChangeListener entityChangeListener) {
    entityChangeListeners_.put(entityChangeListener, null);
  }

  public void entitiesCreated(Collection<? extends Entity> entities) {
    entities.forEach(this::entityCreated);
  }

  public void entityCreated(Entity entity) {
    entityChangeListeners_.keySet().forEach(listener -> {
      try {
        listener.entityCreated(entity);
      } catch (RuntimeException re) {
        // TODO proper exception handling
        re.printStackTrace();
      }
    });
  }

  public void entitiesChanged(Collection<? extends Entity> entities) {
    entities.forEach(this::entityChanged);
  }

  public void entityChanged(Entity entity) {
    entityChangeListeners_.keySet().forEach(listener -> {
      try {
        listener.entityChanged(entity);
      } catch (RuntimeException re) {
        // TODO proper exception handling
        re.printStackTrace();
      }
    });
  }

  public void entitiesRemoved(Collection<? extends Entity> entities) {
    entities.forEach(this::entityRemoved);
  }

  public void entityRemoved(Entity entity) {
    entityChangeListeners_.keySet().forEach(listener -> {
      try {
        listener.entityRemoved(entity);
      } catch (RuntimeException re) {
        // TODO proper exception handling
        re.printStackTrace();
      }
    });
  }

  public <E extends Entity> E storeEntity(E entity) {
    final boolean isNew = !entity.hasId();

    entity = dbAccess_.storeObject(entity);
    if (isNew) {
      entityCreated(entity);
    } else {
      entityChanged(entity);
    }
    return entity;
  }

}
