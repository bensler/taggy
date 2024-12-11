package com.bensler.taggy.ui;

import java.util.Optional;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.bensler.decaf.swing.awt.IconComponent;
import com.bensler.decaf.swing.dialog.BasicContentPanel;
import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class NewTagDialog extends BasicContentPanel<Optional<Tag>, Tag> {

  private final EntityTree<Tag> parentTag_;
  private final JTextField nameTextfield_;
  private final Hierarchy<Tag> allTags_;

  public NewTagDialog(Hierarchy<Tag> pAllTags) {
    super(new FormLayout(
      "r:p, 3dlu, f:p:g",
      "p,  3dlu, f:p:g, 3dlu, c:p"
    ));
    allTags_ = pAllTags;

    final CellConstraints cc = new CellConstraints();
    final IconComponent icon = new IconComponent(new ImageIcon(getClass().getResource("tags_48x48.png")));

    add(icon, cc.xyw(1, 1, 3, "r, t"));
    parentTag_ = new EntityTree<>(MainFrame.TAG_NAME_VIEW);
    addValidationSource(parentTag_);
    parentTag_.setVisibleRowCount(5, 2.0f);
    add(new JLabel("Parent Tag:"), cc.xy(1, 3, "r, t"));
    add(parentTag_.getScrollPane(), cc.xy(3, 3));
    nameTextfield_ = new JTextField(20);
    addValidationSource(nameTextfield_);
    add(new JLabel("Name:"), cc.xy(1, 5));
    add(nameTextfield_, cc.xy(3, 5));
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
  }

  @Override
  protected boolean validateContent(Object validationSource) {
    final Tag selectedTag = parentTag_.getSingleSelection();
    final Set<Tag> potentialSiblings = allTags_.getChildren(selectedTag);
    final String newName = getNewName();

    return potentialSiblings.stream()
      .map(Tag::getName)
      .filter(newName::equals)
      .findFirst()
      .isEmpty();
  }

  private String getNewName() {
    return nameTextfield_.getText().trim();
  }

  @Override
  public Tag getData() {
    return new Tag(parentTag_.getSingleSelection(), getNewName());
  }

}
