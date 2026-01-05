package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.taggy.App.getApp;
import static com.bensler.taggy.ui.Icons.IMAGES_48;
import static com.bensler.taggy.ui.Icons.IMAGE_48;
import static com.bensler.taggy.ui.Icons.X_30;

import java.util.List;

import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.decaf.swing.dialog.BasicContentPanel;
import com.bensler.decaf.swing.dialog.DialogAppearance;
import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersisterImpl;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.ui.ThumbnailOverviewPanel.ScrollingPolicy;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class DeleteImagesConfirmDialog extends BasicContentPanel<List<Blob>, List<Blob>> {

  public static final DialogAppearance APPEARANCE_SINGLE = new DialogAppearance(
    new OverlayIcon(IMAGE_48, new Overlay(X_30, SE)), "Confirmation: Delete Image", "Do you really want to delete this image?"
  );

  public static final DialogAppearance APPEARANCE_MULTI = new DialogAppearance(
    new OverlayIcon(IMAGES_48, new Overlay(X_30, SE)), "Confirmation: Delete Images", "Do you really want to delete these images?"
  );

  private final ThumbnailOverviewPanel thumbs_;

  public DeleteImagesConfirmDialog(int imageCount) {
    super((imageCount > 1) ? APPEARANCE_MULTI : APPEARANCE_SINGLE, new FormLayout("f:p:g", "f:p:g"));
    thumbs_ = new ThumbnailOverviewPanel(ScrollingPolicy.SCROLL_VERTICALLY);
    add(thumbs_.getScrollpane(), new CellConstraints(1, 1));
  }

  @Override
  protected void contextSet(Context ctx) {
    ctx.setPrefs(new PrefPersisterImpl(
      getApp().getPrefs(), new WindowPrefsPersister(new PrefKey(App.PREFS_APP_ROOT, getClass()), ctx_.getDialog())
    ));
    ctx_.setCancelButtonText("No");
    ctx_.setOkButtonText("Yes");
  }

  @Override
  public List<Blob> getData() {
    return inData_;
  }

  @Override
  protected void setData(List<Blob> blobs) {
    thumbs_.setData(blobs);
  }

}
