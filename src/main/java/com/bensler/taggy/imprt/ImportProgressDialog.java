package com.bensler.taggy.imprt;

import static com.bensler.taggy.ui.ThumbnailOverviewPanel.ScrollingPolicy.SCROLL_HORIZONTALLY;

import java.awt.Rectangle;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.bensler.decaf.swing.dialog.WindowClosingTrigger;
import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.util.prefs.DelegatingPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersisterImpl;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.ui.ThumbnailOverviewPanel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

class ImportProgressDialog extends JDialog {

  private final ImportDialog parent_;
  private final JCheckBox autoCloseCb_;
  private final JButton cancelButton_;
  private final ThumbnailOverviewPanel thumbs_;
  private final JProgressBar progress_;
  private final int fileToProcessCount_;
  private final ImportController importController_;
  private final List<FileToImport> filesToImport_;
  private final Tag initialTag_;
  private final PrefPersisterImpl prefs_;
  private boolean canceled_;

  ImportProgressDialog(ImportDialog parent, List<FileToImport> filesToImport, Tag initialTag) {
    super(parent, "Importing Files", true);
    final App app = App.getApp();
    final CellConstraints cc = new CellConstraints();

    parent_ = parent;
    importController_ = app.getImportCtrl();
    initialTag_ = initialTag;
    filesToImport_ = new LinkedList<>(filesToImport);
    canceled_ = false;

    final JPanel mainPanel = new JPanel(new FormLayout(
      "3dlu, f:p:g, 3dlu, f:p, 3dlu",
      "3dlu, f:p:g, 3dlu, p, 3dlu, p, 3dlu"
    ));
    thumbs_ = new ThumbnailOverviewPanel(SCROLL_HORIZONTALLY);
    thumbs_.setPreferredScrollableViewportSize(1, 3);
    mainPanel.add(thumbs_.getScrollPane(), cc.xyw(2, 2, 3, "f, d"));
    fileToProcessCount_ = filesToImport_.size();
    progress_ = new JProgressBar(0, fileToProcessCount_) {
      @Override
      public String getString() {
        return getProgressString(super.getString());
      }
    };
    progress_.setValue(0);
    progress_.setStringPainted(true);
    mainPanel.add(progress_, cc.xyw(2, 4, 3, "f, d"));
    autoCloseCb_ = new JCheckBox("Auto close when done");
    mainPanel.add(autoCloseCb_, cc.xy(2, 6, "l, d"));
    cancelButton_ = new JButton("Cancel");
    cancelButton_.addActionListener(evt -> cancelButtonPressed());
    mainPanel.add(cancelButton_, cc.xy(4, 6));

    setContentPane(mainPanel);
    pack();
    final Rectangle parentBounds = parent.getBounds();
    setLocation(
      (parentBounds.x + (parentBounds.width  / 2)) - (getWidth()  / 2),
      (parentBounds.y + (parentBounds.height / 2)) - (getHeight() / 2)
    );

    final PrefKey prefKey = new PrefKey(App.PREFS_APP_ROOT, getClass());
    prefs_ = new PrefPersisterImpl(app.getPrefs(),
      new WindowPrefsPersister(prefKey, this),
      new DelegatingPrefPersister(new PrefKey(prefKey, "autoClose"),
        () -> Optional.of(autoCloseCb_.isSelected() ? "X" : ""),
        checked -> autoCloseCb_.setSelected(!checked.isBlank())
      )
    );
    new WindowClosingTrigger(this, evt -> {
      synchronized (filesToImport_) {filesToImport_.clear();}
    });
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    SwingUtilities.invokeLater(() -> cancelButton_.requestFocus());
    new Thread(new ImportThread(), "Taggy.Import.Import").start();
  }

  String getProgressString(String percentStr) {
    synchronized (filesToImport_) {
      final int doneFilesCount = (fileToProcessCount_ - filesToImport_.size());

      return "%s/%s (%s) %s".formatted(doneFilesCount, fileToProcessCount_, percentStr, (canceled_ ? "canceled" : ""));
    }
  }

  @Override
  public void dispose() {
    prefs_.store();
    super.dispose();
  }

  private void cancelButtonPressed() {
    synchronized (filesToImport_) {
      if (filesToImport_.isEmpty() || canceled_) {
        closeDialog();
      } else {
        canceled_ = true;
        adjustButtonText();
      }
    }
  }

  private void closeDialog() {
    setVisible(false);
    dispose();
  }

  private void adjustButtonText() {
    synchronized (filesToImport_) {
      if (filesToImport_.isEmpty() || canceled_) {
        SwingUtilities.invokeLater(() -> cancelButton_.setText("Close"));
      }
    }
  }

  Optional<FileToImport> getNextToImport(Optional<FileToImport> lastProcessedItem) {
    synchronized (filesToImport_) {
      lastProcessedItem.ifPresent(lastProcessedFile -> {
        final Blob blob = lastProcessedFile.getBlob();
        final int filesToImportSize;

        filesToImport_.remove(lastProcessedFile);
        filesToImport_.removeAll(ImportController.markDuplicates(filesToImport_, lastProcessedFile.getShaSum()));
        filesToImportSize = filesToImport_.size();
        parent_.fileImported(lastProcessedFile);
        SwingUtilities.invokeLater(() -> {
          progress_.setValue(fileToProcessCount_ - filesToImportSize);
          if (blob != null) {
            thumbs_.addImage(blob);
            thumbs_.scrollToEnd();
          }
        });
      });
      adjustButtonText();
      return (filesToImport_.isEmpty() || canceled_) ? Optional.empty() : Optional.of(filesToImport_.getFirst());
    }
  }

  class ImportThread implements Runnable {
    @Override
    public void run() {
      Optional<FileToImport> fileInProgress = Optional.empty();

      while ((fileInProgress = getNextToImport(fileInProgress)).isPresent()) {
        fileInProgress = Optional.of(importController_.importFile(fileInProgress.get(), initialTag_));
      }
      if (autoCloseCb_.isSelected()) {
        SwingUtilities.invokeLater(() -> closeDialog());
      }
    }
  }

}
