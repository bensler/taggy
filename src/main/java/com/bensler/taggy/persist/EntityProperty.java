package com.bensler.taggy.persist;

public class EntityProperty {

  private final String name_;
  private final EntityPropertyType type_;

  public EntityProperty(String name, EntityPropertyType type) {
    name_ = name;
    type_ = type;
  }

  public String getName() {
    return name_;
  }

  public EntityPropertyType getType() {
    return type_;
  }

}
