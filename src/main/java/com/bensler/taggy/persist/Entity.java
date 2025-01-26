package com.bensler.taggy.persist;

public interface Entity {

  public Integer getId();

  default boolean hasId() {
    return (getId() != null);
  }

}
