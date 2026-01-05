package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.taggy.ui.Icons.IMAGE_48;
import static com.bensler.taggy.ui.Icons.TAGS_MISSING_36;

import java.awt.Dimension;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.bensler.decaf.swing.action.FocusedComponentActionController;
import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.decaf.swing.dialog.DialogAppearance;
import com.bensler.decaf.swing.dialog.HeaderPanel;
import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersisterImpl;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class OrphanDialog extends JDialog {

  public static final OverlayIcon ICON = new OverlayIcon(IMAGE_48, new Overlay(TAGS_MISSING_36, SE));

  private final ThumbnailOverview thumbViewer_;
  private final PrefPersisterImpl prefs_;
  private final BlobController blobCtrl_;

  public OrphanDialog(App app) {
    super(app.getMainFrameFrame());
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "f:p, 3dlu, f:p:g, 3dlu"
    ));

    var appearance = new DialogAppearance(ICON, "Untagged Images", "Images, having no Tags assigned. Assign them here!");
    setTitle(appearance.getWindowTitle());
    mainPanel.add(new HeaderPanel(appearance).getComponent(), new CellConstraints(1, 1, 3, 1));
    blobCtrl_ = app.getBlobCtrl();
    thumbViewer_ = new ThumbnailOverview(app) {
      @Override
      protected void blobChanged(Blob blob) {
        if (blob.isUntagged()) {
          thumbViewer_.addImage(blob);
        } else {
          thumbViewer_.removeImage(blob);
        }
      }
    };
    mainPanel.add(thumbViewer_.getScrollPane(), new CellConstraints(2, 3));

    final ImagesUiController imgUiCtrl_ = new ImagesUiController(app, thumbViewer_.getComponent());
    new FocusedComponentActionController(imgUiCtrl_.getAllActions(), Set.of(thumbViewer_)).attachTo(thumbViewer_, overview -> {}, thumbViewer_::beforeCtxMenuOpen);

    mainPanel.setPreferredSize(new Dimension(400, 400));
    setContentPane(mainPanel);

    pack();
    prefs_ = new PrefPersisterImpl(
      app.getPrefs(), new WindowPrefsPersister(new PrefKey(App.PREFS_APP_ROOT, getClass()), this)
    );
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
  }

  @Override
  public void dispose() {
    prefs_.store();
    super.dispose();
  }

  public void showDialog() {
    SwingUtilities.invokeLater(() -> thumbViewer_.setData(blobCtrl_.findOrphanBlobs()));
    setVisible(true);
  }

}
