package com.bensler.taggy.persist.app;

import static com.bensler.taggy.persist.app.PhotoDbMapper.R_TAG_IMAGE;
import static com.bensler.taggy.persist.base.EntityPropertyType.STRING;

import java.sql.SQLException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.persist.Photo;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.persist.TagProperty;
import com.bensler.taggy.persist.base.DbAccess;
import com.bensler.taggy.persist.base.DbMapper;
import com.bensler.taggy.persist.base.DbSetup;
import com.bensler.taggy.persist.base.DbSetup.Direction;
import com.bensler.taggy.persist.base.EntityProperty;
import com.bensler.taggy.persist.base.EntityPropertyType;
import com.bensler.taggy.persist.base.EntityToLoad;
import com.bensler.taggy.persist.base.EntityToStore;
import com.bensler.taggy.persist.base.EntityType;

public class TagDbMapper extends DbMapper<Tag> {

  public static final EntityProperty<String> P_TAG__NAME = new EntityProperty<>("name", STRING);
  public static final EntityProperty<EntityReference<Tag>> P_TAG__PARENT = new EntityProperty<>("parent", EntityPropertyType.createEntityType(Tag.class, false));
  public static final EntityType<Tag> E_TAG = new EntityType<>(
    Tag.class,
    P_TAG__NAME,
    P_TAG__PARENT
  );

  public TagDbMapper(DbAccess db, DbSetup dbSetup) {
    super(Tag.class, db, dbSetup, E_TAG);
  }

  @Override
  public List<Tag> loadAllEntities(List<Integer> ids) {
    try {
      return dbSetup_.loadAllEntities(db_, E_TAG, ids).stream().map(this::loadTag).toList();
    } catch (SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  private Tag loadTag(EntityToLoad properties) {
    final Optional<EntityReference<Tag>> parentId = properties.getValue(P_TAG__PARENT);

    return new Tag(
      properties.getEntityId(), parentId,
      properties.getValue(P_TAG__NAME).get(),
      properties.getOptionalProperties().entrySet().stream().collect(Collectors.toMap(entry -> TagProperty.valueOf(entry.getKey()), Entry::getValue)),
      properties.getRelationships(Direction.TO, R_TAG_IMAGE, Photo.class)
    );
  }

  @Override
  protected Integer persist(Tag tag, Scope scope) {
    final EntityToStore persistedEntity = dbSetup_.createPersistedEntity(E_TAG, tag.getId());

                                              addProperty(persistedEntity, P_TAG__NAME, tag.getName());
    tag.getParentRef().ifPresent(parentRef -> addProperty(persistedEntity, P_TAG__PARENT, parentRef));
    persistedEntity.putOptionalProperties(tag.getProperties().entrySet().stream().collect(Collectors.toMap(
      entry -> entry.getKey().name(), Entry::getValue
    )));
    if (scope.relationships_) {
      addRelationshipsTo(persistedEntity, R_TAG_IMAGE, tag.getBlobRefs());
    }
    return persist(persistedEntity, scope);
  }

}
