package com.bensler.taggy.persist;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bensler.taggy.persist.PersistencyBaseLayer.PropertyTableEntry;

public class PersistedEntity {

  private final DbSetup dbSetup_;
  private final Optional<Integer> entityId_;
  private final EntityType<?> type_;

  private final List<PropertyTableEntry<?>> properties_;
  private final Map<String, String> optionalProperties_;

  PersistedEntity(DbSetup dbSetup, EntityType<?> type, Optional<Integer> entityId) {
    dbSetup_ = dbSetup;
    entityId_ = entityId;
    type_ = type;
    optionalProperties_ = new HashMap<>();
    properties_ = new ArrayList<>();
  }

  public <JAVA_TYPE> void addProperty(int propertyId, EntityProperty<JAVA_TYPE> propertyType, JAVA_TYPE value) {
    if (!type_.containsProperty(propertyType)) {
      throw new IllegalArgumentException("Given property does not belong to this EntityType");
    }
    propertyType.store(properties_::add, propertyId, value);
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
      id =  db.createEntity(type_);
    }
    db.storeOptionalProperties(id, optionalProperties_);
    db.storeProperties(id, List.copyOf(properties_));
  }



}
