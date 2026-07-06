package com.bensler.taggy.persist.v2;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.bensler.decaf.util.entity.Entity;
import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.persist.DbMapper.Scope;
import com.bensler.taggy.persist.v2.PersistencyBaseLayer.PropertyTableEntry;

public class PersistedEntity {

  private final Optional<Integer> entityId_;
  private final EntityType<?> type_;
  private final Integer typeId_;

  private final List<PropertyTableEntry<?>> properties_;
  private final Map<Integer, Set<Integer>> relationshipsFrom_;
  private final Map<Integer, Set<Integer>> relationshipsTo_;
  private final Map<String, String> optionalProperties_;

  public PersistedEntity(EntityType<?> type, Integer typeId, Optional<Integer> entityId) {
    entityId_ = entityId;
    type_ = type;
    typeId_ = typeId;
    properties_ = new ArrayList<>();
    relationshipsFrom_ = new HashMap<>();
    relationshipsTo_ = new HashMap<>();
    optionalProperties_ = new HashMap<>();
  }

  public BoundEntityProperty getBoundProperty(EntityProperty<?> propertyType) {
    return new BoundEntityProperty(type_, propertyType);
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

  public Integer persist(PersistencyBaseLayer db, Scope scope) throws SQLException {
    final Integer id;
    final boolean newEntity = !entityId_.isPresent();

    scope = (newEntity ? Scope.FULL : scope);
    if (newEntity) {
      id =  db.createEntity(typeId_);
    } else {
      id = entityId_.get();
      if (scope.properties_) {
        db.dropProperties(id);
      }
      if (scope.relationships_) {
        db.dropRelationships(id, relationshipsFrom_.keySet(), relationshipsTo_.keySet());
      }
    }
    if (scope.properties_) {
      db.storeProperties(id, List.copyOf(properties_));
      db.storeOptionalProperties(id, Map.copyOf(optionalProperties_));
    }
    if (scope.relationships_) {
      db.storeRelationships(id, relationshipsFrom_, relationshipsTo_);
    }
    return id;
  }

  public <E extends Entity<E>> void addRelationshipsTo(Integer relationshipKey, Collection<EntityReference<E>> targetRefs) {
    addRelationships(relationshipKey, targetRefs, relationshipsTo_);
  }

  public <E extends Entity<E>> void addRelationshipsFrom(Integer relationshipKey, Collection<EntityReference<E>> sourceRefs) {
    addRelationships(relationshipKey, sourceRefs, relationshipsFrom_);
  }

  private <E extends Entity<E>> void addRelationships(Integer relationshipKey, Collection<EntityReference<E>> refs, Map<Integer, Set<Integer>> target) {
    final Set<Integer> targetSet = target.computeIfAbsent(relationshipKey, _ -> new HashSet<>());

    refs.stream().map(EntityReference::getId).forEach(targetSet::add);
  }


}
