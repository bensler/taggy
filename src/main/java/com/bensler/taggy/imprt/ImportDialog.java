package com.bensler.taggy.imprt;

import static com.bensler.decaf.swing.view.SimplePropertyGetter.createComparableGetter;
import static com.bensler.decaf.swing.view.SimplePropertyGetter.createGetterComparator;
import static com.bensler.decaf.util.cmp.CollatorComparator.COLLATOR_COMPARATOR;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.bensler.decaf.swing.dialog.WindowClosingTrigger;
import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.swing.selection.SelectionMode;
import com.bensler.decaf.swing.table.EntityTable;
import com.bensler.decaf.swing.table.TablePrefPersister;
import com.bensler.decaf.swing.table.TablePropertyView;
import com.bensler.decaf.swing.table.TableView;
import com.bensler.decaf.swing.view.PropertyViewImpl;
import com.bensler.decaf.swing.view.SimpleCellRenderer;
import com.bensler.decaf.util.prefs.BulkPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.taggy.App;
import com.bensler.taggy.imprt.FileToImport.ImportObstacle;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.ui.BlobController;
import com.bensler.taggy.ui.MainFrame;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

class ImportDialog extends JDialog {

  private final ImportController importController_;
  private final BlobController blobCtrl_;
  private final DbAccess db_;
  private final EntityTable<FileToImport> files_;
  private final JLabel fileSizeLabel_;
  private final JButton importButton_;
  private final List<FileToImport> filesToSha_;
  private final FileSizeRenderer fileSizeRenderer_;

  ImportDialog(App app) {
    super(app.getMainFrame().getFrame(), "Import Files", true);
    importController_ = app.getImportCtrl();
    blobCtrl_ = app.getBlobCtrl();
    db_ = app.getDbAccess();
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "3dlu, f:p:g, 3dlu, f:p, 3dlu"
    ));
    fileSizeRenderer_ = new FileSizeRenderer();
    files_ = new EntityTable<>(new TableView<>(
      new TablePropertyView<>("filename", "Filename", new PropertyViewImpl<>(
        createGetterComparator(FileToImport::getName, COLLATOR_COMPARATOR)
      )),
      new TablePropertyView<>("relativePath", "Path", new PropertyViewImpl<>(
        createGetterComparator(FileToImport::getRelativePath, COLLATOR_COMPARATOR)
      )),
      new TablePropertyView<>("type", "Type", new PropertyViewImpl<>(
        new TypeIconRenderer(), createGetterComparator(FileToImport::getType, COLLATOR_COMPARATOR)
      )),
      new TablePropertyView<>("fileSize", "Size", new PropertyViewImpl<>(
        fileSizeRenderer_, createComparableGetter(FileToImport::getFileSize)
      )),
      new TablePropertyView<>("shasum", "sha256-Hash", new PropertyViewImpl<>(
        createGetterComparator(FileToImport::getShaSum, COLLATOR_COMPARATOR)
      )),
      new TablePropertyView<>("importable", "Importable", new PropertyViewImpl<>(
        new IsNewIconRenderer(), createComparableGetter(FileToImport::isImportable)
      ))
    ));
    files_.setSelectionMode(SelectionMode.MULTIPLE_INTERVAL);
    mainPanel.add(files_.getScrollPane(), new CellConstraints(2, 2));
    final JPanel buttonPanel = new JPanel(new FormLayout(
      "r:p:g, 3dlu, p ", "c:p"
    ));
    fileSizeLabel_ = new JLabel();
    importButton_ = new JButton("Import");
    importButton_.setEnabled(false);
    importButton_.addActionListener(evt -> importSelection());
    files_.setSelectionListener((source, files) -> filesSelectionChanged(files));
    buttonPanel.add(fileSizeLabel_, new CellConstraints(1, 1));
    buttonPanel.add(importButton_, new CellConstraints(3, 1));
    mainPanel.add(buttonPanel, new CellConstraints(2, 4));
    mainPanel.setPreferredSize(new Dimension(400, 400));
    setContentPane(mainPanel);
    final List<FileToImport> filesToImport = importController_.getFilesToImport();
    files_.addOrUpdateData(filesToImport);
    filesToSha_ = new LinkedList<>(filesToImport.stream()
      .filter(file -> file.hasObstacle(ImportObstacle.SHA_MISSING) || file.hasObstacle(ImportObstacle.DUPLICATE_CHECK_MISSING)).toList());
    pack();
    final PrefKey baseKey = new PrefKey(App.PREFS_APP_ROOT, getClass());
    final BulkPrefPersister prefs = new BulkPrefPersister(
      app.getPrefs(), new WindowPrefsPersister(baseKey, this),
      new TablePrefPersister(new PrefKey(baseKey, "files"), files_.getComponent())
    );
    new WindowClosingTrigger(this, evt -> {
      synchronized (filesToSha_) {
        filesToSha_.clear();
      }
      prefs.store();
    });
    new Thread(new ShaSumDuplicateCheckThread(), "Taggy.Import.ShaSumDuplicateCheck").start();
  }

  private void filesSelectionChanged(List<FileToImport> files) {
    final long fileSizesSum = files.stream().mapToLong(FileToImport::getFileSize).sum();
    final int filesCount = files.size();
    final boolean selectionNotEmpty = filesCount > 0;

    importButton_.setEnabled(selectionNotEmpty && files.stream().allMatch(FileToImport::isImportable));
    fileSizeLabel_.setText(selectionNotEmpty ? "%s file%s (%s)".formatted(
      filesCount, ((filesCount < 2) ? "" : "s"), fileSizeRenderer_.formatFileSize(fileSizesSum)
    ) : "");
  }

  private void importSelection() {
    new ImportProgressDialog(this, files_.getSelection()).setVisible(true);
  }

  static final class FileSizeRenderer extends SimpleCellRenderer<FileToImport, Long> {

    private final static String[] UNITS = new String[] {"B", "kB", "MB", "GB", "TB"};

    FileSizeRenderer() {
      super(null, SwingConstants.RIGHT);
    }

    String formatFileSize(Long fileSize) {
      int unitIndex = 0;

      while ((fileSize > 2048) && ((unitIndex + 1) < UNITS.length )) {
        fileSize = fileSize >> 10;
        unitIndex++;
      }
      return String.valueOf(fileSize) + " " + UNITS[unitIndex];
    }

    @Override
    public String getText(FileToImport entity, Long fileSize) {
      return formatFileSize(fileSize);
    }
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
      return entity.getImportObstacleAsString();
    }
  }

  void fileToUpdateChanged(FileToImport file) {
    SwingUtilities.invokeLater(() -> {
      files_.addOrUpdateData(List.of(file));
      files_.fireSelectionChanged();
    });
  }

  Optional<FileToImport> getNextToSha(Optional<FileToImport> lastProcessedItem) {
    synchronized (filesToSha_) {
      lastProcessedItem.ifPresent(file -> {
        filesToSha_.remove(file);
        fileToUpdateChanged(file);
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
          final String shaSum;

          if (fileToImport.hasObstacle(ImportObstacle.DUPLICATE_CHECK_MISSING)) {
            shaSum = fileToImport.getShaSum();
          } else {
            shaSum = blobCtrl_.hashFile(file);
            fileToImport.setShaSum(shaSum);
            importController_.putShaSum(file, shaSum);
          }
          if (db_.doesBlobExist(shaSum)) {
            fileToImport.setImportObstacle(ImportObstacle.DUPLICATE, null);
          } else {
            fileToImport.setImportObstacle(null, null);
          }
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

}
