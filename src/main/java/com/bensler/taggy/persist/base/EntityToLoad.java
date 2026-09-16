package com.bensler.taggy.persist.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.bensler.decaf.util.entity.Entity;
import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.persist.base.DbSetup.Direction;

/** TODO class equal to {@link EntityToStore} ? */
public class EntityToLoad {

  private final Integer entityId_;
  private final Map<BoundEntityProperty, List<Object>> properties_;
  private final Map<String, String> optionalProperties_;
  private final Map<Direction, Map<EntityRelationshipType<?, ?>, List<Integer>>> relationsships;

  EntityToLoad(Integer entityId) {
    entityId_ = entityId;
    properties_ = new HashMap<>();
    optionalProperties_ = new HashMap<>();
    relationsships = new HashMap<>();
  }

  public Integer getEntityId() {
    return entityId_;
  }

  public void addValue(BoundEntityProperty property, Object value) {
    properties_.computeIfAbsent(property, lProperty -> new ArrayList<Object>()).add(value);
  }

  public void addOptionalValue(String valueName, String strValue) {
    optionalProperties_.put(valueName, strValue);
  }

  public void addRelationship(Direction direction, EntityRelationshipType<?, ?> relationType, Integer otherEntityId) {
    relationsships.computeIfAbsent(direction, _ -> new HashMap<>())
      .computeIfAbsent(relationType, _ -> new ArrayList<>())
      .add(otherEntityId);
  }

  public <T> Optional<T> getValue(EntityProperty<T> type) {
    return properties_.entrySet().stream()
      .filter(entry -> entry.getKey().is(type))
      .map(entry -> type.loadValue(entry.getValue()))
      .findFirst(); // TODO make properties optional <-> not optional
  }

  public Map<String, String> getOptionalProperties() {
    return Map.copyOf(optionalProperties_);
  }

  public <E extends Entity<E>> Set<EntityReference<E>> getRelationships(Direction direction, EntityRelationshipType<?, ?> relationshipType, Class<E> entityClass) {
    return relationsships.computeIfAbsent(direction, _ -> Map.of())
      .getOrDefault(relationshipType, List.of())
      .stream()
      .map(id -> new EntityReference<>(entityClass, id))
      .collect(Collectors.toSet());
  }

}