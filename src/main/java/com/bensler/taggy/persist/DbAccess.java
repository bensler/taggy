package com.bensler.taggy.persist;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

public class DbAccess {

  private final Session session_;

  public DbAccess(Session session) {
    session_ = session;
  }

  public List<Tag> loadAllTags() {
    return session_.createQuery("FROM Tag", Tag.class).getResultList();
  }

  public Transaction startTxn() {
    return session_.beginTransaction();
  }

  public void removeNoTxn(Object obj) {
    session_.remove(obj);
  }

  public void refresh(Object obj) {
    session_.refresh(obj);
  }

  public <E extends Entity> E merge(E obj) {
    return session_.merge(obj);
  }

  public <E extends Entity> E storeObject(E obj) {
    try (AutoCloseableTxn act = new AutoCloseableTxn(startTxn())) {
      if (obj.getId() == null) {
        session_.persist(obj);
        return obj;
      } else {
        return session_.merge(obj);
      }
    }
  }

  public List<Blob> findOrphanBlobs() {
    return session_.createQuery(
      "FROM Blob AS blob " +
      "LEFT JOIN FETCH blob.tags_ AS tags " +
      "GROUP BY blob " +
      "HAVING COUNT(tags) < 1", Blob.class
    ).getResultList();
  }

  public boolean doesBlobExist(String shaHash) {
    return !session_.createQuery("FROM Blob AS blob WHERE blob.sha256sum_ = :sha256sum", Blob.class)
    .setParameter("sha256sum", shaHash)
    .getResultList().isEmpty();
  }

}
