package com.bensler.taggy.ui;

import static com.bensler.taggy.ui.MainFrame.PREF_BASE_KEY;
import static com.bensler.taggy.ui.ThumbnailEntityListenerAdapter.Operation.ADD_OR_UPDATE;
import static com.bensler.taggy.ui.ThumbnailEntityListenerAdapter.Operation.REMOVE;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.bensler.decaf.swing.action.FocusedComponentActionController;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersister;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.ui.ThumbnailOverviewPanel.ScrollingPolicy;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

class MainThumbnailPanel extends JPanel {

  private final ThumbnailOverview thumbs_;
  private final JLabel statusLabel_;
  private final ImagesUiController imgUiCtrl_;
  private Optional<Tag> currentTag_;
  private int blobsCount_, selectedBlobsCount_;

  MainThumbnailPanel(App app) {
    super(new FormLayout("f:p:g", "f:p:g, 3dlu, f:p"));
    currentTag_ = Optional.empty();
    thumbs_ = new ThumbnailOverview(ScrollingPolicy.SCROLL_VERTICALLY, app.getBlobCtrl());

    app.putZombie(this, new ThumbnailEntityListenerAdapter(
      app, thumbs_.getComponent(),
      blob -> currentTag_.isPresent() && blob.containsTag(currentTag_.get()) ? ADD_OR_UPDATE : REMOVE
    ));
    imgUiCtrl_ = new ImagesUiController(app, thumbs_.getComponent());
    new FocusedComponentActionController(imgUiCtrl_.getAllActions(), Set.of(thumbs_)).attachTo(thumbs_, overview -> {}, thumbs_::beforeCtxMenuOpen);
    thumbs_.addSelectionListener((source, selection) -> selectionChanged(selection.size()));

    add(thumbs_.getScrollPane(), new CellConstraints(1, 1));
    statusLabel_ = new JLabel("", SwingConstants.RIGHT);
    statusLabel_.setBorder(new EmptyBorder(0, 5, 5, 5));
    add(statusLabel_, new CellConstraints(1, 3));
  }

  private void selectionChanged(int selectionCount) {
    selectedBlobsCount_ = selectionCount;
    updateStatusLabel();
  }

  private void setData(Collection<Blob> blobs) {
    thumbs_.setData(blobs);
    blobsCount_ = blobs.size();
    selectedBlobsCount_ = thumbs_.getSelection().size();
    updateStatusLabel();
  }

  private void updateStatusLabel() {
    statusLabel_.setText("Images: %s %s".formatted(blobsCount_, (selectedBlobsCount_> 0) ? " (%s)".formatted(selectedBlobsCount_): ""));
  }

  public void addSelectionListener(Consumer<List<Blob>> listener) {
    thumbs_.addSelectionListener((source, selection) -> listener.accept(selection));
  }

  public ImagesUiController getImagesUiCtrl() {
    return imgUiCtrl_;
  }

  public void setData(Optional<Tag> tag) {
    setData((currentTag_ = tag).map(Tag::getBlobs).orElseGet(Set::of));
  }

  public ThumbnailOverview getEntityComponent() {
    return thumbs_;
  }

  public List<PrefPersister> getPrefPersisters() {
    return List.of(
      imgUiCtrl_.getExportPrefPersister(),
      thumbs_.getPrefPersisters(new PrefKey(PREF_BASE_KEY, "selectedImages"))
    );
  }

}
