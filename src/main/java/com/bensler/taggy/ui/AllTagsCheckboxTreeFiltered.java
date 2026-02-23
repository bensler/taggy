package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.text.TextfieldListener.addTextfieldListener;
import static com.jgoodies.forms.layout.CellConstraints.DEFAULT;
import static com.jgoodies.forms.layout.CellConstraints.FILL;

import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.bensler.decaf.swing.action.FocusedComponentActionController;
import com.bensler.decaf.swing.tree.CheckboxTree;
import com.bensler.decaf.swing.tree.CheckboxTree.CheckedListener;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class AllTagsCheckboxTreeFiltered {

  private final TagsUiController tagCtrl_;
  private final CheckboxTree<Tag> tagTree_;
  private final JPanel component_;

  public AllTagsCheckboxTreeFiltered(CheckedListener<Tag> listener) {
    final App app = App.getApp();

    tagCtrl_ = app.getTagCtrl();
    tagTree_ = new CheckboxTree<>(TagUi.NAME_VIEW, Tag.class);
    tagTree_.setVisibleRowCount(20, 1);
    tagTree_.setData(tagCtrl_.getAllTags());
    tagTree_.addCheckedListener(listener);
    tagTree_.setCtxActions(new FocusedComponentActionController(app.getTagCtrl().getAllTagActions(), Set.of(tagTree_), false));
    app.addEntityChangeListener(app.putZombie(this, new EntityChangeListenerTreeAdapter<>(tagTree_)), Tag.class);

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

    tagTree_.setData(filtering ? tagCtrl_.getAllTagsFiltered(filterStr) : tagCtrl_.getAllTags());
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
    tagTree_.expandCollapseAll(false);
    setCheckedNodes(tags);
    tags.forEach(tag -> tagTree_.expandCollapse(tag, true));
  }

}