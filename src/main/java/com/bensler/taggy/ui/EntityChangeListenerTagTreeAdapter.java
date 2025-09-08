package com.bensler.taggy.ui;

import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.taggy.persist.Tag;

public class EntityChangeListenerTagTreeAdapter extends EntityChangeListenerTreeAdapter<Tag> {

  public EntityChangeListenerTagTreeAdapter(EntityTree<Tag> tree) {
    super(tree);
  }

}
