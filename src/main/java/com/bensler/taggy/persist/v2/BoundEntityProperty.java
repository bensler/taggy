package com.bensler.taggy.persist.v2;

public class BoundEntityProperty {

  private final EntityType<?> entityType_;
  private final EntityProperty<?> property_;

  public BoundEntityProperty(EntityType<?> entityType, EntityProperty<?> property) {
    entityType_ = entityType;
    property_ = property;
  }

  public String getName() {
    return property_.getName();
  }

  public EntityPropertyType<?, ?> getType() {
    return property_.getType();
  }

  public String getTypeName() {
    return property_.getType().getName();
  }

  public EntityProperty<?> getProperty() {
    return property_;
  }

}
