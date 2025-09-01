package com.bensler.taggy;

import java.util.function.Consumer;

public interface EntityChangeListener<E> {

  void entityCreated(E entity);

  void entityChanged(E entity);

  void entityRemoved(E entity);

  static class EntityRemovedAdapter<E> implements EntityChangeListener<E> {

    private final Consumer<E> entityRemovedConsumer_;

    public EntityRemovedAdapter(Consumer<E> entityRemovedConsumer) {
      entityRemovedConsumer_ = entityRemovedConsumer;
    }

    @Override
    public void entityCreated(E entity) { /* noop */ }

    @Override
    public void entityChanged(E entity) { /* noop */ }

    @Override
    public void entityRemoved(E entity) {
      entityRemovedConsumer_.accept(entity);
    }

  }

  static class EntityChangedAdapter<E> implements EntityChangeListener<E> {

    private final Consumer<E> entityChangedConsumer_;

    public EntityChangedAdapter(Consumer<E> entityChangedConsumer) {
      entityChangedConsumer_ = entityChangedConsumer;
    }

    @Override
    public void entityCreated(E entity) { /* noop */ }

    @Override
    public void entityChanged(E entity) {
      entityChangedConsumer_.accept(entity);
    }

    @Override
    public void entityRemoved(E entity) { /* noop */ }

  }

}
