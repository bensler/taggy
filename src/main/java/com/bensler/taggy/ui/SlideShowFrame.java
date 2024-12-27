package com.bensler.taggy.ui;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.commons.imaging.ImageReadException;

import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.util.prefs.BulkPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class SlideShowFrame extends JFrame {

  private final ImageComponent imageComponent_;
  private final BulkPrefPersister prefs_;

  public SlideShowFrame(App app) {
    super("View Image");

    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu"
    ));

    imageComponent_ = new ImageComponent();
    mainPanel.add(new JScrollPane(imageComponent_), new CellConstraints(2, 2));
    setContentPane(mainPanel);
    pack();
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    prefs_ = new BulkPrefPersister(
      app.getPrefs(), new WindowPrefsPersister(new PrefKey(App.PREFS_APP_ROOT, getClass()), this)
    );
  }

  public void setBlob(Blob blob) throws IOException, ImageReadException {
    final App app = App.getApp();
    final File imgFile = app.getBlobCtrl().getFile(blob.getSha256sum());

    imageComponent_.setImage(app.getThumbnailer().loadRotated(imgFile));
  }

  public void close() {
    prefs_.store();
    setVisible(false);
    dispose();
  }

  public void show(List<Blob> blobs) {
    try {
      setVisible(true);
      setBlob(blobs.get(0));
    } catch (IOException | ImageReadException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
