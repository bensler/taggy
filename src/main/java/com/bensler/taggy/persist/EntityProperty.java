package com.bensler.taggy.persist;

import java.util.function.Consumer;

import com.bensler.taggy.persist.PersistencyBaseLayer.PropertyTableEntry;

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

  public void store(Consumer<PropertyTableEntry<?>> tableEntryConsumer, int propertyId, JAVA_TYPE value) {
    type_.store((table, dbValue) -> tableEntryConsumer.accept(new PropertyTableEntry<>(propertyId, table, dbValue)), value);
  }

}
