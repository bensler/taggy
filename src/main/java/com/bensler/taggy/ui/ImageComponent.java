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
import com.bensler.taggy.App;

public class ImageComponent extends JComponent {

  private ResizeThread resizeThread_;
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
    resizeThread_ = App.getApp().getResizeThread();
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
      final Dimension size = getSize();
      final int leftEdge = (imgOrigin_.x + (position.x - origin.x));
      final int rightEdge = leftEdge + drawImgSize_.width;
      int xDiff = 0;

      if (drawImgSize_.width > size.width) {
        if (leftEdge > 0) {
          position.x += -leftEdge;
        }
        if (rightEdge < size.width) {
          position.x += (size.width - rightEdge);
        }
        xDiff = position.x - origin.x;
      }
      final int topEdge = (imgOrigin_.y + (position.y - origin.y));
      final int bottomEdge = topEdge + drawImgSize_.height;
      int yDiff = 0;

      if (drawImgSize_.height > size.height) {
        if (topEdge > 0) {
          position.y += -topEdge;
        }
        if (bottomEdge < size.height) {
          position.y += (size.height - bottomEdge);
        }
        yDiff = position.y - origin.y;
      }
      drawImgDrag_ = new Dimension(xDiff, yDiff);
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
      final Dimension drawImgSizeOld = new Dimension(drawImgSize_);
      // -1 or just negative: zoom in / +1 or just positive: zoom out
      final int wheelRotation = evt.getWheelRotation();
      // limit zoomFactor_ to [3.0, 0.2] with steps of 0.2
      final double wantedZoom = Math.min(3.0, zoomFactor_ + ((wheelRotation < 0) ? 0.2 : -0.2));
      final Dimension size = getSize();
      final double minHorizZoom = (size.getWidth() / img_.getWidth());
      final double minVertZoom = (size.getHeight() / img_.getHeight());
      final double minZoom = Math.min(minHorizZoom, minVertZoom);
      final boolean horizLimits = minHorizZoom < minVertZoom;
      final boolean zoomLimited = wantedZoom < minZoom;

      performZoom(Math.max(minZoom, wantedZoom));
      imgOrigin_ = new Point(
        (zoomLimited && horizLimits)    ? 0 : scaleMove(drawImgSizeOld.width , drawImgSize_.width , imgOrigin_.x, point.x),
        (zoomLimited && (!horizLimits)) ? 0 : scaleMove(drawImgSizeOld.height, drawImgSize_.height, imgOrigin_.y, point.y)
      );
      repaint();
    }
  }

  private int scaleMove(int sizeOld, int sizeNew, int originOld, int centerPoint) {
    return Math.round(((sizeOld * centerPoint) + (sizeNew * originOld) - (sizeNew * centerPoint)) / sizeOld);
  }

  private void performZoom(double zoomFactor) {
    final int newWidth = (int)Math.round(img_.getWidth()  * zoomFactor);
    final int newHeight = (int)Math.round(img_.getHeight() * zoomFactor);

    drawImg_= Scalr.resize(img_, Method.SPEED, Mode.FIT_EXACT, newWidth, newHeight, new BufferedImageOp[0]);
    resizeThread_.enqueue(this, img_, drawImgSize_ = new Dimension(newWidth, newHeight));
    zoomFactor_ = zoomFactor;
  }

  public void setScaledImg(BufferedImage srcImg, BufferedImage scaledImg, Dimension scaledImgSize) {
    if ((img_ == srcImg) && drawImgSize_.equals(scaledImgSize)) {
      drawImg_ = scaledImg;
      repaint();
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
      performZoom(Math.min(
        (size.getWidth() / img_.getWidth()),
        (size.getHeight() / img_.getHeight())
      ));
      imgOrigin_ = new Point(
        (size.width  - drawImgSize_.width) / 2,
        (size.height - drawImgSize_.height) / 2
      );
      lastCompSize_ = size;
    }
    g.drawImage(drawImg_, imgOrigin_.x + drawImgDrag_.width, imgOrigin_.y + drawImgDrag_.height, this);
  }

}
