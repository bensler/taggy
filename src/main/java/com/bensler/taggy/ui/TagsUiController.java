package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.decaf.util.function.ForEachMapperAdapter.forEachMapper;
import static com.bensler.decaf.util.stream.Collectors.toMap;
import static com.bensler.taggy.App.getApp;
import static com.bensler.taggy.persist.TagProperty.REPRESENTED_DATE;
import static com.bensler.taggy.ui.Icons.EDIT_13;
import static com.bensler.taggy.ui.Icons.PLUS_10;
import static com.bensler.taggy.ui.Icons.TAG_48;
import static com.bensler.taggy.ui.Icons.TAG_SIMPLE_13;
import static com.bensler.taggy.ui.Icons.TIMELINE_13;
import static com.bensler.taggy.ui.Icons.X_10;
import static com.bensler.taggy.ui.Icons.X_30;
import static com.bensler.taggy.ui.ThumbnailOverviewPanel.ScrollingPolicy.SCROLL_HORIZONTALLY;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bensler.decaf.swing.action.ActionAppearance;
import com.bensler.decaf.swing.action.ActionGroup;
import com.bensler.decaf.swing.action.FilteredAction;
import com.bensler.decaf.swing.action.UiAction;
import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.decaf.swing.dialog.ConfirmationDialog;
import com.bensler.decaf.swing.dialog.DialogAppearance;
import com.bensler.decaf.swing.dialog.OkCancelDialog;
import com.bensler.decaf.util.Pair;
import com.bensler.decaf.util.tree.Hierarchical;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.persist.TagDbMapper.TagHeadData;

public class TagsUiController {

  public final static DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public static final DateTimeFormatter PROPERTY_DATE_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
  public static final DateTimeFormatter PROPERTY_DATE_YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
  public static final DateTimeFormatter UI_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM");
  public static final DateTimeFormatter UI_WEEK_DAY_FORMATTER = DateTimeFormatter.ofPattern("d (E)");
  public static final String VALUE_DATE_ROOT = "dateRoot";

  private final Hierarchy<Tag> allTags_;
  private final Map<String, Tag> dateTags_;

  private final UiAction editTagAction_;
  private final UiAction newTagAction_;
  private final UiAction newTimelineTagAction_;
  private final UiAction deleteTagAction_;

  public TagsUiController(App app) {
    allTags_ = new Hierarchy<>();
    dateTags_ = app.getDbAccess().loadAll(Tag.class).stream()
      .map(forEachMapper(allTags_::add))
      .map(tag -> new Pair<>(tag.containsProperty(REPRESENTED_DATE), tag))
      .filter(pair -> pair.getLeft().isPresent())
      .collect(toMap(pair -> pair.getLeft().get(), Pair::getRight, HashMap::new));
    editTagAction_ = new UiAction(
      new ActionAppearance(new OverlayIcon(TAG_SIMPLE_13, new Overlay(EDIT_13, SE)), TagDialog.Edit.ICON, "Edit Tag", "Edit currently selected Tag"),
      FilteredAction.one(Tag.class, TagUi.TAG_FILTER, this::editTagUi)
    );
    newTagAction_ = new UiAction(
      new ActionAppearance(new OverlayIcon(TAG_SIMPLE_13, new Overlay(PLUS_10, SE)), TagDialog.Create.ICON, "Create Tag", "Creates a new Tag under the currently selected Tag"),
      FilteredAction.oneOrNone(Tag.class, TagUi.TAG_FILTER, this::createTagUi)
    );
    newTimelineTagAction_ = new UiAction(
      new ActionAppearance(new OverlayIcon(TIMELINE_13, new Overlay(PLUS_10, SE)), null, "Create Timeline Tag", "Creates a new Tag representing a calendar date"),
      FilteredAction.one(Tag.class, TagUi.TIMELINE_TAG_FILTER, tag -> createTimelineUi())
    );
    deleteTagAction_ = new UiAction(
      new ActionAppearance(new OverlayIcon(TAG_SIMPLE_13, new Overlay(X_10, SE)), null, "Delete Tag", "Remove currently selected Tag"),
      FilteredAction.one(Tag.class, this::isLeaf, this::deleteTagUi)
    );
  }

  public Set<Tag> getAllTagsFiltered(String filterStr) {
    final String matchStr = filterStr.toLowerCase().trim();
    final Set<Tag> allNodes = allTags_.getMembers();

    return allNodes.stream()
    .filter(tag -> matchTag(tag, matchStr))
    .flatMap(tag -> allTags_.getSubHierarchyMembers(tag).stream())
    .flatMap(tag -> Hierarchical.toPath(tag).stream())
    .distinct().collect(Collectors.toSet());
  }

  private boolean matchTag(Tag tag, String pattern) {
    return (
      tag.getName().toLowerCase().contains(pattern)
      || tag.containsProperty(REPRESENTED_DATE).stream().anyMatch(dateStr -> dateStr.contains(pattern))
    );
  }

  public UiAction getNewTagAction() {
    return newTagAction_;
  }

  public UiAction getEditTagAction() {
    return editTagAction_;
  }

  public ActionGroup getAllTagActions() {
    return new ActionGroup(editTagAction_, newTagAction_, newTimelineTagAction_, deleteTagAction_);
  }

  Tag getDateTag(String dateStr) {
    return computeIfAbsent(dateStr, this::createDateTag);
  }

  /** @param data according to {@link BlobController#dateParser}*/
  public boolean containsDateTag(String date) {
    return dateTags_.containsKey(date);
  }

  /** had to impl it myself as recursive calls on {@link Map#computeIfAbsent(Object, Function)} fail in a {@link ConcurrentModificationException} */
  private Tag computeIfAbsent(String tagDateKey, Function<String, Tag> tagCreator) {
    Tag tag = dateTags_.get(tagDateKey);

    if (tag == null) {
      tag = tagCreator.apply(tagDateKey);
      dateTags_.put(tagDateKey, tag);
    }
    return tag;
  }

  private Tag createDateTag(String dateStr) {
    final TemporalAccessor date = YYYY_MM_DD.parse(dateStr);
    final Tag parentMonthTag = computeIfAbsent(PROPERTY_DATE_MONTH_FORMATTER.format(date), tagDateKey -> createMonthTag(date, tagDateKey));

    return persistNewTag(new Tag(parentMonthTag, UI_WEEK_DAY_FORMATTER.format(date), Map.of(REPRESENTED_DATE, dateStr)));
  }

  private Tag createMonthTag(TemporalAccessor date, String propertyDateMonth) {
    final Tag parentYearTag = computeIfAbsent(PROPERTY_DATE_YEAR_FORMATTER.format(date), this::createYearTag);

    return persistNewTag(new Tag(parentYearTag, UI_MONTH_FORMATTER.format(date), Map.of(REPRESENTED_DATE, propertyDateMonth)));
  }

  private Tag createYearTag(String propertyDateYear) {
    final Tag datesRootTag = computeIfAbsent(VALUE_DATE_ROOT, tagDateKey -> persistNewTag(new Tag(null, "Timeline", Map.of(REPRESENTED_DATE, VALUE_DATE_ROOT))));

    return persistNewTag(new Tag(datesRootTag, propertyDateYear, Map.of(REPRESENTED_DATE, propertyDateYear)));
  }

  public Set<Tag> getAllTags() {
    return allTags_.getMembers();
  }

  private void deleteTag(Tag tag) {
    final Set<Blob> blobs;
    final App app = getApp();
    final DbAccess db = app.getDbAccess();

    db.runInTxn(() -> db.deleteNoTxn(tag));
    blobs = db.refreshAllRefs(tag.getBlobRefs());
    app.entityRemoved(tag);
    app.entitiesChanged(blobs);
    allTags_.removeNode(tag);
    removeFromDateTags(tag);
  }

  private Tag persistNewTag(Tag newTag) {
    final App app = getApp();
    final Tag createdTag = app.getDbAccess().storeObject(newTag);

    allTags_.add(createdTag);
    app.entityCreated(createdTag);
    addToDateTags(createdTag);
    return createdTag;
  }

  private Tag updateTag(TagHeadData tagHeadData) {
    final App app = getApp();
    final DbAccess db = app.getDbAccess();
    final Tag editedTag;
    final Tag oldTag = db.resolve(tagHeadData.subject_);

    db.runInTxn(() -> db.getTagDbMapper().updateHeadData(tagHeadData));
    editedTag =  db.refresh(tagHeadData.subject_);
    allTags_.add(editedTag);
    removeFromDateTags(oldTag);
    addToDateTags(editedTag);
    app.entityChanged(editedTag);
    return editedTag;
  }

  private void removeFromDateTags(Tag tag) {
    tag.containsProperty(REPRESENTED_DATE).ifPresent(dateTags_::remove);
  }

  private void addToDateTags(Tag tag) {
    tag.containsProperty(REPRESENTED_DATE).ifPresent(dateStr -> dateTags_.put(dateStr, tag));
  }

  private boolean isLeaf(Tag tag) {
    return allTags_.isLeaf(tag);
  }

  private void createTagUi(Optional<Tag> parentTag) {
    new OkCancelDialog<>(getApp().getMainFrameFrame(), new TagDialog.Create()).show(
      parentTag, this::persistNewTag
    );
  }

  private void createTimelineUi() {
    new OkCancelDialog<>(getApp().getMainFrameFrame(), new CreateTimelineTagDialog(this)).show(
      null, this::persistNewTag
    );
  }

  private void editTagUi(Tag tag) {
    new OkCancelDialog<>(getApp().getMainFrameFrame(), new TagDialog.Edit()).show(
      tag, this::updateTag
    );
  }

  private void deleteTagUi(Tag tag) {
    final OverlayIcon icon = new OverlayIcon(TAG_48, new Overlay(X_30, SE));
    final ConfirmationDialog confDlg;
    final int imgCount = tag.getBlobRefs().size();

    if (imgCount > 0) {
      final ThumbnailOverviewPanel thumbs = new ThumbnailOverviewPanel(SCROLL_HORIZONTALLY);

      thumbs.setData(tag.getBlobs());
      confDlg = new ConfirmationDialog(new DialogAppearance(
        icon, "Confirmation: Delete Tag",
        "Do you really want to delete Tag \"%s\"? It has %s image%s assigned.".formatted(
          tag.getName(), imgCount, ((imgCount > 1) ? "s" : "")
        )
      ), thumbs.getScrollPane());
    } else {
      confDlg = new ConfirmationDialog(new DialogAppearance(
        icon, "Confirmation: Delete Tag",
        "Do you really want to delete Tag \"%s\" under \"%s\"?".formatted(
          tag.getName(), Optional.ofNullable(tag.getParent()).map(Tag::getName).orElse("Root")
        )
      ));
    }
    if (confDlg.confirm(getApp().getMainFrameFrame())) {
      deleteTag(tag);
    }
  }

}
