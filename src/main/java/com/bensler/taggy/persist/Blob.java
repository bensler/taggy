package com.bensler.taggy.persist;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.ui.BlobController;

/**
 * Sample of an entity or business class having hierarchical nature.
 */
public class Blob extends Object implements Entity {

    private final Integer id_;
    private final String sha256sum_;
    private final String thumbnailSha_;
    private final String type_;

    private Set<Tag> tags_;
    private Map<String, String> properties_;

    /** Hibernate needs this empty constructor */
    Blob() {
      this(null, null, null, Map.of(), Set.of());
    }

    public Blob(String shaSum, String thumbnailSha, String type, Map<String, String> metaData, Set<Tag> tags) {
      id_ = null;
      sha256sum_ = shaSum;
      thumbnailSha_ = thumbnailSha;
      type_ = type;
      tags_ = new HashSet<>(tags);
      properties_ = new HashMap<>(metaData);
    }

    @Override
    public Integer getId() {
      return id_;
    }

    public String getSha256sum() {
      return sha256sum_;
    }

    public String getThumbnailSha() {
      return thumbnailSha_;
    }

    public String getType() {
      return type_;
    }

    public Set<Tag> getTags() {
      return Set.copyOf(tags_);
    }

    public boolean isUntagged() {
      return tags_.isEmpty();
    }

    public void setTags(Set<Tag> tags) {
      tags_ = new HashSet<>(tags);
    }

    public boolean removeTag(Tag tag) {
      return tags_.remove(tag);
    }

    public Hierarchy<Tag> getTagHierarchy() {
      final Hierarchy<Tag> tagHierarchy = new Hierarchy<>();

      tags_.stream().forEach(tag -> {
        Tag aTag = tag;
        do {
          tagHierarchy.add(aTag);
        } while ((aTag = aTag.getParent()) != null);
      });
      return tagHierarchy;
    }

    public Long getCreationTime() {
      try {
        return Optional.ofNullable(getProperty(BlobController.PROPERTY_DATE_EPOCH_SECONDS))
        .map(Long::valueOf).orElse(null);
      } catch (NumberFormatException nfe) {
        return null;
      }
    }

    public String getProperty(String name) {
      return properties_.get(name);
    }

    public void removeProperty(String name) {
      properties_.remove(name);
    }

    public void addProperty(String name, String value) {
      properties_.put(name, value);
    }

    @Override
    public String toString() {
      return "Blob[%s]".formatted(id_);
    }

    @Override
    public int hashCode() {
      return id_;
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof Blob blob)
      && (id_.equals(blob.id_));
    }

}
