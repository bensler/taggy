package com.bensler.taggy.persist;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.bensler.decaf.util.Named;
import com.bensler.decaf.util.tree.Hierarchical;

/**
 * Sample of an entity or business class having hierarchical nature.
 */
public class Tag extends AbstractEntity<Tag> implements Hierarchical<Tag>, Named {

  private EntityReference<Tag> parent_;
  private String name_;
  private Set<EntityReference<Blob>> blobs_;
  private Map<TagProperty, String> properties_;

  public Tag(Integer id) {
    super(Tag.class, id);
  }

  public Tag(Tag parent, String name, Map<TagProperty, String> properties) {
    this(null, new EntityReference<>(Tag.class, parent.getId()), name, properties, Set.of());
  }

  public Tag(
    Integer id, EntityReference<Tag> parent, String name,
    Map<TagProperty, String> properties,
    Set<EntityReference<Blob>> blobs
  ) {
    super(Tag.class, id);
    parent_ = parent;
    name_ = name;
    properties_ = new HashMap<>(properties);
    blobs_ = new HashSet<>(blobs);
  }

  @Override
  public Tag getParent() {
    return ((parent_ != null) ? parent_.resolve() : null);
  }

  @Override
  public String getName() {
    return name_;
  }

  public void setProperties(Tag parent, String name, Set<Blob> blobs) {
    parent_ = new EntityReference<>(parent);
    name_ = name;
    blobs_ = EntityReference.createCollection(blobs, new HashSet<>());
  }

  public Set<Blob> getBlobs() {
    return DbAccess.INSTANCE.get().resolve(blobs_, new HashSet<>());
  }

  public boolean removeBlob(Blob blob) {
    return blobs_.remove(new EntityReference<>(blob));
  }

  public String getProperty(TagProperty key) {
    return properties_.get(key);
  }

  public boolean conatainsProperty(TagProperty key) {
    return properties_.containsKey(key);
  }

}
