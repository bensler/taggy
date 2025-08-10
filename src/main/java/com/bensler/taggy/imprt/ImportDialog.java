package com.bensler.taggy.imprt;

import static com.bensler.decaf.swing.view.SimplePropertyGetter.createComparableGetter;
import static com.bensler.decaf.swing.view.SimplePropertyGetter.createGetterComparator;
import static com.bensler.decaf.util.cmp.CollatorComparator.COLLATOR_COMPARATOR;
import static com.bensler.decaf.util.prefs.DelegatingPrefPersister.createSplitPanePrefPersister;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.bensler.decaf.swing.awt.WindowHelper;
import com.bensler.decaf.swing.dialog.OkCancelDialog;
import com.bensler.decaf.swing.dialog.WindowClosingTrigger;
import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.swing.selection.SelectionMode;
import com.bensler.decaf.swing.table.EntityTable;
import com.bensler.decaf.swing.table.TablePrefPersister;
import com.bensler.decaf.swing.table.TablePropertyView;
import com.bensler.decaf.swing.table.TableView;
import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.swing.view.PropertyViewImpl;
import com.bensler.decaf.swing.view.SimpleCellRenderer;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersisterImpl;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.App;
import com.bensler.taggy.imprt.FileToImport.ImportObstacle;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.ui.BlobController;
import com.bensler.taggy.ui.MainFrame;
import com.bensler.taggy.ui.TagPrefPersister;
import com.bensler.taggy.ui.TagUi;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

class ImportDialog extends JDialog {

  private final ImportController importController_;
  private final BlobController blobCtrl_;
  private final DbAccess db_;
  private final EntityTable<FileToImport> files_;
  private final EntityTree<Tag> initialTag_;
  private final JButton initialTagButton_;
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
      "3dlu, f:p:g, 3dlu"
    ));
    fileSizeRenderer_ = new FileSizeRenderer();
    final TablePropertyView<FileToImport, String> pathCol;
    files_ = new EntityTable<>(new TableView<>(
      new TablePropertyView<>("filename", "Filename", createGetterComparator(FileToImport::getName, COLLATOR_COMPARATOR)),
      pathCol = new TablePropertyView<>("relativePath", "Path", createGetterComparator(FileToImport::getRelativePath, COLLATOR_COMPARATOR)),
      new TablePropertyView<>("type", "Type", new PropertyViewImpl<>(
        new TypeIconRenderer(), createGetterComparator(FileToImport::getType, COLLATOR_COMPARATOR)
      )),
      new TablePropertyView<>("fileSize", "Size", new PropertyViewImpl<>(
        fileSizeRenderer_, createComparableGetter(FileToImport::getFileSize)
      )),
      new TablePropertyView<>("shasum", "sha256-Hash", createGetterComparator(FileToImport::getShaSum, COLLATOR_COMPARATOR)),
      new TablePropertyView<>("importable", "Importable", new PropertyViewImpl<>(
        new IsNewIconRenderer(), createComparableGetter(FileToImport::isImportable)
      ))
    ), FileToImport.class);
    files_.sortByColumn(pathCol);
    files_.getComponent().setMinimumSize(new Dimension(400, 400));
    files_.setSelectionMode(SelectionMode.MULTIPLE_INTERVAL);

    final JPanel sidePanel = new JPanel(new FormLayout(
      "f:p:g",
      "p, 3dlu, p, 3dlu:g, p, 3dlu, p"
    ));
    initialTag_ = new EntityTree<>(TagUi.NAME_VIEW, Tag.class);
    initialTag_.setVisibleRowCount(10, 0.5f);
    initialTag_.setSelectionMode(SelectionMode.NONE);
    initialTagButton_ = new JButton("Set Initial Tags");
    initialTagButton_.addActionListener(evt -> chooseInitialTag());
    fileSizeLabel_ = new JLabel(" ", JLabel.RIGHT);
    importButton_ = new JButton("Import");
    importButton_.setEnabled(false);
    importButton_.addActionListener(evt -> importSelection());
    files_.addSelectionListener((source, files) -> filesSelectionChanged(files));
    sidePanel.add(initialTag_.getScrollPane(), new CellConstraints(1, 1));
    sidePanel.add(initialTagButton_, new CellConstraints(1, 3));
    sidePanel.add(fileSizeLabel_, new CellConstraints(1, 5));
    sidePanel.add(importButton_, new CellConstraints(1, 7));

    final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, files_.getScrollPane(), sidePanel);
    splitPane.setDividerLocation(.7f);
    mainPanel.add(splitPane, new CellConstraints(2, 2));
    setContentPane(mainPanel);
    final List<FileToImport> filesToImport = importController_.getFilesToImport();
    files_.addOrUpdateData(filesToImport);
    filesToSha_ = new LinkedList<>(filesToImport.stream()
      .filter(file -> file.hasObstacle(ImportObstacle.SHA_MISSING) || file.hasObstacle(ImportObstacle.DUPLICATE_CHECK_MISSING)).toList());
    pack();
    final PrefKey baseKey = new PrefKey(App.PREFS_APP_ROOT, getClass());
    final PrefPersisterImpl prefs = new PrefPersisterImpl(app.getPrefs(),
      new WindowPrefsPersister(baseKey, this),
      createSplitPanePrefPersister(new PrefKey(baseKey, "split"), splitPane),
      TagPrefPersister.create(
        new PrefKey(baseKey, "initialTag"), app.getTagCtrl(),
        this::getInitialTag,
        newInitialTag -> setInitialTag(Optional.of(newInitialTag))
      ),
      new TablePrefPersister(new PrefKey(baseKey, "files"), files_.getComponent())
    );
    new WindowClosingTrigger(this, evt -> {
      synchronized (filesToSha_) {
        filesToSha_.clear();
      }
      prefs.store();
    });
    WindowHelper.centerOnParent(this);
    new Thread(new ShaSumDuplicateCheckThread(), "Taggy.Import.ShaSumDuplicateCheck").start();
  }

  private void filesSelectionChanged(List<FileToImport> files) {
    final long fileSizesSum = files.stream().mapToLong(FileToImport::getFileSize).sum();
    final int filesCount = files.size();
    final boolean selectionNotEmpty = filesCount > 0;

    importButton_.setEnabled(selectionNotEmpty && files.stream().allMatch(FileToImport::isImportable));
    fileSizeLabel_.setText(selectionNotEmpty ? "%s file%s (%s)".formatted(
      filesCount, ((filesCount < 2) ? "" : "s"), fileSizeRenderer_.formatFileSize(fileSizesSum)
    ) : " ");
  }

  private void chooseInitialTag() {
    new OkCancelDialog<>(this, new ChooseInitialTagDialog()).show(getInitialTag()).ifPresent(this::setInitialTag);
  }

  private Tag getInitialTag() {
    final Set<Tag> leafes = initialTag_.getData().getLeafNodes();

    return leafes.isEmpty() ? null : leafes.iterator().next();
  }

  private void setInitialTag(Optional<Tag> tag) {
    final Set<Tag> collector = new HashSet<>();

    tag.ifPresent(newInitialTag -> {
      do {
        collector.add(newInitialTag);
      } while((newInitialTag = newInitialTag.getParent()) != null);
    });
    initialTag_.setData(new Hierarchy<>(collector));
    initialTag_.expandCollapseAll(true);
  }

  private void importSelection() {
    new ImportProgressDialog(this, files_.getSelection(), getInitialTag()).setVisible(true);
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
        } catch (SQLException | IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

}
