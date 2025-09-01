package com.bensler.taggy.ui;

import java.util.ArrayList;
import java.util.List;

import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.swing.tree.EntityTreeModel;
import com.bensler.decaf.util.tree.Hierarchical;
import com.bensler.taggy.EntityChangeListener;

public class EntityChangeListenerTreeAdapter<H extends Hierarchical<H>> implements EntityChangeListener<H> {

  private final EntityTree<H> tree_;
  private final EntityTreeModel<H> model_;

  public EntityChangeListenerTreeAdapter(EntityTree<H> tree) {
    model_ = (tree_ = tree).getModel();
  }

  @Override
  public void entityCreated(H entity) {
    final H parent = entity.getParent();

    if ((parent == null) || model_.contains(parent).isPresent()) {
      model_.addNode(entity);
      tree_.expandCollapse(entity, true);
    }
  }

  @Override
  public void entityChanged(H entity) {
    final List<H> selection = tree_.getSelection();

    model_.contains(entity).ifPresent(lEntity -> model_.addNode(entity));
    if (selection.contains(entity)) {
      final List<H >newSelection = new ArrayList<>();

      newSelection.addAll(selection);
      newSelection.add(entity);
      tree_.select(newSelection);
    }
  }

  @Override
  public void entityRemoved(H entity) {
    model_.contains(entity).ifPresent(model_::removeTree);
  }

}
