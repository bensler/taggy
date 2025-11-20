package com.bensler.taggy.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.bensler.decaf.swing.action.ActionGroup;
import com.bensler.decaf.swing.action.FocusedComponentActionController;
import com.bensler.decaf.swing.action.UiAction;
import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.decaf.util.prefs.DelegatingPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersister;
import com.bensler.decaf.util.prefs.PrefsStorage;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;

class MainThumbnailOverview extends ThumbnailOverview {

  private final ImagesUiController imgUiCtrl_;
  private Optional<Tag> currentTag_;

  MainThumbnailOverview(App app) {
    super(app);
    imgUiCtrl_ = new ImagesUiController(app, comp_);
    new FocusedComponentActionController(new ActionGroup(
      imgUiCtrl_.getSlideshowAction(),
      new ActionGroup(
        imgUiCtrl_.getEditImageTagsAction(),
        imgUiCtrl_.getAddImagesTagsAction()
      ),
      imgUiCtrl_.getExportImageAction(),
      imgUiCtrl_.getDeleteImageAction()
    ), Set.of(this)).attachTo(this, overview -> {}, this::beforeCtxMenuOpen);
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

  public UiAction getSlideshowAction() {
    return imgUiCtrl_.getSlideshowAction();
  }

  public UiAction getExportImageAction() {
    return imgUiCtrl_.getExportImageAction();
  }

  public ActionGroup getToolbarActions() {
    return new ActionGroup(imgUiCtrl_.getEditImageTagsAction(), imgUiCtrl_.getAddImagesTagsAction());
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

  public List<PrefPersister> getPrefPersisters() {
    return List.of(
      imgUiCtrl_.getExportPrefPersister(),
      new DelegatingPrefPersister(new PrefKey(MainFrame.PREF_BASE_KEY, "selectedImages"),
        () -> Optional.of(getSelection().stream().map(blob -> blob.getId().toString()).collect(Collectors.joining(","))),
        prefStr -> trySelect(
          Arrays.stream(prefStr.split(","))
          .map(PrefsStorage::tryParseInt).flatMap(Optional::stream)
          .map(id -> new EntityReference<>(Blob.class, id))
          .toList()
        )
      )
    );
  }

}
