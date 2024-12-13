package com.bensler.taggy.ui;

import static com.bensler.taggy.ui.MainFrame.TAG_NAME_VIEW;

import java.awt.Dimension;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.bensler.decaf.swing.awt.IconComponent;
import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.decaf.swing.dialog.BasicContentPanel;
import com.bensler.decaf.swing.dialog.DialogAppearance;
import com.bensler.decaf.swing.tree.CheckboxTree;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class EditCategoriesDialog extends BasicContentPanel<Blob, Set<Tag>> {

  public static final DialogAppearance APPEARANCE = new DialogAppearance(
    new OverlayIcon(
      new ImageIcon(EditCategoriesDialog.class.getResource("image_48x48.png")),
      new Overlay(new ImageIcon(EditCategoriesDialog.class.getResource("tags_36x36.png")), Alignment2D.SE)
    ),
    "Edit Image Tags",
    "Assign Tags to an Image"
  );

  private final CheckboxTree<Tag> tagTree_;
  private final CheckboxTree<Tag> assignedTags_;

  public EditCategoriesDialog(Icon thumbnail, Hierarchy<Tag> allCategories) {
    super(new FormLayout(
      "f:p:g, 3dlu, f:p",
      "f:p, 3dlu, f:p:g"
    ));
    final CellConstraints cc = new CellConstraints();

    tagTree_ = new CheckboxTree<>(TAG_NAME_VIEW);
    tagTree_.setData(allCategories);
    add(tagTree_.getScrollPane(), cc.xywh(1, 1, 1, 3, "d, f"));
    add(new IconComponent(thumbnail), cc.xy(3, 1, "c, c"));
    assignedTags_ = new CheckboxTree<>(TAG_NAME_VIEW);
    assignedTags_.setVisibleRowCount(15, 1);
    add(assignedTags_.getScrollPane(), cc.xy(3, 3));
    setPreferredSize(new Dimension(600, 600));
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
