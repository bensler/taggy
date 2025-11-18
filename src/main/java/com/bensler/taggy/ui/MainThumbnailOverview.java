package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.action.FilteredAction.atLeastOneFilter;
import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.taggy.ui.Icons.EXPORT_FOLDER_13;
import static com.bensler.taggy.ui.Icons.EXPORT_FOLDER_30;
import static com.bensler.taggy.ui.Icons.IMAGES_48;
import static com.bensler.taggy.ui.Icons.IMAGE_13;
import static com.bensler.taggy.ui.Icons.PLUS_10;
import static com.bensler.taggy.ui.Icons.SLIDESHOW_13;
import static com.bensler.taggy.ui.Icons.SLIDESHOW_48;
import static com.bensler.taggy.ui.Icons.TAG_SIMPLE_13;
import static com.bensler.taggy.ui.Icons.X_10;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;

import com.bensler.decaf.swing.action.ActionAppearance;
import com.bensler.decaf.swing.action.ActionGroup;
import com.bensler.decaf.swing.action.FilteredAction;
import com.bensler.decaf.swing.action.FocusedComponentActionController;
import com.bensler.decaf.swing.action.UiAction;
import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.decaf.swing.dialog.ConfirmationDialog;
import com.bensler.decaf.swing.dialog.DialogAppearance;
import com.bensler.decaf.swing.dialog.OkCancelDialog;
import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.decaf.util.prefs.DelegatingPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersister;
import com.bensler.decaf.util.prefs.PrefsStorage;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;

class MainThumbnailOverview extends ThumbnailOverview {

  private final PrefKey prefBaseKey_;
  private final UiAction exportImageAction_;
  private final DelegatingPrefPersister lastExportFolder_;
  private final UiAction editImageTagsAction_;
  private final UiAction addImagesTagsAction_;
  private final UiAction slideshowAction_;
  private Optional<Tag> currentTag_;

  MainThumbnailOverview(App app, PrefKey prefBaseKey) {
    super(app);
    prefBaseKey_ = prefBaseKey;
    exportImageAction_ = new UiAction(
      new ActionAppearance(EXPORT_FOLDER_13, new OverlayIcon(IMAGES_48, new Overlay(EXPORT_FOLDER_30, SE)), "Export Image", "Export Image to local filesystem"),
      FilteredAction.one(Blob.class, this::exportBlobUi)
    );
    lastExportFolder_ = new DelegatingPrefPersister(new PrefKey(prefBaseKey_, "lastExportFolder"));
    slideshowAction_ = new UiAction(
      new ActionAppearance(SLIDESHOW_13, SLIDESHOW_48, "Slide Show", "View Images in full detail"),
      FilteredAction.many(Blob.class, atLeastOneFilter(), blobs -> app_.getMainFrame().getSlideshowFrame().show(blobs))
    );
    editImageTagsAction_ = new UiAction(
      new ActionAppearance(TAG_SIMPLE_13, EditImageTagsDialog.ICON, "Edit Image Tags", "Edit Tags of this Image"),
      FilteredAction.one(Blob.class, this::editTags)
    );
    addImagesTagsAction_ = new UiAction(
      new ActionAppearance(new OverlayIcon(TAG_SIMPLE_13, new Overlay(PLUS_10, SE)), AddImagesTagsDialog.ICON, "Add Image Tags", "Add Tags to several Images at once"),
      FilteredAction.many(Blob.class, atLeastOneFilter(), this::addTags)
    );
    final UiAction deleteImageAction = new UiAction(
      new ActionAppearance(new OverlayIcon(IMAGE_13, new Overlay(X_10, SE)), null, "Delete Image(s)", "Remove currently selected Image(s)"),
      FilteredAction.many(Blob.class, atLeastOneFilter(), this::deleteImagesConfirm)
    );
    new FocusedComponentActionController(
      new ActionGroup(slideshowAction_, new ActionGroup(editImageTagsAction_, addImagesTagsAction_), exportImageAction_, deleteImageAction), Set.of(this)
    ).attachTo(this, overview -> {}, this::beforeCtxMenuOpen);
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

  public UiAction getExportImageAction() {
    return exportImageAction_;
  }

  public UiAction getSlideshowAction() {
    return slideshowAction_;
  }

  public UiAction getEditImageTagsAction() {
    return editImageTagsAction_;
  }

  public ActionGroup getToolbarActions() {
    return new ActionGroup(editImageTagsAction_, addImagesTagsAction_);
  }

  void deleteImagesConfirm(List<Blob> blobs) {
    new OkCancelDialog<>(comp_, new DeleteImagesConfirmDialog(blobs.size())).show(blobs)
    .ifPresent(blobsToDelete -> blobsToDelete.stream().flatMap(List::stream).forEach(blobCtrl_::deleteBlob));
  }

  void addTags(List<Blob> blobs) {
    new OkCancelDialog<>(comp_, new AddImagesTagsDialog()).show(blobs, tags -> blobCtrl_.addTags(blobs, tags));
  }

  void editTags(Blob blob) {
    new OkCancelDialog<>(comp_, new EditImageTagsDialog()).show(blob, tags -> blobCtrl_.setTags(blob, tags));
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

  void exportBlobUi(Blob blob) {
    final MainFrame mainFrame = app_.getMainFrame();
    final JFileChooser chooser = new JFileChooser();
    final BlobController blobCtrl = app_.getBlobCtrl();
    File file = new File(
      lastExportFolder_.get(mainFrame.getPrefStorage()).orElseGet(() -> System.getProperty("user.home")),
      blobCtrl.getTagString(blob)
    );

    chooser.setSelectedFile(file);
    if (
      (chooser.showSaveDialog(mainFrame.getFrame()) == JFileChooser.APPROVE_OPTION)
      && (
        !(file = chooser.getSelectedFile()).exists()
        || new ConfirmationDialog(new DialogAppearance(
          new OverlayIcon(IMAGES_48, new Overlay(EXPORT_FOLDER_30, SE)), "Confirmation: Overwrite File",
          "File \"%s\" already exists. Do you really want to overwrite it?".formatted(file.getName())
        )).show(mainFrame.getFrame())
      )
    ) {
      lastExportFolder_.put(mainFrame.getPrefStorage(), file.getParent());
      blobCtrl.export(blob, file);
    };
  }

  public List<PrefPersister> getPrefPersisters() {
    return List.of(
      lastExportFolder_,
      new DelegatingPrefPersister(new PrefKey(prefBaseKey_, "selectedImages"),
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
