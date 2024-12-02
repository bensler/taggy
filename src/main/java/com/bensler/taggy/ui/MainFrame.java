package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.view.SimplePropertyGetter.createComparablePropertyGetter;
import static com.bensler.decaf.swing.view.SimplePropertyGetter.createStringPropertyGetter;
import static com.bensler.decaf.util.cmp.CollatorComparator.COLLATOR_COMPARATOR;
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

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.bensler.decaf.swing.action.ActionGroup;
import com.bensler.decaf.swing.action.ActionState;
import com.bensler.decaf.swing.action.Appearance;
import com.bensler.decaf.swing.action.EntityAction;
import com.bensler.decaf.swing.action.SingleEntityActionAdapter;
import com.bensler.decaf.swing.action.SingleEntityFilter;
import com.bensler.decaf.swing.dialog.OkCancelDialog;
import com.bensler.decaf.swing.table.EntityTable;
import com.bensler.decaf.swing.table.TablePropertyView;
import com.bensler.decaf.swing.table.TableView;
import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.swing.view.PropertyViewImpl;
import com.bensler.decaf.swing.view.SimplePropertyGetter;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.Thumbnailer;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class MainFrame {

  public static final PropertyViewImpl<Blob, Integer> BLOB_ID_VIEW = new PropertyViewImpl<>(
    createComparablePropertyGetter(Blob::getId)
  );
  public static final PropertyViewImpl<Tag, String> TAG_NAME_VIEW = new PropertyViewImpl<>(
    createStringPropertyGetter(Tag::getName)
  );

  private static MainFrame instance;

  public static MainFrame getInstance() {
    return instance;
  }

  private final JDialog dialog_;
  private final Session session_;
  private final ImportController importCtrl_;
  private final BlobController blobCtrl_;
  private final Thumbnailer thumbnailer_;
  private final EntityTree<Tag> tagTree_;
  private final ThumbnailOverview thumbnails_;

  private BlobDialog blobDlg_;

  public MainFrame(BlobController blobController, ImportController importCtrl, Session session, Thumbnailer thumbnailer) {
    instance = this;
    session_ = session;
    importCtrl_ = importCtrl;
    blobCtrl_ = blobController;
    SelectionTagPanel selectionTagPanel = new SelectionTagPanel();

    final Hierarchy<Tag> data = new Hierarchy<>();

    thumbnailer_ = thumbnailer;
    dialog_ = new JDialog(null, "Taggy", ModalityType.MODELESS);
    data.addAll(session_.createQuery("FROM Tag", Tag.class).getResultList());
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p, 3dlu, f:p:g, 3dlu, f:p, 3dlu"
    ));

    final JPanel toolbar = new JPanel(new FormLayout("f:p, 3dlu:g", "f:p"));
    toolbar.add(importCtrl_.getImportAction().createToolbarButton(), new CellConstraints(1, 1));
    mainPanel.add(toolbar, new CellConstraints(2, 2));
    new Appearance(null, new ImageIcon(getClass().getResource("vacuum.png")), null, "Scan for new Images to import.");

    thumbnails_ = new ThumbnailOverview(blobCtrl_);
    thumbnails_.setSelectionListener((source, selection) -> selectionTagPanel.setData(selection));
    tagTree_ = new EntityTree<>(TAG_NAME_VIEW);
    tagTree_.setVisibleRowCount(20, .5f);
    tagTree_.setSelectionListener((source, selection) -> {
      if (selection.isEmpty()) {
        thumbnails_.clear();
      } else {
        thumbnails_.setData(List.copyOf(selection.get(0).getBlobs()));
      }
    });

    dialog_.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    tagTree_.setData(data);
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
    testButton.addActionListener(evt -> new OrphanDialog(blobCtrl_).show(session_));
    buttonPanel.add(testButton, new CellConstraints(1, 1, FILL, FILL));
    final JButton closeButton = new JButton("Close");
    closeButton.addActionListener(evt -> dialog_.dispose());
    buttonPanel.add(closeButton, new CellConstraints(3, 1, FILL, FILL));

    mainPanel.setPreferredSize(new Dimension(750, 750));
    dialog_.setContentPane(mainPanel);
    dialog_.pack();
  }

  private EntityTable<Blob> createFileList() {
    return new EntityTable<>(new TableView<>(
      new TablePropertyView<>("id", "Id", BLOB_ID_VIEW),
      new TablePropertyView<>("filename", "Filename", new PropertyViewImpl<>(
        new SimplePropertyGetter<>(Blob::getFilename, COLLATOR_COMPARATOR)
      ))
    ));
  }

  void createTagUi(EntityTree<Tag> eventSource, Optional<Tag> parentTag) {
    new OkCancelDialog<>(blobDlg_, "ToDo", new NewTagDialog(eventSource.getData())).show(parentTag, newTag -> createTag(eventSource, newTag));
  }

  void createTag(EntityTree<Tag> tree, Tag newTag) {
    final Transaction txn = session_.beginTransaction();

    session_.persist(newTag);
    txn.commit(); // TODO rollback in case of exc
    tree.addData(newTag, true);
  }

  public void show() {
    dialog_.setVisible(true);
  }

  public BlobController getBlobCtrl() {
    return blobCtrl_;
  }

  public BlobDialog getBlobDlg() {
    if (blobDlg_ == null) {
      blobDlg_ = new BlobDialog();
    }
    return blobDlg_;
  }

  public Thumbnailer getThumbnailer() {
    return thumbnailer_;
  }

}
