package com.bensler.taggy.persist;

import java.util.HashSet;
import java.util.Set;

import com.bensler.decaf.util.tree.Hierarchy;

/**
 * Sample of an entity or business class having hierarchical nature.
 */
public class Blob extends Object {

    private Integer id_;
    private String filename_;
    private String sha256sum_;
    private String thumbnailSha_;
    private Set<Tag> tags_;

    Blob() {}

    public Blob(final String filename, String shaSum, String thumbnailSha) {
      id_ = null;
      filename_ = filename;
      sha256sum_ = shaSum;
      thumbnailSha_ = thumbnailSha;
      tags_ = new HashSet<>();
    }

    public Integer getId() {
      return id_;
    }

    void setId(Integer id) {
      if (id_ != null) {
        throw new IllegalStateException("id already set");
      } else {
        id_ = id;
      }
    }

    public String getFilename() {
      return filename_;
    }

    void setFilename(String filename) {
      filename_ = filename;
    }

    public String getSha256sum() {
      return sha256sum_;
    }

    void setSha256sum(String sha256sum) {
      sha256sum_ = sha256sum;
    }

    public String getThumbnailSha() {
      return thumbnailSha_;
    }

    void setThumbnailSha(String thumbnailSha) {
      thumbnailSha_ = thumbnailSha;
    }

    public Set<Tag> getTags() {
      return Set.copyOf(tags_);
    }

    void setTags(Set<Tag> tags) {
      tags_ = tags;
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

}
