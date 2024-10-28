package tmp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/**
 *
 */
public class RendererComponent extends JComponent {

  protected final static  int GAP  = 2;

  private final Color selectionBorderColor;
  private final Color selectionForeground;
  private final Color selectionBackground;
  private final Color textForeground;
  private final Color textBackground;

  private final JCheckBox checkbox_;
  private final JLabel contentLabel_;

  public RendererComponent() {
    setOpaque(true);
    final Font fontValue = UIManager.getFont("Tree.font");
    selectionBorderColor = UIManager.getColor("Tree.selectionBorderColor");
    selectionForeground = UIManager.getColor("Tree.selectionForeground");
    selectionBackground = UIManager.getColor("Tree.selectionBackground");
    textForeground = UIManager.getColor("Tree.textForeground");
    textBackground = UIManager.getColor("Tree.textBackground");

    checkbox_ = new JCheckBox();
    if (fontValue != null) {
      checkbox_.setFont(fontValue);
    }
    Boolean drawFocusBorder = (Boolean) UIManager.get("Tree.drawsFocusBorderAroundIcon");
    checkbox_.setFocusPainted((drawFocusBorder != null) && (drawFocusBorder.booleanValue()));
    checkbox_.setBorder(new EmptyBorder(GAP, GAP, GAP, GAP));
    checkbox_.setOpaque(false);
    add(checkbox_);
    contentLabel_ = new JLabel();
    contentLabel_.setIcon(new ImageIcon(getClass().getResource("user.png")));
    contentLabel_.setFont(checkbox_.getFont());
    contentLabel_.setIconTextGap(GAP);
    contentLabel_.setOpaque(false);
    add(contentLabel_);
  }

  public void setContent(boolean enabled, boolean selected, boolean checked, String content) {
    checkbox_.setEnabled(enabled);
    setForeground(selected ? selectionForeground : textForeground);
    setBackground(selected ? selectionBackground : textBackground);

    checkbox_.setSelected(checked);
    contentLabel_.setText(content);
  }

  public JCheckBox getCheckbox() {
    return checkbox_;
  }

  public boolean isCheckboxSelected() {
    return checkbox_.isSelected();
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getPreferredSize() {
    final Dimension cbPrefSize = checkbox_.getPreferredSize();
    final Dimension labelPrefSize = contentLabel_.getPreferredSize();

    return new Dimension(
      cbPrefSize.width + labelPrefSize.width + (6 * GAP),
      Math.max(cbPrefSize.height, labelPrefSize.height) + (2 * GAP)
    );
  }

  @Override
  public void setBounds(int x, int y, int width, int height) {
    super.setBounds(x, y, width, height);

    final Dimension cbPrefSize = checkbox_.getPreferredSize();
    final Dimension labelPrefSize = contentLabel_.getPreferredSize();
    final int vCenter = Math.max(cbPrefSize.height, labelPrefSize.height) / 2;

    checkbox_.setBounds(
      GAP,
      GAP + (vCenter - (cbPrefSize.height / 2)),
      cbPrefSize.width, cbPrefSize.height
    );
    contentLabel_.setBounds(
      GAP + cbPrefSize.width + (2 * GAP),
      GAP + (vCenter - (labelPrefSize.height / 2)),
      labelPrefSize.width, labelPrefSize.height
    );
  }

  public boolean checkboxHit(int x, int y) {
    final Rectangle rendererBounds = getBounds();
    final Rectangle checkboxBounds = checkbox_.getBounds();

    checkboxBounds.translate(rendererBounds.x, rendererBounds.y);
    return checkboxBounds.contains(x, y);
  }

  @Override
  protected void paintComponent(Graphics g) {
    final Dimension size = getSize();
    g.setColor(getBackground());
    g.fillRect(0, 0, size.width, size.height);
  }

  // Override a bunch of (unneeded) methods to improve performance.
  @Override
  protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
  @Override
  public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}
  @Override
  public void firePropertyChange(String propertyName, char oldValue, char newValue) {}
  @Override
  public void firePropertyChange(String propertyName, short oldValue, short newValue) {}
  @Override
  public void firePropertyChange(String propertyName, int oldValue, int newValue) {}
  @Override
  public void firePropertyChange(String propertyName, long oldValue, long newValue) {}
  @Override
  public void firePropertyChange(String propertyName, float oldValue, float newValue) {}
  @Override
  public void firePropertyChange(String propertyName, double oldValue, double newValue) {}
  @Override
  public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
  @Override
  public void invalidate() {}
  @Override
  public void repaint() {}
  @Override
  public void repaint(Rectangle r) {}
  @Override
  public void repaint(long tm, int x, int y, int width, int height) {}
  @Override
  public void revalidate() {}
  @Override
  public void validate() {}

  /** @return if the background is opaque and differs from
   *    the rendering component (JList/JTable/JTree).
   */
  @Override
  public boolean isOpaque() {
    return false;
  }

}
