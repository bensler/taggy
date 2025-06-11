package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.action.ActionState.ENABLED;
import static com.bensler.decaf.swing.action.ActionState.HIDDEN;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JScrollPane;

import com.bensler.decaf.swing.action.ActionAppearance;
import com.bensler.decaf.swing.action.ActionGroup;
import com.bensler.decaf.swing.action.EntityAction;
import com.bensler.decaf.swing.action.SingleEntityActionAdapter;
import com.bensler.decaf.swing.action.SingleEntityFilter;
import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.util.tree.Hierarchical;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;

public class SelectionTagPanel {

  final EntityTree<Tag> tagTree_;

  public SelectionTagPanel(MainFrame mainFrame) {
    tagTree_ = new EntityTree<>(TagUi.NAME_VIEW);
    tagTree_.setVisibleRowCount(20, .5f);
    final EntityAction<Tag> focusAction = new EntityAction<>(
      new ActionAppearance(null, null, "Focus", null),
      new SingleEntityFilter<>(HIDDEN, tag -> ENABLED),
      new SingleEntityActionAdapter<>((source, tag) -> tag.ifPresent(mainFrame::selectTag))
    );
    tagTree_.setContextActions(new ActionGroup<>(focusAction));
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
