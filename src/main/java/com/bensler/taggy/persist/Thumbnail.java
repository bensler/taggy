package com.bensler.taggy.persist;

import java.util.Set;

import com.bensler.taggy.imprt.ImportController;

public class Thumbnail extends Image<Thumbnail> {

  public Thumbnail(Integer id, String shaSum) {
    super(id, Thumbnail.class, shaSum, ImportController.TYPE_JPG, Set.of());
  }

}
