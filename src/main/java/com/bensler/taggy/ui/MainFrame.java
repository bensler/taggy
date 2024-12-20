package com.bensler.taggy.ui;

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

  public static final PropertyViewImpl<Blob, Integer> BLOB_ID_VIEW = new PropertyViewImpl<>(
    createComparablePropertyGetter(Blob::getId)
  );
  public static final PropertyViewImpl<Tag, String> TAG_NAME_VIEW = new PropertyViewImpl<>(
    new ImageIcon(MainFrame.class.getResource("tag_13x13.png")), createStringPropertyGetter(Tag::getName)
  );

  private final App app_;
  private final JFrame frame_;
  private final BulkPrefPersister prefs_;
  private final EntityTree<Tag> tagTree_;
  private final ThumbnailOverview thumbnails_;
  private final Hierarchy<Tag> allTags_;

  private ImageFrame imageFrame_;

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
    tagTree_.setSelectionListener((source, selection) -> {
      if (selection.isEmpty()) {
        thumbnails_.clear();
      } else {
        final Tag tag = selection.get(0);

        app_.getDbAccess().refresh(tag);
        thumbnails_.setData(List.copyOf(tag.getBlobs()));
      }
    });

    frame_.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    tagTree_.setData(allTags_);
    tagTree_.setContextActions(new ActionGroup<>(new EntityAction<>(
      new ActionAppearance(null, null, "New Tag", "Creates a new Tag under the currently selected Tag"),
      new SingleEntityFilter<>(ActionState.ENABLED),
      new SingleEntityActionAdapter<>((source, tag) -> createTagUi(tagTree_, tag))
    )));
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
    (prefs_ = new BulkPrefPersister(app.getPrefs(), List.of(
      new WindowPrefsPersister(baseKey, frame_),
      new SplitpanePrefPersister(new PrefKey(baseKey, "splitLeft"), leftSplitpane),
      new SplitpanePrefPersister(new PrefKey(baseKey, "splitRight"), rightSplitpane)
    ))).apply();
  }

  private void frameClosing() {
    if (imageFrame_ != null) {
      imageFrame_.close();
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
    new OkCancelDialog<>(imageFrame_, new NewTagDialog(tree.getData())).show(
      parentTag, newTag -> tree.addData(app_.getDbAccess().createObject(newTag), true)
    );
  }

  public void show() {
    frame_.setVisible(true);
  }

  public JFrame getFrame() {
    return frame_;
  }

  public ImageFrame getBlobDlg() {
    if (imageFrame_ == null) {
      imageFrame_ = new ImageFrame(app_);
    }
    return imageFrame_;
  }

  public Hierarchy<Tag> getAllTags() {
    return allTags_;
  }

}
