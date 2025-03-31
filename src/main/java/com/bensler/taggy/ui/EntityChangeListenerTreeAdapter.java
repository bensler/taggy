package com.bensler.taggy.ui;

import com.bensler.decaf.swing.tree.EntityTreeModel;
import com.bensler.decaf.util.tree.Hierarchical;
import com.bensler.taggy.EntityChangeListener;

public class EntityChangeListenerTreeAdapter<H extends Hierarchical<H>> implements EntityChangeListener<H> {

  private final EntityTreeModel<H> model_;

  public EntityChangeListenerTreeAdapter(EntityTreeModel<H> model) {
    model_ = model;
  }

  @Override
  public void entityCreated(H entity) {
    final H parent = entity.getParent();

    if ((parent == null) || model_.contains(parent).isPresent()) {
      model_.addNode(entity);
    }
  }

  @Override
  public void entityChanged(H entity) {
    model_.contains(entity).ifPresent(model_::addNode);
  }

  @Override
  public void entityRemoved(H entity) {
    model_.contains(entity).ifPresent(model_::removeTree);
  }

}
