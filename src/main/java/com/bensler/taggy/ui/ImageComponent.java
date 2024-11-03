package com.bensler.taggy.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

public class ImageComponent extends JComponent {

  private BufferedImage img_;

  public ImageComponent(BufferedImage img) {
    setImage(img);
  }

  public void setImage(BufferedImage img) {
    img_ = img;
    revalidate();
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(img_.getWidth(), img_.getHeight());
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    g.drawImage(img_, 0,  0,this);
  }


}
