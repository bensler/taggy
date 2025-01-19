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

  public void remove(Object obj) {
    final Transaction txn = session_.beginTransaction();

    try {
      session_.remove(obj);
    } finally {
      txn.commit(); // TODO rollback in case of exc
    }
  }

  public void refresh(Object obj) {
    session_.refresh(obj);
  }

  public <E> E createObject(E newObj) {
    final Transaction txn = session_.beginTransaction();

    session_.persist(newObj);
    txn.commit(); // TODO rollback in case of exc
    return newObj;
  }

  public <E extends Entity> E storeObject(E obj) {
    final Transaction txn = session_.beginTransaction();

    try {
      if (obj.getId() == null) {
        session_.persist(obj);
        return obj;
      } else {
        return session_.merge(obj);
      }
    } finally {
      txn.commit(); // TODO rollback in case of exc
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
