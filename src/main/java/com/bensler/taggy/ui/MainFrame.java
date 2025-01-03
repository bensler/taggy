package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.decaf.swing.view.SimplePropertyGetter.createComparablePropertyGetter;
import static com.bensler.decaf.swing.view.SimplePropertyGetter.createStringPropertyGetter;
import static com.jgoodies.forms.layout.CellConstraints.CENTER;
import static com.jgoodies.forms.layout.CellConstraints.FILL;
import static com.jgoodies.forms.layout.CellConstraints.RIGHT;
import static javax.swing.JSplitPane.HORIZONTAL_SPLIT;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

import java.awt.Dimension;
import java.util.List;
import java.util.Optional;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.bensler.decaf.swing.action.ActionAppearance;
import com.bensler.decaf.swing.action.ActionGroup;
import com.bensler.decaf.swing.action.ActionState;
import com.bensler.decaf.swing.action.EntityAction;
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
import com.bensler.decaf.util.prefs.BulkPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class MainFrame {

  public static final ImageIcon ICON_TAG_13 = new ImageIcon(MainFrame.class.getResource("tag_13x13.png"));
  public static final ImageIcon ICON_TAG_48 = new ImageIcon(MainFrame.class.getResource("tag_48x48.png"));
  public static final ImageIcon ICON_IMAGE_13 = new ImageIcon(MainFrame.class.getResource("image_13x13.png"));
  public static final ImageIcon ICON_IMAGE_48 = new ImageIcon(MainFrame.class.getResource("image_48x48.png"));

  public static final ImageIcon ICON_PLUS_10 = new ImageIcon(MainFrame.class.getResource("plus_10x10.png"));
  public static final ImageIcon ICON_X_10 = new ImageIcon(MainFrame.class.getResource("x_10x10.png"));
  public static final ImageIcon ICON_X_30 = new ImageIcon(MainFrame.class.getResource("x_30x30.png"));

  public static final PropertyViewImpl<Blob, Integer> BLOB_ID_VIEW = new PropertyViewImpl<>(
    createComparablePropertyGetter(Blob::getId)
  );
  public static final PropertyViewImpl<Tag, String> TAG_NAME_VIEW = new PropertyViewImpl<>(
    ICON_TAG_13, createStringPropertyGetter(Tag::getName)
  );

  private final App app_;
  private final JFrame frame_;
  private final BulkPrefPersister prefs_;
  private final EntityTree<Tag> tagTree_;
  private final ThumbnailOverview thumbnails_;
  private final Hierarchy<Tag> allTags_;

  private SlideShowFrame slideShowFrame_;

  public MainFrame(App app) {
    app_ = app;
    SelectionTagPanel selectionTagPanel = new SelectionTagPanel();

    allTags_ = new Hierarchy<>();
    frame_ = new JFrame("Taggy");
    allTags_.addAll(app_.getDbAccess().loadAllTags());
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p, 3dlu, f:p:g, 3dlu, f:p, 3dlu"
    ));

    final JPanel toolbar = new JPanel(new FormLayout("f:p, 3dlu:g", "f:p"));
    toolbar.add(app_.getImportCtrl().getImportAction().createToolbarButton(), new CellConstraints(1, 1));
    mainPanel.add(toolbar, new CellConstraints(2, 2));
    new ActionAppearance(null, new ImageIcon(getClass().getResource("vacuum.png")), null, "Scan for new Images to import.");

    thumbnails_ = new ThumbnailOverview(app_);
    thumbnails_.setSelectionListener((source, selection) -> selectionTagPanel.setData(selection));
    tagTree_ = new EntityTree<>(TAG_NAME_VIEW);
    tagTree_.setVisibleRowCount(20, .5f);
    tagTree_.setSelectionListener((source, selection) -> displayThumbnailsOfSelectedTag());
    frame_.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    tagTree_.setData(allTags_);
    final EntityAction<Tag> editTagAction = new EntityAction<>(
      new ActionAppearance(ICON_TAG_13, null, "Edit Tag", "Edit currently selected Tag"),
      new SingleEntityFilter<>(ActionState.DISABLED),
      new SingleEntityActionAdapter<>((source, tag) -> editTagUi(tagTree_, tag))
    );
    final EntityAction<Tag> newTagAction = new EntityAction<>(
      new ActionAppearance(new OverlayIcon(ICON_TAG_13, new Overlay(ICON_PLUS_10, SE)), null, "New Tag", "Creates a new Tag under the currently selected Tag"),
      new SingleEntityFilter<>(ActionState.ENABLED),
      new SingleEntityActionAdapter<>((source, tag) -> createTagUi(tagTree_, tag))
    );
    final EntityAction<Tag> deleteTagAction = new EntityAction<>(
      new ActionAppearance(new OverlayIcon(ICON_TAG_13, new Overlay(ICON_X_10, SE)), null, "Delete Tag", "Remove currently selected Tag"),
      new SingleEntityFilter<>(ActionState.DISABLED),
      new SingleEntityActionAdapter<>((source, tag) -> tag.ifPresent(this::deleteTagUi))
    );
    tagTree_.setContextActions(new ActionGroup<>(editTagAction, newTagAction, deleteTagAction));
    final JSplitPane leftSplitpane = new JSplitPane(HORIZONTAL_SPLIT, true,
      tagTree_.getScrollPane(), thumbnails_.getScrollPane()
    );
    final JSplitPane rightSplitpane = new JSplitPane(HORIZONTAL_SPLIT, true,
      leftSplitpane, selectionTagPanel.getComponent()
    );
    rightSplitpane.setResizeWeight(1);
    mainPanel.add(rightSplitpane, new CellConstraints(2, 4));

    final JPanel buttonPanel = new JPanel(new FormLayout("f:p:g, 3dlu, f:p:g", "f:p:g"));
    ((FormLayout)buttonPanel.getLayout()).setColumnGroups(new int[][] {{1, 3}});
    mainPanel.add(buttonPanel, new CellConstraints(2, 6, RIGHT, CENTER));
    final JButton testButton = new JButton("Orphan Files");
    testButton.addActionListener(evt -> new OrphanDialog(app_).show(app_.getDbAccess()));
    buttonPanel.add(testButton, new CellConstraints(1, 1, FILL, FILL));
    final JButton closeButton = new JButton("Close");
    closeButton.addActionListener(evt -> frame_.dispose());
    buttonPanel.add(closeButton, new CellConstraints(3, 1, FILL, FILL));

    mainPanel.setPreferredSize(new Dimension(750, 750));
    frame_.setContentPane(mainPanel);
    frame_.pack();
    new WindowClosingTrigger(frame_, evt -> frameClosing());

    final PrefKey baseKey = new PrefKey(App.PREFS_APP_ROOT, getClass());
    prefs_ = new BulkPrefPersister(app.getPrefs(), List.of(
      new WindowPrefsPersister(baseKey, frame_),
      new SplitpanePrefPersister(new PrefKey(baseKey, "splitLeft"), leftSplitpane),
      new SplitpanePrefPersister(new PrefKey(baseKey, "splitRight"), rightSplitpane)
    ));
  }

  private void frameClosing() {
    if (slideShowFrame_ != null) {
      slideShowFrame_.close();
    }
    prefs_.store();
    try {
      app_.getPrefs().store();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  void createTagUi(EntityTree<Tag> tree, Optional<Tag> parentTag) {
    new OkCancelDialog<>(slideShowFrame_, new NewTagDialog(tree.getData())).show(
      parentTag, newTag -> tree.addData(app_.getDbAccess().createObject(newTag), true)
    );
  }

  void deleteTagUi(Tag tag) {
    if (new ConfirmationDialog(new DialogAppearance(
      new OverlayIcon(ICON_TAG_48, new Overlay(ICON_X_30, SE)), "Confirmation: Delete Tag",
      "Do you really want to delete tag \"%s\" under \"%s\"?".formatted(
        tag.getName(), Optional.ofNullable(tag.getParent()).map(Tag::getName).orElse("Root")
      )
    )).show(frame_)) {
      System.out.println("Delete Tag");
    } else {
      System.out.println("Canceled: Delete Tag");
    }
  }

  void editTagUi(EntityTree<Tag> tree, Optional<Tag> tag) {
//    TODO
    throw new UnsupportedOperationException();
  }

  public void show() {
    frame_.setVisible(true);
  }

  public JFrame getFrame() {
    return frame_;
  }

  public SlideShowFrame getSlideShowFrame() {
    if (slideShowFrame_ == null) {
      slideShowFrame_ = new SlideShowFrame(app_);
    }
    return slideShowFrame_;
  }

  public Hierarchy<Tag> getAllTags() {
    return allTags_;
  }

  public void displayThumbnailsOfSelectedTag() {
    final Tag tag = tagTree_.getSingleSelection();

    if (tag == null) {
      thumbnails_.clear();
    } else {
      app_.getDbAccess().refresh(tag);
      thumbnails_.setData(List.copyOf(tag.getBlobs()));
    }
  }

}
