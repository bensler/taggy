package com.bensler.taggy.ui;

import java.util.Optional;

import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;

class MainThumbnailOverview extends ThumbnailOverview {

  private Optional<Tag> currentTag_;

  MainThumbnailOverview(App app) {
    super(app);
  }

  @Override
  protected void blobChanged(Blob blob) {
    currentTag_.ifPresent(tag -> {
      if (blob.containsTag(tag)) {
        addImage(blob);
      } else {
        removeImage(blob);
      }
    });
  }

  public void setData(Optional<Tag> tag) {
    (currentTag_ = tag).ifPresentOrElse(lTag -> setData(lTag.getBlobs()), this::clear);
  }

}
