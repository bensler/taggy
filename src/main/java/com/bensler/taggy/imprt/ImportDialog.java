package com.bensler.taggy.imprt;

import static com.bensler.decaf.swing.view.SimplePropertyGetter.createComparableGetter;
import static com.bensler.decaf.swing.view.SimplePropertyGetter.createGetterComparator;
import static com.bensler.decaf.util.cmp.CollatorComparator.COLLATOR_COMPARATOR;
import static com.bensler.decaf.util.prefs.DelegatingPrefPersister.createSplitPanePrefPersister;
import static com.bensler.taggy.imprt.ImportController.ICON_LARGE;
import static com.bensler.taggy.ui.Icons.IMAGE_13;
import static com.bensler.taggy.ui.Icons.PLUS_10;
import static com.bensler.taggy.ui.Icons.X_10;
import static java.lang.Boolean.TRUE;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.bensler.decaf.swing.awt.WindowHelper;
import com.bensler.decaf.swing.dialog.DialogAppearance;
import com.bensler.decaf.swing.dialog.HeaderPanel;
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
import com.bensler.decaf.util.tree.Hierarchical;
import com.bensler.taggy.App;
import com.bensler.taggy.imprt.FileToImport.ImportObstacle;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.ui.BlobController;
import com.bensler.taggy.ui.Icons;
import com.bensler.taggy.ui.TagPrefPersister;
import com.bensler.taggy.ui.TagUi;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

class ImportDialog extends JDialog {

  final PropertyViewImpl<UiFile, String> FILE_NAME_VIEW = new PropertyViewImpl<>(
    new SimpleCellRenderer<>((_, name) -> name, (_, _) -> Icons.FOLDER_16), createGetterComparator(UiFile::getName, COLLATOR_COMPARATOR)
  );

  public static final SimpleCellRenderer<FileToImport, String> TYPE_ICON_RENDERER = new SimpleCellRenderer<> (
    null, (file, type) -> (file.getType() != null) ? IMAGE_13 : null
  );

  public static final SimpleCellRenderer<FileToImport, Boolean> IS_NEW_ICON_RENDERER = new SimpleCellRenderer<> (
    (file, importable) -> file.getImportObstacleAsString(),
    (file, importable) -> TRUE.equals(importable) ? PLUS_10 : X_10
  );

  private final ImportController importController_;
  private final BlobController blobCtrl_;
  private final DbAccess db_;
  private final EntityTable<FileToImport> files_;
  private final EntityTree<UiFile> srcFolder_;
  private final JButton srcFolderButton_;
  private final EntityTree<Tag> initialTag_;
  private final JButton initialTagButton_;
  private final JLabel fileSizeLabel_;
  private final JButton importButton_;
  private final List<FileToImport> filesToSha_;
  private final FileSizeRenderer fileSizeRenderer_;
  private final ShaSumDuplicateCheckThread shaSumThread_;

  ImportDialog(App app) {
    super(app.getMainFrameFrame(), true);
    importController_ = app.getImportCtrl();
    blobCtrl_ = app.getBlobCtrl();
    db_ = app.getDbAccess();
    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu",
      "f:p, 3dlu, f:p:g, 3dlu"
    ));
    var appearance = new DialogAppearance(ICON_LARGE, "Import Images", "Import images into the database.");
    setTitle(appearance.getWindowTitle());
    mainPanel.add(new HeaderPanel(appearance).getComponent(), new CellConstraints(1, 1, 3, 1));

    fileSizeRenderer_ = new FileSizeRenderer();
    final TablePropertyView<FileToImport, String> pathCol;
    files_ = new EntityTable<>(new TableView<>(
      new TablePropertyView<>("filename", "Filename", createGetterComparator(FileToImport::getName, COLLATOR_COMPARATOR)),
      pathCol = new TablePropertyView<>("relativePath", "Path", createGetterComparator(FileToImport::getRelativePath, COLLATOR_COMPARATOR)),
      new TablePropertyView<>("type", "Type", new PropertyViewImpl<>(
        TYPE_ICON_RENDERER, createGetterComparator(file -> Optional.ofNullable(file.getType()).orElseGet(() -> file.getFileType()), COLLATOR_COMPARATOR)
      )),
      new TablePropertyView<>("fileSize", "Size", new PropertyViewImpl<>(
        fileSizeRenderer_, createComparableGetter(FileToImport::getFileSize)
      )),
      new TablePropertyView<>("shasum", "sha256-Hash", createGetterComparator(FileToImport::getShaSum, COLLATOR_COMPARATOR)),
      new TablePropertyView<>("importable", "Importable", new PropertyViewImpl<>(
        IS_NEW_ICON_RENDERER, createComparableGetter(FileToImport::isImportable)
      ))
    ), FileToImport.class);
    files_.sortByColumn(pathCol);
    files_.getComponent().setMinimumSize(new Dimension(400, 400));
    files_.setSelectionMode(SelectionMode.MULTIPLE_INTERVAL);

    final JPanel sidePanel = new JPanel(new FormLayout(
      "f:p:g",
      "p, 3dlu, p, 6dlu, p, 3dlu, p, 3dlu:g, p, 3dlu, p"
    ));

    srcFolder_ = new EntityTree<>(FILE_NAME_VIEW, UiFile.class);
    srcFolder_.setVisibleRowCount(6, 0.5f);
    srcFolder_.setSelectionMode(SelectionMode.NONE);
    srcFolderButton_ = new JButton("Source Folder");
    srcFolderButton_.addActionListener(evt -> chooseImportDir());
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
    sidePanel.add(srcFolder_.getScrollPane(), new CellConstraints(1, 1));
    sidePanel.add(srcFolderButton_, new CellConstraints(1, 3));
    sidePanel.add(initialTag_.getScrollPane(), new CellConstraints(1, 5));
    sidePanel.add(initialTagButton_, new CellConstraints(1, 7));
    sidePanel.add(fileSizeLabel_, new CellConstraints(1, 9));
    sidePanel.add(importButton_, new CellConstraints(1, 11));

    final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, files_.getScrollPane(), sidePanel);
    splitPane.setDividerLocation(.7f);
    mainPanel.add(splitPane, new CellConstraints(2, 3));
    setContentPane(mainPanel);
    filesToSha_ = new LinkedList<>();
    pack();
    final PrefKey baseKey = new PrefKey(App.PREFS_APP_ROOT, getClass());
    final PrefPersisterImpl prefs = new PrefPersisterImpl(app.getPrefs(), new WindowPrefsPersister(baseKey, this),
      importController_.getPrefPersister(baseKey),
      createSplitPanePrefPersister(new PrefKey(baseKey, "split"), splitPane),
      TagPrefPersister.create(
        new PrefKey(baseKey, "initialTag"), this::getInitialTag,
        newInitialTag -> setInitialTag(Optional.of(newInitialTag))
      ),
      new TablePrefPersister(new PrefKey(baseKey, "files"), files_.getComponent())
    );
    new Thread(shaSumThread_ = new ShaSumDuplicateCheckThread(), "Taggy.Import.ShaSumDuplicateCheck").start();
    new WindowClosingTrigger(this, evt -> {
      synchronized (filesToSha_) {
        filesToSha_.clear();
      }
      shaSumThread_.wakeup(false);
      prefs.store();
    });
    WindowHelper.centerOnParent(this);
    populateFileList();
    displaySrcFolder();
  }

  private void populateFileList() {
    final List<FileToImport> filesToImport = importController_.getFilesToImport();
    files_.clear();
    files_.addOrUpdateData(filesToImport);
    filesToSha_.clear();
    filesToSha_.addAll(filesToImport.stream()
      .filter(file -> file.hasObstacle(ImportObstacle.SHA_MISSING) || file.hasObstacle(ImportObstacle.DUPLICATE_CHECK_MISSING)).toList());
    shaSumThread_.wakeup(true);
  }

  private void filesSelectionChanged(List<FileToImport> files) {
    final long fileSizesSum = files.stream().mapToLong(FileToImport::getFileSize).sum();
    final int filesCount = files.size();
    final boolean selectionNotEmpty = filesCount > 0;

    importButton_.setEnabled(selectionNotEmpty && files.stream().allMatch(FileToImport::isImportable));
    fileSizeLabel_.setText(selectionNotEmpty ? "%s file%s (%s)".formatted(
      filesCount, ((filesCount < 2) ? "" : "s"), FileSizeRenderer.formatFileSize(fileSizesSum)
    ) : " ");
  }

  private void chooseInitialTag() {
    new OkCancelDialog<>(this, new ChooseInitialTagDialog()).show(getInitialTag()).ifPresent(this::setInitialTag);
  }

  private void chooseImportDir() {
    final JFileChooser fileChooser = new JFileChooser(importController_.getImportDir());

    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.setFileHidingEnabled(false);
    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      final File newImportDir = fileChooser.getSelectedFile();

      importController_.setImportDir(newImportDir);
      displaySrcFolder();
      populateFileList();
    }
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
    initialTag_.setData(collector);
    initialTag_.expandCollapseAll(true);
  }

  private void importSelection() {
    new ImportProgressDialog(this, files_.getSelection(), getInitialTag()).setVisible(true);
  }

  static final class FileSizeRenderer extends SimpleCellRenderer<FileToImport, Long> {

    private final static String[] UNITS = new String[] {"B", "kB", "MB", "GB", "TB"};

    static String formatFileSize(Long fileSize) {
      int unitIndex = 0;

      while ((fileSize > 2048) && ((unitIndex + 1) < UNITS.length )) {
        fileSize = fileSize >> 10;
        unitIndex++;
      }
      return String.valueOf(fileSize) + " " + UNITS[unitIndex];
    }

    FileSizeRenderer() {
      super((entity, property) -> formatFileSize(property), null, SwingConstants.RIGHT);
    }
  }

  void fileImported(FileToImport doneFile) {
    final List<FileToImport> changedFiles = new ArrayList<>(List.of(doneFile));

    changedFiles.addAll(ImportController.markDuplicates(files_.getValues(), doneFile.getShaSum()));
    fileToUpdateChanged(changedFiles);
  }

  void fileToUpdateChanged(List<FileToImport> doneFiles) {
    SwingUtilities.invokeLater(() -> {
      files_.addOrUpdateData(doneFiles);
      files_.fireSelectionChanged();
    });
  }

  Optional<FileToImport> getNextToSha(Optional<FileToImport> lastProcessedItem) {
    synchronized (filesToSha_) {
      lastProcessedItem.ifPresent(file -> {
        filesToSha_.remove(file);
        fileToUpdateChanged(List.of(file));
      });
      return (!filesToSha_.isEmpty()) ? Optional.of(filesToSha_.getFirst()) : Optional.empty();
    }
  }

  class ShaSumDuplicateCheckThread implements Runnable {

    private final LinkedBlockingQueue<Boolean> continueWorkQueue_;

    ShaSumDuplicateCheckThread() {
      continueWorkQueue_ = new LinkedBlockingQueue<>();
    }

    void wakeup(boolean continueWorking) {
      try {
        continueWorkQueue_.put(continueWorking);
      } catch (InterruptedException ie) { /* no capacity limit, no wait, no interruption */ }
    }

    @Override
    public void run() {
      try {
        do {
          doWork();
        } while (continueWorkQueue_.take());
      } catch (InterruptedException ie) { /* just a wakeup call */ }
    }

    private void doWork() {
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
          if (db_.getBlobDbMapper().doesBlobExist(shaSum)) {
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

  private void displaySrcFolder() {
    srcFolder_.setData(Hierarchical.toPath(new UiFile(importController_.getImportDir())));
    srcFolder_.expandCollapseAll(true);
  }

  static class UiFile implements Hierarchical<UiFile> {

    private final UiFile parent_;
    private final String name_;

    UiFile(File file) {
      parent_ = Optional.ofNullable(file.getParentFile())
        .filter(lFile -> !lFile.getName().isEmpty())
        .map(UiFile::new).orElse(null);
      name_ = file.getName();
    }

    public String getName() {
      return name_;
    }

    @Override
    public UiFile getParent() {
      return parent_;
    }
  }

}
