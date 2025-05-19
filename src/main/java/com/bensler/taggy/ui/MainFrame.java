package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.action.ActionState.DISABLED;
import static com.bensler.decaf.swing.action.ActionState.ENABLED;
import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.decaf.swing.view.SimplePropertyGetter.createGetter;
import static com.bensler.decaf.util.cmp.CollatorComparator.COLLATOR_COMPARATOR;
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

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.tree.TreePath;

import org.hibernate.internal.util.compare.ComparableComparator;

import com.bensler.decaf.swing.SplitpanePrefPersister;
import com.bensler.decaf.swing.action.ActionAppearance;
import com.bensler.decaf.swing.action.ActionGroup;
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
import com.bensler.decaf.swing.view.EntityPropertyComparator;
import com.bensler.decaf.swing.view.PropertyViewImpl;
import com.bensler.decaf.swing.view.SimplePropertyGetter;
import com.bensler.decaf.util.cmp.ComparatorChain;
import com.bensler.decaf.util.prefs.BulkPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.persist.TagProperty;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class MainFrame {

  public static final ImageIcon ICON_IMAGE_13 = new ImageIcon(MainFrame.class.getResource("image_13x13.png"));
  public static final ImageIcon ICON_IMAGE_48 = new ImageIcon(MainFrame.class.getResource("image_48x48.png"));

  public static final ImageIcon ICON_IMAGES_48 = new ImageIcon(MainFrame.class.getResource("images_48x48.png"));

  public static final ImageIcon ICON_SLIDESHOW_13 = new ImageIcon(MainFrame.class.getResource("slideshow_13x13.png"));
  public static final ImageIcon ICON_SLIDESHOW_48 = new ImageIcon(MainFrame.class.getResource("slideshow_48x48.png"));

  public static final ImageIcon ICON_TAG_13 = new ImageIcon(MainFrame.class.getResource("tag_13x13.png"));
  public static final ImageIcon ICON_TAG_48 = new ImageIcon(MainFrame.class.getResource("tag_48x48.png"));

  public static final ImageIcon ICON_TAGS_36 = new ImageIcon(MainFrame.class.getResource("tags_36x36.png"));
  public static final ImageIcon ICON_TAGS_48 = new ImageIcon(MainFrame.class.getResource("tags_48x48.png"));

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

  public static final EntityPropertyComparator<Tag, String> TAG_NAME_COMPARATOR = new EntityPropertyComparator<>(Tag::getName, COLLATOR_COMPARATOR);

  public static final EntityPropertyComparator<Tag, String> TAG_DATE_COMPARATOR = new EntityPropertyComparator<>(
    tag -> tag.getProperty(TagProperty.REPRESENTED_DATE), new ComparableComparator<>()
  );

  public static final PropertyViewImpl<Tag, String> TAG_NAME_VIEW = new PropertyViewImpl<>(
    ICON_TAG_13, createGetter(Tag::getName, new ComparatorChain<>(List.of(TAG_DATE_COMPARATOR, TAG_NAME_COMPARATOR)))
  );

  private final App app_;
  private final JFrame frame_;
  private final BulkPrefPersister prefs_;
  private final EntityTree<Tag> tagTree_;
  private final ThumbnailOverview thumbnails_;
  private final TagController tagCtrl_;
  private final EntityChangeListenerTreeAdapter<Tag> treeAdapter_; // save it from GC
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

    final SelectionTagPanel selectionTagPanel = new SelectionTagPanel();
    final JPanel toolbar = new JPanel(new FormLayout("f:p, 3dlu:g", "f:p"));
    toolbar.add(app_.getImportCtrl().getImportAction().createToolbarButton(), new CellConstraints(1, 1));
    mainPanel.add(toolbar, new CellConstraints(2, 2));

    thumbnails_ = new ThumbnailOverview(app_);
    thumbnails_.setSelectionListener((source, selection) -> selectionTagPanel.setData(selection));
    tagTree_ = new EntityTree<>(TAG_NAME_VIEW);
    tagTree_.setVisibleRowCount(20, .5f);
    tagTree_.setSelectionListener((source, selection) -> displayThumbnailsOfSelectedTag());
    app_.addEntityChangeListener(treeAdapter_ = new EntityChangeListenerTreeAdapter<>(tagTree_), Tag.class);
    frame_.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    tagCtrl_.setAllTags(tagTree_);
    final EntityAction<Tag> editTagAction = new EntityAction<>(
      new ActionAppearance(new OverlayIcon(ICON_TAG_13, new Overlay(ICON_EDIT_13, SE)), null, "Edit Tag", "Edit currently selected Tag"),
      new SingleEntityFilter<>(DISABLED),
      new SingleEntityActionAdapter<>((source, tag) -> tag.ifPresent(this::editTagUi))
    );
    final EntityAction<Tag> newTagAction = new EntityAction<>(
      new ActionAppearance(new OverlayIcon(ICON_TAG_13, new Overlay(ICON_PLUS_10, SE)), null, "Create Tag", "Creates a new Tag under the currently selected Tag"),
      new SingleEntityFilter<>(ENABLED),
      new SingleEntityActionAdapter<>((source, tag) -> createTagUi(tag))
    );
    final EntityAction<Tag> deleteTagAction = new EntityAction<>(
      new ActionAppearance(new OverlayIcon(ICON_TAG_13, new Overlay(ICON_X_10, SE)), null, "Delete Tag", "Remove currently selected Tag"),
      new SingleEntityFilter<>(DISABLED, tag -> tagCtrl_.isLeaf(tag) ? ENABLED : DISABLED),
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

    final JPanel buttonPanel = new JPanel(new FormLayout("f:p:g", "f:p:g"));
    mainPanel.add(buttonPanel, new CellConstraints(2, 6, RIGHT, CENTER));
    final JButton testButton = new JButton("Orphan Files");
    testButton.addActionListener(evt -> new OrphanDialog(app_).show(app_.getDbAccess()));
    buttonPanel.add(testButton, new CellConstraints(1, 1, FILL, FILL));

    mainPanel.setPreferredSize(new Dimension(750, 750));
    frame_.setContentPane(mainPanel);
    frame_.pack();
    new WindowClosingTrigger(frame_, evt -> frameClosing());

    final PrefKey baseKey = new PrefKey(App.PREFS_APP_ROOT, getClass());
    prefs_ = new BulkPrefPersister(app_.getPrefs(),
      new WindowPrefsPersister(baseKey, frame_),
      new TagPrefPersister(new PrefKey(baseKey, "selectedTag"), tagCtrl_, tagTree_::getSingleSelection, tagTree_::select),
      new SplitpanePrefPersister(new PrefKey(baseKey, "splitLeft"), leftSplitpane),
      new SplitpanePrefPersister(new PrefKey(baseKey, "splitRight"), rightSplitpane)
    );
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

  void editTagUi(Tag tag) {
    new OkCancelDialog<>(frame_, new TagDialog.Edit(tagTree_.getData())).show(
      tag, newTag -> {
        tagTree_.removeTree(tag);
        tagTree_.select(tagCtrl_.updateTag(tag, newTag));
      }
    );
  }

  void deleteTagUi(Tag tag) {
    if (new ConfirmationDialog(new DialogAppearance(
      new OverlayIcon(ICON_TAG_48, new Overlay(ICON_X_30, SE)), "Confirmation: Delete Tag",
      "Do you really want to delete tag \"%s\" under \"%s\"?".formatted(
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
