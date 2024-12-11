package com.bensler.taggy.ui;

import static com.bensler.taggy.ui.MainFrame.TAG_NAME_VIEW;

import java.awt.Dimension;
import java.util.Set;

import javax.swing.ImageIcon;

import com.bensler.decaf.swing.awt.IconComponent;
import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D;
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
      "f:p:g",
      "p,  3dlu, f:p:g"
    ));
    final CellConstraints cc = new CellConstraints();
    final OverlayIcon overlayIcon = new OverlayIcon(new ImageIcon(getClass().getResource("image_48x48.png")));

    overlayIcon.addIcon(new ImageIcon(getClass().getResource("tags_24x24.png")), Alignment2D.SW);
    add(new IconComponent(overlayIcon), cc.xy(1, 1, "r, t"));
    tagTree_ = new CheckboxTree<>(TAG_NAME_VIEW);
    tagTree_.setData(allCategories);
    add(tagTree_.getScrollPane(), cc.xy(1, 3));
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
  }

  @Override
  protected boolean validateContent(Object eventSource) {
    return true;
  }

}
