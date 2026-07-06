package com.bensler.taggy.persist;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.bensler.decaf.util.Named;
import com.bensler.decaf.util.entity.AbstractEntity;
import com.bensler.decaf.util.entity.Entity;
import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.decaf.util.tree.Hierarchical;

/**
 * Sample of an entity or business class having hierarchical nature.
 */
public class Tag extends AbstractEntity<Tag> implements Hierarchical<Tag>, Named {

  public static <E extends Entity<E>> Optional<EntityReference<E>> createParentRef(E entity) {
    return Optional.ofNullable(entity).map(EntityReference::new);
  }

  private final Optional<EntityReference<Tag>> parent_;
  private final String name_;
  private final Set<EntityReference<Photo>> images_;
  private final Map<TagProperty, String> properties_;

  public Tag(Optional<EntityReference<Tag>> parent, String name, Map<TagProperty, String> properties) {
    this(null, parent, name, properties, Set.of());
  }

  public Tag(
    Integer id, Optional<EntityReference<Tag>> parent, String name,
    Map<TagProperty, String> properties,
    Set<EntityReference<Photo>> images
  ) {
    super(Tag.class, id);
    parent_ = parent;
    name_ = name;
    properties_ = Map.copyOf(properties);
    images_ = Set.copyOf(images);
  }

  @Override
  public Tag getParent() {
    return parent_.map(parent -> DbAccess.INSTANCE.get().resolve(parent)).orElse(null);
  }

  public Optional<EntityReference<Tag>> getParentRef() {
    return parent_;
  }

  @Override
  public String getName() {
    return name_;
  }

  public Set<EntityReference<Photo>> getBlobRefs() {
    return images_;
  }

  public Set<Photo> getImages() {
    return DbAccess.INSTANCE.get().resolveAll(images_, new HashSet<>());
  }

  public Set<TagProperty> getPropertyKeys() {
    return properties_.keySet();
  }

  public Optional<String> containsProperty(TagProperty key) {
    return Optional.ofNullable(properties_.get(key));
  }

  public Map<TagProperty, String> getProperties() {
    return properties_;
  }

}
