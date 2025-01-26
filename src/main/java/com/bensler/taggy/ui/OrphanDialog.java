package com.bensler.taggy.ui;

import static com.jgoodies.forms.layout.CellConstraints.FILL;
import static com.jgoodies.forms.layout.CellConstraints.RIGHT;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.util.prefs.BulkPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.taggy.App;
import com.bensler.taggy.EntityChangeListener;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.DbAccess;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class OrphanDialog extends JDialog implements EntityChangeListener {

  private final ThumbnailOverview thumbViewer_;
  private final BulkPrefPersister prefs_;

  public OrphanDialog(App app) {
    super(app.getMainFrame().getFrame(), "Uncategorized Files");
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu, f:p, 3dlu"
    ));

    thumbViewer_ = new ThumbnailOverview(app);
    mainPanel.add(thumbViewer_.getScrollPane(), new CellConstraints(2, 2));

    final JButton closeButton = new JButton("Close");
    closeButton.addActionListener(evt -> dispose());
    mainPanel.add(closeButton, new CellConstraints(2, 4, RIGHT, FILL));

    mainPanel.setPreferredSize(new Dimension(400, 400));
    setContentPane(mainPanel);

    pack();
    prefs_ = new BulkPrefPersister(
      app.getPrefs(), new WindowPrefsPersister(new PrefKey(App.PREFS_APP_ROOT, getClass()), this)
    );
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    app.addEntityChangeListener(this);
  }

  @Override
  public void dispose() {
    prefs_.store();
    super.dispose();
  }

  public void show(DbAccess dbAccess) {
    SwingUtilities.invokeLater(() -> thumbViewer_.setData(dbAccess.findOrphanBlobs()));
    setVisible(true);
  }

  @Override
  public void entityRemoved(Object entity) {
    thumbViewer_.contains(entity).ifPresent(thumbViewer_::removeImage);
  }

  @Override
  public void entityChanged(Object entity) {
    thumbViewer_.contains(entity).ifPresent(blob -> {
      if (!blob.getTags().isEmpty()) {
        thumbViewer_.removeImage(blob);
      }
    });
  }

  @Override
  public void entityCreated(Object entity) {
    if (entity instanceof Blob blob) {
      thumbViewer_.addImage(blob);
    }
  }

}
