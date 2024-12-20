package com.bensler.taggy.ui;

import javax.swing.JSplitPane;

import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.Prefs;

public class SplitpanePrefPersister {

  private final PrefKey prefKey_;
  private final JSplitPane splitPane_;

  public SplitpanePrefPersister(PrefKey prefKey, JSplitPane splitPane) {
     prefKey_ = prefKey;
     splitPane_ = splitPane;
  }

  public void apply(Prefs prefs) {
    prefs.get(prefKey_).flatMap(Prefs::tryParseInt).ifPresent(splitPane_::setDividerLocation);
  }

  public void store(Prefs prefs) {
    prefs.put(prefKey_, String.valueOf(splitPane_.getDividerLocation()));
  }

}
