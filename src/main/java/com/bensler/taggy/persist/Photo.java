package com.bensler.taggy.persist;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.ui.BlobController;

public class Photo extends Image<Photo> {

  private final Thumbnail thumbnail_;
  private final Map<String, String> properties_;

  public Photo(Integer id, String shaSum, Thumbnail thumbnail, String type, Map<String, String> metaData, Set<EntityReference<Tag>> tags) {
    super(id, Photo.class, shaSum, type, tags);
    thumbnail_ = thumbnail;
    properties_ = new HashMap<>(metaData);
  }

  public Long getCreationTime() {
    try {
      return Optional.ofNullable(getProperty(BlobController.PROPERTY_DATE_EPOCH_SECONDS))
      .map(Long::valueOf).orElse(null);
    } catch (NumberFormatException nfe) {
      return null;
    }
  }

  public Thumbnail getThumbnail() {
    return thumbnail_;
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
