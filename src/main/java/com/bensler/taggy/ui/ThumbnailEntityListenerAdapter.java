package com.bensler.taggy.ui;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.bensler.taggy.App;
import com.bensler.taggy.EntityChangeListener;
import com.bensler.taggy.persist.Photo;

public class ThumbnailEntityListenerAdapter implements EntityChangeListener<Photo> {

  public static enum Operation {
    ADD_OR_UPDATE,
    REMOVE
  }

  private final Map<Operation, Consumer<Photo>> operationActions_;
  private final ThumbnailOverviewPanel thumbs_;
  private final Function<Photo, Operation> updateDecider_;

  public ThumbnailEntityListenerAdapter(App app, ThumbnailOverviewPanel thumbs, Function<Photo, Operation> updateDecider) {
    thumbs_ = thumbs;
    updateDecider_ = updateDecider;
    operationActions_ = Map.of(
      Operation.ADD_OR_UPDATE, thumbs_::addImage,
      Operation.REMOVE, thumbs_::removeImage
    );
    app.addEntityChangeListener(this, Photo.class);
  }

  protected void blobChanged(Photo blob) {
   operationActions_.get(updateDecider_.apply(blob)).accept(blob);
  }

  @Override
  public void entityCreated(Photo blob) {
    blobChanged(blob);
  }

  @Override
  public void entityChanged(Photo blob) {
    blobChanged(blob);
  }

  @Override
  public void entityRemoved(Photo entity) {
    thumbs_.contains(entity).ifPresent(thumbs_::removeImage);
  }

}
