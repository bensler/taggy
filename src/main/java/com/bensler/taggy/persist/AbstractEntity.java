package com.bensler.taggy.persist;

public abstract class AbstractEntity<E extends Entity<E>> implements Entity<E> {

  protected final Class<E> clazz_;
  protected final Integer id_;

  protected AbstractEntity(Class<E> clazz, Integer id) {
    clazz_ = clazz;
    id_ = id;
  }

  @Override
  public Integer getId() {
    return id_;
  }

  @Override
  public Class<E> getEntityClass() {
    return clazz_;
  }

  @Override
  public String toString() {
    return "%s[%s]".formatted(clazz_.getSimpleName(), id_);
  }

  @Override
  public int hashCode() {
    return id_;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof Entity entity) && equals(entity);
  }

  protected boolean equals(Entity<?> entity) {
    return clazz_.equals(entity.getEntityClass()) && id_.equals(entity.getId());
  }

}
