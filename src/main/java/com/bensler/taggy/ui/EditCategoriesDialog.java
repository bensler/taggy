package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.taggy.ui.MainFrame.ICON_IMAGE_48;
import static com.bensler.taggy.ui.MainFrame.ICON_TAGS_36;
import static com.bensler.taggy.ui.MainFrame.TAG_NAME_VIEW;
import static javax.swing.JSplitPane.HORIZONTAL_SPLIT;
import static javax.swing.JSplitPane.VERTICAL_SPLIT;

import java.awt.Dimension;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JSplitPane;

import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.decaf.swing.dialog.BasicContentPanel;
import com.bensler.decaf.swing.dialog.DialogAppearance;
import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.swing.tree.CheckboxTree;
import com.bensler.decaf.util.prefs.BulkPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.tree.Hierarchical;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class EditCategoriesDialog extends BasicContentPanel<Blob, Set<Tag>> {

  public static final DialogAppearance APPEARANCE = new DialogAppearance(
    new OverlayIcon(ICON_IMAGE_48, new Overlay(ICON_TAGS_36, SE)),
    "Edit Image Tags", "Assign Tags to an Image"
  );

  private final CheckboxTree<Tag> allTags_;
  private final CheckboxTree<Tag> assignedTags_;
  private final ImageComponent imgComp_;
  private final JSplitPane verticalSplitpane_;
  private final JSplitPane horizontalSplitpane_;
  private final App app_;

  public EditCategoriesDialog() {
    super(new FormLayout("f:p:g", "f:p:g"));
    app_ = App.getApp();
    allTags_ = new CheckboxTree<>(TAG_NAME_VIEW);
    allTags_.setVisibleRowCount(20, 1);
    allTags_.setData(app_.getMainFrame().getAllTags());
    allTags_.addCheckedListener(this::setAssignedTags);
    imgComp_ = new ImageComponent();
    assignedTags_ = new CheckboxTree<>(TAG_NAME_VIEW);
    assignedTags_.setVisibleRowCount(15, 1);
    assignedTags_.addCheckedListener(this::assignedTagsTreeChanged);
    verticalSplitpane_ = new JSplitPane(VERTICAL_SPLIT, true, imgComp_, assignedTags_.getScrollPane());
    verticalSplitpane_.setResizeWeight(.5f);
    horizontalSplitpane_ = new JSplitPane(HORIZONTAL_SPLIT, true, allTags_.getScrollPane(), verticalSplitpane_);
    horizontalSplitpane_.setResizeWeight(.5f);
    add(horizontalSplitpane_, new CellConstraints(1, 1));
    // TODO crap, hard coded size ----------------------vvvvvvvv
    horizontalSplitpane_.setPreferredSize(new Dimension(200, 200));
  }

  @Override
  protected void contextSet(Context ctx) {
    final PrefKey baseKey = new PrefKey(App.PREFS_APP_ROOT, getClass());

    ctx.setPrefs(new BulkPrefPersister(
      app_.getPrefs(),
      new WindowPrefsPersister(baseKey, ctx_.getDialog()),
      new SplitpanePrefPersister(new PrefKey(baseKey, "verticalSplitpane"), verticalSplitpane_),
      new SplitpanePrefPersister(new PrefKey(baseKey, "horizontalSplitpane"), horizontalSplitpane_)
    ));
  }

  @Override
  public DialogAppearance getAppearance() {
    return APPEARANCE;
  }

  @Override
  public Set<Tag> getData() {
    return allTags_.getCheckedNodes();
  }

  @Override
  protected void setData(Blob blob) {
    try {
      final Set<Tag> tags = blob.getTags();

      imgComp_.setImage(app_.getThumbnailer().loadRotated(
        app_.getBlobCtrl().getFile(blob.getSha256sum())
      ));
      allTags_.expandCollapseAll(false);
      allTags_.setCheckedNodes(tags);
      tags.forEach(tag -> allTags_.expandCollapse(tag, true));
      setAssignedTags(tags);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  protected boolean validateContent(Object eventSource) {
    return true;
  }

  private void setAssignedTags(Set<Tag> checkedTags) {
    assignedTags_.setData(new Hierarchy<>(checkedTags.stream()
      .flatMap(tag -> Hierarchical.toPath(tag).stream())
      .distinct()
      .collect(Collectors.toSet())));
    assignedTags_.setCheckedNodes(checkedTags);
    assignedTags_.expandCollapseAll(true);
  }

  private void assignedTagsTreeChanged(Set<Tag> checkedTags) {
    allTags_.setCheckedNodes(checkedTags);
    setAssignedTags(checkedTags);
  }

}
