package com.bensler.taggy.persist;

public class EntityProperty<JAVA_TYPE> {

  private final String name_;
  private final EntityPropertyType<JAVA_TYPE, ?> type_;

  public EntityProperty(String name, EntityPropertyType<JAVA_TYPE, ?> type) {
    name_ = name;
    type_ = type;
  }

  public String getName() {
    return name_;
  }

  public EntityPropertyType<JAVA_TYPE, ?> getType() {
    return type_;
  }

}
