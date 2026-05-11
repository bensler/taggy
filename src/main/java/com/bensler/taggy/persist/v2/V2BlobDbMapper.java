package com.bensler.taggy.persist.v2;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.persist.Blob;
import com.bensler.taggy.persist.BlobDbMapper;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.Tag;

public class V2BlobDbMapper extends AbstractV2DbMapper<Blob> implements BlobDbMapper {

  public V2BlobDbMapper(DbAccess db) {
    super(Blob.class, db);
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
