package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.action.ActionState.DISABLED;
import static com.bensler.decaf.swing.action.ActionState.ENABLED;
import static com.bensler.decaf.swing.view.SimplePropertyGetter.createGetter;
import static com.bensler.decaf.util.cmp.CollatorComparator.COLLATOR_COMPARATOR;

import java.util.List;

import javax.swing.Icon;

import com.bensler.decaf.swing.action.SingleEntityFilter;
import com.bensler.decaf.swing.view.EntityPropertyComparator;
import com.bensler.decaf.swing.view.PropertyViewImpl;
import com.bensler.decaf.swing.view.SimpleCellRenderer;
import com.bensler.decaf.util.cmp.ComparableComparator;
import com.bensler.decaf.util.cmp.ComparatorChain;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.persist.TagProperty;

public final class TagUi {

  public static final EntityPropertyComparator<Tag, String> NAME_COMPARATOR = new EntityPropertyComparator<>(Tag::getName, COLLATOR_COMPARATOR);

  public static final EntityPropertyComparator<Tag, String> DATE_COMPARATOR = new EntityPropertyComparator<>(
    tag -> tag.getProperty(TagProperty.REPRESENTED_DATE), new ComparableComparator<>()
  );

  public static final PropertyViewImpl<Tag, String> NAME_VIEW = new PropertyViewImpl<>(
    new TagCellRenderer(), createGetter(Tag::getName, new ComparatorChain<>(List.of(DATE_COMPARATOR, NAME_COMPARATOR)))
  );

  public static final SingleEntityFilter<Tag> TIMELINE_TAG_FILTER = new SingleEntityFilter<>(
    tag -> (tag.containsProperty(TagProperty.REPRESENTED_DATE) ? ENABLED : DISABLED)
  );

  public static final SingleEntityFilter<Tag> TAG_FILTER = new SingleEntityFilter<>(
    tag -> (TIMELINE_TAG_FILTER.getActionState(tag) == ENABLED ? DISABLED : ENABLED)
  );

  public static final SimpleCellRenderer<Tag, String> CELL_RENDERER = new SimpleCellRenderer<>() {

    @Override
    protected Icon getIcon(Tag tag, String name) {
      return (tag.getProperty(TagProperty.REPRESENTED_DATE) == null)
        ? MainFrame.ICON_TAG_SIMPLE_13 : MainFrame.ICON_TIMELINE_13;
    }

  };

  private TagUi() {}

}
