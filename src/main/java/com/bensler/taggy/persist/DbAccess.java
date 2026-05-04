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
import java.util.concurrent.atomic.AtomicReference;

import com.bensler.decaf.util.entity.Entity;
import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.App;

public class DbAccess {

  public final static ThreadLocal<DbAccess> INSTANCE = ThreadLocal.withInitial(() -> App.getApp().getDbAccess());

  private final Map<EntityReference<?>, Entity<?>> entityCache_;
  private final Map<Class<?>, DbMapper<?>> mapper_;
  private final Connection connection_;

  public DbAccess(Connection connection) throws SQLException {
    (connection_ = connection).setAutoCommit(false);
    entityCache_ = new HashMap<>();
    mapper_ = new HashMap<>();
    INSTANCE.set(this);
  }

  public <M extends DbMapper<?>> M registerMapper(M mapper) {
    final Class<?> entityClass = mapper.getEntityClass();

    if (mapper_.containsKey(entityClass)) {
      throw new IllegalArgumentException();
    } else {
      mapper_.put(entityClass, mapper);
      return mapper;
    }
  }

  public <E extends Entity<E>> List<E> loadAll(Class<E> clazz) {
    return loadEntities(clazz, List.of());
  }

  public void deleteNoTxn(Entity<?> entity) throws SQLException {
    mapper_.get(entity.getEntityClass()).remove(entity.getId());
  }

  public <E extends Entity<E>> E refresh(EntityReference<E> entityRef) {
    entityCache_.remove(entityRef);
    return resolve(entityRef);
  }

  public <E extends Entity<E>> Set<E> refreshAll(Collection<E> entities) {
    return refreshAllRefs(entities.stream().map(entity -> new EntityReference<>(entity)).toList());
  }

  public <E extends Entity<E>> Set<E> refreshAllRefs(Collection<EntityReference<E>> refs) {
    final Set<E> result = new HashSet<>();

    if (!refs.isEmpty()) {
      loadAll(refs, result);
    }
    return result;
  }

  public <E extends Entity<E>> E storeObject(E entity) {
    final Class<E> entityClass = entity.getEntityClass();
    final DbMapper<E> mapper = (DbMapper<E>)mapper_.get(entityClass);
    final AtomicReference<EntityReference<E>> ref = new AtomicReference<>();

    runInTxn(() -> {
      if (entity.hasId()) {
        mapper.update(entity);
        ref.set(new EntityReference<>(entity));
        entityCache_.remove(ref.get());
      } else {
        ref.set(new EntityReference<>(entityClass, mapper.insert(entity)));
      }
    });
    return resolve(ref.get());
  }

  private <E extends Entity<E>> void addToCache(E entity) {
    entityCache_.put(new EntityReference<>(entity), entity);
  }

  private <E extends Entity<E>> List<E> loadEntities(Class<E> entityClass, List<Integer> refs) {
    return mapper_.get(entityClass).loadAllEntities(refs).stream()
    .map(entityClass::cast)
    .map(forEachMapper(this::addToCache))
    .toList();
  }

  public <E extends Entity<E>> E load(EntityReference<E> reference) {
    final List<E> entities = loadEntities(reference.getEntityClass(), List.of(reference.getId()));

    if (entities.isEmpty()) {
      throw new IllegalStateException("Could not resolve \"%s\"".formatted(reference));
    } else {
      return entities.get(0);
    }
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
      collector.addAll(loadEntities(
        references.iterator().next().getEntityClass(),
        references.stream().map(EntityReference::getId).toList()
      ));
    }
    return collector;
  }

  public <E extends Entity<E>> E resolve(EntityReference<E> entityRef) {
    final E entity = (E)entityCache_.get(entityRef);

    return (entity != null) ? entity : load(entityRef);
  }

  public void runInTxn(PersistentWrite write) {
    try {
      write.runInTxn();
      connection_.commit();
    } catch (SQLException sqle) {
      rollback();
      throw new RuntimeException(sqle);
    }

  }

  public void rollback() {
    try {
      connection_.rollback();
    } catch (SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  public PreparedStatement prepareStatement(String sql) throws SQLException {
    return connection_.prepareStatement(sql);
  }

}
