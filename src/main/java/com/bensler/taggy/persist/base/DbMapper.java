package com.bensler.taggy.persist.base;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import com.bensler.decaf.util.entity.Entity;
import com.bensler.decaf.util.entity.EntityReference;

public abstract class DbMapper<E extends Entity<E>> {

  public enum Scope {
    PROPERTIES   (true,  false),
    RELATIONSHIPS(false, true),
    FULL         (true,  true);

    public final boolean properties_;
    public final boolean relationships_;

    Scope(boolean properties, boolean relationships) {
      properties_ = properties;
      relationships_ = relationships;
    }
  }

  protected final Class<E> entityClass_;
  protected final DbAccess db_;
  protected final DbSetup dbSetup_;

  protected DbMapper(Class<E> entityClass, DbAccess db, DbSetup dbSetup) {
    entityClass_= entityClass;
    db_ = db;
    dbSetup_ = dbSetup;
  }

  public abstract List<E> loadAllEntities(List<Integer> ids);

  protected abstract Integer persist(E entity, Scope scope);

  public void update(E entity, Scope scope) {
    persist(entity, scope);
  }

  public Integer insert(E entity) {
    return persist(entity, Scope.FULL);
  }

  public E loadOne(Integer id) {
    return loadAllEntities(List.of(id)).get(0);
  }

  public final Class<E> getEntityClass() {
    return entityClass_;
  }

  public Integer persist(EntityToStore entity, Scope scope) {
    return db_.runInTxn2(con -> entity.persist(new PersistencyBaseLayer(con), scope));
  }

  public <JAVA_TYPE> void addProperty(EntityToStore persistedEntity, EntityProperty<JAVA_TYPE> propertyType, JAVA_TYPE value) {
    persistedEntity.addProperty(dbSetup_.getPropertyKey(persistedEntity.getBoundProperty(propertyType)), propertyType, value);
  }

  public <S extends Entity<S>, T extends Entity<T>> void addRelationshipsFrom(
    EntityToStore persistedEntity, EntityRelationshipType<S, T> relationshipType, Collection<EntityReference<S>> sourceRefs
  ) {
    persistedEntity.addRelationshipsFrom(dbSetup_.getRelationshipKey(relationshipType), sourceRefs);
  }

  public <S extends Entity<S>, T extends Entity<T>> void addRelationshipsTo(
    EntityToStore persistedEntity, EntityRelationshipType<S, T> relationshipType, Collection<EntityReference<T>> targetRefs
  ) {
    persistedEntity.addRelationshipsTo(dbSetup_.getRelationshipKey(relationshipType), targetRefs);
  }

  public <JAVA_TYPE> void addRelationship(EntityToStore persistedEntity, EntityProperty<JAVA_TYPE> propertyType, JAVA_TYPE value) {
    persistedEntity.addProperty(dbSetup_.getPropertyKey(persistedEntity.getBoundProperty(propertyType)), propertyType, value);
  }

  public void remove(Integer id) throws SQLException {
    try (PreparedStatement stmt = db_.prepareStatement("DELETE FROM entity WHERE id=?")) {
      stmt.setInt(1, id);
      stmt.execute();
    }
  }

}
