package com.bensler.taggy.ui;

import static com.bensler.decaf.util.prefs.DelegatingPrefPersister.createSplitPanePrefPersister;
import static com.bensler.taggy.ui.Icons.SLIDESHOW_48;
import static com.bensler.taggy.ui.ThumbnailEntityListenerAdapter.Operation.ADD_OR_UPDATE;
import static com.bensler.taggy.ui.ThumbnailEntityListenerAdapter.Operation.REMOVE;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersisterImpl;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.ui.ThumbnailOverviewPanel.ScrollingPolicy;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class SlideshowFrame extends JFrame {

  private final ImageComponent imageComponent_;
  private final ThumbnailOverviewPanel thumbs_;
  private final ThumbnailEntityListenerAdapter blobChangeListener_; // prevent GC from eating it
  private final PrefPersisterImpl prefs_;

  public SlideshowFrame(App app) {
    super("Slideshow");

    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu"
    ));

    setIconImages(List.of(SLIDESHOW_48.getImage()));
    imageComponent_ = new ImageComponent();
    thumbs_ = new ThumbnailOverviewPanel(ScrollingPolicy.SCROLL_HORIZONTALLY);
    thumbs_.setFocusable();
    thumbs_.addSelectionListener((source, selection) -> setBlob(thumbs_.getSingleSelection()));
    blobChangeListener_ = new ThumbnailEntityListenerAdapter(
      app, thumbs_,
      blob -> thumbs_.contains(blob).isPresent() ? ADD_OR_UPDATE : REMOVE
    );

    final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, imageComponent_, thumbs_.getScrollpane());
    splitPane.setResizeWeight(1);
    mainPanel.add(splitPane, new CellConstraints(2, 2));
    setContentPane(mainPanel);
    pack();
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    final PrefKey baseKey = new PrefKey(App.PREFS_APP_ROOT, getClass());
    prefs_ = new PrefPersisterImpl(app.getPrefs(),
      new WindowPrefsPersister(baseKey, this),
      createSplitPanePrefPersister(new PrefKey(baseKey, "split"), splitPane)
    );
  }

  public void setBlob(Blob blob) {
    if (blob != null) {
      new Thread(() -> {
        try {
          displayImage(App.getApp().getBlobCtrl().loadRotated(blob));
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }, "Taggy.Slideshow.ImageLoader").start();
    } else {
      imageComponent_.setImage(ImageComponent.EMPTY_IMAGE);
    }
  }

  private void displayImage(BufferedImage img) {
    SwingUtilities.invokeLater(() -> imageComponent_.setImage(img));
  }

  public void close() {
    prefs_.store();
    setVisible(false);
    dispose();
  }

  public void show(List<Blob> blobs) {
    setVisible(true);
    thumbs_.setData(blobs);
    if (!blobs.isEmpty()) {
      thumbs_.select(blobs.getFirst());
    }
  }

}
