package com.bensler.taggy.ui;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.bensler.taggy.App;
import com.bensler.taggy.EntityChangeListener;
import com.bensler.taggy.persist.Blob;

public class ThumbnailEntityListenerAdapter implements EntityChangeListener<Blob> {

  public static enum Operation {
    ADD_OR_UPDATE,
    REMOVE
  }

  private final Map<Operation, Consumer<Blob>> operationActions_;
  private final ThumbnailOverviewPanel thumbs_;
  private final Function<Blob, Operation> updateDecider_;

  public ThumbnailEntityListenerAdapter(App app, ThumbnailOverviewPanel thumbs, Function<Blob, Operation> updateDecider) {
    thumbs_ = thumbs;
    updateDecider_ = updateDecider;
    operationActions_ = Map.of(
      Operation.ADD_OR_UPDATE, thumbs_::addImage,
      Operation.REMOVE, thumbs_::removeImage
    );
    app.addEntityChangeListener(this, Blob.class);
  }

  protected void blobChanged(Blob blob) {
   operationActions_.get(updateDecider_.apply(blob)).accept(blob);
  }

  @Override
  public void entityCreated(Blob blob) {
    blobChanged(blob);
  }

  @Override
  public void entityChanged(Blob blob) {
    blobChanged(blob);
  }

  @Override
  public void entityRemoved(Blob entity) {
    thumbs_.contains(entity).ifPresent(thumbs_::removeImage);
  }

}
