package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.action.ActionState.DISABLED;
import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.taggy.ui.MainFrame.ICON_IMAGE_13;
import static com.bensler.taggy.ui.MainFrame.ICON_SLIDESHOW_13;
import static com.bensler.taggy.ui.MainFrame.ICON_TAG_13;
import static com.bensler.taggy.ui.MainFrame.ICON_X_10;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import com.bensler.decaf.swing.EntityComponent;
import com.bensler.decaf.swing.action.ActionAppearance;
import com.bensler.decaf.swing.action.ActionGroup;
import com.bensler.decaf.swing.action.ContextMenuMouseAdapter;
import com.bensler.decaf.swing.action.DoubleClickMouseAdapter;
import com.bensler.decaf.swing.action.EntityAction;
import com.bensler.decaf.swing.action.SingleEntityActionAdapter;
import com.bensler.decaf.swing.action.SingleEntityFilter;
import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.decaf.swing.dialog.OkCancelDialog;
import com.bensler.decaf.swing.selection.EntitySelectionListener;
import com.bensler.taggy.App;
import com.bensler.taggy.EntityChangeListener;
import com.bensler.taggy.EntityChangeListener.EntityRemovedAdapter;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.ui.ThumbnailOverviewPanel.ScrollingPolicy;

public class ThumbnailOverview implements EntityComponent<Blob> {

  private final App app_;
  private final BlobController blobCtrl_;
  private final ThumbnailOverviewPanel comp_;
  private final ActionGroup<Blob> contextActions_;
  @SuppressWarnings("unused") // keep it referenced as App holds it weakly
  private final EntityChangeListener entityRemoveListener_;

  public ThumbnailOverview(App app) {
    blobCtrl_ = (app_ = app).getBlobCtrl();
    comp_ = new ThumbnailOverviewPanel(app, ScrollingPolicy.SCROLL_VERTICALLY);
    comp_.setFocusable();
    final EntityAction<Blob> slideshowAction = new EntityAction<>(
      new ActionAppearance(ICON_SLIDESHOW_13, null, "Slide Show", "ViewImages in full detail"),
      null, (source, blobs) -> app_.getMainFrame().getSlideshowFrame().show(blobs)
    );
    final EntityAction<Blob> editImageTagsAction = new EntityAction<>(
      new ActionAppearance(ICON_TAG_13, null, "Edit Image Tags", "Edit Tags of this Image"),
      new SingleEntityFilter<>(DISABLED),
      new SingleEntityActionAdapter<>((source, blob) -> blob.ifPresent(this::editTags))
    );
    final EntityAction<Blob> deleteImageAction = new EntityAction<>(
      new ActionAppearance(new OverlayIcon(ICON_IMAGE_13, new Overlay(ICON_X_10, SE)), null, "Delete Image(s)", "Remove currently selected Image(s)"),
      EntityAction.atLeastOneFilter(DISABLED), (source, blobs) -> deleteImagesConfirm(blobs)
    );
    contextActions_ = new ActionGroup<>(slideshowAction, editImageTagsAction, deleteImageAction);
    comp_.addMouseListener(new ContextMenuMouseAdapter(this::triggerContextMenu));
    comp_.addMouseListener(new DoubleClickMouseAdapter(evt -> doubleClick()));
    app_.addEntityChangeListener(entityRemoveListener_ = new EntityRemovedAdapter(entity -> contains(entity).ifPresent(this::removeImage)));
  }

  void doubleClick() {
    contextActions_.createContextMenu(this).triggerPrimaryAction();
  }

  void triggerContextMenu(MouseEvent evt) {
    final Optional<Blob> clickedBlob = comp_.blobAt(evt.getPoint());

    clickedBlob.ifPresent(blob -> {
      if (!getSelection().contains(blob)) {
        select(blob);
      }
    });
    contextActions_.createContextMenu(this).showPopupMenu(evt);
  }

  void deleteImagesConfirm(List<Blob> blobs) {
    new OkCancelDialog<>(comp_, new DeleteImagesConfirmDialog(blobs.size())).show(blobs)
    .stream().flatMap(List::stream).forEach(blobCtrl_::deleteBlob);
  }

  void editTags(Blob blob) {
    new OkCancelDialog<>(comp_, new EditImageTagsDialog()).show(blob, tags -> {
      blob.setTags(tags);
      app_.storeEntity(blob);
      app_.getMainFrame().displayThumbnailsOfSelectedTag();
    });
  }

  @Override
  public List<Blob> getSelection() {
    return comp_.getSelection();
  }

  @Override
  public Blob getSingleSelection() {
    return comp_.getSingleSelection();
  }

  @Override
  public void setSelectionListener(EntitySelectionListener<Blob> listener) {
    comp_.setSelectionListener(listener);
  }

  @Override
  public void clearSelection() {
    comp_.clearSelection();
  }

  @Override
  public void select(Collection<Blob> entities) {
    comp_.select(entities);
  }

  @Override
  public void select(Blob entity) {
    comp_.select(entity);
  }

  @Override
  public JComponent getComponent() {
    return comp_;
  }

  @Override
  public JScrollPane getScrollPane() {
    return comp_.getScrollpane();
  }

  @Override
  public Optional<Blob> contains(Object entity) {
    return comp_.contains(entity);
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  public void setData(List<Blob> blobs) {
    comp_.setData(blobs);
  }

  public void clear() {
    comp_.clear();
  }

  public void addImage(Blob blob) {
    comp_.addImage(blob);
  }

  public void removeImage(Blob blob) {
    comp_.removeImage(blob);
  }

}
