package com.bensler.taggy.ui;

import java.awt.Window;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.bensler.taggy.persist.Blob;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class BlobDialog extends JDialog {

  private final BlobController blobCtrl_;

  private ImageComponent imageComponent_;

  public BlobDialog(Window parent, BlobController blobCtrl, Blob blob) throws IOException {
    super(parent, "Blob");
    blobCtrl_ = blobCtrl;

    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu"
    ));
    imageComponent_ = new ImageComponent(ImageIO.read( blobCtrl_.getFile(blob.getSha256sum())));
    mainPanel.add(new JScrollPane(imageComponent_), new CellConstraints(2, 2));
    setContentPane(mainPanel);
    pack();
    setVisible(true);
  }

}
