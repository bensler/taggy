package com.bensler.taggy.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

public class ImageComponent extends JComponent {

  private BufferedImage img_;
  private Image drawImg_;
  private Dimension drawImgCompSize_;

  public ImageComponent() {
    setImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
  }

  public void setImage(BufferedImage img) {
    img_ = img;
    drawImgCompSize_ = new Dimension(-1, -1);
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

    if (!size.equals(drawImgCompSize_)) {
      final double widthRatio = (img_.getWidth() / size.getWidth());
      final double heightRatio = (img_.getHeight() / size.getHeight());

      if (widthRatio > heightRatio) {
        drawImg_ = img_.getScaledInstance(size.width, -1, 0);
      } else {
        drawImg_ = img_.getScaledInstance(-1, size.height, 0);
      }
      drawImgCompSize_ = size;
    }

    g.drawImage(
      drawImg_,
      (size.width - drawImg_.getWidth(null)) / 2,
      (size.height - drawImg_.getHeight(null)) / 2,
      this
    );
  }


}
