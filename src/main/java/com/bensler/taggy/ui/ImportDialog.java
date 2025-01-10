package com.bensler.taggy.ui;

import static com.bensler.decaf.util.cmp.CollatorComparator.COLLATOR_COMPARATOR;

import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.bensler.decaf.swing.dialog.WindowClosingTrigger;
import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.swing.selection.SelectionMode;
import com.bensler.decaf.swing.table.EntityTable;
import com.bensler.decaf.swing.table.TablePropertyView;
import com.bensler.decaf.swing.table.TableView;
import com.bensler.decaf.swing.view.PropertyViewImpl;
import com.bensler.decaf.swing.view.SimpleCellRenderer;
import com.bensler.decaf.swing.view.SimplePropertyGetter;
import com.bensler.decaf.util.prefs.BulkPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.taggy.App;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ImportDialog extends JDialog {

  private final ImportController importController_;
  private final EntityTable<FileToImport> files_;

  public ImportDialog(App app) {
    super(app.getMainFrame().getFrame(), "Import Files", true);
    importController_ = app.getImportCtrl();
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu, p, 3dlu"
    ));
    files_ = new EntityTable<>(new TableView<>(
      new TablePropertyView<>("filename", "Filename", new PropertyViewImpl<>(
        new SimplePropertyGetter<>(FileToImport::getName, COLLATOR_COMPARATOR)
      )),
      new TablePropertyView<>("type", "Type", new PropertyViewImpl<>(
        new TypeIconRenderer(),
        new SimplePropertyGetter<>(FileToImport::getType, COLLATOR_COMPARATOR)
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
    final BulkPrefPersister prefs = new BulkPrefPersister(
      app.getPrefs(), new WindowPrefsPersister(new PrefKey(App.PREFS_APP_ROOT, getClass()), this)
    );

    new WindowClosingTrigger(this, evt -> prefs.store());
  }

  private void importSelection() {
    files_.getSelection().forEach(importController_::importFile);
  }

  static final class TypeIconRenderer extends SimpleCellRenderer {
    TypeIconRenderer() {
      super(MainFrame.ICON_IMAGE_13);
    }

    @Override
    public Icon getIcon(Object viewable, Object cellValue) {
      return (cellValue != null) ? super.getIcon(viewable, cellValue) : null;
    }
  }

}
