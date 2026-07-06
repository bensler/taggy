package com.bensler.taggy.persist.v2;

import java.sql.SQLException;
import java.util.List;

import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.Thumbnail;
import com.bensler.taggy.persist.v2.DbSetup.LoadEntityCollector;

public class ThumbnailDbMapper extends AbstractV2DbMapper<Thumbnail> {

  public static final EntityType<Thumbnail> E_THUMBNAIL = new EntityType<>(
    Thumbnail.class, V2PhotoDbMapper.E_IMAGE
  );

  public ThumbnailDbMapper(DbAccess db, DbSetup dbSetup) {
    super(Thumbnail.class, db, dbSetup);
    db.runInTxn(con -> dbSetup_.registerEntityTypes(con, List.of(E_THUMBNAIL)));
  }

  @Override
  public List<Thumbnail> loadAllEntities(List<Integer> ids) {
    try {
      return dbSetup_.loadAllEntities(db_, E_THUMBNAIL, ids).stream().map(this::loadThumbnail).toList();
    } catch (SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  private Thumbnail loadThumbnail(LoadEntityCollector properties) {
    return new Thumbnail(
      properties.getEntityId(),
      properties.getValue(V2PhotoDbMapper.P_BLOB__FILE).get()
    );
  }

  @Override
  public void remove(Integer id) throws SQLException {
    removeEntity("entity", "id", id);
  }

  @Override
  public void update(Thumbnail blob, Scope scope) {
    persistThumbnail(blob);
  }

  @Override
  public Integer insert(Thumbnail blob) {
    return persistThumbnail(blob);
  }

  private Integer persistThumbnail(Thumbnail blob) {
    final PersistedEntity persistedEntity = dbSetup_.createPersistedEntity(E_THUMBNAIL, blob.getId());

    addProperty(persistedEntity, V2PhotoDbMapper.P_BLOB__FILE, blob.getSha256sum());
    addProperty(persistedEntity, V2PhotoDbMapper.P_BLOB__TYPE, blob.getType());

    return persist(persistedEntity, Scope.PROPERTIES);
  }

}
