package com.bensler.taggy.persist.v2;

import static com.bensler.taggy.persist.v2.EntityPropertyType.ENTITY;
import static com.bensler.taggy.persist.v2.EntityPropertyType.STRING;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.BlobDbMapper;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.Image;
import com.bensler.taggy.persist.Tag;

public class V2BlobDbMapper extends AbstractV2DbMapper<Blob> implements BlobDbMapper {

  public static final EntityProperty<String> P_BLOB__FILE = new EntityProperty<>("file", STRING);
  public static final EntityProperty<String> P_BLOB__TYPE = new EntityProperty<>("type", STRING);
  public static final EntityType<Blob> E_BLOB = new EntityType<>(
    Blob.class,
    P_BLOB__FILE,
    P_BLOB__TYPE
  );
  public static final EntityProperty<EntityReference<?>> P_IMAGE__FULL_SCALE_IMAGE = new EntityProperty<>("fullScaleImage", ENTITY);
  public static final EntityType<Image> E_IMAGE = new EntityType<>(
    Image.class, E_BLOB,
    P_IMAGE__FULL_SCALE_IMAGE
  );

  public V2BlobDbMapper(DbAccess db, DbSetup dbSetup) {
    super(Blob.class, db, dbSetup);
    db.runInTxn(con -> {
      dbSetup_.registerEntityTypes(con, List.of(E_BLOB, E_IMAGE));
    });
  }

  @Override
  public List<Blob> loadAllEntities(List<Integer> ids) {
    return null; // TODO
  }

  @Override
  public void remove(Integer id) throws SQLException {
    removeEntity("entity", "id", id);
  }

  @Override
  public void update(Blob blob) throws SQLException {
  }

  @Override
  public Integer insert(Blob blob) throws SQLException {
    return null; // TODO
  }

  @Override
  public List<Integer> findOrphanBlobs() {
    return null; // TODO
  }

  @Override
  public boolean doesBlobExist(String shaHash) {
    return false;// TODO
  }

  @Override
  public void setTags(EntityReference<Blob> blobRef, Set<Tag> tags) {
    // TODO
  }

}
