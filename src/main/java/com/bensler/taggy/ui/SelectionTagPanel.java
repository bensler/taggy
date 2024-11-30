package com.bensler.taggy.ui;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JScrollPane;

import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;

public class SelectionTagPanel {

  final EntityTree<Tag> tagTree_;

  public SelectionTagPanel() {
    tagTree_ = new EntityTree<>(MainFrame.TAG_NAME_VIEW);
    tagTree_.setVisibleRowCount(20, .5f);
  }

  public void setData(Collection<Blob> blobs) {
    final Set<Tag> assignedTags = blobs.stream()
        .map(Blob::getTags)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
    final Set<Tag> allTags = new HashSet<>(assignedTags);
    assignedTags.forEach(tag -> {
      while (((tag = tag.getParent()) != null) && (!allTags.contains(tag))) {
        allTags.add(tag);
      }
    });

    tagTree_.setData(new Hierarchy<>(allTags));
    tagTree_.expandCollapseAll(true);
  }

  public JScrollPane getComponent() {
    return tagTree_.getScrollPane();
  }

}
