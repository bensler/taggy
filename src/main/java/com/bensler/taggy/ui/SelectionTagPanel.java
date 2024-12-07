package com.bensler.taggy.ui;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JScrollPane;

import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.util.tree.Hierarchical;
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
    final Set<Tag> allTags = blobs.stream()
        .map(Blob::getTags)
        .flatMap(Set::stream)
        .distinct()
        .flatMap(tag -> Hierarchical.toPath(tag).stream())
        .distinct()
        .collect(Collectors.toSet());

    tagTree_.setData(new Hierarchy<>(allTags));
    tagTree_.expandCollapseAll(true);
  }

  public JScrollPane getComponent() {
    return tagTree_.getScrollPane();
  }

}
