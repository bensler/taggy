package com.bensler.taggy.persist;

import static com.bensler.decaf.util.function.ForEachMapperAdapter.forEachMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbAccess {

  public final static ThreadLocal<DbAccess> INSTANCE = new ThreadLocal<>();

  private final Connection session_;

  private final Map<EntityReference<?>, Entity<?>> entityCache_;
  private final Map<Class<?>, DbMapper<?>> mapper_;

  public DbAccess(Connection session) throws SQLException {
    (session_ = session).setAutoCommit(false);
    entityCache_ = new HashMap<>();
    mapper_ = Map.of(
      Tag.class, new TagDbMapper(),
      Blob.class, new BlobDbMapper()
    );
  }

  public <E extends Entity<E>> List<E> loadAll(Class<E> clazz) {
    return mapper_.get(clazz).loadAll(session_).stream()
      .map(clazz::cast)
      .map(forEachMapper(entity -> entityCache_.put(new EntityReference<>(entity), entity)))
      .toList();
  }

  public AutoCloseableTxn startTxn() {
    return new AutoCloseableTxn(session_);
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

  public <E extends Entity<E>> E load(EntityReference<E> reference) {
    final Class<E> entityClass = reference.getEntityClass();
    final List<?> entities = mapper_.get(entityClass).loadAll(session_, List.of(reference.getId()));

    return (!entities.isEmpty() ? entityClass.cast(entities.get(0)) : null);
  }

  public <ENTITY extends Entity<ENTITY>, CIN extends Collection<EntityReference<ENTITY>>, COUT extends Collection<ENTITY>> COUT resolve(
    CIN references, COUT collector
  ) {
    if (!references.isEmpty()) {
      loadAll(references.iterator().next().getEntityClass(), references, collector);
    }
    return collector;
  }

  public <ENTITY extends Entity<ENTITY>, CIN extends Collection<EntityReference<ENTITY>>, COUT extends Collection<ENTITY>> void loadAll(
    Class<ENTITY> entityClass, CIN references, COUT collector
  ) {
    collector.addAll(mapper_.get(entityClass).loadAll(
      session_, references.stream().map(EntityReference::getId).toList()
    ).stream().map(entityClass::cast).toList());
  }

  public <E extends Entity<E>> E resolve(EntityReference<E> entityRef) {
    final E entity = (E)entityCache_.get(entityRef);

    return (entity != null) ? entity : load(entityRef);
  }

}
