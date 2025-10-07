package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.action.ActionState.DISABLED;
import static com.bensler.decaf.swing.action.ActionState.ENABLED;
import static com.bensler.decaf.swing.action.ActionState.HIDDEN;
import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.decaf.util.prefs.DelegatingPrefPersister.createSplitPanePrefPersister;
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

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import com.bensler.decaf.swing.action.ActionAppearance;
import com.bensler.decaf.swing.action.ActionGroup;
import com.bensler.decaf.swing.action.ActionState;
import com.bensler.decaf.swing.action.EntityAction;
import com.bensler.decaf.swing.action.FocusedComponentActionController;
import com.bensler.decaf.swing.action.SingleEntityActionAdapter;
import com.bensler.decaf.swing.action.SingleEntityFilter;
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
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class MainFrame {

  public static final ImageIcon ICON_IMAGE_13 = new ImageIcon(MainFrame.class.getResource("image_13x13.png"));
  public static final ImageIcon ICON_IMAGE_48 = new ImageIcon(MainFrame.class.getResource("image_48x48.png"));

  public static final ImageIcon ICON_IMAGES_48 = new ImageIcon(MainFrame.class.getResource("images_48x48.png"));

  public static final ImageIcon ICON_SLIDESHOW_13 = new ImageIcon(MainFrame.class.getResource("slideshow_13x13.png"));
  public static final ImageIcon ICON_SLIDESHOW_48 = new ImageIcon(MainFrame.class.getResource("slideshow_48x48.png"));

  public static final ImageIcon ICON_TAG_SIMPLE_13 = new ImageIcon(MainFrame.class.getResource("tag-simple_13x13.png"));
  public static final ImageIcon ICON_TAG_48 = new ImageIcon(MainFrame.class.getResource("tag_48x48.png"));

  public static final ImageIcon ICON_TAGS_36 = new ImageIcon(MainFrame.class.getResource("tags_36x36.png"));
  public static final ImageIcon ICON_TAGS_48 = new ImageIcon(MainFrame.class.getResource("tags_48x48.png"));

  public static final ImageIcon ICON_TIMELINE_13 = new ImageIcon(MainFrame.class.getResource("calendar_13x13.png"));
  public static final ImageIcon ICON_TIMELINE_48 = new ImageIcon(MainFrame.class.getResource("calendar_48x48.png"));

  public static final ImageIcon ICON_EDIT_13 = new ImageIcon(MainFrame.class.getResource("edit_13x13.png"));
  public static final ImageIcon ICON_EDIT_30 = new ImageIcon(MainFrame.class.getResource("edit_30x30.png"));

  public static final ImageIcon ICON_PLUS_10 = new ImageIcon(MainFrame.class.getResource("plus_10x10.png"));
  public static final ImageIcon ICON_PLUS_20 = new ImageIcon(MainFrame.class.getResource("plus_20x20.png"));
  public static final ImageIcon ICON_PLUS_30 = new ImageIcon(MainFrame.class.getResource("plus_30x30.png"));

  public static final ImageIcon ICON_X_10 = new ImageIcon(MainFrame.class.getResource("x_10x10.png"));
  public static final ImageIcon ICON_X_30 = new ImageIcon(MainFrame.class.getResource("x_30x30.png"));

  public static final PropertyViewImpl<Blob, Integer> BLOB_ID_VIEW = new PropertyViewImpl<>(
    SimplePropertyGetter.createComparableGetter(Blob::getId)
  );

  private final App app_;
  private final JFrame frame_;
  private final PrefPersisterImpl prefs_;
  private final EntityTree<Tag> tagTree_;
  private final MainThumbnailOverview thumbnails_;
  private final TagController tagCtrl_;
  private final EntityChangeListenerTreeAdapter treeAdapter_; // save it from GC
  private final FocusedComponentActionController actionCtrl_; // save it from GC
  private SlideshowFrame slideshowFrame_;

  public MainFrame(App app) {
    app_ = app;
    tagCtrl_ = app_.getTagCtrl();
    frame_ = new JFrame("Taggy");
    frame_.setIconImages(List.of(createImage()));
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p, 3dlu, f:p:g, 3dlu, f:p, 3dlu"
    ));
    final EntityAction<Tag> editTagAction = new EntityAction<>(
      new ActionAppearance(new OverlayIcon(ICON_TAG_SIMPLE_13, new Overlay(ICON_EDIT_13, SE)), TagDialog.Edit.ICON, "Edit Tag", "Edit currently selected Tag"),
      Tag.class, TagUi.TAG_FILTER, new SingleEntityActionAdapter<>((source, tag) -> tag.ifPresent(this::editTagUi))
    );
    final SingleEntityFilter<Tag> tagFilter = new SingleEntityFilter<>(HIDDEN, TagUi.TAG_FILTER) {
      @Override
      public ActionState getActionState(List<Tag> entities) {
        return (tagTree_.getSelection().isEmpty() ? ENABLED : super.getActionState(entities));
      }
    };
    final EntityAction<Tag> newTagAction = new EntityAction<>(
      new ActionAppearance(new OverlayIcon(ICON_TAG_SIMPLE_13, new Overlay(ICON_PLUS_10, SE)), TagDialog.Create.ICON, "Create Tag", "Creates a new Tag under the currently selected Tag"),
      Tag.class, tagFilter, new SingleEntityActionAdapter<>((source, tag) -> createTagUi(tag))
    );
    final EntityAction<Tag> newTimelineTagAction = new EntityAction<>(
      new ActionAppearance(new OverlayIcon(ICON_TIMELINE_13, new Overlay(ICON_PLUS_10, SE)), null, "Create Timeline Tag", "Creates a new Tag representing a calendar date"),
      Tag.class, TagUi.TIMELINE_TAG_FILTER, new SingleEntityActionAdapter<>((source, tag) -> createTimelineUi())
    );
    final EntityAction<Tag> deleteTagAction = new EntityAction<>(
      new ActionAppearance(new OverlayIcon(ICON_TAG_SIMPLE_13, new Overlay(ICON_X_10, SE)), null, "Delete Tag", "Remove currently selected Tag"),
      Tag.class, new SingleEntityFilter<>(HIDDEN, tag -> tagCtrl_.isLeaf(tag) ? ENABLED : DISABLED),
      new SingleEntityActionAdapter<>((source, tag) -> tag.ifPresent(this::deleteTagUi))
    );

    final SelectedBlobsDetailPanel selectionTagPanel = new SelectedBlobsDetailPanel(this);
    (thumbnails_ = new MainThumbnailOverview(app_)).addSelectionListener((source, selection) -> selectionTagPanel.setData(selection));
    tagTree_ = new EntityTree<>(TagUi.NAME_VIEW, Tag.class);
    tagTree_.setVisibleRowCount(20, .5f);
    tagTree_.addSelectionListener((source, selection) -> displayThumbnailsOfSelectedTag(selection));
    tagTree_.setCtxActions(new FocusedComponentActionController(
      new ActionGroup(editTagAction, newTagAction, newTimelineTagAction, deleteTagAction), Set.of(tagTree_)
    ));
    app_.addEntityChangeListener(treeAdapter_ = new EntityChangeListenerTreeAdapter(tagTree_), Tag.class);
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
      new ActionGroup(app_.getImportCtrl().getImportAction()),
      new ActionGroup(newTagAction, editTagAction),
      new ActionGroup(
        new ActionAppearance(new OverlayIcon(ICON_IMAGES_48, new Overlay(ICON_EDIT_30, SE)), null, null, "Edit Images"),
        thumbnails_.getSlideshowAction()
      ),
      thumbnails_.getToolbarActions(),
      new ActionGroup(thumbnails_.getSlideshowAction())
    ), List.of(tagTree_, thumbnails_, selectionTagPanel.getTagTree()))).createToolbar(), new CellConstraints(2, 2));

    mainPanel.setPreferredSize(new Dimension(750, 750));
    frame_.setContentPane(mainPanel);
    frame_.pack();
    new WindowClosingTrigger(frame_, evt -> frameClosing());

    final PrefKey baseKey = new PrefKey(App.PREFS_APP_ROOT, getClass());
    prefs_ = new PrefPersisterImpl(app_.getPrefs(),
      new WindowPrefsPersister(baseKey, frame_),
      TagPrefPersister.create(new PrefKey(baseKey, "selectedTag"), tagTree_::getSingleSelection, tagTree_::select),
      createSplitPanePrefPersister(new PrefKey(baseKey, "splitLeft"), leftSplitpane),
      createSplitPanePrefPersister(new PrefKey(baseKey, "splitRight"), rightSplitpane),
      selectionTagPanel.createPrefPersister(new PrefKey(baseKey, "selectionTagPanel"))
    ) {
      @Override
      public void apply() {
        SwingUtilities.invokeLater(super::apply);
      }
    };
  }

  private Image createImage() {
    final Icon icon = new OverlayIcon(ICON_IMAGE_48, new Overlay(ICON_TAGS_36, SE));
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
    new OkCancelDialog<>(frame_, new TagDialog.Create(tagTree_.getData())).show(
      parentTag, newTag -> tagTree_.select(tagCtrl_.persistNewTag(newTag))
    );
  }

  void createTimelineUi() {
    new OkCancelDialog<>(frame_, new CreateTimelineTagDialog(tagCtrl_)).show(
      null, newTag -> tagTree_.select(tagCtrl_.persistNewTag(newTag))
    );
  }

  void editTagUi(Tag tag) {
    new OkCancelDialog<>(frame_, new TagDialog.Edit(tagTree_.getData())).show(
      tag, tagHeadData -> tagTree_.select(tagCtrl_.updateTag(tagHeadData))
    );
  }

  void deleteTagUi(Tag tag) {
    if (new ConfirmationDialog(new DialogAppearance(
      new OverlayIcon(ICON_TAG_48, new Overlay(ICON_X_30, SE)), "Confirmation: Delete Tag",
      "Do you really want to delete Tag \"%s\" under \"%s\"?".formatted(
        tag.getName(), Optional.ofNullable(tag.getParent()).map(Tag::getName).orElse("Root")
      )
    )).show(frame_)) {
      final TreePath parentPath = tagTree_.getModel().getTreePath(tag).getParentPath();

      tagCtrl_.deleteTag(tag);
      if (parentPath.getPathCount() > 1) {
        tagTree_.select((Tag)parentPath.getLastPathComponent());
      } else {
        tagTree_.select(List.of());
      }
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
