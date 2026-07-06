package com.bensler.taggy.persist.v2;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.bensler.decaf.util.entity.Entity;

public class EntityType<E extends Entity<E>> {

  private final Class<E> entityClass_;
  private final String name_;
  private final Optional<EntityType<?>> parentType_;
  private final Map<String, BoundEntityProperty> properties_;

  public EntityType(Class<E> entityClass, EntityProperty<?>... properties) {
    this(entityClass, Optional.empty(), properties);
  }

  public EntityType(Class<E> entityClass, EntityType<?> parentType, EntityProperty<?>... properties) {
    this(entityClass, Optional.of(parentType), properties);
  }

  public EntityType(Class<E> entityClass, Optional<EntityType<?>> parentType, EntityProperty<?>... properties) {
    name_ = (entityClass_ = entityClass).getName();
    parentType_ = parentType;
    properties_ = Stream.concat(
      parentType_.map(parent -> parent.properties_.values()).stream().flatMap(Collection::stream).map(BoundEntityProperty::getProperty),
      Arrays.stream(properties)
    ).map(property -> new BoundEntityProperty(this, property)).collect(toMap(BoundEntityProperty::getName, identity()));
  }

  public Class<E> getEntityClass() {
    return entityClass_;
  }

  public String getClassName() {
    return name_;
  }

  public Collection<BoundEntityProperty> getProperties() {
     return properties_.values();
  }

  public Optional<BoundEntityProperty> getProperty(String propertyName, EntityPropertyType<?, ?> propertyType) {
    final Optional<BoundEntityProperty> property = Optional.ofNullable(properties_.get(propertyName));

    property.map(BoundEntityProperty::getType).ifPresent(type-> {
      if (type != propertyType) {
        throw new IllegalStateException("Type mismatch (\"%s\" vs \"%s\") in property \"%s\"".formatted(
          type, propertyType, name_ + "." + propertyName
        ));
      }
    });
    return property;
  }

  public boolean containsProperty(EntityProperty<?> property) {
    return properties_.values().stream().map(BoundEntityProperty::getProperty).filter(prop -> prop.equals(property)).findAny().isPresent();
  }

}
