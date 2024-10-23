package com.bensler;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.jgoodies.looks.plastic.theme.DesertYellow;

public class CheckBoxNodeTreeSample {

  public static void main(String args[]) throws UnsupportedLookAndFeelException {
    Plastic3DLookAndFeel.setCurrentTheme(new DesertYellow());
    UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
    new CheckBoxNodeTreeSample();
  }

  final Set<NamedVector> selectedNodes;

  CheckBoxNodeTreeSample() {
    selectedNodes = new HashSet<>();
    JFrame frame = new JFrame("CheckBox Tree");
    JTree tree = new JTree(new NamedVector("Root", false, new NamedVector[] {
      new NamedVector("Accessibility", true, new NamedVector[] {
        new NamedVector("Move system caret with focus/selection changes", false),
        new NamedVector("Always expand alt text for images", true)
      }),
      new NamedVector("Browsing", false, new NamedVector[] {
        new NamedVector("Notify when downloads complete", true),
        new NamedVector("Disable script debugging", true),
        new NamedVector("Use AutoComplete", true),
        new NamedVector("Browse in a new process", false)
      })
    })) {
      @Override
      public String convertValueToText(Object value, boolean selected,boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (
          (value instanceof DefaultMutableTreeNode mutableTreeNode)
          && (mutableTreeNode.getUserObject() instanceof NamedVector namedVector)
        ) {
          return namedVector.name;
        } else {
          return super.convertValueToText(value, selected, expanded, leaf, row, hasFocus);
        }
      }
    };
    tree.setRowHeight(0);
    tree.setCellRenderer(new CheckBoxNodeRenderer());
    tree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent e) {
        final TreePath path = tree.getSelectionPath();

        if ((path != null) && (e.getKeyChar() == ' ')) {
          final NamedVector userObject = (NamedVector)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();

          if (selectedNodes.contains(userObject)) {
            selectedNodes.remove(userObject);
          } else {
            selectedNodes.add(userObject);
          }
          tree.getModel().valueForPathChanged(path, userObject);
        }
      }
    });
    tree.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        final TreePath path = tree.getPathForLocation(e.getX(), e.getY());

        if (path != null) {
          final DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
          final NamedVector userObject = (NamedVector)node.getUserObject();
          final Rectangle bounds = tree.getPathBounds(path);
          final RendererComponent rendererComp = (RendererComponent)tree.getCellRenderer().getTreeCellRendererComponent(
            tree, node, true,
            tree.isExpanded(path),
            tree.getModel().isLeaf(node),
            tree.getRowForPath(path),
            true
          );

          rendererComp.setBounds(bounds);
          if (rendererComp.checkboxHit(e.getX(), e.getY())) {
            if (selectedNodes.contains(userObject)) {
              selectedNodes.remove(userObject);
            } else {
              selectedNodes.add(userObject);
            }
            tree.getModel().valueForPathChanged(path, userObject);
          };
        }
      }
    });

    JScrollPane scrollPane = new JScrollPane(tree);
    frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
    frame.setSize(300, 150);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.setVisible(true);
  }

  class CheckBoxNodeRenderer implements TreeCellRenderer {

    private final JLabel emptyLabel;
    private final RendererComponent rendererComponent;

    public CheckBoxNodeRenderer() {
      emptyLabel = new JLabel();
      rendererComponent = new RendererComponent();
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
        boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (
          (value instanceof DefaultMutableTreeNode mutableTreeNode)
          && (mutableTreeNode.getUserObject() instanceof NamedVector namedVector)
        ) {
          rendererComponent.setContent(tree.isEnabled(), selected, selectedNodes.contains(namedVector), namedVector.getName());
          return rendererComponent;
        }
        return emptyLabel;
    }

  }

  class NamedVector extends Vector<NamedVector> {

    private final String name;

    public NamedVector(String pName, boolean selected, NamedVector... elements) {
      name = pName;
      if (selected) {
        selectedNodes.add(this);
      }
      addAll(List.of(elements));
    }

    public String getName() {
      return name;
    }

    @Override
    public synchronized boolean equals(Object o) {
      return this == o;
    }

    @Override
    public synchronized int hashCode() {
      return 7;
    }

  }

}