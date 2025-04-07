package com.bensler.taggy.ui;

import static com.bensler.decaf.util.function.ForEachMapperAdapter.forEachMapper;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.util.Pair;
import com.bensler.decaf.util.stream.Collectors;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.Tag;

public class TagController {

  public static final String PROPERTY_DATE = "tag.date";
  public static final String VALUE_DATE_ROOT = "dateRoot";

  private final App app_;

  private final Hierarchy<Tag> allTags_;
  private final Map<String, Tag> dateTags_;

  public TagController(App app) {
    app_ = app;
    allTags_ = new Hierarchy<>();
    dateTags_ = app_.getDbAccess().loadAllTags().stream()
      .map(forEachMapper(allTags_::add))
      .map(tag -> new Pair<>(tag.getProperty(PROPERTY_DATE), tag))
      .filter(pair -> (pair.getLeft() != null))
      .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, HashMap::new));
  }

  Tag getDateTag(String dateStr) {
    return computeIfAbsent(dateStr, () -> createDateTag(dateStr));
  }

  Tag computeIfAbsent(String tagDateStr, Supplier<Tag> tagCreator) {
    Tag tag = dateTags_.get(tagDateStr);

    if (tag == null) {
      tag = tagCreator.get();
      dateTags_.put(tagDateStr, tag);
    }
    return tag;
  }

  DateTimeFormatter propertyDateMonthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
  DateTimeFormatter propertyDateYearFormatter = DateTimeFormatter.ofPattern("yyyy");
  DateTimeFormatter uiMonthFormatter = DateTimeFormatter.ofPattern("MM (MMMM)");
  DateTimeFormatter uiWeekDayFormatter = DateTimeFormatter.ofPattern("dd (E)");

  private Tag createDateTag(String dateStr) {
    final TemporalAccessor date = BlobController.dateFormatter.parse(dateStr);
    final String propertyDateMonth = propertyDateMonthFormatter.format(date);
    final Tag parentMonthTag = computeIfAbsent(propertyDateMonth, () -> createMonthTag(date, propertyDateMonth));

    return persistNewTag(new Tag(parentMonthTag, uiWeekDayFormatter.format(date), Map.of(PROPERTY_DATE, dateStr)));
  }

  private Tag createMonthTag(TemporalAccessor date, String propertyDateMonth) {
    final String year = propertyDateYearFormatter.format(date);
    final Tag parentYearTag = computeIfAbsent(year, () -> createYearTag(date, year));

    return persistNewTag(new Tag(parentYearTag, uiMonthFormatter.format(date), Map.of(PROPERTY_DATE, propertyDateMonth)));
  }

  private Tag createYearTag(TemporalAccessor date, String propertyDateYear) {
    final Tag datesRootTag = computeIfAbsent(VALUE_DATE_ROOT, () -> persistNewTag(new Tag(null, "Timeline", Map.of(PROPERTY_DATE, VALUE_DATE_ROOT))));

    return persistNewTag(new Tag(datesRootTag, propertyDateYear, Map.of(PROPERTY_DATE, propertyDateYear)));
  }

  void setAllTags(EntityTree<Tag> tree) {
    tree.setData(allTags_);
  }

  void deleteTag(Tag tag) {
    final Set<Blob> blobs = tag.getBlobs();
    final DbAccess db = app_.getDbAccess();

    app_.deleteEntity(tag);
    allTags_.removeNode(tag);
    Optional.ofNullable(tag.getProperty(PROPERTY_DATE)).ifPresent(dateTags_::remove);
    blobs.forEach(db::refresh);
  }

  Tag resolveTag(Tag tag) {
    return allTags_.resolve(tag);
  }

  Tag persistNewTag(Tag newTag) {
    final Tag createdTag = app_.storeEntity(newTag);

    allTags_.add(createdTag);
    Optional.ofNullable(createdTag.getProperty(PROPERTY_DATE)).ifPresent(dateStr -> dateTags_.put(dateStr, createdTag));
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
