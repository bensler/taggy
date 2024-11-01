package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.view.SimplePropertyGetter.createComparablePropertyGetter;
import static com.bensler.decaf.swing.view.SimplePropertyGetter.createStringPropertyGetter;
import static com.bensler.decaf.util.cmp.CollatorComparator.COLLATOR_COMPARATOR;
import static com.jgoodies.forms.layout.CellConstraints.CENTER;
import static com.jgoodies.forms.layout.CellConstraints.FILL;
import static com.jgoodies.forms.layout.CellConstraints.RIGHT;
import static javax.swing.JSplitPane.HORIZONTAL_SPLIT;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.hibernate.Session;

import com.bensler.decaf.swing.table.EntityTable;
import com.bensler.decaf.swing.table.TablePropertyView;
import com.bensler.decaf.swing.table.TableView;
import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.swing.view.PropertyViewImpl;
import com.bensler.decaf.swing.view.SimplePropertyGetter;
import com.bensler.decaf.util.tree.Hierarchy;
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

  final JDialog dialog_;
  final Session session_;
  final BlobController blobController_;
  final EntityTree<Tag> tagTree_;
  final ThumbnailOverviewPanel thumbnails_;

  public MainFrame(BlobController blobController, Session session, Hierarchy<Tag> data) {
    session_ = session;
    blobController_ = blobController;
    dialog_ = new JDialog(null, "Taggy", ModalityType.MODELESS);
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu, f:p, 3dlu"
    ));

    thumbnails_ = new ThumbnailOverviewPanel(blobController_);
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
    final JScrollPane thumbnailScrollpane = new JScrollPane(thumbnails_, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);

    tagTree_.getComponent().addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        // TODO ctx menu -> create tag
        System.out.println("mouse");
      }
    });
    thumbnailScrollpane.getViewport().setBackground(thumbnails_.getBackground());
    mainPanel.add(new JSplitPane(
      HORIZONTAL_SPLIT, true,
      tagTree_.getScrollPane(),
      thumbnailScrollpane
    ), new CellConstraints(2, 2));

    final JPanel buttonPanel = new JPanel(new FormLayout("f:p:g, 3dlu, f:p:g", "f:p:g"));
    ((FormLayout)buttonPanel.getLayout()).setColumnGroups(new int[][] {{1, 3}});
    mainPanel.add(buttonPanel, new CellConstraints(2, 4, RIGHT, CENTER));
    final JButton testButton = new JButton("Orphan Files");
    testButton.addActionListener(evt -> new OrphanDialog(dialog_, blobController_).show(session_));
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

  public void show() {
    dialog_.setVisible(true);
  }

}
