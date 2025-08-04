package com.bensler.taggy.persist;

public interface Entity<E extends Entity<E>> {

  public Integer getId();

  default boolean hasId() {
    return (getId() != null);
  }

  public Class<E> getEntityClass();

}
