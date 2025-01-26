package com.bensler.taggy;

public interface EntityChangeListener {

  void entityCreated(Object entity);

  void entityChanged(Object entity);

  void entityRemoved(Object entity);

}
