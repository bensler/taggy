package com.bensler.taggy.persist;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Objects;

public class EntityReference<E extends Entity<E>> {

  public static <ENTITY extends Entity<ENTITY>, CIN extends Collection<ENTITY>, COUT extends Collection<EntityReference<ENTITY>>> COUT createCollection(
    CIN entities, COUT collector
  ) {
    entities.stream().forEach(entity -> collector.add(new EntityReference<>(entity)));
    return collector;
  }

  private final Class<E> entityClass_;
  private final Integer id_;

  public EntityReference(E entity) {
    this(entity.getEntityClass(), Objects.requireNonNull(entity.getId()));
  }

  public EntityReference(Class<E> entityClass, Integer id) {
    entityClass_ = requireNonNull(entityClass);
    id_ = requireNonNull(id);
  }

  public Integer getId() {
    return id_;
  }

  public Class<E> getEntityClass() {
    return entityClass_;
  }

  public E resolve() {
    return DbAccess.INSTANCE.get().resolve(this);
  }

  @Override
  public String toString() {
    return "Ref[%s[%s]]".formatted(entityClass_.getSimpleName(), id_);
  }

  @Override
  public int hashCode() {
    return id_.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (
      (obj instanceof EntityReference otherRef)
      && (entityClass_.equals(otherRef.entityClass_))
      && (id_.equals(otherRef.id_))
    );
  }


}
