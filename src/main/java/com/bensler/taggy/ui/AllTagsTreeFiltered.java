package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.text.TextfieldListener.addTextfieldListener;
import static com.bensler.taggy.ui.MainFrame.TAG_NAME_VIEW;
import static com.jgoodies.forms.layout.CellConstraints.DEFAULT;
import static com.jgoodies.forms.layout.CellConstraints.FILL;

import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.bensler.decaf.swing.tree.CheckboxTree;
import com.bensler.decaf.swing.tree.CheckboxTree.CheckedListener;
import com.bensler.decaf.util.tree.Hierarchical;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class AllTagsTreeFiltered {

  private final CheckboxTree<Tag> tagTree_;
  private final Set<Tag> allTags_;
  private final JPanel component_;

  public AllTagsTreeFiltered(CheckedListener<Tag> listener) {
    final App app = App.getApp();
    tagTree_ = new CheckboxTree<>(TAG_NAME_VIEW);
    tagTree_.setVisibleRowCount(20, 1);
    app.getTagCtrl().setAllTags(tagTree_);
    allTags_ = tagTree_.getData().getMembers();
    tagTree_.addCheckedListener(listener);

    final JTextField filterTf = new JTextField(5);
    component_ = new JPanel(new FormLayout("3dlu, p, 3dlu, f:p:g", "3dlu, p, 3dlu, f:p:g"));
    component_.add(new JLabel("Filter:"), new CellConstraints(2, 2));
    component_.add(filterTf, new CellConstraints(4, 2));
    component_.add(tagTree_.getScrollPane(), new CellConstraints(1, 4, 4, 1, FILL, DEFAULT));

    addTextfieldListener(filterTf, this::filterChanged);
  }

  private void filterChanged(String filterStr) {
    final String matchStr = filterStr.toLowerCase().trim();
    final boolean filtering = !matchStr.isEmpty();
    final Hierarchy<Tag> filteredTags = new Hierarchy<>(allTags_.stream()
      .filter(tag -> tag.getName().toLowerCase().contains(matchStr))
      .flatMap(tag -> Hierarchical.toPath(tag).stream())
      .distinct()
      .collect(Collectors.toSet())
    );

    tagTree_.setData(filteredTags);
    tagTree_.expandCollapseAll(filtering);
    if (!filtering) {
      tagTree_.getCheckedNodes().forEach(tag -> tagTree_.expandCollapse(tag, true));
    }
  }

  public JPanel getComponent() {
    return component_;
  }

  public Set<Tag> getCheckedNodes() {
    return tagTree_.getCheckedNodes();
  }

  public void setCheckedNodes(Set<Tag> checkedTags) {
    tagTree_.setCheckedNodes(checkedTags);
  }

  public void expandCollapseAll(boolean expanded) {
    tagTree_.expandCollapseAll(expanded);
  }

  public void makeAllTagsVisible(Set<Tag> tags) {
    setCheckedNodes(tags);
    tagTree_.expandCollapseAll(false);
    tagTree_.setCheckedNodes(tags);
    tags.forEach(tag -> tagTree_.expandCollapse(tag, true));
  }

}