package com.bensler.taggy.ui;

import static com.bensler.taggy.ui.Icons.TAG_SIMPLE_13;
import static com.bensler.taggy.ui.Icons.TIMELINE_13;

import javax.swing.Icon;

import com.bensler.decaf.swing.view.SimpleCellRenderer;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.persist.TagProperty;

public class TagCellRenderer extends SimpleCellRenderer<Tag, String> {

  @Override
  protected Icon getIcon(Tag tag, String name) {
    return (tag.getProperty(TagProperty.REPRESENTED_DATE) == null)
      ? TAG_SIMPLE_13 : TIMELINE_13;
  }

}
