package com.bensler.taggy.persist;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.bensler.decaf.util.entity.EntityReference;

public interface BlobDbMapper extends DbMapper<Blob> {

  boolean doesBlobExist(String shaHash) throws SQLException;

  List<Integer> findOrphanBlobs() throws SQLException;

  void setTags(EntityReference<Blob> blobRef, Set<Tag> tags) throws SQLException;

}
