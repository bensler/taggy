package com.bensler.taggy.ui;

import static com.bensler.decaf.util.cmp.CollatorComparator.COLLATOR_COMPARATOR;
import static com.jgoodies.forms.layout.CellConstraints.CENTER;
import static com.jgoodies.forms.layout.CellConstraints.FILL;
import static com.jgoodies.forms.layout.CellConstraints.RIGHT;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.hibernate.Session;

import com.bensler.decaf.swing.table.EntityTable;
import com.bensler.decaf.swing.table.TablePropertyView;
import com.bensler.decaf.swing.table.TableView;
import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.swing.view.PropertyViewImpl;
import com.bensler.decaf.swing.view.SimplePropertyGetter;
import com.bensler.decaf.util.cmp.ComparableComparator;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class OldTagDialog {

  public static final PropertyViewImpl<Blob, Integer> BLOB_ID_VIEW = new PropertyViewImpl<>(
    new SimplePropertyGetter<>(Blob::getId, new ComparableComparator<>())
  );
  public static final PropertyViewImpl<Tag, String> TAG_NAME_VIEW = new PropertyViewImpl<>(
    new SimplePropertyGetter<>(Tag::getName, COLLATOR_COMPARATOR)
  );

  final JDialog dialog_;
  final Session session_;
  final BlobController blobController_;
  final EntityTree<Tag> tagTree_;
  final EntityTable<Blob> fileList_;
  final EntityTree<Tag> blobTagTree_;

  public OldTagDialog(BlobController blobController, Session session, Hierarchy<Tag> data) {
    session_ = session;
    blobController_ = blobController;
    dialog_ = new JDialog(null, "Taggy", ModalityType.MODELESS);
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu, f:p:g, 3dlu, p, 3dlu"
    ));
    final JPanel detailPanel = new JPanel(new FormLayout(
      "f:p:g, 3dlu, f:p:g",
      "f:p:g"
    ));

    blobTagTree_ = new EntityTree<>(TAG_NAME_VIEW);
    fileList_ = createFileList();
    fileList_.setSelectionListener((source, selection) -> {
      blobTagTree_.setData((selection.size() == 1) ?  selection.get(0).getTagHierarchy() : new Hierarchy<>());
      blobTagTree_.expandCollapseAll(true);
    });
    fileList_.getComponent().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          new EditCategoriesDialog(dialog_, data, blobController_, fileList_.getSingleSelection()).setVisible(true);
        }
      }
    });
    tagTree_ = new EntityTree<>(TAG_NAME_VIEW);
    tagTree_.setVisibleRowCount(20, .5f);
    tagTree_.setSelectionListener((source, selection) -> {
      if (selection.isEmpty()) {
        fileList_.clear();
      } else {
        fileList_.setData(selection.get(0).getBlobs());
      }
      blobTagTree_.setData(new Hierarchy<>());
    });

    dialog_.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    tagTree_.setData(data);
    mainPanel.add(tagTree_.getScrollPane(), new CellConstraints(2, 2));
    mainPanel.add(detailPanel, new CellConstraints(2, 4));
    detailPanel.add(fileList_.getScrollPane(), new CellConstraints(1, 1));
    detailPanel.add(blobTagTree_.getScrollPane(), new CellConstraints(3, 1));

    final JPanel buttonPanel = new JPanel(new FormLayout("f:p:g, 3dlu, f:p:g", "f:p:g"));
    ((FormLayout)buttonPanel.getLayout()).setColumnGroups(new int[][] {{1, 3}});
    mainPanel.add(buttonPanel, new CellConstraints(2, 6, RIGHT, CENTER));
    final JButton testButton = new JButton("Orphan Files");
    testButton.addActionListener(evt -> new OrphanDialog(dialog_, blobController_).show(session_));
    buttonPanel.add(testButton, new CellConstraints(1, 1, FILL, FILL));
    final JButton closeButton = new JButton("Close");
    closeButton.addActionListener(evt -> dialog_.dispose());
    buttonPanel.add(closeButton, new CellConstraints(3, 1, FILL, FILL));

    mainPanel.setPreferredSize(new Dimension(500, 750));
    dialog_.setContentPane(mainPanel);
    dialog_.pack();
    tagTree_.expandCollapseAll(true);
  }

  private EntityTable<Blob> createFileList() {
    return new EntityTable<>(new TableView<>(
      new TablePropertyView<>("id", "Id", BLOB_ID_VIEW),
      new TablePropertyView<>("filename", "Filename", new PropertyViewImpl<>(
        new SimplePropertyGetter<>(Blob::getFilename, COLLATOR_COMPARATOR)
      ))
    ));
  }

  public void show() {
    dialog_.setVisible(true);
  }

}
