package com.bensler.taggy.ui;

import static com.jgoodies.forms.layout.CellConstraints.FILL;
import static com.jgoodies.forms.layout.CellConstraints.RIGHT;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersisterImpl;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class OrphanDialog extends JDialog {

  private final ThumbnailOverview thumbViewer_;
  private final PrefPersisterImpl prefs_;
  private final BlobController blobCtrl_;

  public OrphanDialog(App app) {
    super(app.getMainFrameFrame(), "Uncategorized Files");
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu, f:p, 3dlu"
    ));

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
    mainPanel.add(thumbViewer_.getScrollPane(), new CellConstraints(2, 2));

    final JButton closeButton = new JButton("Close");
    closeButton.addActionListener(evt -> dispose());
    mainPanel.add(closeButton, new CellConstraints(2, 4, RIGHT, FILL));

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
