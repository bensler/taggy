package com.bensler.taggy.ui;

import java.awt.Dimension;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JPanel;

import com.bensler.decaf.swing.tree.CheckboxTree;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class EditCategoriesDialog extends JDialog {

  private final BlobController blobController_;
  private final Blob blob_;

  public EditCategoriesDialog(JDialog parent, Hierarchy<Tag> allCategories, BlobController blobController, Blob blob) {
    super(parent, "Edit Categories", true);
    blobController_ = blobController;
    blob_ = blob;
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu"
    ));
    CheckboxTree<Tag> cbTree = new CheckboxTree<>(MainFrame.TAG_NAME_VIEW);
    cbTree.setData(allCategories);
    final Set<Tag> tags = blob.getTags();

    cbTree.setCheckedNodes(tags);
    tags.forEach(tag -> cbTree.expandCollapse(tag, true));
    mainPanel.add(cbTree.getScrollPane(), new CellConstraints(2, 2));

    mainPanel.setPreferredSize(new Dimension(400, 400));
    setContentPane(mainPanel);
    pack();
  }

}
