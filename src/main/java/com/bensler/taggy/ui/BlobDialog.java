package com.bensler.taggy.ui;

import java.awt.Window;
import java.io.File;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.commons.imaging.ImageReadException;

import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class BlobDialog extends JDialog {

  private final ImageComponent imageComponent_;

  public BlobDialog(App app) {
    super((Window)null, "Blob");

    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu"
    ));

    imageComponent_ = new ImageComponent();
    mainPanel.add(new JScrollPane(imageComponent_), new CellConstraints(2, 2));
    setContentPane(mainPanel);
    pack();
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    app.getWindowSizePersister().listenTo(this);
  }

  public void setBlob(Blob blob) throws IOException, ImageReadException {
    final App app = App.getApp();
    final File imgFile = app.getBlobCtrl().getFile(blob.getSha256sum());

    imageComponent_.setImage(app.getThumbnailer().loadRotated(imgFile));
  }
}
