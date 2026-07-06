package com.bensler.taggy.persist;

import java.util.HashSet;
import java.util.Set;

import com.bensler.decaf.util.entity.AbstractEntity;
import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.decaf.util.tree.Hierarchy;

public class Blob<E extends Blob<E>> extends AbstractEntity<E> {

  private final String sha256sum_;
  private final String type_;
  private final Set<EntityReference<Tag>> tags_;

  public Blob(Integer id, Class<E> clazz, String shaSum, String type, Set<EntityReference<Tag>> tags) {
    super(clazz, id);
    sha256sum_ = shaSum;
    type_ = type;
    tags_ = Set.copyOf(tags);
  }

  public String getSha256sum() {
    return sha256sum_;
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

}
