package com.bensler.taggy.ui;

import static com.jgoodies.forms.layout.CellConstraints.FILL;
import static com.jgoodies.forms.layout.CellConstraints.RIGHT;

import java.awt.Dimension;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.bensler.taggy.App;
import com.bensler.taggy.persist.DbAccess;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class OrphanDialog extends JDialog {

  private final ThumbnailOverview thumbViewer_;

  public OrphanDialog(App app) {
    super((Window)null, "Uncategorized Files");
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
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
  }

  public void show(DbAccess dbAccess) {
    SwingUtilities.invokeLater(() -> thumbViewer_.setData(dbAccess.findOrphanBlobs()));
    setVisible(true);
  }

}
