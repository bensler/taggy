package com.bensler.taggy.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.decaf.util.prefs.DelegatingPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersister;
import com.bensler.decaf.util.prefs.Prefs;
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

  private void trySelect(List<EntityReference<Blob>> blobRefs) {
    comp_.select(blobRefs);
    if (!getSelection().isEmpty()) {
      comp_.requestFocus();
    }
  }

  public PrefPersister createPrefPersister(PrefKey prefKey) {
    return new DelegatingPrefPersister(prefKey,
      () -> Optional.of(getSelection().stream().map(blob -> blob.getId().toString()).collect(Collectors.joining(","))),
      prefStr -> trySelect(
        Arrays.stream(prefStr.split(","))
        .map(Prefs::tryParseInt).flatMap(Optional::stream)
        .map(id -> new EntityReference<>(Blob.class, id))
        .toList()
      )
    );
  }

}
