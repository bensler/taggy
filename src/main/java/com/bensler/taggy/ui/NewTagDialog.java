package com.bensler.taggy.ui;

import java.util.Optional;

import javax.swing.JLabel;
import javax.swing.JTextField;

import com.bensler.decaf.swing.dialog.BasicContentPanel;
import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class NewTagDialog extends BasicContentPanel<Optional<Tag>, Tag> {

  private final EntityTree<Tag> parentTag_;
  private final JTextField nameTextfield_;

  public NewTagDialog() {
    super(new FormLayout(
      "r:p, 3dlu, f:p:g",
      "f:p:g, 3dlu, c:p"
    ));

    final CellConstraints cc = new CellConstraints();
    parentTag_ = new EntityTree<>(MainFrame.TAG_NAME_VIEW);
    parentTag_.setVisibleRowCount(5, 2.0f);
    add(new JLabel("Parent Tag:"), cc.xy(1, 1, "r, t"));
    add(parentTag_.getScrollPane(), cc.xy(3, 1));
    nameTextfield_ = new JTextField(20);
    add(new JLabel("Name:"), cc.xy(1, 3));
    add(nameTextfield_, cc.xy(3, 3));
  }

  @Override
  public void setData(Optional<Tag> inData) {
    final Hierarchy<Tag> parents = new Hierarchy<>();
    Tag parent = inData.orElse(null);

    while (parent != null) {
      parents.add(parent);
      parent = parent.getParent();
    }
    parentTag_.setData(parents);
    inData.ifPresent(parentTag_::select);
    ctx_.setValid(true);
  }

  @Override
  public Tag getData() {
    return new Tag(parentTag_.getSingleSelection(), nameTextfield_.getText());
  }

}
