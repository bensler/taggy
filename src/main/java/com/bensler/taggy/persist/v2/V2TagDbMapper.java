package com.bensler.taggy.persist.v2;

import static com.bensler.taggy.persist.v2.EntityPropertyType.ENTITY;
import static com.bensler.taggy.persist.v2.EntityPropertyType.STRING;

import java.sql.SQLException;
import java.util.List;

import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.persist.TagDbMapper;

public class V2TagDbMapper extends AbstractV2DbMapper<Tag> implements TagDbMapper {

  public static final EntityProperty<String> P_TAG__NAME = new EntityProperty<>("name", STRING);
  public static final EntityProperty<EntityReference<?>> P_TAG__PARENT = new EntityProperty<>("parent", ENTITY);
  public static final EntityType<Tag> E_TAG = new EntityType<>(
    Tag.class,
    P_TAG__NAME,
    P_TAG__PARENT
  );

  public V2TagDbMapper(DbAccess db, DbSetup dbSetup) {
    super(Tag.class, db, dbSetup);
    db.runInTxn(con -> dbSetup_.registerEntityTypes(con, List.of(E_TAG)));
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
