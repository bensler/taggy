package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.taggy.ui.Icons.EDIT_30;
import static com.bensler.taggy.ui.Icons.IMAGES_48;

import javax.swing.ImageIcon;

import org.imgscalr.Scalr;

import com.bensler.decaf.swing.action.ActionAppearance;
import com.bensler.decaf.swing.action.ActionGroup;
import com.bensler.decaf.swing.action.FilteredAction;
import com.bensler.decaf.swing.action.UiAction;
import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.taggy.persist.Blob;

public class BlobUiController {

  private final ActionGroup editImageActions_;

  public BlobUiController() {
    editImageActions_ = new ActionGroup(
      new ActionAppearance(new OverlayIcon(IMAGES_48, new Overlay(EDIT_30, SE)), null, null, "Edit Images"),
      createAction(Icons.ROTATE_R_13, "Rotate Clockwise", Scalr.Rotation.CW_90),
      createAction(Icons.ROTATE_L_13, "Rotate Counterclockwise", Scalr.Rotation.CW_270),
      createAction(Icons.FLIP_H_13, "Flip Horizontally", Scalr.Rotation.FLIP_HORZ),
      createAction(Icons.FLIP_V_13, "Flip Vertically", Scalr.Rotation.FLIP_VERT)
    );
  }

  private UiAction createAction(ImageIcon icon, String menuText, Scalr.Rotation imageChange) {
    return new UiAction(
      new ActionAppearance(icon, null, menuText, null),
      FilteredAction.one(Blob.class, blob -> editImage(blob, imageChange))
    );
  }

  public ActionGroup getEditImageActions() {
    return editImageActions_;
  }

  private void editImage(Blob image, Scalr.Rotation imageChange) {
    System.out.println(image + " -> " + imageChange);
  }

}
