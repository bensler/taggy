package com.bensler.taggy.persist;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Transaction;

public class DbAccess {

  public final static ThreadLocal<DbAccess> INSTANCE = new ThreadLocal<>();

  private final Connection session_;

  private final Map<EntityReference<?>, ?> entityCache_;

  public DbAccess(Connection session) throws SQLException {
    (session_ = session).setAutoCommit(false);
    entityCache_ = new HashMap<>();
  }

  public List<Tag> loadAllTags() {
    return new TagDbMapper().loadAll(session_);
  }

  public Transaction startTxn() {
    return null; //session_.beginTransaction();
  }

  public void removeNoTxn(Object obj) {
//    session_.remove(obj);
  }

  public void refresh(Object obj) {
//    session_.refresh(obj);
  }

  public <E extends Entity<E>> E merge(E obj) {
    return obj; // session_.merge(obj);
  }

  public <E extends Entity<E>> E storeObject(E obj) {
    return obj;
//    try (AutoCloseableTxn act = new AutoCloseableTxn(startTxn())) {
//      if (obj.getId() == null) {
//        session_.persist(obj);
//        return obj;
//      } else {
//        return session_.merge(obj);
//      }
//    }
  }

  public List<Blob> findOrphanBlobs() {
//    TODO
//    return session_.createQuery(
//      "FROM Blob AS blob " +
//      "LEFT JOIN FETCH blob.tags_ AS tags " +
//      "GROUP BY blob " +
//      "HAVING COUNT(tags) < 1", Blob.class
//    ).getResultList();
    return List.of();
  }

  public boolean doesBlobExist(String shaHash) {
    return false;
//    return !session_.createQuery("FROM Blob AS blob WHERE blob.sha256sum_ = :sha256sum", Blob.class)
//    .setParameter("sha256sum", shaHash)
//    .getResultList().isEmpty();
  }

  public <E extends Entity<E>> E load(EntityReference<E> entityRef) {

    return null;
  }

  public <ENTITY extends Entity<ENTITY>, CIN extends Collection<EntityReference<ENTITY>>, COUT extends Collection<ENTITY>> COUT resolve(
    CIN entities, COUT collector
  ) {
//    entities.stream().forEach(entity -> collector.add(new EntityReference<>(entity)));
    return collector;
  }

  public <E extends Entity<E>> E resolve(EntityReference<E> entityRef) {
    final E entity = (E)entityCache_.get(entityRef);

    return (entity != null) ? entity : load(entityRef);
  }

}
