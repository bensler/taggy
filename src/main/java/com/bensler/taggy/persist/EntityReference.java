package com.bensler.taggy.persist;

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
    entityClass_ = entityClass;
    id_ = id;
  }

  public E resolve() {
    return DbAccess.INSTANCE.get().resolve(this);
  }

}
