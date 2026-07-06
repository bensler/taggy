package com.bensler.taggy.persist.v2;

import java.util.List;
import java.util.function.Consumer;

import com.bensler.taggy.persist.v2.PersistencyBaseLayer.PropertyTableEntry;

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

  public JAVA_TYPE loadValue(List<Object> value) {
    return type_.load(value);
  }

  public <BASE_TYPE> void store(Consumer<PropertyTableEntry<BASE_TYPE>> tableEntryConsumer, int propertyId, JAVA_TYPE value) {
    ((EntityPropertyType<JAVA_TYPE, BASE_TYPE>)type_).store((table, dbValue) -> tableEntryConsumer.accept(new PropertyTableEntry<>(propertyId, table, dbValue)), value);
  }

  @Override
  public int hashCode() {
    return name_.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return ((obj instanceof EntityProperty<?> other) && name_.equals(other.name_));
  }

}
