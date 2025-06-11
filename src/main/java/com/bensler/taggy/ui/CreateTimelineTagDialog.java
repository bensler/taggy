package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.taggy.ui.MainFrame.ICON_PLUS_30;

import javax.swing.JLabel;
import javax.swing.JTextField;

import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.decaf.swing.dialog.BasicContentPanel;
import com.bensler.decaf.swing.dialog.DialogAppearance;
import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.util.prefs.BulkPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class CreateTimelineTagDialog extends BasicContentPanel<Void, Tag> {

  protected final JTextField nameTextfield_;
  protected final Hierarchy<Tag> allTags_;

  public CreateTimelineTagDialog(Hierarchy<Tag> allTags) {
    super(new DialogAppearance(
      new OverlayIcon(MainFrame.ICON_TIMELINE_48, new Overlay(ICON_PLUS_30, SE)), "Create Timeline Tag", "Create a New Tag representing a Calendar Date"
    ), new FormLayout(
      "r:p, 3dlu, f:p:g",
      "c:p"
    ));
    allTags_ = allTags;

    final CellConstraints cc = new CellConstraints();

    nameTextfield_ = new JTextField(20);
    addValidationSource(nameTextfield_);
    add(new JLabel("Name:"), cc.xy(1, 1));
    add(nameTextfield_, cc.xy(3, 1));
  }

  @Override
  protected void contextSet(Context ctx) {
    ctx.setPrefs(new BulkPrefPersister(
      App.getApp().getPrefs(),
      new WindowPrefsPersister(new PrefKey(App.PREFS_APP_ROOT, CreateTimelineTagDialog.class), ctx_.getDialog())
    ));
    ctx.setComponentToFocus(nameTextfield_);
  }

  @Override
  protected boolean validateContent(Object validationSource) {
    return true; //TOOD
  }

  protected String getNewName() {
    return nameTextfield_.getText().trim();
  }

  @Override
  public void setData(Void nothing) {
//    parentTag_.setData(allTags_);
//    optParent.ifPresent(parentTag_::select);
  }

  @Override
  public Tag getData() {
    return null; // TODO new Tag(parentTag_.getSingleSelection(), getNewName(), Map.of());
  }


}
