package com.bensler.taggy.persist.v2;

class BoundEntityProperty {

  private final EntityType<?> entityType_;
  private final EntityProperty<?> property_;

  BoundEntityProperty(EntityType<?> entityType, EntityProperty<?> property) {
    entityType_ = entityType;
    property_ = property;
  }

  String getName() {
    return property_.getName();
  }

  EntityPropertyType<?, ?> getType() {
    return property_.getType();
  }

  String getTypeName() {
    return property_.getType().getName();
  }

  EntityProperty<?> getProperty() {
    return property_;
  }

  @Override
  public int hashCode() {
    return entityType_.hashCode() + property_.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (
      (obj instanceof BoundEntityProperty other)
      && entityType_.equals(other.entityType_)
      && property_.equals(other.property_)
    );
  }

  @Override
  public String toString() {
    return entityType_.getClassName() + ":" + property_.getName() + "(" + property_.getType().getName() + ")";
  }

  boolean is(EntityProperty<?> property) {
    return property_.equals(property);
  }

}
