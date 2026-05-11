package com.bensler.taggy.persist.v2;

import java.sql.SQLException;
import java.util.List;

import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.persist.TagDbMapper;

public class V2TagDbMapper extends AbstractV2DbMapper<Tag> implements TagDbMapper {

  public V2TagDbMapper(DbAccess db) {
    super(Tag.class, db);
  }

  @Override
  public List<Tag> loadAllEntities(List<Integer> ids) {
    return null; // TODO
  }

  @Override
  public void remove(Integer id) throws SQLException {
    removeEntity("entity", "id", id);
  }

  @Override
  public void updateHeadData(TagHeadData tagHeadData) {
    // TODO
  }

  @Override
  public void update(Tag tag) throws SQLException {
    // TODO
  }

  @Override
  public Integer insert(Tag tag) throws SQLException {
    return null; // TODO
  }

}
