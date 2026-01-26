package com.bensler.taggy.ui;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JScrollPane;

import com.bensler.decaf.swing.EntityComponent;
import com.bensler.decaf.swing.selection.EntitySelectionListener;
import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.decaf.util.prefs.DelegatingPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefsStorage;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.ui.ThumbnailOverviewPanel.ScrollingPolicy;

public class ThumbnailOverview implements EntityComponent<Blob>, FocusListener {

  protected final BlobController blobCtrl_;
  protected final ThumbnailOverviewPanel comp_;
  private final Set<FocusListener> focusListeners_;

  public ThumbnailOverview(ScrollingPolicy scrollingPolicy, BlobController blobCtrl) {
    focusListeners_ = new HashSet<>();
    blobCtrl_ = blobCtrl;
    comp_ = new ThumbnailOverviewPanel(scrollingPolicy);
    comp_.setFocusable();
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
  public ThumbnailOverviewPanel getComponent() {
    return comp_;
  }

  @Override
  public JScrollPane getScrollPane() {
    return comp_.getScrollPane();
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

  @Override
  public void addFocusListener(FocusListener listener) {
    focusListeners_.add(Objects.requireNonNull(listener));
  }

  private void trySelect(List<EntityReference<Blob>> blobRefs) {
    comp_.select(blobRefs);
    if (!getSelection().isEmpty()) {
      comp_.requestFocus();
    }
  }

  public DelegatingPrefPersister getPrefPersisters(PrefKey prefKey) {
    return new DelegatingPrefPersister(prefKey,
      () -> Optional.of(getSelection().stream().map(blob -> blob.getId().toString()).collect(Collectors.joining(","))),
      prefStr -> trySelect(
        Arrays.stream(prefStr.split(","))
        .map(PrefsStorage::tryParseInt).flatMap(Optional::stream)
        .map(id -> new EntityReference<>(Blob.class, id))
        .toList()
      )
    );
  }

}
