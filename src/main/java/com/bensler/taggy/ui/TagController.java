package com.bensler.taggy.ui;

import static com.bensler.decaf.util.function.ForEachMapperAdapter.forEachMapper;
import static com.bensler.taggy.persist.TagProperty.REPRESENTED_DATE;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.util.Pair;
import com.bensler.decaf.util.stream.Collectors;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.AutoCloseableTxn;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.Tag;

public class TagController {

  public static final DateTimeFormatter PROPERTY_DATE_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
  public static final DateTimeFormatter PROPERTY_DATE_YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
  public static final DateTimeFormatter UI_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM");
  public static final DateTimeFormatter UI_WEEK_DAY_FORMATTER = DateTimeFormatter.ofPattern("d (E)");
  public static final String VALUE_DATE_ROOT = "dateRoot";

  private final App app_;

  private final Hierarchy<Tag> allTags_;
  private final Map<String, Tag> dateTags_;

  public TagController(App app) {
    app_ = app;
    allTags_ = new Hierarchy<>();
    dateTags_ = app_.getDbAccess().loadAllTags().stream()
      .map(forEachMapper(allTags_::add))
      .map(tag -> new Pair<>(tag.getProperty(REPRESENTED_DATE), tag))
      .filter(pair -> (pair.getLeft() != null))
      .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, HashMap::new));
  }

  Tag getDateTag(String dateStr) {
    return computeIfAbsent(dateStr, this::createDateTag);
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
    final TemporalAccessor date = BlobController.dateFormatter.parse(dateStr);
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

  public void setAllTags(EntityTree<Tag> tree) {
    tree.setData(allTags_);
  }

  void deleteTag(Tag tag) {
    final Set<Blob> blobs = tag.getBlobs();
    final DbAccess db = app_.getDbAccess();

    try (AutoCloseableTxn act = new AutoCloseableTxn(db.startTxn())) {
      db.removeNoTxn(tag);
      blobs.stream()
      .map(forEachMapper(blob -> blob.removeTag(tag)))
      .forEach(db::merge);
    }
    app_.entityRemoved(tag);
    app_.entitiesChanged(blobs);
    allTags_.removeNode(tag);
    Optional.ofNullable(tag.getProperty(REPRESENTED_DATE)).ifPresent(dateTags_::remove);
  }

  Tag resolveTag(Tag tag) {
    return allTags_.resolve(tag);
  }

  Tag persistNewTag(Tag newTag) {
    final Tag createdTag = app_.storeEntity(newTag);

    allTags_.add(createdTag);
    Optional.ofNullable(createdTag.getProperty(REPRESENTED_DATE)).ifPresent(dateStr -> dateTags_.put(dateStr, createdTag));
    return createdTag;
  }

  Tag updateTag(Tag tag, Tag updatedTag) {
    final Tag editedTag;

    allTags_.removeNode(tag);
    editedTag = app_.storeEntity(updatedTag);
    allTags_.add(editedTag);
    return editedTag;
  }

  boolean isLeaf(Tag tag) {
    return allTags_.isLeaf(tag);
  }

}
