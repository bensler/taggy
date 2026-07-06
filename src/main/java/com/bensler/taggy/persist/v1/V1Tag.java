package com.bensler.taggy.persist.v1;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.bensler.decaf.util.Named;
import com.bensler.decaf.util.entity.AbstractEntity;
import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.decaf.util.tree.Hierarchical;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.TagProperty;

/**
 * Sample of an entity or business class having hierarchical nature.
 */
public class V1Tag extends AbstractEntity<V1Tag> implements Hierarchical<V1Tag>, Named {

  public static <R> Optional<R> getProperty(V1Tag tag, Function<V1Tag, R> resultProvider) {
    return Optional.ofNullable(tag).map(resultProvider);
  }

  private final Optional<EntityReference<V1Tag>> parent_;
  private final String name_;
  private final Set<EntityReference<V1Blob>> blobs_;
  private final Map<TagProperty, String> properties_;

  public V1Tag(V1Tag parent, String name, Map<TagProperty, String> properties) {
    this(null, getProperty(parent, EntityReference::new), name, properties, Set.of());
  }

  public V1Tag(
    Integer id, Optional<EntityReference<V1Tag>> parent, String name,
    Map<TagProperty, String> properties,
    Set<EntityReference<V1Blob>> blobs
  ) {
    super(V1Tag.class, id);
    parent_ = parent;
    name_ = name;
    properties_ = Map.copyOf(properties);
    blobs_ = Set.copyOf(blobs);
  }

  @Override
  public V1Tag getParent() {
    return parent_.map(parent -> DbAccess.INSTANCE.get().resolve(parent)).orElse(null);
  }

  public Optional<EntityReference<V1Tag>> getParentRef() {
    return parent_;
  }

  @Override
  public String getName() {
    return name_;
  }

  public Set<EntityReference<V1Blob>> getBlobRefs() {
    return blobs_;
  }

  public Set<V1Blob> getBlobs() {
    return DbAccess.INSTANCE.get().resolveAll(blobs_, new HashSet<>());
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
