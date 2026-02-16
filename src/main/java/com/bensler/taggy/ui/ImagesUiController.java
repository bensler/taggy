package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.action.FilteredAction.atLeastOneFilter;
import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.taggy.App.getApp;
import static com.bensler.taggy.ui.Icons.EDIT_13;
import static com.bensler.taggy.ui.Icons.EDIT_30;
import static com.bensler.taggy.ui.Icons.EXPORT_FOLDER_13;
import static com.bensler.taggy.ui.Icons.EXPORT_FOLDER_30;
import static com.bensler.taggy.ui.Icons.IMAGE_13;
import static com.bensler.taggy.ui.Icons.IMAGE_48;
import static com.bensler.taggy.ui.Icons.PLUS_10;
import static com.bensler.taggy.ui.Icons.SLIDESHOW_13;
import static com.bensler.taggy.ui.Icons.SLIDESHOW_48;
import static com.bensler.taggy.ui.Icons.TAG_SIMPLE_13;
import static com.bensler.taggy.ui.Icons.X_10;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import com.bensler.decaf.swing.action.ActionAppearance;
import com.bensler.decaf.swing.action.ActionGroup;
import com.bensler.decaf.swing.action.FilteredAction;
import com.bensler.decaf.swing.action.UiAction;
import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.decaf.swing.dialog.ConfirmationDialog;
import com.bensler.decaf.swing.dialog.DialogAppearance;
import com.bensler.decaf.swing.dialog.OkCancelDialog;
import com.bensler.decaf.util.prefs.DelegatingPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefsStorage;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;

public class ImagesUiController {

  private static final OverlayIcon EXPORT_ICON_48 = new OverlayIcon(IMAGE_48, new Overlay(EXPORT_FOLDER_30, SE));

  private final BlobController blobCtrl_;
  private final JComponent dialogParentComp_;
  private final DelegatingPrefPersister lastExportFolder_;

  private final UiAction slideshowAction_;
  private final UiAction editImageTagsAction_;
  private final UiAction addImagesTagsAction_;
  private final UiAction exportImageAction_;
  private final UiAction deleteImageAction_;
  private final ActionGroup editImageActions_;
  private final ActionGroup tagsActions_;

  public ImagesUiController(App app, JComponent dialogParentComp) {
    blobCtrl_ = app.getBlobCtrl();
    dialogParentComp_ = dialogParentComp;
    lastExportFolder_ = new DelegatingPrefPersister(new PrefKey(MainFrame.PREF_BASE_KEY, "lastExportFolder"));
    slideshowAction_ = new UiAction(
      new ActionAppearance(SLIDESHOW_13, SLIDESHOW_48, "Slide Show", "View Images in full detail"),
      FilteredAction.many(Blob.class, atLeastOneFilter(), blobs -> app.getMainFrame().getSlideshowFrame().show(blobs))
    );
    editImageTagsAction_ = new UiAction(
      new ActionAppearance(TAG_SIMPLE_13, EditImageTagsDialog.ICON, "Edit Image Tags", "Edit Tags of this Image"),
      FilteredAction.one(Blob.class, this::editTags)
    );
    addImagesTagsAction_ = new UiAction(
      new ActionAppearance(new OverlayIcon(TAG_SIMPLE_13, new Overlay(PLUS_10, SE)), AddImagesTagsDialog.ICON, "Add Image Tags", "Add Tags to several Images at once"),
      FilteredAction.many(Blob.class, atLeastOneFilter(), this::addTags)
    );
    exportImageAction_ = new UiAction(
      new ActionAppearance(EXPORT_FOLDER_13, EXPORT_ICON_48, "Export Image", "Export Image to local filesystem"),

      FilteredAction.one(Blob.class, this::exportBlobUi)
    );
    deleteImageAction_ = new UiAction(
      new ActionAppearance(new OverlayIcon(IMAGE_13, new Overlay(X_10, SE)), null, "Delete Image(s)", "Remove currently selected Image(s)"),
      FilteredAction.many(Blob.class, atLeastOneFilter(), this::deleteImagesConfirm)
    );
    tagsActions_ = new ActionGroup(
      editImageTagsAction_,
      addImagesTagsAction_
    );
    editImageActions_ = new ActionGroup(
      new ActionAppearance(
        new OverlayIcon(IMAGE_13, new Overlay(EDIT_13, SE)),
        new OverlayIcon(IMAGE_48, new Overlay(EDIT_30, SE)),
        null, "Edit Images"
      ),
      createAction(Icons.ROTATE_R_13, "Rotate Clockwise", 1),
      createAction(Icons.ROTATE_L_13, "Rotate Counterclockwise", -1)
    );
  }

  private UiAction createAction(ImageIcon icon, String menuText, int direction) {
    return new UiAction(
      new ActionAppearance(icon, null, menuText, null),
      FilteredAction.one(Blob.class, blob -> rotateBlob(blob, direction))
    );
  }

  private void rotateBlob(Blob blob, int direction) {
    try {
      blobCtrl_.rotateBlob(blob, direction);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public ActionGroup getAllActions() {
    return new ActionGroup(
      slideshowAction_,
      tagsActions_,
      exportImageAction_,
      editImageActions_,
      deleteImageAction_
    );
  }

  public ActionGroup getEditImageActions() {
    return editImageActions_;
  }

  public UiAction getSlideshowAction() {
    return slideshowAction_;
  }

  public ActionGroup getTagsActions() {
    return tagsActions_;
  }

  public UiAction getExportImageAction() {
    return exportImageAction_;
  }

  public DelegatingPrefPersister getExportPrefPersister() {
    return lastExportFolder_;
  }

  private void deleteImagesConfirm(List<Blob> blobs) {
    new OkCancelDialog<>(dialogParentComp_, new DeleteImagesConfirmDialog(blobs.size())).show(blobs)
    .ifPresent(blobsToDelete -> blobsToDelete.stream().flatMap(List::stream).forEach(blobCtrl_::deleteBlob));
  }

  private void addTags(List<Blob> blobs) {
    new OkCancelDialog<>(dialogParentComp_, new AddImagesTagsDialog()).show(blobs, tags -> blobCtrl_.addTags(blobs, tags));
  }

  private void editTags(Blob blob) {
    new OkCancelDialog<>(dialogParentComp_, new EditImageTagsDialog()).show(blob, tags -> blobCtrl_.setTags(blob, tags));
  }

  private void exportBlobUi(Blob blob) {
    final App app = getApp();
    final PrefsStorage prefs = app.getMainFrame().getPrefStorage();
    final JFrame frame = app.getMainFrameFrame();
    final JFileChooser chooser = new JFileChooser();
    final BlobController blobCtrl = app.getBlobCtrl();
    File file = new File(
      lastExportFolder_.get(prefs).orElseGet(() -> System.getProperty("user.home")),
      blobCtrl.getTagString(blob)
    );

    chooser.setSelectedFile(file);
    if (
      (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION)
      && (
        !(file = chooser.getSelectedFile()).exists()
        || new ConfirmationDialog(new DialogAppearance(
          EXPORT_ICON_48, "Confirmation: Overwrite File",
          "File \"%s\" already exists. Do you really want to overwrite it?".formatted(file.getName())
        )).confirm(frame)
      )
    ) {
      lastExportFolder_.put(prefs, file.getParent());
      blobCtrl.export(blob, file);
    };
  }

}
