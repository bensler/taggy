package com.bensler.taggy.ui;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.SwingUtilities;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

public class ResizeThread {

  private final LinkedBlockingQueue<Task> queue_;
  private final Thread workerThread_;

  private boolean terminate_;

  public ResizeThread() {
    terminate_ = false;
    queue_ = new LinkedBlockingQueue<>();
    (workerThread_ = new Thread(() -> workerThread())).start();
  }

  private void workerThread() {
    while (true) {
      try {
        queue_.take().perform();
      } catch (InterruptedException ie) { /* just got woken up*/ }
      final boolean terminate;

      synchronized (this) {
        terminate = terminate_;
      }
      if (terminate) {
        break;
      }
    }
  }

  public synchronized void enqueue(ImageComponent customer, BufferedImage img, Dimension targetSize) {
    queue_.removeIf(task -> task.customer_ == customer);
    try {
      queue_.put(new Task(customer, img, targetSize));
    } catch (InterruptedException ie) { /* no capacity limit -> no real wait */ }
  }

  public void terminate() {
    synchronized (this) {
      terminate_ = true;
    }
    workerThread_.interrupt();
  }

  static class Task {

    private final ImageComponent customer_;
    private final BufferedImage srcImg_;
    private final Dimension targetSize_;
    private BufferedImage scaledImg_;

    Task(ImageComponent customer, BufferedImage img, Dimension targetSize) {
      customer_ = customer;
      srcImg_ = img;
      targetSize_ = new Dimension(targetSize);
    }

    void perform() {
      scaledImg_ = Scalr.resize(srcImg_, Method.ULTRA_QUALITY, Mode.FIT_EXACT, targetSize_.width, targetSize_.height, new BufferedImageOp[0]);
      SwingUtilities.invokeLater(() -> customer_.setScaledImg(srcImg_, scaledImg_, targetSize_));
    }

  }

}
