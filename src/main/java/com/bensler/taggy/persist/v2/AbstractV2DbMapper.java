package com.bensler.taggy.persist.v2;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import com.bensler.decaf.util.entity.Entity;
import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.DbMapper;

public abstract class AbstractV2DbMapper<E extends Entity<E>> implements DbMapper<E> {

  protected final Class<E> entityClass_;
  protected final DbAccess db_;
  protected final DbSetup dbSetup_;

  protected AbstractV2DbMapper(Class<E> entityClass, DbAccess db, DbSetup dbSetup) {
    entityClass_= entityClass;
    db_ = db;
    dbSetup_ = dbSetup;
  }

  @Override
  public final Class<E> getEntityClass() {
    return entityClass_;
  }

  public Integer persist(PersistedEntity entity, Scope scope) {
    return db_.runInTxn2(con -> entity.persist(new PersistencyBaseLayer(con), scope));
  }

  public <JAVA_TYPE> void addProperty(PersistedEntity persistedEntity, EntityProperty<JAVA_TYPE> propertyType, JAVA_TYPE value) {
    persistedEntity.addProperty(dbSetup_.getPropertyKey(persistedEntity.getBoundProperty(propertyType)), propertyType, value);
  }

  public <S extends Entity<S>, T extends Entity<T>> void addRelationshipsFrom(
    PersistedEntity persistedEntity, EntityRelationshipType<S, T> relationshipType, Collection<EntityReference<S>> sourceRefs
  ) {
    persistedEntity.addRelationshipsFrom(dbSetup_.getRelationshipKey(relationshipType), sourceRefs);
  }

  public <S extends Entity<S>, T extends Entity<T>> void addRelationshipsTo(
    PersistedEntity persistedEntity, EntityRelationshipType<S, T> relationshipType, Collection<EntityReference<T>> targetRefs
  ) {
    persistedEntity.addRelationshipsTo(dbSetup_.getRelationshipKey(relationshipType), targetRefs);
  }

  public <JAVA_TYPE> void addRelationship(PersistedEntity persistedEntity, EntityProperty<JAVA_TYPE> propertyType, JAVA_TYPE value) {
    persistedEntity.addProperty(dbSetup_.getPropertyKey(persistedEntity.getBoundProperty(propertyType)), propertyType, value);
  }

  protected void removeEntity(String tableName, String idColName, Integer id) throws SQLException {
    try (PreparedStatement stmt = db_.prepareStatement("DELETE FROM %s WHERE %s=?".formatted(tableName, idColName))) {
      stmt.setInt(1, id);
      stmt.execute();
    }
  }

}
