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
public class Tag extends Object implements Hierarchical<Tag>, Named, Entity {

    private Integer id_;
    private Tag parent_;
    private String name_;
    private Set<Blob> blobs_;
    private Map<String, String> properties_;

    Tag() {}

    public Tag(Integer id) {
      id_ = id;
    }

    public Tag(final Tag parent, final String name, Map<String, String> properties) {
      id_ = null;
      parent_ = parent;
      name_ = name;
      blobs_ = new HashSet<>();
      properties_ = new HashMap<>(properties);
    }

    @Override
    public Integer getId() {
      return id_;
    }

    @Override
    public Tag getParent() {
      return parent_;
    }

    @Override
    public String getName() {
      return name_;
    }

    public void setProperties(Tag parent, String name, Set<Blob> blobs) {
      parent_ = parent;
      name_ = name;
      blobs_ = new HashSet<>(blobs);
    }

    public Set<Blob> getBlobs() {
      return Set.copyOf(blobs_);
    }

    public String getProperty(String name) {
      return properties_.get(name);
    }

    @Override
    public String toString() {
      return name_;
    }

    @Override
    public int hashCode() {
      return id_;
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof Tag tag)
      && (id_.equals(tag.id_));
    }

}
