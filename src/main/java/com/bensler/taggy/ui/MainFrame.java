package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.view.SimplePropertyGetter.createComparablePropertyGetter;
import static com.bensler.decaf.swing.view.SimplePropertyGetter.createStringPropertyGetter;
import static com.jgoodies.forms.layout.CellConstraints.CENTER;
import static com.jgoodies.forms.layout.CellConstraints.FILL;
import static com.jgoodies.forms.layout.CellConstraints.RIGHT;
import static javax.swing.JSplitPane.HORIZONTAL_SPLIT;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.util.List;
import java.util.Optional;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.bensler.decaf.swing.action.ActionGroup;
import com.bensler.decaf.swing.action.ActionState;
import com.bensler.decaf.swing.action.Appearance;
import com.bensler.decaf.swing.action.EntityAction;
import com.bensler.decaf.swing.action.SingleEntityActionAdapter;
import com.bensler.decaf.swing.action.SingleEntityFilter;
import com.bensler.decaf.swing.dialog.OkCancelDialog;
import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.swing.view.PropertyViewImpl;
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
    new ImageIcon(MainFrame.class.getResource("tag.png")), createStringPropertyGetter(Tag::getName)
  );

  private final JDialog dialog_;
  private final App app_;
  private final EntityTree<Tag> tagTree_;
  private final ThumbnailOverview thumbnails_;
  private final Hierarchy<Tag> allTags_;

  private BlobDialog blobDlg_;

  public MainFrame(App app) {
    app_ = app;
    SelectionTagPanel selectionTagPanel = new SelectionTagPanel();

    allTags_ = new Hierarchy<>();
    dialog_ = new JDialog(null, "Taggy", ModalityType.MODELESS);
    allTags_.addAll(app_.getDbAccess().loadAllTags());
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p, 3dlu, f:p:g, 3dlu, f:p, 3dlu"
    ));

    final JPanel toolbar = new JPanel(new FormLayout("f:p, 3dlu:g", "f:p"));
    toolbar.add(app_.getImportCtrl().getImportAction().createToolbarButton(), new CellConstraints(1, 1));
    mainPanel.add(toolbar, new CellConstraints(2, 2));
    new Appearance(null, new ImageIcon(getClass().getResource("vacuum.png")), null, "Scan for new Images to import.");

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

    dialog_.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    tagTree_.setData(allTags_);
    tagTree_.setContextActions(new ActionGroup<>(new EntityAction<>(
      new Appearance(null, null, "New Tag", "Creates a new Tag under the currently selected Tag"),
      new SingleEntityFilter<>(ActionState.ENABLED),
      new SingleEntityActionAdapter<>((source, tag) -> createTagUi(tagTree_, tag))
    )));
    final JSplitPane largeSplitPane = new JSplitPane(HORIZONTAL_SPLIT, true,
      new JSplitPane(HORIZONTAL_SPLIT, true,
        tagTree_.getScrollPane(),
        thumbnails_.getScrollPane()
      ), selectionTagPanel.getComponent()
    );
    largeSplitPane.setResizeWeight(1);
    mainPanel.add(largeSplitPane, new CellConstraints(2, 4));

    final JPanel buttonPanel = new JPanel(new FormLayout("f:p:g, 3dlu, f:p:g", "f:p:g"));
    ((FormLayout)buttonPanel.getLayout()).setColumnGroups(new int[][] {{1, 3}});
    mainPanel.add(buttonPanel, new CellConstraints(2, 6, RIGHT, CENTER));
    final JButton testButton = new JButton("Orphan Files");
    testButton.addActionListener(evt -> new OrphanDialog(app_).show(app_.getDbAccess()));
    buttonPanel.add(testButton, new CellConstraints(1, 1, FILL, FILL));
    final JButton closeButton = new JButton("Close");
    closeButton.addActionListener(evt -> dialog_.dispose());
    buttonPanel.add(closeButton, new CellConstraints(3, 1, FILL, FILL));

    mainPanel.setPreferredSize(new Dimension(750, 750));
    dialog_.setContentPane(mainPanel);
    dialog_.pack();
  }

  void createTagUi(EntityTree<Tag> tree, Optional<Tag> parentTag) {
    new OkCancelDialog<>(blobDlg_, "ToDo", new NewTagDialog(tree.getData())).show(
      parentTag, newTag -> tree.addData(app_.getDbAccess().createObject(newTag), true)
    );
  }

  public void show() {
    dialog_.setVisible(true);
  }

  public JDialog getFrame() {
    return dialog_;
  }

  public BlobDialog getBlobDlg() {
    if (blobDlg_ == null) {
      blobDlg_ = new BlobDialog();
    }
    return blobDlg_;
  }

  public Hierarchy<Tag> getAllTags() {
    return allTags_;
  }

}
