package com.bensler.taggy;

import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

public class ZombieBox {

  private final WeakHashMap<Object, Set<Object>> aliveKeeper_;

  ZombieBox() {
    aliveKeeper_ = new WeakHashMap<>();
  }

  public void put(Object key, Object value) {
    aliveKeeper_.computeIfAbsent(key, _ -> new HashSet<>()).add(value);
  }

}
