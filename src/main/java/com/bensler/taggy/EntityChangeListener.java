package com.bensler.taggy;

public interface EntityChangeListener {

  void entityRemoved(Object entity);

  void entityChanged(Object entity);

}
