package com.bensler.taggy.imprt;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.taggy.ui.Icons.IMAGE_48;
import static com.bensler.taggy.ui.Icons.TAGS_36;

import java.util.Optional;

import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.decaf.swing.dialog.BasicContentPanel;
import com.bensler.decaf.swing.dialog.DialogAppearance;
import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersisterImpl;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.ui.TagUi;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ChooseInitialTagDialog extends BasicContentPanel<Tag, Tag> {

  private final EntityTree<Tag> allTags_;

  public ChooseInitialTagDialog() {
    super(new DialogAppearance(
      new OverlayIcon(IMAGE_48, new Overlay(TAGS_36, SE)),
      "Choose Initial Tag", "Tag to be assigned automatically on import."
    ), new FormLayout("f:p:g", "f:p:g"));
    allTags_ = new EntityTree<>(TagUi.NAME_VIEW, Tag.class);
    allTags_.setVisibleRowCount(20, 1);
    add(allTags_.getScrollPane(), new CellConstraints(1, 1));
    App.getApp().getTagCtrl().setAllTags(allTags_);
  }

  @Override
  protected void contextSet(Context ctx) {
    final PrefKey baseKey = new PrefKey(App.PREFS_APP_ROOT, getClass());

    ctx.setPrefs(new PrefPersisterImpl(
      App.getApp().getPrefs(), new WindowPrefsPersister(baseKey, ctx_.getDialog())
    ));
  }

  @Override
  public Tag getData() {
    return allTags_.getSingleSelection();
  }

  @Override
  protected void setData(Tag tag) {
    Optional.ofNullable(tag).flatMap(allTags_::contains).ifPresent(lTag -> allTags_.select(lTag));
  }

}
