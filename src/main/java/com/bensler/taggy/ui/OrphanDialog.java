package com.bensler.taggy.ui;

import static com.jgoodies.forms.layout.CellConstraints.FILL;
import static com.jgoodies.forms.layout.CellConstraints.RIGHT;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.hibernate.Session;

import com.bensler.taggy.persist.Blob;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class OrphanDialog extends JDialog {

  private final BlobController blobController_;
  private final ThumbnailOverviewPanel thumbViewer_;

  public OrphanDialog(JDialog parent, BlobController blobController) {
    super(parent, "Uncategorized Files", true);
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu, f:p, 3dlu"
    ));

    blobController_ = blobController;

    thumbViewer_ = new ThumbnailOverviewPanel(blobController_);
    final JScrollPane scrollPane = new JScrollPane(thumbViewer_, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.getViewport().setBackground(thumbViewer_.getBackground());
    mainPanel.add(scrollPane, new CellConstraints(2, 2));

    final JButton closeButton = new JButton("Close");
    closeButton.addActionListener(evt -> dispose());
    mainPanel.add(closeButton, new CellConstraints(2, 4, RIGHT, FILL));

    mainPanel.setPreferredSize(new Dimension(400, 400));
    setContentPane(mainPanel);
    pack();
  }

  public void show(Session session) {
    SwingUtilities.invokeLater(() -> thumbViewer_.setData(session.createQuery(
      "from Blob as blob " +
      "left join fetch blob.tags as tags " +
      "group by blob " +
      "having count(tags) < 1", Blob.class
    ).getResultList()));
    setVisible(true);
  }

}
