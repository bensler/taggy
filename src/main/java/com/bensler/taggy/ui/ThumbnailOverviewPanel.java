package com.bensler.taggy.ui;

import static com.bensler.taggy.App.getApp;
import static com.bensler.taggy.imprt.Thumbnailer.THUMBNAIL_SIZE;
import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_BEVEL;
import static java.util.Objects.requireNonNull;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.UIManager;

import com.bensler.decaf.swing.awt.ColorHelper;
import com.bensler.decaf.swing.selection.EntitySelectionListener;
import com.bensler.decaf.swing.view.SimplePropertyGetter;
import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.persist.Blob;

public class ThumbnailOverviewPanel extends JComponent implements Scrollable {

  public enum ScrollingPolicy {
    SCROLL_HORIZONTALLY(VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_AS_NEEDED, false) {
      @Override
      Dimension getGridSize(int tilesCount, Dimension compSize) {
        final int actualHeight = compSize.height;

        if (actualHeight <= 0) {
          return new Dimension(1, tilesCount);
        } else {
          final int rowCount = Math.max(1, (actualHeight - GAP) / (TILE_SIZE + GAP));
          final int colCount = (tilesCount / rowCount) + (((tilesCount % rowCount) > 0) ? 1 : 0);

          return new Dimension(colCount, ((colCount < 2) ? tilesCount : rowCount));
        }
      }
      @Override
      Rectangle getScrollToEndTarget(ThumbnailOverviewPanel view) {
        return new Rectangle(view.getPreferredSize().width - 10, 0, 10, 10);
      }
    },
    SCROLL_VERTICALLY(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER, true) {
      @Override
      Dimension getGridSize(int tilesCount, Dimension compSize) {
        final int actualWidth = compSize.width;

        if (actualWidth <= 0) {
          return new Dimension(tilesCount, 1);
        } else {
          final int colCount = Math.max(1, (actualWidth - GAP) / (TILE_SIZE + GAP));
          final int rowCount = (tilesCount / colCount) + (((tilesCount % colCount) > 0) ? 1 : 0);

          return new Dimension(((rowCount < 2) ? tilesCount : colCount), rowCount);
        }
      }
      @Override
      Rectangle getScrollToEndTarget(ThumbnailOverviewPanel view) {
        return new Rectangle(0, view.getPreferredSize().height - 10, 10, 10);
      }
    };

    final int verticalScrollbarPolicy_, horizontalScrollbarPolicy_;
    final boolean tracksViewportWidth_;

    ScrollingPolicy(
      int verticalScrollbarPolicy, int horizontalScrollbarPolicy, boolean tracksViewportWidth
    ) {
      verticalScrollbarPolicy_ = verticalScrollbarPolicy;
      horizontalScrollbarPolicy_ = horizontalScrollbarPolicy;
      tracksViewportWidth_ = tracksViewportWidth;
    }

    abstract Dimension getGridSize(int tilesCount, Dimension compSize);
    abstract Rectangle getScrollToEndTarget(ThumbnailOverviewPanel thumbnailOverviewPanel);

  }

  private final static int GAP = 4;
  private final static int INSET = 5;
  private final static int TILE_SIZE = THUMBNAIL_SIZE + (2 * INSET);
  private final static BasicStroke STROKE_SOLID = new BasicStroke(1.0f);
  private final static BasicStroke STROKE_DASH = new BasicStroke(
    1.0f, CAP_BUTT, JOIN_BEVEL, 0.0f, new float[] {4.0f, 4.0f}, 0
  );
  private static final Comparator<Blob> BLOB_COMPARATOR = SimplePropertyGetter.createComparableGetter(Blob::getCreationTime);

  private final Color backgroundSelectionColor_;
  private final Color backgroundSelectionColorUnfocused_;

  private final List<Blob> blobs_;
  private final Map<Blob, ImageIcon> images_;
  private Dimension gridOffsetPx;
  private final JScrollPane scrollPane_;
  private final ScrollingPolicy scrollingPolicy_;

  private final List<Blob> selection_;
  private final Set<EntitySelectionListener<Blob>> selectionListeners_;
  private Dimension prefViewPortSize_;

  public ThumbnailOverviewPanel(ScrollingPolicy scrollingPolicy) {
    blobs_ = new ArrayList<>();
    images_ = new HashMap<>();
    selection_ = new ArrayList<>();
    selectionListeners_ = new HashSet<>();

    backgroundSelectionColor_ = UIManager.getColor("Tree.selectionBackground");
    backgroundSelectionColorUnfocused_ = ColorHelper.mix(
      backgroundSelectionColor_, 2,
      UIManager.getColor("Tree.background"), 1
    );
    setBackground(UIManager.getColor("Tree.textBackground"));
    gridOffsetPx = new Dimension();
    scrollingPolicy_ = scrollingPolicy;
    scrollPane_ = new JScrollPane(
      this, scrollingPolicy_.verticalScrollbarPolicy_, scrollingPolicy_.horizontalScrollbarPolicy_
    );
    scrollPane_.getViewport().setBackground(getBackground());
    setPreferredScrollableViewportSize(1, 3);
  }

  public void scrollToEnd() {
    scrollRectToVisible(scrollingPolicy_.getScrollToEndTarget(this));
  }

  public JScrollPane getScrollpane() {
    return scrollPane_;
  }

  public void setFocusable() {
    setFocusable(true);
    addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        repaint();
      }

      @Override
      public void focusLost(FocusEvent e) {
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
        if ((evt.getKeyCode() == KeyEvent.VK_A) && (evt.getModifiersEx() == KeyEvent.CTRL_DOWN_MASK)) {
          try (var _ = new SelectionEvent()) {
            selection_.clear();
            selection_.addAll(blobs_);
          }
        }
      }
    });
  }

  void mouseClicked(MouseEvent evt) {
    requestFocus();
    if (evt.getButton() == MouseEvent.BUTTON1) {
      if ((evt.getClickCount() == 1)) {
        blobAt(evt.getPoint()).ifPresentOrElse(blob -> {
          try (var _ = new SelectionEvent()) {
            if (evt.isControlDown()) {
              if (selection_.contains(blob)) {
                selection_.remove(blob);
              } else {
                selection_.add(blob);
              }
            } else {
              selection_.clear();
              selection_.add(blob);
            }
          }
        }, () -> {
          if (evt.isControlDown()) {
            clearSelection();
          }
        });
      }
    }
  }

  Optional<Blob> blobAt(Point position) {
    final int gapPlusTileSize = GAP + TILE_SIZE;

    position.x -= gridOffsetPx.width;
    position.y -= gridOffsetPx.height;
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

  public void setData(Collection<Blob> data) {
    blobs_.clear();
    images_.clear();

    try (var _ = new SelectionEvent(true)) {
      data.forEach(blob -> {
        if (addImageInternally(blob) && selection_.contains(blob)) {
          selection_.set(selection_.indexOf(blob), blob);
        }
      });
      selection_.retainAll(blobs_);
    }
    Collections.sort(blobs_, BLOB_COMPARATOR);
    revalidate();
    repaint();
  }

  /** @return if it was already contained before */
  private boolean addImageInternally(Blob blob) {
    try {
      images_.put(blob, new ImageIcon(ImageIO.read(getApp().getBlobCtrl().getFile(blob.getThumbnailSha()))));
    } catch (IOException e) {
      // TODO display error thumb
      e.printStackTrace();
    }

    final int oldIndex = blobs_.indexOf(blob);
    final boolean containedBefore = (oldIndex >= 0);

    blobs_.remove(blob);
    blobs_.add((containedBefore ? oldIndex : blobs_.size()), blob);
    return containedBefore;
  }

  public void addImage(Blob blob) {
    final boolean containedBefore = addImageInternally(blob);

    Collections.sort(blobs_, BLOB_COMPARATOR);
    if (selection_.contains(blob)) {
      try (var _ = new SelectionEvent(containedBefore)) {
        selection_.set(selection_.indexOf(blob), blob);
      }
    }
    revalidate();
    repaint();
  }

  public void removeImage(Blob blob) {
    if (blobs_.remove(blob)) {
      images_.remove(blob);
      try (var _ = new SelectionEvent()) {
        selection_.remove(blob);
      }
      revalidate();
      repaint();
    }
  }

  public void clear() {
    setData(List.of());
  }

  @Override
  protected void paintComponent(Graphics g) {
    final Dimension size = getSize();
    final Graphics2D g2d = (Graphics2D)g;

    // TODO render tiles inside clip region only (g.getClipBounds()))
    g.setColor(getBackground());
    g.fillRect(0, 0, size.width, size.height);

    final Dimension gridSize = getGridSize(); // unit: tiles
    final Dimension gridDimension = new Dimension( // unit: px
      GAP + (gridSize.width * (TILE_SIZE + GAP)),
      GAP + (gridSize.height * (TILE_SIZE + GAP))
    );
    gridOffsetPx = new Dimension(
      (Math.max(0, (size.width  - gridDimension.width )) / 2),
      (Math.max(0, (size.height - gridDimension.height)) / 2)
    );
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
    final int tileOriginX = gridOffsetPx.width + GAP + (col * (TILE_SIZE + GAP));
    final int tileOriginY = gridOffsetPx.height + GAP + (row * (TILE_SIZE + GAP));
    final int paddingX = (THUMBNAIL_SIZE - Math.min(THUMBNAIL_SIZE, icon.getIconWidth())) / 2;
    final int paddingY = (THUMBNAIL_SIZE - Math.min(THUMBNAIL_SIZE, icon.getIconHeight())) / 2;
    final boolean selected = selection_.contains(blob);

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
    Dimension gridSize = getGridSize();

    if ((gridSize.width == 0) && (gridSize.height == 0)) {
      gridSize = new Dimension(1, 1);
    }
    return gridSizeToPixel(gridSize.height, gridSize.width);
  }

  private static Dimension gridSizeToPixel(int rows, int cols) {
    return new Dimension(
      (cols * TILE_SIZE) + ((cols + 1) * GAP),
      (rows * TILE_SIZE) + ((rows + 1) * GAP)
    );
  }
  public Dimension getGridSize() {
    return scrollingPolicy_.getGridSize(blobs_.size(), getSize());
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return prefViewPortSize_;
  }

  public Dimension setPreferredScrollableViewportSize(int rows, int cols) {
    return prefViewPortSize_ = gridSizeToPixel(rows, cols);
  }

  @Override
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return THUMBNAIL_SIZE;
  }

  @Override
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return THUMBNAIL_SIZE;
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return scrollingPolicy_.tracksViewportWidth_;
  }

  @Override
  public boolean getScrollableTracksViewportHeight() {
    return !scrollingPolicy_.tracksViewportWidth_;
  }

  private class SelectionEvent implements AutoCloseable {

    final boolean  fireSelectionEventUnconditionally_;
    final List<Blob> oldSelection_;

    SelectionEvent() {
      this(false);
    }

    SelectionEvent(boolean fireSelectionEventUnconditionally) {
      fireSelectionEventUnconditionally_ = fireSelectionEventUnconditionally;
      oldSelection_ = List.copyOf(selection_);
    }

    @Override
    public void close() {
      if (fireSelectionEventUnconditionally_ || (!oldSelection_.equals(selection_))) {
        // TODO ------------------------------------------- vvvv
        selectionListeners_.forEach(l -> l.selectionChanged(null, selection_));
        repaint();
      }
    }
  }

  public void addSelectionListener(EntitySelectionListener<Blob> listener) {
    selectionListeners_.add(requireNonNull(listener));
  }

  public List<Blob> getSelection() {
    return List.copyOf(selection_);
  }

  public Blob getSingleSelection() {
    return ((selection_.isEmpty()) ? null : selection_.get(0));
  }

  public void clearSelection() {
    if (!selection_.isEmpty()) {
      try (var _ = new SelectionEvent()) {
        selection_.clear();
      }
    }
  }

  public void select(Collection<?> blobs) {
    try (var _ = new SelectionEvent()) {
      selection_.clear();
      blobs.stream().flatMap(blob -> EntityReference.resolve(blob, images_.keySet()).stream()).forEach(selection_::add);
    }
  }

  public void select(Object blob) {
    select(List.of(blob));
  }

  public Optional<Blob> contains(Object blob) {
    return (images_.containsKey(blob) ? Optional.of((Blob)blob) : Optional.empty());
  }

}
