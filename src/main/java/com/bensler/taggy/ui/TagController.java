package com.bensler.taggy.ui;

import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.Tag;

public class TagController {

  private final App app_;

  private final Hierarchy<Tag> allTags_;

  public TagController(App app) {
    app_ = app;
    allTags_ = new Hierarchy<>();
    allTags_.addAll(app_.getDbAccess().loadAllTags());
  }

  void setAllTags(EntityTree<Tag> tree) {
    tree.setData(allTags_);
  }

  void removeTag(Tag tag) {
    allTags_.removeNode(tag);
  }

  Tag resolveTag(Tag tag) {
    return allTags_.resolve(tag);
  }

  Tag persistTag(Tag newTag) {
    final Tag createdTag = app_.getDbAccess().storeObject(newTag);

    allTags_.add(createdTag);
    return createdTag;
  }

  Tag updateTag(Tag tag, Tag updatedTag) {
    final Tag editedTag;

    allTags_.removeNode(tag);
    editedTag = app_.getDbAccess().storeObject(updatedTag);
    allTags_.add(editedTag);
    return editedTag;
  }

  boolean isLeaf(Tag tag) {
    return allTags_.isLeaf(tag);
  }

}
