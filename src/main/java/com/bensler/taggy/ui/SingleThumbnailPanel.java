package com.bensler.taggy.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import com.bensler.taggy.persist.Blob;

public class SingleThumbnailPanel extends JComponent {

  private final BlobController blobController_;

  private BufferedImage currentImage_;

  public SingleThumbnailPanel(BlobController blobController) {
    blobController_ = blobController;
  }

  public void showThumbnail(Blob blob) {
    currentImage_ = null;
    if (blob != null) {
      try {
        currentImage_ = ImageIO.read(blobController_.getFile(blob.getThumbnailSha()));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(200, 200);
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (currentImage_ != null) {
      ((Graphics2D)g).drawImage(currentImage_, 0, 0, this);
    }
  }

}
