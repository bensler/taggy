package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.action.FilteredAction.atLeastOneFilter;
import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.taggy.ui.MainFrame.ICON_IMAGE_13;
import static com.bensler.taggy.ui.MainFrame.ICON_PLUS_10;
import static com.bensler.taggy.ui.MainFrame.ICON_SLIDESHOW_13;
import static com.bensler.taggy.ui.MainFrame.ICON_SLIDESHOW_48;
import static com.bensler.taggy.ui.MainFrame.ICON_TAG_SIMPLE_13;
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
import com.bensler.decaf.swing.action.FilteredAction;
import com.bensler.decaf.swing.action.FocusedComponentActionController;
import com.bensler.decaf.swing.action.UiAction;
import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.decaf.swing.dialog.OkCancelDialog;
import com.bensler.decaf.swing.selection.EntitySelectionListener;
import com.bensler.taggy.App;
import com.bensler.taggy.EntityChangeListener;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.ui.ThumbnailOverviewPanel.ScrollingPolicy;

public abstract class ThumbnailOverview implements EntityComponent<Blob>, FocusListener, EntityChangeListener<Blob> {

  private final App app_;
  private final BlobController blobCtrl_;
  protected final ThumbnailOverviewPanel comp_;

  private final UiAction editImageTagsAction_;
  private final UiAction addImagesTagsAction_;
  private final UiAction slideshowAction_;
  private final Set<FocusListener> focusListeners_;

  public ThumbnailOverview(App app) {
    focusListeners_ = new HashSet<>();
    blobCtrl_ = (app_ = app).getBlobCtrl();
    comp_ = new ThumbnailOverviewPanel(app, ScrollingPolicy.SCROLL_VERTICALLY);
    comp_.setFocusable();
    slideshowAction_ = new UiAction(
      new ActionAppearance(ICON_SLIDESHOW_13, ICON_SLIDESHOW_48, "Slide Show", "View Images in full detail"),
      FilteredAction.many(Blob.class, atLeastOneFilter(), blobs -> app_.getMainFrame().getSlideshowFrame().show(blobs))
    );
    editImageTagsAction_ = new UiAction(
      new ActionAppearance(ICON_TAG_SIMPLE_13, EditImageTagsDialog.ICON, "Edit Image Tags", "Edit Tags of this Image"),
      FilteredAction.one(Blob.class, this::editTags)
    );
    addImagesTagsAction_ = new UiAction(
      new ActionAppearance(new OverlayIcon(ICON_TAG_SIMPLE_13, new Overlay(ICON_PLUS_10, SE)), AddImagesTagsDialog.ICON, "Add Image Tags", "Add Tags to several Images at once"),
      FilteredAction.many(Blob.class, atLeastOneFilter(), this::addTags)
    );
    final UiAction deleteImageAction = new UiAction(
      new ActionAppearance(new OverlayIcon(ICON_IMAGE_13, new Overlay(ICON_X_10, SE)), null, "Delete Image(s)", "Remove currently selected Image(s)"),
      FilteredAction.many(Blob.class, atLeastOneFilter(), this::deleteImagesConfirm)
    );
    new FocusedComponentActionController(
      new ActionGroup(slideshowAction_, new ActionGroup(editImageTagsAction_, addImagesTagsAction_), deleteImageAction), Set.of(this)
    ).attachTo(this, overview -> {}, this::beforeCtxMenuOpen);
    app_.addEntityChangeListener(this, Blob.class);
    comp_.addFocusListener(this);
  }

  public void beforeCtxMenuOpen(MouseEvent evt) {
    comp_.blobAt(evt.getPoint()).ifPresentOrElse(blob -> {
      if (!getSelection().contains(blob)) {
        select(blob);
      }
    }, this::clearSelection);
  }

  @Override
  public void focusLost(FocusEvent e) { }

  @Override
  public void focusGained(FocusEvent e) {
    comp_.repaint();
    focusListeners_.forEach(l -> l.focusGained(this));
  }

  @Override
  public Class<Blob> getEntityClass() {
    return Blob.class;
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
  public void select(Collection<?> entities) {
    comp_.select(entities);
  }

  @Override
  public void select(Object entity) {
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

  public void setData(Collection<Blob> blobs) {
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

  @Override
  public void entityCreated(Blob blob) {
    blobChanged(blob);
  }

  @Override
  public void entityChanged(Blob blob) {
    blobChanged(blob);
  }

  protected abstract void blobChanged(Blob blob);

  @Override
  public void entityRemoved(Blob entity) {
    contains(entity).ifPresent(this::removeImage);
  }

}
