package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.decaf.util.prefs.DelegatingPrefPersister.createSplitPanePrefPersister;
import static com.bensler.taggy.App.getApp;
import static com.bensler.taggy.ui.Icons.IMAGE_48;
import static com.bensler.taggy.ui.Icons.TAGS_36;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static javax.swing.JSplitPane.HORIZONTAL_SPLIT;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import com.bensler.decaf.swing.EntityComponent;
import com.bensler.decaf.swing.action.ActionAppearance;
import com.bensler.decaf.swing.action.ActionGroup;
import com.bensler.decaf.swing.action.FilteredAction;
import com.bensler.decaf.swing.action.FocusedComponentActionController;
import com.bensler.decaf.swing.action.UiAction;
import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.decaf.swing.dialog.WindowClosingTrigger;
import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersisterImpl;
import com.bensler.decaf.util.prefs.PrefsStorage;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class MainFrame {

  public static final PrefKey PREF_BASE_KEY = new PrefKey(App.PREFS_APP_ROOT, MainFrame.class);

  private final JFrame frame_;
  private final PrefPersisterImpl prefs_;
  private final EntityTree<Tag> tagTree_;
  private final MainThumbnailPanel thumbnails_;
  private final TagsUiController tagCtrl_;
  private SlideshowFrame slideshowFrame_;

  public MainFrame(App app) {
    tagCtrl_ = app.getTagCtrl();
    frame_ = new JFrame("Taggy");
    frame_.setIconImages(List.of(createImage()));
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p, 3dlu, f:p:g, 3dlu"
    ));

    final SelectedBlobsDetailPanel selectionTagPanel = new SelectedBlobsDetailPanel(this);
    (thumbnails_ = new MainThumbnailPanel(app)).addSelectionListener(selectionTagPanel::setData);
    final ImagesUiController imagesUiCtrl = thumbnails_.getImagesUiCtrl();
    tagTree_ = new EntityTree<>(TagUi.NAME_VIEW, Tag.class);
    tagTree_.setVisibleRowCount(20, .5f);
    tagTree_.addSelectionListener((source, selection) -> displayThumbnailsOfSelectedTag(selection));
    tagTree_.setCtxActions(new FocusedComponentActionController(tagCtrl_.getAllTagActions(), Set.of(tagTree_)));
    app.addEntityChangeListener(app.putZombie(this, new EntityChangeListenerTreeAdapter<>(tagTree_)), Tag.class);
    frame_.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    final AllTagsTreeFiltered filteredTagsTree = new AllTagsTreeFiltered(app, tagTree_);
    final JSplitPane leftSplitpane = new JSplitPane(HORIZONTAL_SPLIT, true,
      filteredTagsTree.getComponent(), thumbnails_
    );
    final JSplitPane rightSplitpane = new JSplitPane(HORIZONTAL_SPLIT, true,
      leftSplitpane, selectionTagPanel.getComponent()
    );
    rightSplitpane.setResizeWeight(1);
    mainPanel.add(rightSplitpane, new CellConstraints(2, 4));

    final UiAction orphanAction = new UiAction(
      new ActionAppearance(OrphanDialog.ICON_SMALL, OrphanDialog.ICON_LARGE, "Untagged Images", "Show Images without any Tags assigned"),
      FilteredAction.allwaysOn(() -> new OrphanDialog(app).showDialog())
    );
    final List<EntityComponent<?>> entityVompoenents = List.of(tagTree_, thumbnails_.getEntityComponent(), selectionTagPanel.getTagTree());

    frame_.setJMenuBar(createActionController(app, createMenuActionGroup(app, imagesUiCtrl, orphanAction), entityVompoenents).createMenuBar());
    mainPanel.add(createActionController(app, createToolbarActionGroup(app, imagesUiCtrl, orphanAction), entityVompoenents).createToolbar(), new CellConstraints(2, 2));

    mainPanel.setPreferredSize(new Dimension(750, 750));
    frame_.setContentPane(mainPanel);
    frame_.pack();
    new WindowClosingTrigger(frame_, evt -> frameClosing());
    prefs_ = new PrefPersisterImpl(app.getPrefs(), Stream.concat(
      thumbnails_.getPrefPersisters().stream(),
      Stream.of(
        new WindowPrefsPersister(PREF_BASE_KEY, frame_),
        TagPrefPersister.create(new PrefKey(PREF_BASE_KEY, "selectedTag"), tagTree_::getSingleSelection, tagTree_::select),
        createSplitPanePrefPersister(new PrefKey(PREF_BASE_KEY, "splitLeft"), leftSplitpane),
        createSplitPanePrefPersister(new PrefKey(PREF_BASE_KEY, "splitRight"), rightSplitpane),
        selectionTagPanel.createPrefPersister(new PrefKey(PREF_BASE_KEY, "selectionTagPanel"))
      )
    ).toList()) {
      @Override
      public void apply() {
        SwingUtilities.invokeLater(super::apply);
      }
    };
  }

  private ActionGroup createMenuActionGroup(App app, ImagesUiController imagesUiCtrl, UiAction orphanAction) {
    return new ActionGroup(
      new ActionGroup(new ActionAppearance(Icons.TAG_SIMPLE_13, null, "Tag", null), tagCtrl_.getNewTagAction(), tagCtrl_.getEditTagAction()),
      new ActionGroup(new ActionAppearance(Icons.IMAGE_13, null, "Image", null),
        imagesUiCtrl.getSlideshowAction(),
        imagesUiCtrl.getTagsActions(),
        imagesUiCtrl.getEditImageActions(),
        imagesUiCtrl.getExportImageAction(),
        new ActionGroup(
          app.getImportCtrl().getImportAction(),
          orphanAction
        )
      )
    );
  }

  private ActionGroup createToolbarActionGroup(App app, ImagesUiController imagesUiCtrl, UiAction orphanAction) {
    return new ActionGroup(
      new ActionGroup(tagCtrl_.getNewTagAction(), tagCtrl_.getEditTagAction()),
      new ActionGroup(
        app.getImportCtrl().getImportAction(),
        orphanAction,
        imagesUiCtrl.getExportImageAction()
      ),
      imagesUiCtrl.getEditImageActions(),
      imagesUiCtrl.getTagsActions(),
      new ActionGroup(imagesUiCtrl.getSlideshowAction())
    );
  }

  FocusedComponentActionController createActionController(App app, ActionGroup actions, Collection<EntityComponent<?>> entityComponents) {
    return app.putZombie(this,  new FocusedComponentActionController(actions, entityComponents));
  }

  private Image createImage() {
    final Icon icon = new OverlayIcon(IMAGE_48, new Overlay(TAGS_36, SE));
    final BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), TYPE_INT_ARGB);

    icon.paintIcon(frame_, image.getGraphics(), 0, 0);
    return image;
  }

  private void frameClosing() {
    final App app = getApp();

    if (slideshowFrame_ != null) {
      slideshowFrame_.close();
    }
    app.getResizeThread().terminate();
    prefs_.store();
    try {
      app.getPrefs().store();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  void selectTag(Tag tag) {
    tagTree_.select(tag);
  }

  public void show() {
    frame_.setVisible(true);
  }

  public JFrame getFrame() {
    return frame_;
  }

  public PrefsStorage getPrefStorage() {
    return prefs_.getStorage();
  }

  public SlideshowFrame getSlideshowFrame() {
    if (slideshowFrame_ == null) {
      slideshowFrame_ = new SlideshowFrame(getApp());
    }
    return slideshowFrame_;
  }

  void tagChanged(Tag tag) {
    if (tag.equals(tagTree_.getSingleSelection())) {
      displayThumbnailsOfSelectedTag(List.of(tag));
    }
  }

  void displayThumbnailsOfSelectedTag(List<Tag> selection) {
    thumbnails_.setData(Optional.ofNullable((selection.isEmpty() ? null : selection.get(0))));
  }

}
