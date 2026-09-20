package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.text.TextfieldListener.addTextfieldListener;
import static com.jgoodies.forms.layout.CellConstraints.DEFAULT;
import static com.jgoodies.forms.layout.CellConstraints.FILL;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.Keymap;

import com.bensler.decaf.swing.ActionAdapter;
import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class AllTagsTreeFiltered {

  private final TagsUiController tagCtrl_;
  private final EntityTree<Tag> tagTree_;
  private final JPanel component_;
  private final List<Tag> matchingTags_;

  public AllTagsTreeFiltered(App app, EntityTree<Tag> tagTree) {
    matchingTags_ = new ArrayList<>();
    tagCtrl_ = app.getTagCtrl();
    tagTree_ = tagTree;
    tagTree_.setData(tagCtrl_.getAllTags());
    app.addEntityChangeListener(app.putZombie(this, new EntityChangeListenerTreeAdapter<>(tagTree_)), Tag.class);

    final JTextField filterTf = new JTextField(5);
    final Keymap tfKeymap = filterTf.getKeymap();
    component_ = new JPanel(new FormLayout("3dlu, p, 3dlu, f:p:g", "3dlu, p, 3dlu, f:p:g"));
    component_.add(new JLabel("Filter:"), new CellConstraints(2, 2));
    component_.add(filterTf, new CellConstraints(4, 2));
    component_.add(tagTree_.getScrollPane(), new CellConstraints(1, 4, 4, 1, FILL, DEFAULT));
    tfKeymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), new ActionAdapter(_ -> selectFirstLeaf()));
    filterTf.setKeymap(tfKeymap);

    addTextfieldListener(filterTf, this::filterChanged);
  }

  private void selectFirstLeaf() {
    if (!matchingTags_.isEmpty()) {
      tagTree_.select(matchingTags_.getFirst());
    }
  }

  private void filterChanged(String filterStr) {
    final String matchStr = filterStr.toLowerCase().trim();
    final boolean filtering = !matchStr.isEmpty();

    matchingTags_.clear();
    if (filtering) {
      matchingTags_.addAll(tagCtrl_.getTagsMatchingStr(filterStr));
      if (!matchingTags_.isEmpty()) {
        tagTree_.setData(tagCtrl_.getSubHierarchyContaining(matchingTags_));
        tagTree_.expandCollapseAll(true);
      } else {
        tagTree_.setData(Set.of());
      }
    } else {
      tagTree_.setData(tagCtrl_.getAllTags());
    }
  }

  public JPanel getComponent() {
    return component_;
  }

}