package com.bensler.taggy.persist;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.bensler.decaf.util.Named;
import com.bensler.decaf.util.entity.AbstractEntity;
import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.decaf.util.tree.Hierarchical;

/**
 * Sample of an entity or business class having hierarchical nature.
 */
public class Tag extends AbstractEntity<Tag> implements Hierarchical<Tag>, Named {

  public static <R> R getProperty(Tag tag, Function<Tag, R> resultProvider) {
    return Optional.ofNullable(tag).map(resultProvider).orElse(null);
  }

  private final EntityReference<Tag> parent_;
  private final String name_;
  private final Set<EntityReference<Blob>> blobs_;
  private final Map<TagProperty, String> properties_;

  public Tag(Tag parent, String name, Map<TagProperty, String> properties) {
    this(null, getProperty(parent, EntityReference::new), name, properties, Set.of());
  }

  public Tag(
    Integer id, EntityReference<Tag> parent, String name,
    Map<TagProperty, String> properties,
    Set<EntityReference<Blob>> blobs
  ) {
    super(Tag.class, id);
    parent_ = parent;
    name_ = name;
    properties_ = Map.copyOf(properties);
    blobs_ = Set.copyOf(blobs);
  }

  @Override
  public Tag getParent() {
    return ((parent_ != null) ? DbAccess.INSTANCE.get().resolve(parent_) : null);
  }

  @Override
  public String getName() {
    return name_;
  }

  public Set<EntityReference<Blob>> getBlobRefs() {
    return blobs_;
  }

  public Set<Blob> getBlobs() {
    return DbAccess.INSTANCE.get().resolveAll(blobs_, new HashSet<>());
  }

  public String getProperty(TagProperty key) {
    return properties_.get(key);
  }

  public Set<TagProperty> getPropertyKeys() {
    return properties_.keySet();
  }

  public boolean containsProperty(TagProperty key) {
    return properties_.containsKey(key);
  }

  public Map<TagProperty, String> getProperties() {
    return properties_;
  }

}
