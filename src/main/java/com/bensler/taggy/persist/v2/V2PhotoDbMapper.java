package com.bensler.taggy.persist.v2;

import static com.bensler.taggy.persist.v2.EntityPropertyType.BLOB;
import static com.bensler.taggy.persist.v2.EntityPropertyType.ENTITY;
import static com.bensler.taggy.persist.v2.EntityPropertyType.STRING;
import static java.util.function.Function.identity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.Image;
import com.bensler.taggy.persist.Photo;
import com.bensler.taggy.persist.PhotoDbMapper;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.persist.Thumbnail;
import com.bensler.taggy.persist.v2.DbSetup.Direction;
import com.bensler.taggy.persist.v2.DbSetup.LoadEntityCollector;

public class V2PhotoDbMapper extends AbstractV2DbMapper<Photo> implements PhotoDbMapper {

  public static final EntityProperty<String> P_BLOB__FILE = new EntityProperty<>("file", BLOB);
  public static final EntityProperty<String> P_BLOB__TYPE = new EntityProperty<>("type", STRING);
  public static final EntityType<?> E_BLOB = new EntityType<>(
    Blob.class,
    P_BLOB__FILE,
    P_BLOB__TYPE
  );

  public static final EntityType<?> E_IMAGE = new EntityType<>(
    Image.class, E_BLOB
  );

  public static final EntityProperty<Integer> P_PHOTO__THUMBNAIL = new EntityProperty<>("thumbnail", ENTITY);
  public static final EntityType<Photo> E_PHOTO = new EntityType<>(
    Photo.class, E_IMAGE,
    P_PHOTO__THUMBNAIL
  );

  public static final EntityRelationshipType<Tag, Photo> R_TAG_IMAGE = new EntityRelationshipType<>("tag-blob", V2TagDbMapper.E_TAG, E_PHOTO);

  private final ThumbnailDbMapper thumbnailDbMapper_;

  public V2PhotoDbMapper(ThumbnailDbMapper thumbnailDbMapper, DbAccess db, DbSetup dbSetup) {
    super(Photo.class, db, dbSetup);
    thumbnailDbMapper_ = thumbnailDbMapper;
    db.runInTxn(con -> {
      dbSetup_.registerEntityTypes(con, List.of(E_BLOB, E_IMAGE, E_PHOTO));
      dbSetup_.registerRelationshipTypes(con, List.of(R_TAG_IMAGE));
    });
  }

  public ThumbnailDbMapper getThumbnailDbMapper() {
    return thumbnailDbMapper_;
  }

  @Override
  public List<Photo> loadAllEntities(List<Integer> ids) {
    try {
      final Collection<LoadEntityCollector> loadedEntities = dbSetup_.loadAllEntities(db_, E_PHOTO, ids);
      final Map<Integer, Thumbnail> thumbsById  = thumbnailDbMapper_.loadAllEntities(loadedEntities.stream()
        .map(properties -> properties.getValue(P_PHOTO__THUMBNAIL).get()).toList()
      ).stream().collect(Collectors.toMap(Thumbnail::getId, identity()));

      return loadedEntities.stream().map(properties -> loadPhoto(properties, thumbsById)).toList();
    } catch (SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  private Photo loadPhoto(LoadEntityCollector properties, Map<Integer, Thumbnail> thumbsById) {
    return new Photo(
      properties.getEntityId(),
      properties.getValue(P_BLOB__FILE).get(),
      thumbsById.get(properties.getValue(P_PHOTO__THUMBNAIL).get()),
      properties.getValue(P_BLOB__TYPE).get(),
      properties.getOptionalProperties(),
      properties.getRelationships(Direction.FROM, R_TAG_IMAGE, Tag.class)
    );
  }

  @Override
  public void remove(Integer id) throws SQLException {
    removeEntity("entity", "id", id);
  }

  @Override
  public void update(Photo photo, Scope scope) throws SQLException {
    persistBlob(photo, scope);
  }

  @Override
  public Integer insert(Photo photo) throws SQLException {
    return persistBlob(photo, Scope.FULL);
  }

  private Integer persistBlob(Photo photo, Scope scope) {
    final PersistedEntity persistedEntity = dbSetup_.createPersistedEntity(E_PHOTO, photo.getId());

    addProperty(persistedEntity, P_BLOB__FILE, photo.getSha256sum());
    addProperty(persistedEntity, P_PHOTO__THUMBNAIL, photo.getThumbnail().getId());
    addProperty(persistedEntity, P_BLOB__TYPE, photo.getType());
    persistedEntity.putOptionalProperties(photo.getMetaData());

    if (scope.relationships_) {
      addRelationshipsFrom(persistedEntity, R_TAG_IMAGE, photo.getTagRefs());
    }
    return persist(persistedEntity, scope);
  }

  @Override
  public List<Integer> findOrphanBlobs() throws SQLException {
    final List<Integer> ids = new ArrayList<>();

    try (PreparedStatement stmt = DbAccess.INSTANCE.get().prepareStatement("""
      SELECT e.id
      FROM entity e
      WHERE e.entity_type_id=?
      AND e.id NOT IN (
        SELECT DISTINCT er.target_entity_id
        FROM entity_relationship er
        WHERE er.type_id=?
      )
    """)) {
      final ResultSet result;

      stmt.setInt(1, dbSetup_.getEntityTypeId(E_PHOTO));
      stmt.setInt(2, dbSetup_.getRelationshipKey(R_TAG_IMAGE));
      result = stmt.executeQuery();
      while (result.next()) {
        ids.add(result.getInt(1));
      }
    }
    return ids;
  }

  @Override
  public boolean doesBlobExist(String shaHash) throws SQLException {
    try (PreparedStatement stmt = DbAccess.INSTANCE.get().prepareStatement("SELECT * FROM property_blob pb WHERE pb.value=? LIMIT 1")) {
      stmt.setString(1, shaHash);
      return stmt.executeQuery().next();
    }
  }

  @Override
  public void setTags(EntityReference<Photo> photoRef, Set<Tag> tags) {
    final PersistedEntity persistedEntity = dbSetup_.createPersistedEntity(E_PHOTO, photoRef.getId());

    addRelationshipsFrom(persistedEntity, R_TAG_IMAGE, EntityReference.createCollection(tags, new HashSet<>()));
    persist(persistedEntity, Scope.RELATIONSHIPS);
  }

}
