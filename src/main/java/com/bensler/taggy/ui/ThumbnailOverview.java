package com.bensler.taggy.ui;

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
import com.bensler.decaf.swing.selection.EntitySelectionListener;
import com.bensler.taggy.App;
import com.bensler.taggy.EntityChangeListener;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.ui.ThumbnailOverviewPanel.ScrollingPolicy;

public abstract class ThumbnailOverview implements EntityComponent<Blob>, FocusListener, EntityChangeListener<Blob> {

  protected final App app_;
  protected final BlobController blobCtrl_;
  protected final ThumbnailOverviewPanel comp_;
  private final Set<FocusListener> focusListeners_;

  public ThumbnailOverview(App app) {
    focusListeners_ = new HashSet<>();
    blobCtrl_ = (app_ = app).getBlobCtrl();
    comp_ = new ThumbnailOverviewPanel(app, ScrollingPolicy.SCROLL_VERTICALLY);
    comp_.setFocusable();
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
