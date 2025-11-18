package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.decaf.util.prefs.DelegatingPrefPersister.createSplitPanePrefPersister;
import static com.bensler.taggy.ui.Icons.EDIT_13;
import static com.bensler.taggy.ui.Icons.EDIT_30;
import static com.bensler.taggy.ui.Icons.IMAGES_48;
import static com.bensler.taggy.ui.Icons.IMAGE_48;
import static com.bensler.taggy.ui.Icons.PLUS_10;
import static com.bensler.taggy.ui.Icons.TAGS_36;
import static com.bensler.taggy.ui.Icons.TAG_48;
import static com.bensler.taggy.ui.Icons.TAG_SIMPLE_13;
import static com.bensler.taggy.ui.Icons.TIMELINE_13;
import static com.bensler.taggy.ui.Icons.X_10;
import static com.bensler.taggy.ui.Icons.X_30;
import static com.jgoodies.forms.layout.CellConstraints.CENTER;
import static com.jgoodies.forms.layout.CellConstraints.FILL;
import static com.jgoodies.forms.layout.CellConstraints.RIGHT;
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
import javax.swing.JButton;
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
import com.bensler.decaf.swing.dialog.ConfirmationDialog;
import com.bensler.decaf.swing.dialog.DialogAppearance;
import com.bensler.decaf.swing.dialog.OkCancelDialog;
import com.bensler.decaf.swing.dialog.WindowClosingTrigger;
import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.swing.view.PropertyViewImpl;
import com.bensler.decaf.swing.view.SimplePropertyGetter;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersisterImpl;
import com.bensler.decaf.util.prefs.PrefsStorage;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class MainFrame {

  public static final PropertyViewImpl<Blob, Integer> BLOB_ID_VIEW = new PropertyViewImpl<>(
    SimplePropertyGetter.createComparableGetter(Blob::getId)
  );

  private final App app_;
  private final JFrame frame_;
  private final PrefPersisterImpl prefs_;
  private final EntityTree<Tag> tagTree_;
  private final MainThumbnailOverview thumbnails_;
  private final TagController tagCtrl_;
  @SuppressWarnings("unused") // save it from GC
  private final EntityChangeListenerTreeAdapter<Tag> treeAdapter_;
  @SuppressWarnings("unused") // save it from GC
  private final FocusedComponentActionController actionCtrl_;
  private SlideshowFrame slideshowFrame_;

  public MainFrame(App app) {
    final PrefKey baseKey = new PrefKey(App.PREFS_APP_ROOT, getClass());
    app_ = app;
    tagCtrl_ = app_.getTagCtrl();
    frame_ = new JFrame("Taggy");
    frame_.setIconImages(List.of(createImage()));
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p, 3dlu, f:p:g, 3dlu, f:p, 3dlu"
    ));
    final UiAction editTagAction = new UiAction(
      new ActionAppearance(new OverlayIcon(TAG_SIMPLE_13, new Overlay(EDIT_13, SE)), TagDialog.Edit.ICON, "Edit Tag", "Edit currently selected Tag"),
      FilteredAction.one(Tag.class, TagUi.TAG_FILTER, this::editTagUi)
    );
    final UiAction newTagAction = new UiAction(
      new ActionAppearance(new OverlayIcon(TAG_SIMPLE_13, new Overlay(PLUS_10, SE)), TagDialog.Create.ICON, "Create Tag", "Creates a new Tag under the currently selected Tag"),
      FilteredAction.oneOrNone(Tag.class, TagUi.TAG_FILTER, this::createTagUi)
    );
    final UiAction newTimelineTagAction = new UiAction(
      new ActionAppearance(new OverlayIcon(TIMELINE_13, new Overlay(PLUS_10, SE)), null, "Create Timeline Tag", "Creates a new Tag representing a calendar date"),
      FilteredAction.one(Tag.class, TagUi.TIMELINE_TAG_FILTER, tag -> createTimelineUi())
    );
    final UiAction deleteTagAction = new UiAction(
      new ActionAppearance(new OverlayIcon(TAG_SIMPLE_13, new Overlay(X_10, SE)), null, "Delete Tag", "Remove currently selected Tag"),
      FilteredAction.one(Tag.class, tagCtrl_::isLeaf, this::deleteTagUi)
    );

    final SelectedBlobsDetailPanel selectionTagPanel = new SelectedBlobsDetailPanel(this);
    (thumbnails_ = new MainThumbnailOverview(app_, baseKey)).addSelectionListener((source, selection) -> selectionTagPanel.setData(selection));
    tagTree_ = new EntityTree<>(TagUi.NAME_VIEW, Tag.class);
    tagTree_.setVisibleRowCount(20, .5f);
    tagTree_.addSelectionListener((source, selection) -> displayThumbnailsOfSelectedTag(selection));
    tagTree_.setCtxActions(new FocusedComponentActionController(
      new ActionGroup(editTagAction, newTagAction, newTimelineTagAction, deleteTagAction), Set.of(tagTree_)
    ));
    app_.addEntityChangeListener(treeAdapter_ = new EntityChangeListenerTreeAdapter<>(tagTree_), Tag.class);
    frame_.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    tagCtrl_.setAllTags(tagTree_);
    final JSplitPane leftSplitpane = new JSplitPane(HORIZONTAL_SPLIT, true,
      tagTree_.getScrollPane(), thumbnails_.getScrollPane()
    );
    final JSplitPane rightSplitpane = new JSplitPane(HORIZONTAL_SPLIT, true,
      leftSplitpane, selectionTagPanel.getComponent()
    );
    rightSplitpane.setResizeWeight(1);
    mainPanel.add(rightSplitpane, new CellConstraints(2, 4));

    final JPanel buttonPanel = new JPanel(new FormLayout("f:p:g", "f:p:g"));
    mainPanel.add(buttonPanel, new CellConstraints(2, 6, RIGHT, CENTER));
    final JButton orphanDialogButton = new JButton("Orphan Files");
    orphanDialogButton.addActionListener(evt -> new OrphanDialog(app_).showDialog());
    buttonPanel.add(orphanDialogButton, new CellConstraints(1, 1, FILL, FILL));

    mainPanel.add((actionCtrl_ = new FocusedComponentActionController(new ActionGroup(
      new ActionGroup(newTagAction, editTagAction),
      new ActionGroup(
        app_.getImportCtrl().getImportAction(),
        thumbnails_.getExportImageAction()
      ),
      new ActionGroup(
        new ActionAppearance(new OverlayIcon(IMAGES_48, new Overlay(EDIT_30, SE)), null, null, "Edit Images"),
        thumbnails_.getSlideshowAction(),
        thumbnails_.getEditImageTagsAction()
      ),
      thumbnails_.getToolbarActions(),
      new ActionGroup(thumbnails_.getSlideshowAction())
    ), List.of(tagTree_, thumbnails_, selectionTagPanel.getTagTree()))).createToolbar(), new CellConstraints(2, 2));

    mainPanel.setPreferredSize(new Dimension(750, 750));
    frame_.setContentPane(mainPanel);
    frame_.pack();
    new WindowClosingTrigger(frame_, evt -> frameClosing());
    prefs_ = new PrefPersisterImpl(app_.getPrefs(), Stream.concat(
      thumbnails_.getPrefPersisters().stream(), Stream.of(
        new WindowPrefsPersister(baseKey, frame_),
        TagPrefPersister.create(new PrefKey(baseKey, "selectedTag"), tagTree_::getSingleSelection, tagTree_::select),
        createSplitPanePrefPersister(new PrefKey(baseKey, "splitLeft"), leftSplitpane),
        createSplitPanePrefPersister(new PrefKey(baseKey, "splitRight"), rightSplitpane),
        selectionTagPanel.createPrefPersister(new PrefKey(baseKey, "selectionTagPanel"))
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
    prefs_.store();
    try {
      app_.getPrefs().store();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  void createTagUi(Optional<Tag> parentTag) {
    new OkCancelDialog<>(frame_, new TagDialog.Create(tagCtrl_.getAllTags())).show(
      parentTag, newTag -> tagCtrl_.persistNewTag(newTag)
    );
  }

  void createTimelineUi() {
    new OkCancelDialog<>(frame_, new CreateTimelineTagDialog(tagCtrl_)).show(
      null, newTag -> tagCtrl_.persistNewTag(newTag)
    );
  }

  void editTagUi(Tag tag) {
    new OkCancelDialog<>(frame_, new TagDialog.Edit(tagCtrl_.getAllTags())).show(
      tag, tagHeadData -> tagCtrl_.updateTag(tagHeadData)
    );
  }

  void deleteTagUi(Tag tag) {
    if (new ConfirmationDialog(new DialogAppearance(
      new OverlayIcon(TAG_48, new Overlay(X_30, SE)), "Confirmation: Delete Tag",
      "Do you really want to delete Tag \"%s\" under \"%s\"?".formatted(
        tag.getName(), Optional.ofNullable(tag.getParent()).map(Tag::getName).orElse("Root")
      )
    )).show(frame_)) {
//      final TreePath parentPath = tagTree_.getModel().getTreePath(tag).getParentPath();

      tagCtrl_.deleteTag(tag);
//      if (parentPath.getPathCount() > 1) {
//        tagTree_.select(parentPath.getLastPathComponent());
//      } else {
//        tagTree_.select(List.of());
//      }
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
