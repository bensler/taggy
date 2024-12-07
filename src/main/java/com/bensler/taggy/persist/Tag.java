package com.bensler.taggy.persist;

import java.util.HashSet;
import java.util.Set;

import com.bensler.decaf.util.Named;
import com.bensler.decaf.util.tree.Hierarchical;

/**
 * Sample of an entity or business class having hierarchical nature.
 */
public class Tag extends Object implements Hierarchical<Tag>, Named {

    private Integer id_;
    private Tag parent_;
    private String name_;
    private Set<Blob> blobs_;

    Tag() {}

    public Tag(final Tag parent, final String name) {
      id_ = null;
      parent_ = parent;
      name_ = name;
      blobs_ = new HashSet<>();
    }

    public Integer getId() {
      return id_;
    }

    void setId(Integer id) {
      id_ = id;
    }

    @Override
    public Tag getParent() {
      return parent_;
    }

    void setParent(Tag parent) {
      parent_ = parent;
    }

    @Override
    public String getName() {
      return name_;
    }

    void setName(String name) {
      name_ = name;
    }

    public Set<Blob> getBlobs() {
      return Set.copyOf(blobs_);
    }

    void setBlobs(Set<Blob> blobs) {
      blobs_ = new HashSet<>(blobs);
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
