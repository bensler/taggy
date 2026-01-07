package com.bensler.taggy.persist;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.bensler.decaf.util.entity.AbstractEntity;
import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.ui.BlobController;

public class Blob extends AbstractEntity<Blob> {

  private final String sha256sum_;
  private final String thumbnailSha_;
  private final String type_;

  private final Map<String, String> properties_;
  private final Set<EntityReference<Tag>> tags_;

  public Blob(Integer id, String shaSum, String thumbnailSha, String type, Map<String, String> metaData, Set<EntityReference<Tag>> tags) {
    super(Blob.class, id);
    sha256sum_ = shaSum;
    thumbnailSha_ = thumbnailSha;
    type_ = type;
    tags_ = new HashSet<>(tags);
    properties_ = new HashMap<>(metaData);
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

  public Set<EntityReference<Tag>> getTagRefs() {
    return Set.copyOf(tags_);
  }

  public Set<Tag> getTags() {
    return DbAccess.INSTANCE.get().resolveAll(tags_, new HashSet<>());
  }

  public boolean isUntagged() {
    return tags_.isEmpty();
  }

  public boolean containsTag(Tag tag) {
    return tags_.contains(new EntityReference<>(tag));
  }

  public Hierarchy<Tag> getTagHierarchy() {
    final Hierarchy<Tag> tagHierarchy = new Hierarchy<>();

    getTags().stream().forEach(tag -> {
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

  public Set<String> getPropertyNames() {
    return properties_.keySet();
  }

  public String getProperty(String name) {
    return properties_.get(name);
  }

  public void addProperty(String name, String value) {
    if (value != null) {
      properties_.put(name, value);
    } else {
      properties_.remove(name);
    }
  }

  public Map<String, String> getMetaData() {
    return Map.copyOf(properties_);
  }

}
