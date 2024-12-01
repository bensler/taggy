package com.bensler.taggy.ui;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

import java.util.Collection;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import com.bensler.decaf.swing.EntityComponent;
import com.bensler.decaf.swing.selection.EntitySelectionListener;
import com.bensler.taggy.persist.Blob;

public class ThumbnailOverview implements EntityComponent<Blob> {

  private final JScrollPane scrollPane_;
  private final ThumbnailOverviewPanel comp_;

  public ThumbnailOverview(BlobController blobCtrl) {
    comp_ = new ThumbnailOverviewPanel(blobCtrl);
    scrollPane_ = new JScrollPane(comp_, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane_.getViewport().setBackground(comp_.getBackground());

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
    return scrollPane_;
  }

  @Override
  public boolean contains(Blob entity) {
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

}
