package com.bensler.taggy.persist;

import java.util.Set;

import com.bensler.decaf.util.entity.EntityReference;

public class Image<E extends Image<E>> extends Blob<E> {

  public Image(Integer id, Class<E> clazz, String shaSum, String type, Set<EntityReference<Tag>> tags) {
    super(id, clazz, shaSum, type, tags);
  }

}
