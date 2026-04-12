package com.bensler.taggy.persist;

import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.bensler.decaf.util.entity.Entity;

public class EntityType<E extends Entity<E>> {

  private final String name_;
  private final Optional<EntityType<?>> parentType_;
  private final Map<String, EntityProperty> properties_;

  public EntityType(Class<E> entityClass, EntityProperty... properties) {
    this(entityClass, Optional.empty(), properties);
  }

  public EntityType(Class<E> entityClass, EntityType<?> parentType, EntityProperty... properties) {
    this(entityClass, Optional.of(parentType), properties);
  }

  public EntityType(Class<E> entityClass, Optional<EntityType<?>> parentType, EntityProperty... properties) {
    name_ = entityClass.getName();
    parentType_ = parentType;
    properties_ = Arrays.stream(properties).collect(toMap(EntityProperty::getName, property -> property));
  }

  public String getClassName() {
    return name_;
  }

  public Optional<String> getParentClassName() {
    return parentType_.map(EntityType::getClassName);
  }

  public Set<EntityProperty> getProperties() {
    return new HashSet<>(properties_.values());
  }

  public Optional<EntityProperty> getProperty(String propertyName, EntityPropertyType<?> propertyType) {
    final Optional<EntityProperty> property = Optional.ofNullable(properties_.get(propertyName));

    property.map(EntityProperty::getType).ifPresent(type-> {
      if (type != propertyType) {
        throw new IllegalStateException("Type mismatch (\"%s\" vs \"%s\") in property \"%s\"".formatted(
          type, propertyType, name_ + "." + propertyName
        ));
      }
    });
    return property;
  }

}
