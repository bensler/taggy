package com.bensler.taggy.persist.v2;

import com.bensler.decaf.util.entity.Entity;

public class EntityRelationshipType<S extends Entity<S>, T extends Entity<T>> {

  private final String name_;
  private final EntityType<S> sourceType_;
  private final EntityType<T> targetType_;

  public EntityRelationshipType(String name, EntityType<S> sourceType, EntityType<T> targetType) {
    name_ = name;
    sourceType_ = sourceType;
    targetType_ = targetType;
  }

  public String getName() {
    return name_;
  }

  public EntityType<S> getSourceType() {
    return sourceType_;
  }

  public EntityType<T> getTargetType() {
    return targetType_;
  }

}
