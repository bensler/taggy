package com.bensler.taggy.persist;

import static com.bensler.decaf.util.function.ForEachMapperAdapter.forEachMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  public void deleteNoTxn(Entity<?> entity) {
    try {
      mapper_.get(entity.getEntityClass()).remove(session_, entity.getId());
    } catch (SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  public <E extends Entity<E>> E refresh(E entity) {
    final EntityReference<E> reference = new EntityReference<>(entity);

    entityCache_.remove(reference);
    return resolve(reference);
  }

  public <E extends Entity<E>> Set<E> refreshAll(Collection<E> entities) {
    final Set<E> result = new HashSet<>();

    if (!entities.isEmpty()) {
      resolveAll(entities.stream().map(entity -> new EntityReference<>(entity)).toList(), result);
    }
    return result ;
  }

  public <E extends Entity<E>> E storeObject(E obj) {
    return obj;
    // TODO
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

  public boolean doesBlobExist(String shaHash) throws SQLException {
    try (PreparedStatement stmt = session_.prepareStatement("SELECT * FROM blob AS b WHERE b.sha256sum=? LIMIT 1")) {
      stmt.setString(1, shaHash);
      return stmt.executeQuery().next();
    }
  }

  public <E extends Entity<E>> E load(EntityReference<E> reference) {
    final Class<E> entityClass = reference.getEntityClass();
    final List<?> entities = mapper_.get(entityClass).loadAll(session_, List.of(reference.getId()));

    return (!entities.isEmpty() ? entityClass.cast(entities.get(0)) : null);
  }

  public <ENTITY extends Entity<ENTITY>, CIN extends Collection<EntityReference<ENTITY>>, COUT extends Collection<ENTITY>> COUT resolveAll(
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

  public void commit() throws SQLException {
    session_.commit();
  }

  public void rollback() {
    try {
      session_.rollback();
    } catch (SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

}
