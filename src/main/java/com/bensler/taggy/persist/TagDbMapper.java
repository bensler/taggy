package com.bensler.taggy.persist;

import java.sql.SQLException;

import com.bensler.decaf.util.entity.EntityReference;

public interface TagDbMapper extends DbMapper<Tag> {

  class TagHeadData {

    public final EntityReference<Tag> subject_;
    public final Tag parent_;
    public final String name_;

    public TagHeadData(Tag subject, Tag parent, String name) {
      subject_ = new EntityReference<>(subject);
      parent_ = parent;
      name_ = name;
    }

  }

  void updateHeadData(TagHeadData tagHeadData) throws SQLException;

}
