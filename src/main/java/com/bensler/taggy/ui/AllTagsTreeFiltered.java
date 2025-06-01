package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.text.TextfieldListener.addTextfieldListener;
import static com.bensler.taggy.ui.MainFrame.TAG_NAME_VIEW;
import static com.jgoodies.forms.layout.CellConstraints.DEFAULT;
import static com.jgoodies.forms.layout.CellConstraints.FILL;

import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.bensler.decaf.swing.tree.CheckboxTree;
import com.bensler.decaf.swing.tree.CheckboxTree.CheckedListener;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class AllTagsTreeFiltered {

  private final CheckboxTree<Tag> allTags_;
  private final JPanel component_;

  public AllTagsTreeFiltered(CheckedListener<Tag> listener) {
    final App app = App.getApp();
    allTags_ = new CheckboxTree<>(TAG_NAME_VIEW);
    allTags_.setVisibleRowCount(20, 1);
    app.getTagCtrl().setAllTags(allTags_);
    allTags_.addCheckedListener(listener);

    final JTextField filterTf = new JTextField(5);
    component_ = new JPanel(new FormLayout("3dlu, p, 3dlu, f:p:g", "3dlu, p, 3dlu, f:p:g"));
    component_.add(new JLabel("Filter:"), new CellConstraints(2, 2));
    component_.add(filterTf, new CellConstraints(4, 2));
    component_.add(allTags_.getScrollPane(), new CellConstraints(1, 4, 4, 1, FILL, DEFAULT));

    addTextfieldListener(filterTf, str -> System.out.println("# " + str));
  }

  public JPanel getComponent() {
    return component_;
  }

  public Set<Tag> getCheckedNodes() {
    return allTags_.getCheckedNodes();
  }

  public void setCheckedNodes(Set<Tag> checkedTags) {
    allTags_.setCheckedNodes(checkedTags);
  }

  public void expandCollapseAll(boolean expanded) {
    allTags_.expandCollapseAll(expanded);
  }

  public void makeAllTagsVisible(Set<Tag> tags) {
    allTags_.expandCollapseAll(false);
    allTags_.setCheckedNodes(tags);
    tags.forEach(tag -> allTags_.expandCollapse(tag, true));
  }

}