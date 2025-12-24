package com.bensler.taggy.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

import javax.swing.JComponent;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

import com.bensler.decaf.swing.awt.MouseDragCtrl;
import com.bensler.decaf.util.TimerTrap;

public class ImageComponent extends JComponent {

  private BufferedImage img_;
  private Image drawImg_;
  private Dimension lastCompSize_;
  private Dimension drawImgSize_;
  private Point imgOrigin_;
  private Dimension drawImgDrag_;
  private double zoomFactor_;

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

  boolean mouseDragging(Point origin, Point position) {
    final boolean imgHit = (
         (origin.x >= imgOrigin_.x) && (origin.x <= (imgOrigin_.x + drawImgSize_.width))
      && (origin.y >= imgOrigin_.y) && (origin.y <= (imgOrigin_.y + drawImgSize_.height))
    );

    if (imgHit) {
      drawImgDrag_ = new Dimension(position.x - origin.x, position.y - origin.y);
      repaint();
    }
    return imgHit;
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
      final Dimension drawImgSizeOld = new Dimension(drawImg_.getWidth(null), drawImg_.getHeight(null));
      // -1 or just negative: zoom in / +1 or just positive: zoom out
      final int wheelRotation = evt.getWheelRotation();
      // limit zoomFactor_ to [3.0, 0.2] with steps of 0.2
      zoomFactor_ = Math.min(3.0, Math.max(0.1, zoomFactor_ + ((wheelRotation < 0) ? 0.2 : -0.2)));
      final int newWidth = (int)Math.round(img_.getWidth()  * zoomFactor_);
      final int newHeight = (int)Math.round(img_.getHeight() * zoomFactor_);

      try (var _ = new TimerTrap(durationMillies -> System.out.println("### scaled fast: " + durationMillies))) {
        drawImg_= Scalr.resize(img_, Method.SPEED, Mode.FIT_EXACT, newWidth, newHeight, new BufferedImageOp[0]);
      }

      drawImgSize_ = new Dimension(drawImg_.getWidth(null), drawImg_.getHeight(null));
      imgOrigin_ = new Point(
        scaleMove(drawImgSizeOld.width , drawImgSize_.width , imgOrigin_.x, point.x),
        scaleMove(drawImgSizeOld.height, drawImgSize_.height, imgOrigin_.y, point.y)
      );
      repaint();
    }
  }

  private int scaleMove(int sizeOld, int sizeNew, int originOld, int centerPoint) {
    return Math.round(((sizeOld * centerPoint) + (sizeNew * originOld) - (sizeNew * centerPoint)) / sizeOld);
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
      zoomFactor_ = Math.min(
        (size.getWidth() / img_.getWidth()),
        (size.getHeight() / img_.getHeight())
      );
      try (var _ = new TimerTrap("ImageComponent.resizeDrawImg")) {
        drawImg_ = img_.getScaledInstance((int)Math.round(img_.getWidth() * zoomFactor_), -1, 0);
      }
      drawImgSize_ = new Dimension(drawImg_.getWidth(null), drawImg_.getHeight(null));
      imgOrigin_ = new Point(
        (size.width  - drawImgSize_.width) / 2,
        (size.height - drawImgSize_.height) / 2
      );
      lastCompSize_ = size;
    }
    g.drawImage(drawImg_, imgOrigin_.x + drawImgDrag_.width, imgOrigin_.y + drawImgDrag_.height, this);
  }

}
