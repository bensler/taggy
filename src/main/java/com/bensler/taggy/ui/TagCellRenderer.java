package com.bensler.taggy.ui;

import javax.swing.Icon;

import com.bensler.decaf.swing.view.SimpleCellRenderer;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.persist.TagProperty;

public class TagCellRenderer extends SimpleCellRenderer<Tag, String> {

  public TagCellRenderer() {
    super();
  }

  @Override
  protected Icon getIcon(Tag tag, String name) {
    return (tag.getProperty(TagProperty.REPRESENTED_DATE) == null)
      ? MainFrame.ICON_TAG_13 : MainFrame.ICON_TIMELINE_13;
  }

}
