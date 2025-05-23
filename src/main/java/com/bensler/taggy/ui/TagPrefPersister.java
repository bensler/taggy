package com.bensler.taggy.ui;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersister;
import com.bensler.decaf.util.prefs.Prefs;
import com.bensler.taggy.persist.Tag;

public class TagPrefPersister implements PrefPersister {

    private final PrefKey prefKey_;
    private final TagController tagCtrl_;
    private final Supplier<Tag> persist_;
    private final Consumer<Tag> apply_;

    public TagPrefPersister(
      PrefKey prefKey, TagController tagCtrl,
      Supplier<Tag> persist, Consumer<Tag> apply
    ) {
       prefKey_ = prefKey;
       tagCtrl_ = tagCtrl;
       persist_ = persist;
       apply_ = apply;
    }

    @Override
    public void apply(Prefs prefs) {
      prefs.get(prefKey_)
      .flatMap(Prefs::tryParseInt)
      .map(Tag::new)
      .map(tagCtrl_::resolveTag)
      .ifPresent(apply_);
    }

    @Override
    public void store(Prefs prefs) {
      Optional.ofNullable(persist_.get())
      .map(Tag::getId).map(String::valueOf)
      .ifPresent(idStr -> prefs.put(prefKey_, idStr));
    }

  }