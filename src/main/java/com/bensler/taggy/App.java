package com.bensler.taggy;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.UIManager;

import com.bensler.decaf.util.entity.Entity;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefsStorage;
import com.bensler.taggy.imprt.ImportController;
import com.bensler.taggy.imprt.Thumbnailer;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.DbConnector;
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
  private final PrefsStorage prefs_;
  private final BlobController blobCtrl_;
  private final TagController tagCtrl_;
  private final DbAccess dbAccess_;
  private final ImportController importCtrl_;
  private final Thumbnailer thumbnailer_;
  private final MainFrame mainFrame_;

  private final Map<Class<?>, Map<EntityChangeListener<?>, Object>> entityChangeListeners_;

  private App() throws Exception {
    Plastic3DLookAndFeel.setCurrentTheme(new DesertYellow());
    UIManager.setLookAndFeel(new Plastic3DLookAndFeel());

    final File dataDir = getDataDir();
    entityChangeListeners_ = new HashMap<>();
    db_ = new SqliteDbConnector(dataDir, "taggy.sqlite.db");
    db_.performFlywayMigration();
    dbAccess_ = new DbAccess(db_.getSession());
    prefs_ = new PrefsStorage(new File(getBaseDir(), "prefs.xml"));
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

  public JFrame getMainFrameFrame() {
    return mainFrame_.getFrame();
  }

  public PrefsStorage getPrefs() {
    return prefs_;
  }

  public void run() {
    mainFrame_.show();
  }

  public <E> void addEntityChangeListener(EntityChangeListener<E> listener, Class<E> clazz) {
    entityChangeListeners_.computeIfAbsent(clazz, key -> new WeakHashMap<>()).put(listener, null);
  }

  public void entitiesCreated(Collection<?> entities) {
    entities.forEach(this::entityCreated);
  }

  public void entityCreated(Object entity) {
    fireEvent(entity, listener -> listener.entityCreated(entity));
  }

  void fireEvent(Object entity, Consumer<EntityChangeListener<Object>> fireEventFunction) {
    Optional.ofNullable(entityChangeListeners_.get(entity.getClass())).ifPresent(listeners -> listeners.keySet().forEach(listener -> {
      try {
        fireEventFunction.accept((EntityChangeListener<Object>)listener);
      } catch (RuntimeException re) {
        // TODO proper exception handling
        re.printStackTrace();
      }
    }));
  }

  public void entitiesChanged(Collection<?> entities) {
    entities.forEach(this::entityChanged);
  }

  public void entityChanged(Object entity) {
    fireEvent(entity, listener -> listener.entityChanged(entity));
  }

  public void entitiesRemoved(Collection<?> entities) {
    entities.forEach(this::entityRemoved);
  }

  public void entityRemoved(Object entity) {
    fireEvent(entity, listener -> listener.entityRemoved(entity));
  }

  public <E extends Entity<E>> E storeEntity(E entity) {
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
