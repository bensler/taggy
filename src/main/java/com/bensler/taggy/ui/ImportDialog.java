package com.bensler.taggy.ui;

import static com.bensler.decaf.util.cmp.CollatorComparator.COLLATOR_COMPARATOR;
import static com.jgoodies.forms.layout.CellConstraints.CENTER;
import static com.jgoodies.forms.layout.CellConstraints.RIGHT;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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
import com.bensler.taggy.persist.DbAccess;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ImportDialog extends JDialog {

  private final ImportController importController_;
  private final BlobController blobCtrl_;
  private final DbAccess db_;
  private final EntityTable<FileToImport> files_;
  private final List<FileToImport> filesToSha_;

  public ImportDialog(App app) {
    super(app.getMainFrame().getFrame(), "Import Files", true);
    importController_ = app.getImportCtrl();
    blobCtrl_ = app.getBlobCtrl();
    db_ = app.getDbAccess();
    filesToSha_ = new LinkedList<>();
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
      )),
      new TablePropertyView<>("shasum", "sha256-Hash", new PropertyViewImpl<>(
        new SimplePropertyGetter<>(FileToImport::getShaSum, COLLATOR_COMPARATOR)
      )),
      new TablePropertyView<>("importable", "Importable", new PropertyViewImpl<>(
        new IsNewIconRenderer(),
        SimplePropertyGetter.createComparablePropertyGetter(FileToImport::isImportable)
      ))
    ));
    files_.setSelectionMode(SelectionMode.MULTIPLE_INTERVAL);
    mainPanel.add(files_.getScrollPane(), new CellConstraints(2, 2));
    final JButton importButton = new JButton("Import");
    importButton.setEnabled(false);
    importButton.addActionListener(evt -> importSelection());
    files_.setSelectionListener((source, files) -> importButton.setEnabled(
      (!files.isEmpty())
      && files.stream().allMatch(FileToImport::isImportable)
    ));
    mainPanel.add(importButton, new CellConstraints(2, 4, RIGHT, CENTER));
    mainPanel.setPreferredSize(new Dimension(400, 400));
    setContentPane(mainPanel);
    List<FileToImport> filesToImport = importController_.getFilesToImport();
    files_.setData(filesToImport);
    filesToSha_.addAll(filesToImport);
    pack();
    final BulkPrefPersister prefs = new BulkPrefPersister(
      app.getPrefs(), new WindowPrefsPersister(new PrefKey(App.PREFS_APP_ROOT, getClass()), this)
    );
    new WindowClosingTrigger(this, evt -> {
      synchronized (filesToSha_) {
        filesToSha_.clear();
      }
      prefs.store();
    });
    new Thread(new ShaSumDuplicateCheckThread(), "Taggy.Import.ShaSumDuplicateCheck").start();
  }

  private void importSelection() {
    files_.getSelection().stream().map(importController_::importFile).forEach(files_::updateData);
  }

  static final class TypeIconRenderer extends SimpleCellRenderer<FileToImport, String> {
    TypeIconRenderer() {
      super(MainFrame.ICON_IMAGE_13);
    }

    @Override
    public Icon getIcon(FileToImport entity, String property) {
      return (property != null) ? icon_ : null;
    }
  }

  static final class IsNewIconRenderer extends SimpleCellRenderer<FileToImport, Boolean> {
    @Override
    public Icon getIcon(FileToImport entity, Boolean importable) {
      return Boolean.TRUE.equals(importable) ? MainFrame.ICON_PLUS_10 : MainFrame.ICON_X_10;
    }

    @Override
    protected String getText(FileToImport entity, Boolean property) {
      return entity.getImportObstacle();
    }
  }

  Optional<FileToImport> getNextToSha(Optional<FileToImport> lastProcessedItem) {
    synchronized (filesToSha_) {
      lastProcessedItem.ifPresent(file -> {
        filesToSha_.remove(file);
        SwingUtilities.invokeLater(() -> files_.updateData(file));
      });
      return (!filesToSha_.isEmpty()) ? Optional.of(filesToSha_.getFirst()) : Optional.empty();
    }
  }

  class ShaSumDuplicateCheckThread implements Runnable {
    @Override
    public void run() {
      Optional<FileToImport> fileInProgress = Optional.empty();

      while((fileInProgress = getNextToSha(fileInProgress)).isPresent()) {
        final FileToImport fileToImport = fileInProgress.get();
        final File file = fileToImport.getFile();

        try {
          final String shaSum = blobCtrl_.hashFile(file);

          fileToImport.setShaSum(shaSum);
          if (db_.doesBlobExist(shaSum)) {
            fileToImport.setImportObstacle("Duplicate");
          }
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

}
