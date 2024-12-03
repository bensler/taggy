package com.bensler.taggy.ui;

import static com.bensler.decaf.util.cmp.CollatorComparator.COLLATOR_COMPARATOR;

import java.awt.Dimension;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JPanel;

import com.bensler.decaf.swing.table.EntityTable;
import com.bensler.decaf.swing.table.TablePropertyView;
import com.bensler.decaf.swing.table.TableView;
import com.bensler.decaf.swing.view.PropertyViewImpl;
import com.bensler.decaf.swing.view.SimplePropertyGetter;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ImportDialog extends JDialog {

  private final BlobController blobController_;
  private final ImportController importController_;
  private final EntityTable<File> files_;

  public ImportDialog(JDialog parent, BlobController blobController, ImportController importController) {
    super(parent, "Import Files", true);
    blobController_ = blobController;
    importController_ = importController;
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu"
    ));
    files_ = new EntityTable<>(new TableView<>(
      new TablePropertyView<>("filename", "Filename", new PropertyViewImpl<>(
        new SimplePropertyGetter<>(File::getName, COLLATOR_COMPARATOR)
      ))
    ));
    mainPanel.add(files_.getScrollPane(), new CellConstraints(2, 2));
    mainPanel.setPreferredSize(new Dimension(400, 400));
    setContentPane(mainPanel);
    files_.setData(importController_.getFilesToImport());
    pack();
  }

}
