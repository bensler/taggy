package com.bensler.taggy;

import java.util.function.Consumer;

import com.bensler.taggy.persist.Entity;

public interface EntityChangeListener {

  void entityCreated(Entity entity);

  void entityChanged(Entity entity);

  void entityRemoved(Entity entity);

  static class EntityRemovedAdapter implements EntityChangeListener {

    private final Consumer<Entity> entityRemovedConsumer_;

    public EntityRemovedAdapter(Consumer<Entity> entityRemovedConsumer) {
      entityRemovedConsumer_ = entityRemovedConsumer;
    }

    @Override
    public void entityCreated(Entity entity) { /* noop */ }

    @Override
    public void entityChanged(Entity entity) { /* noop */ }

    @Override
    public void entityRemoved(Entity entity) {
      entityRemovedConsumer_.accept(entity);
    }

  }

}
