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

import com.bensler.taggy.App;

public class DbAccess {

  public final static ThreadLocal<DbAccess> INSTANCE = ThreadLocal.withInitial(() -> App.getApp().getDbAccess());

  private final Map<EntityReference<?>, Entity<?>> entityCache_;
  private final Map<Class<?>, DbMapper<?>> mapper_;

  final Connection session_;

  public DbAccess(Connection session) throws SQLException {
    (session_ = session).setAutoCommit(false);
    entityCache_ = new HashMap<>();
    mapper_ = Map.of(
      Tag.class, new TagDbMapper(this),
      Blob.class, new BlobDbMapper(this)
    );
    INSTANCE.set(this);
  }

  public BlobDbMapper getBlobDbMapper() {
    return (BlobDbMapper)mapper_.get(Blob.class);
  }

  public TagDbMapper getTagDbMapper() {
    return (TagDbMapper)mapper_.get(Tag.class);
  }

  public <E extends Entity<E>> List<E> loadAll(Class<E> clazz) {
    return mapper_.get(clazz).loadAll().stream()
      .map(clazz::cast)
      .map(forEachMapper(entity -> entityCache_.put(new EntityReference<>(entity), entity)))
      .toList();
  }

  public <E extends Entity<E>> void addToCache(E entity) {
    entityCache_.put(new EntityReference<>(entity), entity);
  }

  public void deleteNoTxn(Entity<?> entity) {
    try {
      mapper_.get(entity.getEntityClass()).remove(entity.getId());
    } catch (SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  public <E extends Entity<E>> E refresh(EntityReference<E> entityRef) {
    entityCache_.remove(entityRef);
    return resolve(entityRef);
  }

  public <E extends Entity<E>> Set<E> refreshAll(Collection<E> entities) {
    final Set<E> result = new HashSet<>();

    if (!entities.isEmpty()) {
      loadAll(entities.stream().map(entity -> new EntityReference<>(entity)).toList(), result);
    }
    return result ;
  }

  public <E extends Entity<E>> E storeObject(E entity) throws SQLException {
    final Class<E> entityClass = entity.getEntityClass();
    final DbMapper<E> mapper = (DbMapper<E>)mapper_.get(entityClass);
    final EntityReference<E> ref;

    try {
      if (entity.hasId()) {
        mapper.update(entity);
        ref = new EntityReference<>(entity);
        entityCache_.remove(ref);
      } else {
        ref = new EntityReference<>(entityClass, mapper.insert(entity));
      }
      commit();
    } catch (SQLException sqle) {
      rollback();
      throw sqle;
    }
    return resolve(ref);
  }

  public <E extends Entity<E>> E load(EntityReference<E> reference) {
    final Class<E> entityClass = reference.getEntityClass();
    final List<?> entities = mapper_.get(entityClass).loadAll(List.of(reference.getId()));

    return (!entities.isEmpty() ? entityClass.cast(entities.get(0)) : null);
  }

  public <ENTITY extends Entity<ENTITY>, CIN extends Collection<EntityReference<ENTITY>>, COUT extends Collection<ENTITY>> COUT resolveAll(
    CIN references, COUT collector
  ) {
    final Set<EntityReference<ENTITY>> toLoad = new HashSet<>();

    for (EntityReference<ENTITY> ref : references) {
      if (entityCache_.containsKey(ref)) {
        collector.add(ref.getEntityClass().cast(entityCache_.get(ref)));
      } else {
        toLoad.add(ref);
      }
    };
    return loadAll(toLoad, collector);
  }

  public <ENTITY extends Entity<ENTITY>, CIN extends Collection<EntityReference<ENTITY>>, COUT extends Collection<ENTITY>> COUT loadAll(
    CIN references, COUT collector
  ) {
    if (!references.isEmpty()) {
      loadAll(references.iterator().next().getEntityClass(), references, collector);
    }
    return collector;
  }

  <ENTITY extends Entity<ENTITY>, CIN extends Collection<EntityReference<ENTITY>>, COUT extends Collection<ENTITY>> void loadAll(
    Class<ENTITY> entityClass, CIN references, COUT collector
  ) {
    collector.addAll(mapper_.get(entityClass).loadAll(
      references.stream().map(EntityReference::getId).toList()
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

  public PreparedStatement prepareStatement(String sql) throws SQLException {
    return session_.prepareStatement(sql);
  }

}
