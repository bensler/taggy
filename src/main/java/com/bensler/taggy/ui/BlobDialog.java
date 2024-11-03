package com.bensler.taggy.ui;

import java.awt.Window;
import java.io.File;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.commons.imaging.ImageReadException;

import com.bensler.taggy.Thumbnailer;
import com.bensler.taggy.persist.Blob;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class BlobDialog extends JDialog {

  private final ImageComponent imageComponent_;

  private final BlobController blobCtrl_;
  private final Thumbnailer thumbnailer_;

  public BlobDialog(Window parent) {
    super(parent, "Blob");

    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu"
    ));

    final MainFrame mainFrame = MainFrame.getInstance();
    blobCtrl_ = mainFrame.getBlobCtrl();
    thumbnailer_ = mainFrame.getThumbnailer();
    imageComponent_ = new ImageComponent();
    mainPanel.add(new JScrollPane(imageComponent_), new CellConstraints(2, 2));
    setContentPane(mainPanel);
    pack();
    setVisible(true);
  }

  public void setBlob(Blob blob) throws IOException, ImageReadException {
    final File imgFile = blobCtrl_.getFile(blob.getSha256sum());

    imageComponent_.setImage(thumbnailer_.loadRotated(imgFile));
  }
}
