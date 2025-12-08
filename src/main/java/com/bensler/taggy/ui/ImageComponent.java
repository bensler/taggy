package com.bensler.taggy.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.Optional;

import javax.swing.JComponent;

import com.bensler.decaf.swing.awt.MouseDragCtrl;
import com.bensler.decaf.util.TimerTrap;

public class ImageComponent extends JComponent {

  private BufferedImage img_;
  private Image drawImg_;
  private Dimension lastCompSize_;
  private Dimension drawImgSize_;
  private Point imgOrigin_;
  private Dimension drawImgDrag_;
  private float zoomFactor_;

  public ImageComponent() {
    setImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
    addMouseWheelListener(this::mouseWheelWheeled);
    new MouseDragCtrl(this, this::mouseDragging, this::mouseDragged);
  }

  void mouseDragged(Point origin, Point position) {
    imgOrigin_.translate(drawImgDrag_.width, drawImgDrag_.height);
    drawImgDrag_ = new Dimension();
    repaint();
  }

  void mouseDragging(Point origin, Point position) {
    drawImgDrag_ = new Dimension(position.x - origin.x, position.y - origin.y);
    repaint();
  }

  public void setImage(BufferedImage img) {
    img_ = img;
    lastCompSize_ = new Dimension(-1, -1);
    drawImgSize_ = new Dimension(-1, -1);
    imgOrigin_ = new Point(0, 0);
    drawImgDrag_ = new Dimension();
    zoomFactor_ = 1.0f;
    revalidate();
    repaint();
  }

  private void mouseWheelWheeled(MouseWheelEvent evt) {
    if (
      (drawImgDrag_.width == 0) && (drawImgDrag_.height == 0) // no ongoing drag operation
      && (evt.getPoint() instanceof Point point) // local var
      && (point.x > imgOrigin_.x) && (point.x < (imgOrigin_.x + drawImgSize_.width))  // being over the
      && (point.y > imgOrigin_.y) && (point.y < (imgOrigin_.y + drawImgSize_.height)) // actual image
    ) {
      // -1 or just negative: zoom in
      // +1 or just positive: zoom out
      int wheelRotation = evt.getWheelRotation();
      System.out.println(wheelRotation + " # " +evt.getPoint());
    }
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(100, 100);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    final Dimension size = getSize();

    if (!size.equals(lastCompSize_)) {
      final double widthRatio = (img_.getWidth() / size.getWidth());
      final double heightRatio = (img_.getHeight() / size.getHeight());
      final double ratio;

      try (var _ = new TimerTrap("ImageComponent.resizeDrawImg")) {
        if (widthRatio > heightRatio) {
          drawImg_ = img_.getScaledInstance(size.width, -1, 0);
          ratio = widthRatio;
        } else {
          drawImg_ = img_.getScaledInstance(-1, size.height, 0);
          ratio = heightRatio;
        }
      }
      drawImgSize_ = new Dimension(drawImg_.getWidth(null), drawImg_.getHeight(null));
      imgOrigin_ = new Point(
        (size.width  - drawImgSize_.width) / 2,
        (size.height - drawImgSize_.height) / 2
      );
      Optional<Integer> newPercentage = Optional.of(Math.round(100 / (float)ratio));
      lastCompSize_ = size;
    }
    g.drawImage(drawImg_, imgOrigin_.x + drawImgDrag_.width, imgOrigin_.y + drawImgDrag_.height, this);
  }


}
