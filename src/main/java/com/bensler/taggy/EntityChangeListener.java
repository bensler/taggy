package com.bensler.taggy;

import com.bensler.taggy.persist.Entity;

public interface EntityChangeListener {

  void entityCreated(Entity entity);

  void entityChanged(Entity entity);

  void entityRemoved(Entity entity);

}
