package com.bensler.taggy.persist;

import java.util.HashSet;
import java.util.Set;

import com.bensler.decaf.util.tree.Hierarchy;

/**
 * Sample of an entity or business class having hierarchical nature.
 */
public class Blob extends Object implements Entity {

    private final Integer id_;
    private final String filename_;
    private final String sha256sum_;
    private final String thumbnailSha_;
    private final String type_;

    private Set<Tag> tags_;

    /** Hibernate needs this empty constructor */
    private Blob() {
      this(null, null, null, null);
    }

    public Blob(final String filename, String shaSum, String thumbnailSha, String type) {
      id_ = null;
      filename_ = filename;
      sha256sum_ = shaSum;
      thumbnailSha_ = thumbnailSha;
      type_ = type;
      tags_ = new HashSet<>();
    }

    @Override
    public Integer getId() {
      return id_;
    }

    public String getFilename() {
      return filename_;
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
      tags_ = new HashSet<  >(tags);
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

    @Override
    public String toString() {
      return filename_;
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
