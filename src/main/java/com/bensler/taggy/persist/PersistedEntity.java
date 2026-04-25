package com.bensler.taggy.persist;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bensler.taggy.persist.PersistencyBaseLayer.PropertyTableEntry;

public class PersistedEntity {

  private final Optional<Integer> entityId_;
  private final Optional<EntityType<?>> type_;

  private final List<PropertyTableEntry<?>> properties_;
  private final Map<String, String> optionalProperties_;

  public PersistedEntity(EntityType<?> type) {
    entityId_ = Optional.empty();
    type_ = Optional.of(type);
    optionalProperties_ = new HashMap<>();
    properties_ = new ArrayList<>();
  }

  public <JAVA_TYPE> void addProperty(int propertyId, EntityProperty<JAVA_TYPE> propertyType, JAVA_TYPE value) {
//    propertyType.
    properties_.add(new PropertyTableEntry<>(propertyId, null, null));
  }

  public void putOptionalProperties(Map<String, String> optionalProperties) {
    optionalProperties_.putAll(optionalProperties);
  }

  public void persist(PersistencyBaseLayer db) throws SQLException {
    final Integer id;

    if (entityId_.isPresent()) {
      id = entityId_.get();
      db.dropProperties(id);
    } else {
      id =  db.createEntity(type_.get());
    }
    db.storeOptionalProperties(id, optionalProperties_);
    db.storeProperties(id, List.copyOf(properties_));
  }



}
