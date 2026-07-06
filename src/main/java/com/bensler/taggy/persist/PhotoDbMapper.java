package com.bensler.taggy.persist;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.bensler.decaf.util.entity.EntityReference;

public interface PhotoDbMapper extends DbMapper<Photo> {

  boolean doesBlobExist(String shaHash) throws SQLException;

  List<Integer> findOrphanBlobs() throws SQLException;

  void setTags(EntityReference<Photo> blobRef, Set<Tag> tags) throws SQLException;

}
