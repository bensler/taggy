package com.bensler.taggy.ui;

import java.util.ArrayList;
import java.util.List;

import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.util.tree.Hierarchical;
import com.bensler.taggy.EntityChangeListener;

public class EntityChangeListenerTreeAdapter<H extends Hierarchical<H>> implements EntityChangeListener<H> {

  private final EntityTree<H> tree_;

  public EntityChangeListenerTreeAdapter(EntityTree<H> tree) {
    tree_ = tree;
  }

  @Override
  public void entityCreated(H entity) {
    final H parent = entity.getParent();

    if ((parent == null) || tree_.contains(parent).isPresent()) {
      tree_.addData(entity, true);
    }
  }

  @Override
  public void entityChanged(H entity) {
    final List<H> selection = tree_.getSelection();

    tree_.replaceOrAdd(entity);
    if (selection.contains(entity)) {
      final List<H> newSelection = new ArrayList<>();

      newSelection.addAll(selection);
      newSelection.add(entity);
      tree_.select(newSelection);
    }
  }

  @Override
  public void entityRemoved(H entity) {
    tree_.contains(entity).ifPresent(tree_::removeTree);
  }

}
