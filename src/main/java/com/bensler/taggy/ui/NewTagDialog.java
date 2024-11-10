package com.bensler.taggy.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Optional;

import com.bensler.decaf.swing.dialog.BasicContentPanel;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.FormLayout;

public class NewTagDialog extends BasicContentPanel<Optional<Tag>, Tag> {

  public NewTagDialog() {
    super(new FormLayout("", ""));
    setPreferredSize(new Dimension(200, 200));
    setBackground(Color.GREEN);
  }

  @Override
  public void setData(Optional<Tag> inData) {
    ctx_.setValid(true);
    // TODO Auto-generated method stub

  }

  @Override
  public Tag getData() {
    return new Tag(inData_.orElse(null), "la li lu");
  }

}
