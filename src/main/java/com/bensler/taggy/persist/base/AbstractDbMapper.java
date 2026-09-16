package com.bensler.taggy.persist.base;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import com.bensler.decaf.util.entity.Entity;
import com.bensler.decaf.util.entity.EntityReference;

public abstract class AbstractDbMapper<E extends Entity<E>> implements DbMapper<E> {

  protected final Class<E> entityClass_;
  protected final DbAccess db_;
  protected final DbSetup dbSetup_;

  protected AbstractDbMapper(Class<E> entityClass, DbAccess db, DbSetup dbSetup) {
    entityClass_= entityClass;
    db_ = db;
    dbSetup_ = dbSetup;
  }

  @Override
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

  @Override
  public void remove(Integer id) throws SQLException {
    try (PreparedStatement stmt = db_.prepareStatement("DELETE FROM entity WHERE id=?")) {
      stmt.setInt(1, id);
      stmt.execute();
    }
  }

}
