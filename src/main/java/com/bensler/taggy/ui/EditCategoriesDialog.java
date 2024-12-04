package com.bensler.taggy.ui;

import static com.bensler.taggy.ui.MainFrame.TAG_NAME_VIEW;

import java.awt.Dimension;
import java.util.Set;

import com.bensler.decaf.swing.dialog.BasicContentPanel;
import com.bensler.decaf.swing.tree.CheckboxTree;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class EditCategoriesDialog extends BasicContentPanel<Blob, Set<Tag>> {

  private final CheckboxTree<Tag> tagTree_;

  public EditCategoriesDialog(Hierarchy<Tag> allCategories) {
    super(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu"
    ));
    tagTree_ = new CheckboxTree<>(TAG_NAME_VIEW);
    tagTree_.setData(allCategories);
    add(tagTree_.getScrollPane(), new CellConstraints(2, 2));
    setPreferredSize(new Dimension(400, 400));
  }

  @Override
  public Set<Tag> getData() {
    return tagTree_.getCheckedNodes();
  }

  @Override
  protected void setData(Blob blob) {
    final Set<Tag> tags = blob.getTags();

    tagTree_.setCheckedNodes(tags);
    tags.forEach(tag -> tagTree_.expandCollapse(tag, true));
    ctx_.setValid(true);
  }

}
