package com.bensler.taggy.ui;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.decaf.util.prefs.DelegatingPrefPersister;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.Prefs;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.Tag;

public class TagPrefPersister {

  public static DelegatingPrefPersister create(
    PrefKey prefKey, Supplier<Tag> persist, Consumer<Tag> apply
  ) {
    return new DelegatingPrefPersister(prefKey,
      () -> Optional.ofNullable(persist.get()).map(Tag::getId).map(String::valueOf),
      value -> Prefs.tryParseInt(value).map(id -> DbAccess.INSTANCE.get().resolve(new EntityReference<>(Tag.class, id))).ifPresent(apply)
    );
  }

  private TagPrefPersister() {}

}