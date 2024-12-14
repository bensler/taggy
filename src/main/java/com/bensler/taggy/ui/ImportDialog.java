package com.bensler.taggy.ui;

import static com.bensler.decaf.util.cmp.CollatorComparator.COLLATOR_COMPARATOR;

import java.awt.Dimension;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.bensler.decaf.swing.selection.SelectionMode;
import com.bensler.decaf.swing.table.EntityTable;
import com.bensler.decaf.swing.table.TablePropertyView;
import com.bensler.decaf.swing.table.TableView;
import com.bensler.decaf.swing.view.PropertyViewImpl;
import com.bensler.decaf.swing.view.SimplePropertyGetter;
import com.bensler.taggy.App;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ImportDialog extends JDialog {

  private final ImportController importController_;
  private final EntityTable<File> files_;

  public ImportDialog(App app) {
    super(app.getMainFrame().getFrame(), "Import Files", true);
    importController_ = app.getImportCtrl();
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu, p, 3dlu"
    ));
    files_ = new EntityTable<>(new TableView<>(
      new TablePropertyView<>("filename", "Filename", new PropertyViewImpl<>(
        new SimplePropertyGetter<>(File::getName, COLLATOR_COMPARATOR)
      ))
    ));
    files_.setSelectionMode(SelectionMode.MULTIPLE_INTERVAL);
    mainPanel.add(files_.getScrollPane(), new CellConstraints(2, 2));
    final JButton importButton = new JButton("Import");
    importButton.setEnabled(false);
    importButton.addActionListener(evt -> importSelection());
    files_.setSelectionListener((source, files) -> importButton.setEnabled(!files.isEmpty()));
    mainPanel.add(importButton, new CellConstraints(2, 4, CellConstraints.RIGHT, CellConstraints.CENTER));
    mainPanel.setPreferredSize(new Dimension(400, 400));
    setContentPane(mainPanel);
    files_.setData(importController_.getFilesToImport());
    pack();
    app.getWindowSizePersister().listenTo(this);
  }

  private void importSelection() {
    files_.getSelection().forEach(importController_::importFile);
  }

}
