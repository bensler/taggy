package com.bensler.taggy.ui;

import static com.bensler.taggy.ui.BlobController.THUMBNAIL_SIZE;
import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_BEVEL;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.UIManager;

import org.apache.commons.imaging.ImageReadException;

import com.bensler.decaf.swing.awt.ColorHelper;
import com.bensler.taggy.persist.Blob;

public class ThumbnailOverviewPanel extends JComponent implements Scrollable {

  private final static int GAP = 4;
  private final static int INSET = 5;
  private final static int TILE_SIZE = THUMBNAIL_SIZE + (2 * INSET);
  private final static BasicStroke STROKE_SOLID = new BasicStroke(1.0f);
  private final static BasicStroke STROKE_DASH = new BasicStroke(
    1.0f, CAP_BUTT, JOIN_BEVEL, 0.0f, new float[] {4.0f, 4.0f}, 0
  );

  private final Color backgroundSelectionColor_;
  private final Color backgroundSelectionColorUnfocused_;

  private final BlobController blobController_;
  private final List<Blob> blobs_;
  private final Map<Blob, ImageIcon> images_;
  private Blob selectedBlob_;

  public ThumbnailOverviewPanel(BlobController blobController) {
    blobController_ = blobController;
    blobs_ = new ArrayList<>();
    images_ = new HashMap<>();

    backgroundSelectionColor_ = UIManager.getColor("Tree.selectionBackground");
    backgroundSelectionColorUnfocused_ = ColorHelper.mix(
      backgroundSelectionColor_, 2,
      UIManager.getColor("Tree.background"), 1
    );
    setBackground(UIManager.getColor("Tree.textBackground"));
    addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent e) {
        repaint();
      }

      @Override
      public void focusGained(FocusEvent e) {
        repaint();
      }
    });
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent evt) {
        ThumbnailOverviewPanel.this.mouseClicked(evt);
      }
    });
    addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent evt) {
        System.out.println(evt.getKeyCode() + " # " + evt.getModifiersEx());
      }
    });
  }

  void mouseClicked(MouseEvent evt) {
    final Blob oldSelection = selectedBlob_;
    final boolean doubleClick = (evt.getClickCount() == 2);

    requestFocus();
    if (doubleClick) {
      if (selectedBlob_ != null) {
        try {
          final BlobDialog blobDlg = MainFrame.getInstance().getBlobDlg();

          blobDlg.setVisible(true);
          blobDlg.setBlob(selectedBlob_);
        } catch (IOException | ImageReadException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    } else {
      selectedBlob_ = null;
      blobAt(evt.getPoint()).ifPresent(clickedBlob -> {
        selectedBlob_ = clickedBlob;
      });
      if (oldSelection != selectedBlob_) {
        // TODO fire event
        repaint(); // TODO repaint tile only
      }
    }
  }

  private Optional<Blob> blobAt(Point position) {
    final int gapPlusTileSize = GAP + TILE_SIZE;

    requestFocus();
    if ((position.x % gapPlusTileSize) > GAP) {
      int col = (position.x / gapPlusTileSize);
      int row = (position.y / gapPlusTileSize);
      int colCount = (getSize().width - GAP) / (TILE_SIZE + GAP);

      if (col < colCount) {
        final int blobIndex = (row * colCount) + col;

        if (blobIndex < blobs_.size()) {
          return Optional.of(blobs_.get(blobIndex));
        }
      }
    }
    return Optional.empty();
  }

  public void setData(List<Blob> data) {
    blobs_.clear();
    images_.clear();
    blobs_.addAll(data);
    selectedBlob_ = null;
    for (Blob blob : blobs_) {
      try {
        images_.put(blob, new ImageIcon(ImageIO.read(blobController_.getFile(blob.getThumbnailSha()))));
      } catch (IOException e) {
        // TODO display error thumb
        e.printStackTrace();
      }
    }
    revalidate();
    repaint();
  }

  public void clear() {
    setData(List.of());
  }

  @Override
  protected void paintComponent(Graphics g) {
    final Dimension size = getSize();
    final Graphics2D g2d = (Graphics2D)g;

    // TODO render tiles inside clip region only
    // g.getClipBounds());
    g.setColor(getBackground());
    g.fillRect(0, 0, size.width, size.height);

    final Dimension gridSize = getGridSize();
    int tileCounter = 0;

    for (Blob blob : blobs_) {
      drawTile(
        (tileCounter / gridSize.width),
        (tileCounter % gridSize.width),
        blob, g2d
      );
      tileCounter++;
    }
  }

  private void drawTile(int row, int col, Blob blob, Graphics2D g) {
    final ImageIcon icon = images_.get(blob);
    final int tileOriginX = GAP + (col * (TILE_SIZE + GAP));
    final int tileOriginY = GAP + (row * (TILE_SIZE + GAP));
    final int paddingX = (THUMBNAIL_SIZE - Math.min(THUMBNAIL_SIZE, icon.getIconWidth())) / 2;
    final int paddingY = (THUMBNAIL_SIZE - Math.min(THUMBNAIL_SIZE, icon.getIconHeight())) / 2;
    final boolean selected = blob == selectedBlob_;

    if (selected) {
      g.setColor(hasFocus() ? backgroundSelectionColor_ : backgroundSelectionColorUnfocused_);
      g.fillRect(tileOriginX, tileOriginY, TILE_SIZE, TILE_SIZE);
    }
    // dashed frame
    g.setStroke(STROKE_DASH);
    g.setColor(selected ? Color.BLACK : backgroundSelectionColorUnfocused_);
    if (selected) {
      g.drawRect(tileOriginX -1, tileOriginY - 1, TILE_SIZE + 1, TILE_SIZE + 1);
    } else {
      g.drawRect(tileOriginX, tileOriginY, TILE_SIZE - 1, TILE_SIZE - 1);
    }
    icon.paintIcon(
      this, g,
      INSET + tileOriginX + paddingX,
      INSET + tileOriginY + paddingY
    );
  }

  @Override
  public Dimension getPreferredSize() {
    final Dimension gridSize = getGridSize();

    return new Dimension(
      (gridSize.width  * TILE_SIZE) + ((gridSize.width  + 1) * GAP),
      (gridSize.height * TILE_SIZE) + ((gridSize.height + 1) * GAP)
    );
  }

  public Dimension getGridSize() {
    final int tilesCount = blobs_.size();
    final int actualWidth = getSize().width;

    if (actualWidth <= 0) {
      return new Dimension(tilesCount, 1);
    } else {
      final int colCount = (actualWidth - GAP) / (TILE_SIZE + GAP);
      final int rowCount = (tilesCount / colCount) + (((tilesCount % colCount) > 0) ? 1 : 0);

      return new Dimension(colCount, rowCount);
    }
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return new Dimension(100, 100);
  }

  @Override
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return BlobController.THUMBNAIL_SIZE;
  }

  @Override
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return BlobController.THUMBNAIL_SIZE;
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return true;
  }

  @Override
  public boolean getScrollableTracksViewportHeight() {
    return false;
  }

}
