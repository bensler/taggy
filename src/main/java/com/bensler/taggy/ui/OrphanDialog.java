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
import com.bensler.taggy.EntityChangeListener;
import com.bensler.taggy.persist.Blob;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class OrphanDialog extends JDialog implements EntityChangeListener<Blob> {

  private final ThumbnailOverview thumbViewer_;
  private final PrefPersisterImpl prefs_;
  private final BlobController blobCtrl_;

  public OrphanDialog(App app) {
    super(app.getMainFrame().getFrame(), "Uncategorized Files");
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu, f:p, 3dlu"
    ));

    blobCtrl_ = app.getBlobCtrl();
    thumbViewer_ = new ThumbnailOverview(app);
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
    app.addEntityChangeListener(this, Blob.class);
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

  @Override
  public void entityRemoved(Blob blob) { /* thumbViewer_ listens for rm'ed Blobs on its own */ }

  @Override
  public void entityChanged(Blob changedBlob) {
    thumbViewer_.contains(changedBlob).ifPresent(blob -> {
      if (blob.isUntagged()) {
        thumbViewer_.addImage(blob);
      } else {
        thumbViewer_.removeImage(blob);
      }
    });
  }

  @Override
  public void entityCreated(Blob blob) {
    if (blob.isUntagged()) {
      thumbViewer_.addImage(blob);
    }
  }

}
