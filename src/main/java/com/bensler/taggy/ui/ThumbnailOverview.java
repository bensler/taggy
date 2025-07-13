package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.action.ActionState.DISABLED;
import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.taggy.ui.MainFrame.ICON_IMAGE_13;
import static com.bensler.taggy.ui.MainFrame.ICON_PLUS_10;
import static com.bensler.taggy.ui.MainFrame.ICON_SLIDESHOW_13;
import static com.bensler.taggy.ui.MainFrame.ICON_SLIDESHOW_48;
import static com.bensler.taggy.ui.MainFrame.ICON_TAG_13;
import static com.bensler.taggy.ui.MainFrame.ICON_X_10;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

public class ThumbnailOverview implements EntityComponent<Blob>, FocusListener {

  private final App app_;
  private final BlobController blobCtrl_;
  private final ThumbnailOverviewPanel comp_;
  private final ActionGroup contextActions_;
  @SuppressWarnings("unused") // keep it referenced as App holds it weakly
  private final EntityChangeListener<Blob> entityRemoveListener_;
  private final EntityAction<Blob> slideshowAction_;
  private final Set<FocusListener> focusListeners_;

  public ThumbnailOverview(App app) {
    focusListeners_ = new HashSet<>();
    blobCtrl_ = (app_ = app).getBlobCtrl();
    comp_ = new ThumbnailOverviewPanel(app, ScrollingPolicy.SCROLL_VERTICALLY);
    comp_.setFocusable();
    slideshowAction_ = new EntityAction<>(
      new ActionAppearance(ICON_SLIDESHOW_13, ICON_SLIDESHOW_48, "Slide Show", "ViewImages in full detail"),
      Blob.class, null, (source, blobs) -> app_.getMainFrame().getSlideshowFrame().show(blobs)
    );
    final EntityAction<Blob> editImageTagsAction = new EntityAction<>(
      new ActionAppearance(ICON_TAG_13, null, "Edit Image Tags", "Edit Tags of this Image"),
      Blob.class, new SingleEntityFilter<>(DISABLED),
      new SingleEntityActionAdapter<>((source, blob) -> blob.ifPresent(this::editTags))
    );
    final EntityAction<Blob> addImageTagsAction = new EntityAction<>(
      new ActionAppearance(new OverlayIcon(ICON_TAG_13, new Overlay(ICON_PLUS_10, SE)), null, "Add Image Tags", "Add Tags to several Images at once"),
      Blob.class, null, (source, blobs) -> addTags(blobs)
    );
    final EntityAction<Blob> deleteImageAction = new EntityAction<>(
      new ActionAppearance(new OverlayIcon(ICON_IMAGE_13, new Overlay(ICON_X_10, SE)), null, "Delete Image(s)", "Remove currently selected Image(s)"),
      Blob.class, EntityAction.atLeastOneFilter(DISABLED), (source, blobs) -> deleteImagesConfirm(blobs)
    );
    contextActions_ = new ActionGroup(slideshowAction_, editImageTagsAction, addImageTagsAction, deleteImageAction);
    comp_.addMouseListener(new ContextMenuMouseAdapter(this::triggerContextMenu));
    comp_.addMouseListener(new DoubleClickMouseAdapter(evt -> doubleClick()));
    app_.addEntityChangeListener(entityRemoveListener_ = new EntityRemovedAdapter<>(entity -> contains(entity).ifPresent(this::removeImage)), Blob.class);
    comp_.addFocusListener(this);
  }

  @Override
  public void focusLost(FocusEvent e) {
    focusLost();
  }

  @Override
  public void focusGained(FocusEvent e) {
    focusGained();
  }

  private void focusLost() {
    comp_.repaint();
    focusListeners_.forEach(l -> l.focusLost(this));
  }


  private void focusGained() {
    comp_.repaint();
    focusListeners_.forEach(l -> l.focusGained(this));
  }

  @Override
  public Class<Blob> getEntityClass() {
    return Blob.class;
  }

  public EntityAction<Blob> getSlideshowAction() {
    return slideshowAction_;
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
    .ifPresent(blobsToDelete -> blobsToDelete.stream().flatMap(List::stream).forEach(blobCtrl_::deleteBlob));
  }

  void addTags(List<Blob> blobs) {
    new OkCancelDialog<>(comp_, new AddImagesTagsDialog()).show(blobs, tags -> blobCtrl_.addTags(blobs, tags));
  }

  void editTags(Blob blob) {
    new OkCancelDialog<>(comp_, new EditImageTagsDialog()).show(blob, tags -> blobCtrl_.setTags(blob, tags));
  }

  @Override
  public List<Blob> getSelection() {
    return comp_.getSelection();
  }

  @Override
  public void addSelectionListener(EntitySelectionListener<Blob> listener) {
    // TODO rm this hack ------vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    comp_.addSelectionListener((source, selection) -> listener.selectionChanged(this, selection));
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

  @Override
  public void addFocusListener(FocusListener listener) {
    focusListeners_.add(Objects.requireNonNull(listener));
  }

}
