package com.bensler.taggy.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

public class ImageComponent extends JComponent {

  private BufferedImage img_;

  public ImageComponent() {
    img_ = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
  }

  public void setImage(BufferedImage img) {
    img_ = img;
    revalidate();
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(500, 500);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    final Dimension size = getSize();
    final double widthRatio = (img_.getWidth() / size.getWidth());
    final double heightRatio = (img_.getHeight() / size.getHeight());
    final Image drawImage;

    if (widthRatio > heightRatio) {
      drawImage = img_.getScaledInstance(size.width, -1, 0);
    } else {
      drawImage = img_.getScaledInstance(-1, size.height, 0);
    }

    g.drawImage(drawImage, 0,  0, this);
  }


}
