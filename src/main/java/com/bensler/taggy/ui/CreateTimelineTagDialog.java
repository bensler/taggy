package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.taggy.ui.MainFrame.ICON_PLUS_30;

import java.time.format.DateTimeParseException;

import javax.swing.JLabel;
import javax.swing.JTextField;

import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.decaf.swing.dialog.BasicContentPanel;
import com.bensler.decaf.swing.dialog.DialogAppearance;
import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.util.prefs.BulkPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class CreateTimelineTagDialog extends BasicContentPanel<Void, Tag> {

  private final JTextField textfield_;
  private final TagController tagCtrl_;

  public CreateTimelineTagDialog(TagController tagCtrl) {
    super(new DialogAppearance(
      new OverlayIcon(MainFrame.ICON_TIMELINE_48, new Overlay(ICON_PLUS_30, SE)),
      "Create Timeline Tag", "Create a New Tag representing a Calendar Date", true
    ), new FormLayout(
      "r:p, 3dlu, f:p:g",
      "c:p"
    ));
    final CellConstraints cc = new CellConstraints();

    tagCtrl_ = tagCtrl;
    textfield_ = new JTextField(20);
    addValidationSource(textfield_);
    add(new JLabel("Date (YYYY-MM-DD):"), cc.xy(1, 1));
    add(textfield_, cc.xy(3, 1));
  }

  @Override
  protected void contextSet(Context ctx) {
    ctx.setPrefs(new BulkPrefPersister(
      App.getApp().getPrefs(),
      new WindowPrefsPersister(new PrefKey(App.PREFS_APP_ROOT, CreateTimelineTagDialog.class), ctx_.getDialog())
    ));
    ctx.setComponentToFocus(textfield_);
  }

  @Override
  protected void validateContent(ValidationContext validationCtx, Object eventSource) {
    final String text = textfield_.getText().trim();

    try {
      TagController.YYYY_MM_DD.parse(text);
      if (tagCtrl_.containsDateTag(text)) {
        validationCtx.addErrorMsg("There is already a Timeline Tag for that date");
      };
    } catch (DateTimeParseException dtpe) {
      validationCtx.addErrorMsg("Date format must be YYYY-MM-DD");
    }
  }

  @Override
  public Tag getData() {
    return tagCtrl_.getDateTag(textfield_.getText().trim());
  }


}
