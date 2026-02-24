package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.text.TextfieldListener.addTextfieldListener;
import static com.jgoodies.forms.layout.CellConstraints.DEFAULT;
import static com.jgoodies.forms.layout.CellConstraints.FILL;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class AllTagsTreeFiltered {

  private final TagsUiController tagCtrl_;
  private final EntityTree<Tag> tagTree_;
  private final JPanel component_;

  public AllTagsTreeFiltered(App app, EntityTree<Tag> tagTree) {
    tagCtrl_ = app.getTagCtrl();
    tagTree_ = tagTree;
    tagTree_.setData(tagCtrl_.getAllTags());
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
  }

  public JPanel getComponent() {
    return component_;
  }

}