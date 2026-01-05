package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.decaf.util.prefs.DelegatingPrefPersister.createSplitPanePrefPersister;
import static com.bensler.taggy.App.getApp;
import static com.bensler.taggy.ui.Icons.IMAGE_48;
import static com.bensler.taggy.ui.Icons.TAGS_36;
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
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersisterImpl;
import com.bensler.decaf.util.tree.Hierarchical;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class EditImageTagsDialog extends BasicContentPanel<Blob, Set<Tag>> {

  public static final OverlayIcon ICON = new OverlayIcon(IMAGE_48, new Overlay(TAGS_36, SE));

  private final AllTagsTreeFiltered allTags_;
  private final CheckboxTree<Tag> assignedTags_;
  private final ImageComponent imgComp_;
  private final JSplitPane verticalSplitpane_;
  private final JSplitPane horizontalSplitpane_;

  public EditImageTagsDialog() {
    super(new DialogAppearance(
      ICON,
      "Edit Image Tags", "Assign Tags to an Image"
    ), new FormLayout("f:p:g", "f:p:g"));
    allTags_ = new AllTagsTreeFiltered(this::setAssignedTags);
    imgComp_ = new ImageComponent();
    assignedTags_ = new CheckboxTree<>(TagUi.NAME_VIEW, Tag.class);
    assignedTags_.setVisibleRowCount(15, 1);
    assignedTags_.addCheckedListener(this::assignedTagsTreeChanged);
    verticalSplitpane_ = new JSplitPane(VERTICAL_SPLIT, true, imgComp_, assignedTags_.getScrollPane());
    verticalSplitpane_.setResizeWeight(.5f);
    horizontalSplitpane_ = new JSplitPane(HORIZONTAL_SPLIT, true, allTags_.getComponent(), verticalSplitpane_);
    horizontalSplitpane_.setResizeWeight(.5f);
    add(horizontalSplitpane_, new CellConstraints(1, 1));
    // TODO crap, hard coded size ----------------------vvvvvvvv
    horizontalSplitpane_.setPreferredSize(new Dimension(200, 200));
  }

  @Override
  protected void contextSet(Context ctx) {
    final PrefKey baseKey = new PrefKey(App.PREFS_APP_ROOT, getClass());

    ctx.setPrefs(new PrefPersisterImpl(
      getApp().getPrefs(),
      new WindowPrefsPersister(baseKey, ctx_.getDialog()),
      createSplitPanePrefPersister(new PrefKey(baseKey, "verticalSplitpane"), verticalSplitpane_),
      createSplitPanePrefPersister(new PrefKey(baseKey, "horizontalSplitpane"), horizontalSplitpane_)
    ));
  }

  @Override
  public Set<Tag> getData() {
    return allTags_.getCheckedNodes();
  }

  @Override
  protected void setData(Blob blob) {
    try {
      final Set<Tag> tags = blob.getTags();

      imgComp_.setImage(getApp().getBlobCtrl().loadRotated(blob));
      allTags_.makeAllTagsVisible(tags);
      setAssignedTags(tags);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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
