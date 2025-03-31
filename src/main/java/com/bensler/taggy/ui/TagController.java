package com.bensler.taggy.ui;

import static com.bensler.decaf.util.function.ForEachMapperAdapter.forEachMapper;

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
    return computeIfAbsent(dateStr, this::createDateTag);
  }

  Tag computeIfAbsent(String tagDateStr, Function<String, Tag> tagCreator) {
    Tag tag = dateTags_.get(tagDateStr);

    if (tag == null) {
      tag = tagCreator.apply(tagDateStr);
      dateTags_.put(tagDateStr, tag);
    }
    return tag;
  }

  private Tag createDateTag(String dateStr) {
    final String[] ymd = dateStr.split("-");
    final String day = ymd[2];
    final String ym = ymd[0] + "-" + ymd[1];
    final Tag parentMonthTag = computeIfAbsent(ym, this::createMonthTag);

    return persistNewTag(new Tag(parentMonthTag, day, Map.of(PROPERTY_DATE, dateStr)));
  }

  private Tag createMonthTag(String monthStr) {
    final String[] ym = monthStr.split("-");
    final String month = ym[1];
    final String year = ym[0];
    final Tag parentYearTag = computeIfAbsent(year, this::createYearTag);

    return persistNewTag(new Tag(parentYearTag, month, Map.of(PROPERTY_DATE, monthStr)));
  }

  private Tag createYearTag(String yearStr) {
    final Tag datesRootTag = computeIfAbsent(VALUE_DATE_ROOT, dateRootStr -> persistNewTag(new Tag(null, "Timeline", Map.of(PROPERTY_DATE, dateRootStr))));

    return persistNewTag(new Tag(datesRootTag, yearStr, Map.of(PROPERTY_DATE, yearStr)));
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
