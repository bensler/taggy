package com.bensler.taggy.ui;

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
    model_.contains(entity).ifPresent(lEntity -> model_.addNode(entity));
  }

  @Override
  public void entityRemoved(H entity) {
    model_.contains(entity).ifPresent(model_::removeTree);
  }

}
