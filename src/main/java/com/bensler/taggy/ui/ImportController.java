package com.bensler.taggy.ui;

import javax.swing.ImageIcon;

import com.bensler.decaf.swing.action.Appearance;
import com.bensler.decaf.swing.action.EntityAction;

public class ImportController {

  public static final Appearance IMPORT_ACTION_APPEARANCE =  new Appearance(
    null, new ImageIcon(ImportController.class.getResource("vacuum.png")), null, "Scan for new Images to import."
  );

  private final EntityAction<Object> actionImport_;

  ImportController() {
    actionImport_ = new EntityAction<>(
      IMPORT_ACTION_APPEARANCE, null,
      (source, entities) -> doImport()
    );
  }

  public EntityAction<?> getImportAction() {
    return actionImport_;
  }

  private void doImport() {
    System.out.println("Kuckuck!");
  }

}
