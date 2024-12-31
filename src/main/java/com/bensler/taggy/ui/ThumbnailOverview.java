package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.taggy.ui.MainFrame.ICON_IMAGE_13;
import static com.bensler.taggy.ui.MainFrame.ICON_TAG_13;
import static com.bensler.taggy.ui.MainFrame.ICON_X_10;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import com.bensler.decaf.swing.EntityComponent;
import com.bensler.decaf.swing.action.ActionAppearance;
import com.bensler.decaf.swing.action.ActionGroup;
import com.bensler.decaf.swing.action.ActionState;
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
import com.bensler.taggy.persist.Blob;

public class ThumbnailOverview implements EntityComponent<Blob> {

  private final App app_;
  private final JScrollPane scrollPane_;
  private final ThumbnailOverviewPanel comp_;
  private final ActionGroup<Blob> contextActions_;

  public ThumbnailOverview(App app) {
    app_ = app;
    comp_ = new ThumbnailOverviewPanel(app);
    scrollPane_ = new JScrollPane(comp_, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane_.getViewport().setBackground(comp_.getBackground());
    final EntityAction<Blob> slideshowAction = new EntityAction<>(
      new ActionAppearance(null, null, "Slide Show", "ViewImages in full detail"),
      null, (source, blobs) -> app_.getMainFrame().getSlideShowFrame().show(blobs)
    );
    final EntityAction<Blob> editImageTagsAction = new EntityAction<>(
      new ActionAppearance(ICON_TAG_13, null, "Edit Tags", "Edit Tags of this Image"),
      new SingleEntityFilter<>(ActionState.DISABLED),
      new SingleEntityActionAdapter<>((source, blob) -> blob.ifPresent(this::editTags))
    );
    final EntityAction<Blob> deleteImageAction = new EntityAction<>(
      new ActionAppearance(new OverlayIcon(ICON_IMAGE_13, new Overlay(ICON_X_10, SE)), null, "Delete Image", "Remove currently selected Image"),
      new SingleEntityFilter<>(ActionState.DISABLED),
      new SingleEntityActionAdapter<>((source, blob) -> blob.ifPresent(this::editTags))
    );
    contextActions_ = new ActionGroup<>(slideshowAction, editImageTagsAction, deleteImageAction);
    comp_.addMouseListener(new ContextMenuMouseAdapter(this::triggerContextMenu));
    comp_.addMouseListener(new DoubleClickMouseAdapter(evt -> doubleClick()));
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

  void editTags(Blob blob) {
    new OkCancelDialog<>(comp_, new EditCategoriesDialog()).show(blob, tags -> {
      blob.setTags(tags);
      app_.getDbAccess().storeObject(blob);
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
