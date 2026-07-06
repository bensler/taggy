package com.bensler.taggy.persist.v2;

import static com.bensler.taggy.persist.v2.EntityPropertyType.ENTITY;
import static com.bensler.taggy.persist.v2.EntityPropertyType.STRING;
import static com.bensler.taggy.persist.v2.V2PhotoDbMapper.R_TAG_IMAGE;

import java.sql.SQLException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.Photo;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.persist.TagProperty;
import com.bensler.taggy.persist.v2.DbSetup.Direction;
import com.bensler.taggy.persist.v2.DbSetup.LoadEntityCollector;

public class V2TagDbMapper extends AbstractV2DbMapper<Tag> {

  public static final EntityProperty<String> P_TAG__NAME = new EntityProperty<>("name", STRING);
  public static final EntityProperty<Integer> P_TAG__PARENT = new EntityProperty<>("parent", ENTITY);
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
    try {
      return dbSetup_.loadAllEntities(db_, E_TAG, ids).stream().map(this::loadTag).toList();
    } catch (SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  private Tag loadTag(LoadEntityCollector properties) {
    final Optional<Integer> parentId = properties.getValue(P_TAG__PARENT);

    return new Tag(
      properties.getEntityId(), parentId.map(lParentId -> new EntityReference<Tag>(Tag.class, lParentId)),
      properties.getValue(P_TAG__NAME).get(),
      properties.getOptionalProperties().entrySet().stream().collect(Collectors.toMap(entry -> TagProperty.valueOf(entry.getKey()), Entry::getValue)),
      properties.getRelationships(Direction.TO, R_TAG_IMAGE, Photo.class)
    );
  }

  @Override
  public void remove(Integer id) throws SQLException {
    removeEntity("entity", "id", id);
  }

  @Override
  public void update(Tag tag, Scope scope) {
    persistTag(tag, scope);
  }

  @Override
  public Integer insert(Tag tag) {
    return persistTag(tag, Scope.FULL);
  }

  private Integer persistTag(Tag tag, Scope scope) {
    final PersistedEntity persistedEntity = dbSetup_.createPersistedEntity(E_TAG, tag.getId());

                                              addProperty(persistedEntity, P_TAG__NAME, tag.getName());
    tag.getParentRef().ifPresent(parentRef -> addProperty(persistedEntity, P_TAG__PARENT, parentRef.getId()));
    persistedEntity.putOptionalProperties(tag.getProperties().entrySet().stream().collect(Collectors.toMap(
      entry -> entry.getKey().name(), Entry::getValue
    )));
    if (scope.relationships_) {
      addRelationshipsTo(persistedEntity, R_TAG_IMAGE, tag.getBlobRefs());
    }
    return persist(persistedEntity, scope);
  }

}
