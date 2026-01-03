package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.decaf.util.prefs.DelegatingPrefPersister.createSplitPanePrefPersister;
import static com.bensler.taggy.ui.Icons.EDIT_30;
import static com.bensler.taggy.ui.Icons.IMAGES_48;
import static com.bensler.taggy.ui.Icons.IMAGE_48;
import static com.bensler.taggy.ui.Icons.TAGS_36;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static javax.swing.JSplitPane.HORIZONTAL_SPLIT;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

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
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class MainFrame {

  public static final PrefKey PREF_BASE_KEY = new PrefKey(App.PREFS_APP_ROOT, MainFrame.class);

  private final App app_;
  private final JFrame frame_;
  private final PrefPersisterImpl prefs_;
  private final EntityTree<Tag> tagTree_;
  private final MainThumbnailPanel thumbnails_;
  private final TagsUiController tagCtrl_;
  @SuppressWarnings("unused") // save it from GC
  private final EntityChangeListenerTreeAdapter<Tag> treeAdapter_;
  @SuppressWarnings("unused") // save it from GC
  private final FocusedComponentActionController actionCtrl_;
  private SlideshowFrame slideshowFrame_;

  public MainFrame(App app) {
    app_ = app;
    tagCtrl_ = app_.getTagCtrl();
    frame_ = new JFrame("Taggy");
    frame_.setIconImages(List.of(createImage()));
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p, 3dlu, f:p:g, 3dlu"
    ));

    final SelectedBlobsDetailPanel selectionTagPanel = new SelectedBlobsDetailPanel(this);
    (thumbnails_ = new MainThumbnailPanel(app_)).addSelectionListener(selectionTagPanel::setData);
    tagTree_ = new EntityTree<>(TagUi.NAME_VIEW, Tag.class);
    tagTree_.setVisibleRowCount(20, .5f);
    tagTree_.addSelectionListener((source, selection) -> displayThumbnailsOfSelectedTag(selection));
    tagTree_.setCtxActions(new FocusedComponentActionController(tagCtrl_.getAllTagActions(), Set.of(tagTree_)));
    app_.addEntityChangeListener(treeAdapter_ = new EntityChangeListenerTreeAdapter<>(tagTree_), Tag.class);
    frame_.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    tagCtrl_.setAllTags(tagTree_);
    final JSplitPane leftSplitpane = new JSplitPane(HORIZONTAL_SPLIT, true,
      tagTree_.getScrollPane(), thumbnails_
    );
    final JSplitPane rightSplitpane = new JSplitPane(HORIZONTAL_SPLIT, true,
      leftSplitpane, selectionTagPanel.getComponent()
    );
    rightSplitpane.setResizeWeight(1);
    mainPanel.add(rightSplitpane, new CellConstraints(2, 4));

    mainPanel.add((actionCtrl_ = new FocusedComponentActionController(new ActionGroup(
      new ActionGroup(tagCtrl_.getNewTagAction(), tagCtrl_.getEditTagAction()),
      new ActionGroup(
        app_.getImportCtrl().getImportAction(),
        new UiAction(
          new ActionAppearance(OrphanDialog.ICON, null, null, "Show Images without any Tags assigned"),
          FilteredAction.many(Void.class, FilteredAction.allwaysOnFilter(), entities -> new OrphanDialog(app_).showDialog())
        ),
        thumbnails_.getExportImageAction()
      ),
      new ActionGroup(
        new ActionAppearance(new OverlayIcon(IMAGES_48, new Overlay(EDIT_30, SE)), null, null, "Edit Images"),
        new UiAction(
          new ActionAppearance(Icons.ROTATE_R_13, null, "Rotate Clockwise", null),
          FilteredAction.one(Blob.class, blob -> {})
        ),
        new UiAction(
          new ActionAppearance(Icons.ROTATE_L_13, null, "Rotate Counterclockwise", null),
          FilteredAction.one(Blob.class, blob -> {})
        )
      ),
      thumbnails_.getToolbarActions(),
      new ActionGroup(thumbnails_.getSlideshowAction())
    ), List.of(tagTree_, thumbnails_.getEntityComponent(), selectionTagPanel.getTagTree()))).createToolbar(), new CellConstraints(2, 2));

    mainPanel.setPreferredSize(new Dimension(750, 750));
    frame_.setContentPane(mainPanel);
    frame_.pack();
    new WindowClosingTrigger(frame_, evt -> frameClosing());
    prefs_ = new PrefPersisterImpl(app_.getPrefs(), Stream.concat(
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

  private Image createImage() {
    final Icon icon = new OverlayIcon(IMAGE_48, new Overlay(TAGS_36, SE));
    final BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), TYPE_INT_ARGB);

    icon.paintIcon(frame_, image.getGraphics(), 0, 0);
    return image;
  }

  private void frameClosing() {
    if (slideshowFrame_ != null) {
      slideshowFrame_.close();
    }
    app_.getResizeThread().terminate();
    prefs_.store();
    try {
      app_.getPrefs().store();
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
      slideshowFrame_ = new SlideshowFrame(app_);
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
