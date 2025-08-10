package com.bensler.taggy.ui;

import static com.bensler.decaf.swing.awt.OverlayIcon.Alignment2D.SE;
import static com.bensler.taggy.ui.MainFrame.ICON_EDIT_30;
import static com.bensler.taggy.ui.MainFrame.ICON_PLUS_30;
import static com.bensler.taggy.ui.MainFrame.ICON_TAG_48;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JTextField;

import com.bensler.decaf.swing.awt.OverlayIcon;
import com.bensler.decaf.swing.awt.OverlayIcon.Overlay;
import com.bensler.decaf.swing.dialog.BasicContentPanel;
import com.bensler.decaf.swing.dialog.DialogAppearance;
import com.bensler.decaf.swing.dialog.WindowPrefsPersister;
import com.bensler.decaf.swing.tree.EntityTree;
import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.PrefPersisterImpl;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.App;
import com.bensler.taggy.persist.EntityReference;
import com.bensler.taggy.persist.Tag;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public abstract class TagDialog<IN> extends BasicContentPanel<IN, Tag> {

  protected final EntityTree<Tag> parentTag_;
  protected final JTextField nameTextfield_;
  protected final Hierarchy<Tag> allTags_;

  protected TagDialog(Hierarchy<Tag> allTags, DialogAppearance appearance) {
    super(appearance, new FormLayout(
      "r:p, 3dlu, f:p:g",
      "f:p:g, 3dlu, c:p"
    ));
    allTags_ = allTags;

    final CellConstraints cc = new CellConstraints();

    parentTag_ = new EntityTree<>(TagUi.NAME_VIEW, Tag.class);
    addValidationSource(parentTag_);
    parentTag_.setVisibleRowCount(5, 2.0f);
    add(new JLabel("Parent Tag:"), cc.xy(1, 1, "r, t"));
    add(parentTag_.getScrollPane(), cc.xy(3, 1));
    nameTextfield_ = new JTextField(20);
    addValidationSource(nameTextfield_);
    add(new JLabel("Name:"), cc.xy(1, 3));
    add(nameTextfield_, cc.xy(3, 3));
  }

  @Override
  protected void contextSet(Context ctx) {
    ctx.setPrefs(new PrefPersisterImpl(App.getApp().getPrefs(), new WindowPrefsPersister(new PrefKey(
      App.PREFS_APP_ROOT, TagDialog.class.getSimpleName() + "." + getClass().getSimpleName()), ctx_.getDialog()
    )));
    ctx.setComponentToFocus(nameTextfield_);
  }

  @Override
  protected void validateContent(ValidationContext validationCtx, Object eventSource) {
    final Tag selectedTag = parentTag_.getSingleSelection();
    final Set<Tag> potentialSiblings = allTags_.getChildren(selectedTag);
    final String newName = getNewName();

    if (newName.isEmpty()) {
      validationCtx.addErrorMsg("Name must not be empty.");
    } else {
      final boolean topLevel = (selectedTag == null);
      final boolean nameAlreadyExists = !potentialSiblings.stream()
        .map(Tag::getName)
        .filter(newName::equals)
        .findFirst()
        .isEmpty();

      if (nameAlreadyExists) {
        validationCtx.addErrorMsg(topLevel ? "There is alread a top level Tag \"%s\"".formatted(newName)
        : "Parent Tag \"%s\" already has a child Tag \"%s\"".formatted(selectedTag.getName(), newName));
      }
    }
  }

  protected String getNewName() {
    return nameTextfield_.getText().trim();
  }

  public static class Create extends TagDialog<Optional<Tag>> {

    public final static OverlayIcon ICON = new OverlayIcon(ICON_TAG_48, new Overlay(ICON_PLUS_30, SE));

    public Create(Hierarchy<Tag> allTags) {
      super(allTags, new DialogAppearance(ICON, "Create Tag", "Create a New Tag", true));
    }

    @Override
    public void setData(Optional<Tag> optParent) {
      parentTag_.setData(allTags_);
      optParent.ifPresent(parentTag_::select);
    }

    @Override
    public Tag getData() {
      return new Tag(parentTag_.getSingleSelection(), getNewName(), Map.of());
    }

  }

  public static class Edit extends TagDialog<Tag> {

    public static final OverlayIcon ICON = new OverlayIcon(ICON_TAG_48, new Overlay(ICON_EDIT_30, SE));

    public Edit(Hierarchy<Tag> allTags) {
      super(allTags, new DialogAppearance(ICON, "Edit Tag", "Edit an existing Tag", true));
    }

    @Override
    public void setData(Tag node) {
      allTags_.removeTree(node);
      parentTag_.setData(allTags_);
      parentTag_.select(node.getParent());
      nameTextfield_.setText(node.getName());
    }

    @Override
    public Tag getData() {
      return new Tag(
        inData_.getId(), Tag.getProperty(parentTag_.getSingleSelection(), EntityReference::new), getNewName(),
        inData_.getProperties(), EntityReference.createCollection(inData_.getBlobs(), new HashSet<>())
      );
    }

  }

}
