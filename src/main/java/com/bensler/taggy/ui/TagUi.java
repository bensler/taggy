package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.view.SimplePropertyGetter.createGetter;
import static com.bensler.decaf.util.cmp.CollatorComparator.COLLATOR_COMPARATOR;
import static com.bensler.taggy.persist.TagProperty.REPRESENTED_DATE;

import java.util.List;

import javax.swing.Icon;

import com.bensler.decaf.swing.action.FilteredAction.FilterOne;
import com.bensler.decaf.swing.view.EntityPropertyComparator;
import com.bensler.decaf.swing.view.PropertyViewImpl;
import com.bensler.decaf.swing.view.SimpleCellRenderer;
import com.bensler.decaf.util.cmp.ComparableComparator;
import com.bensler.decaf.util.cmp.ComparatorChain;
import com.bensler.taggy.persist.Tag;

public final class TagUi {

  public static final EntityPropertyComparator<Tag, String> NAME_COMPARATOR = new EntityPropertyComparator<>(Tag::getName, COLLATOR_COMPARATOR);

  public static final EntityPropertyComparator<Tag, String> DATE_COMPARATOR = new EntityPropertyComparator<>(
    tag -> tag.getProperty(REPRESENTED_DATE), new ComparableComparator<>()
  );

  public static final FilterOne<Tag> TIMELINE_TAG_FILTER = tag -> tag.containsProperty(REPRESENTED_DATE);

  public static final FilterOne<Tag> TAG_FILTER = tag -> !TIMELINE_TAG_FILTER.matches(tag);

  public static final SimpleCellRenderer<Tag, String> CELL_RENDERER = new SimpleCellRenderer<>() {
    @Override
    protected Icon getIcon(Tag tag, String name) {
      return (tag.containsProperty(REPRESENTED_DATE) ? Icons.TIMELINE_13 : Icons.TAG_SIMPLE_13);
    }
  };

  public static final PropertyViewImpl<Tag, String> NAME_VIEW = new PropertyViewImpl<>(
    CELL_RENDERER, createGetter(Tag::getName, new ComparatorChain<>(List.of(DATE_COMPARATOR, NAME_COMPARATOR)))
  );

  private TagUi() {}

}
