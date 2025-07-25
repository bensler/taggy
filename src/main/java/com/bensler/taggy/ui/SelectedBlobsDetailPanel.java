package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.action.ActionState.ENABLED;
import static com.bensler.decaf.swing.action.ActionState.HIDDEN;
import static com.bensler.decaf.swing.view.SimplePropertyGetter.createGetterComparator;
import static com.bensler.decaf.util.cmp.CollatorComparator.COLLATOR_COMPARATOR;
import static javax.swing.JSplitPane.VERTICAL_SPLIT;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JSplitPane;

import com.bensler.decaf.swing.action.ActionAppearance;
import com.bensler.decaf.swing.action.ActionGroup;
import com.bensler.decaf.swing.action.EntityAction;
import com.bensler.decaf.swing.action.FocusedComponentActionController;
import com.bensler.decaf.swing.action.SingleEntityActionAdapter;
import com.bensler.decaf.swing.action.SingleEntityFilter;
import com.bensler.decaf.swing.table.EntityTable;
import com.bensler.decaf.swing.table.TablePrefPersister;
import com.bensler.decaf.swing.table.TablePropertyView;
import com.bensler.decaf.swing.table.TableView;
import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.swing.view.PropertyViewImpl;
import com.bensler.decaf.util.Pair;
import com.bensler.decaf.util.prefs.BulkPrefPersister;
import com.bensler.decaf.util.prefs.DelegatingPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersister;
import com.bensler.decaf.util.prefs.Prefs;
import com.bensler.decaf.util.tree.Hierarchical;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.Tag;

public class SelectedBlobsDetailPanel {

  static class NameValuePair extends Pair<String, String> {
    NameValuePair(String left, String right) {
      super(left, right);
    }
  }

  private final EntityTree<Tag> tagTree_;
  private final EntityTable<NameValuePair> propertiesTable_;
  private final JSplitPane splitpane_;
  /** keep divider position {@link #propertiesTable_} is hidden */
  private int lastSplitpaneDividerLocation=100;

  public SelectedBlobsDetailPanel(MainFrame mainFrame) {
    tagTree_ = new EntityTree<>(TagUi.NAME_VIEW, Tag.class);
    tagTree_.setVisibleRowCount(20, .5f);
    final EntityAction<Tag> focusAction = new EntityAction<>(
      new ActionAppearance(null, null, "Focus", null), Tag.class,
      new SingleEntityFilter<>(HIDDEN, tag -> ENABLED),
      new SingleEntityActionAdapter<>((source, tag) -> tag.ifPresent(mainFrame::selectTag))
    );
    tagTree_.setCtxActions(new FocusedComponentActionController(new ActionGroup(focusAction), Set.of(tagTree_)));
    final TablePropertyView<NameValuePair, String> propertyKeyColumn;
    propertiesTable_ = new EntityTable<>(new TableView<>(
      propertyKeyColumn = new TablePropertyView<>("key", "Name", createGetterComparator(NameValuePair::getLeft, COLLATOR_COMPARATOR)),
      new TablePropertyView<>("value", "Value", new PropertyViewImpl<>(createGetterComparator(NameValuePair::getRight, COLLATOR_COMPARATOR)))
    ), NameValuePair.class);
    propertiesTable_.sortByColumn(propertyKeyColumn);
    splitpane_ = new JSplitPane(VERTICAL_SPLIT, true,
      tagTree_.getScrollPane(), propertiesTable_.getScrollPane()
    );
    splitpane_.setDividerLocation(.7f);
    hidePropertiesTable();
    lastSplitpaneDividerLocation = -1;
  }

  public void setData(List<Blob> blobs) {
    final Set<Tag> allTags = blobs.stream()
      .map(Blob::getTags)
      .flatMap(Set::stream)
      .distinct()
      .flatMap(tag -> Hierarchical.toPath(tag).stream())
      .distinct()
      .collect(Collectors.toSet());

    tagTree_.setData(new Hierarchy<>(allTags));
    tagTree_.expandCollapseAll(true);

    if (blobs.size() == 1) {
      final Blob blob = blobs.get(0);

      propertiesTable_.clear();
      propertiesTable_.addOrUpdateData(
        blob.getPropertyNames().stream()
        .map(name -> new NameValuePair(name, blob.getProperty(name)))
        .collect(Collectors.toList())
      );
      if (splitpane_.getBottomComponent() == null) {
        splitpane_.setBottomComponent(propertiesTable_.getScrollPane());
        if (lastSplitpaneDividerLocation > 0) {
          splitpane_.setDividerLocation(lastSplitpaneDividerLocation);
        }
      }
    } else {
      hidePropertiesTable();
    }
  }
  private boolean isPropertiesTableHidden() {
    return (splitpane_.getBottomComponent() == null);
  }

  private void hidePropertiesTable() {
    if (!isPropertiesTableHidden()) {
      lastSplitpaneDividerLocation = splitpane_.getDividerLocation();
      splitpane_.setBottomComponent(null);
    }
    propertiesTable_.clear();
  }

  public JSplitPane getComponent() {
    return splitpane_;
  }

  private void setDividerLocation(int location) {
    if (isPropertiesTableHidden()) {
      lastSplitpaneDividerLocation = location;
    } else {
      splitpane_.setDividerLocation(location);
    }
  }

  private int getDividerLocation() {
    return (isPropertiesTableHidden() && (lastSplitpaneDividerLocation > 0) ? lastSplitpaneDividerLocation : splitpane_.getDividerLocation());
  }

  public PrefPersister createPrefPersister(PrefKey prefKey) {
    return new BulkPrefPersister(
      new DelegatingPrefPersister(new PrefKey(prefKey, "split"),
        () -> Optional.of(String.valueOf(getDividerLocation())),
        value -> Prefs.tryParseInt(value).ifPresent(this::setDividerLocation)
      ),
      new TablePrefPersister(new PrefKey(prefKey, "properties"), propertiesTable_.getComponent())
    );
  }

}
