package com.revolsys.swing.action;

import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

import com.revolsys.i18n.I18nCharSequence;
import com.revolsys.swing.Icons;
import com.revolsys.swing.action.enablecheck.EnableCheck;
import com.revolsys.swing.menu.Button;
import com.revolsys.swing.menu.CheckBoxMenuItem;
import com.revolsys.swing.menu.ToggleButton;
import com.revolsys.util.OS;
import com.revolsys.util.Property;

public abstract class AbstractAction extends javax.swing.AbstractAction {
  private static final long serialVersionUID = 1L;

  private boolean checkBox;

  private EnableCheck enableCheck;

  private final ActionEnabledPropertyChangeListener enabledListener = new ActionEnabledPropertyChangeListener(
    this);

  public AbstractAction() {
  }

  public AbstractAction(final CharSequence name, final String toolTip, final Icon icon) {
    if (name != null) {
      putValue(NAME, name.toString());
    }
    if (toolTip != null) {
      putValue(SHORT_DESCRIPTION, toolTip.toString());
    }
    if (icon != null) {
      putValue(SMALL_ICON, icon);
    }
    if (name instanceof I18nCharSequence) {
      final I18nCharSequence i18nName = (I18nCharSequence)name;
      i18nName.getI18n().addPropertyChangeListener("locale", new PropertyChangeListener() {
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
          putValue(NAME, name.toString());
        }
      });
    }
  }

  public Icon getDisabledIcon() {
    final Icon icon = getIcon();
    return Icons.getDisabledIcon(icon);
  }

  public EnableCheck getEnableCheck() {
    return this.enableCheck;
  }

  public Icon getIcon() {
    return (Icon)getValue(SMALL_ICON);
  }

  public Icon getLargeIcon() {
    return (Icon)getValue(LARGE_ICON_KEY);
  }

  public Integer getMnemonic() {
    return (Integer)getValue(MNEMONIC_KEY);
  }

  public String getName() {
    return (String)getValue(NAME);
  }

  public String getToolTip() {
    return (String)getValue(SHORT_DESCRIPTION);
  }

  public boolean isCheckBox() {
    return this.checkBox;
  }

  @Override
  public boolean isEnabled() {
    if (this.enableCheck != null) {
      return this.enableCheck.isEnabled();
    }
    return super.isEnabled();
  }

  public JButton newButton() {
    final Button button = new Button(this);
    return button;
  }

  public CheckBoxMenuItem newCheckboxMenuItem() {
    final CheckBoxMenuItem menuItem = new CheckBoxMenuItem(this);
    final Icon disabledIcon = getDisabledIcon();
    menuItem.setDisabledIcon(disabledIcon);
    menuItem.setEnabled(isEnabled());
    return menuItem;
  }

  public JMenuItem newMenuItem() {
    final JMenuItem menuItem = new JMenuItem(this);

    final Icon disabledIcon = getDisabledIcon();
    menuItem.setDisabledIcon(disabledIcon);
    final boolean enabled = isEnabled();
    menuItem.setEnabled(enabled);
    return menuItem;
  }

  public JToggleButton newToggleButton() {
    final ToggleButton button = new ToggleButton(this);
    return button;
  }

  public void setAcceleratorControlKey(final int keyCode) {
    int modifiers;
    if (OS.isMac()) {
      modifiers = InputEvent.META_MASK;
    } else {
      modifiers = InputEvent.CTRL_MASK;
    }
    setAcceleratorKey(KeyStroke.getKeyStroke(keyCode, modifiers));
    setMnemonicKey(keyCode);
  }

  public void setAcceleratorKey(final KeyStroke keyStroke) {
    putValue(ACCELERATOR_KEY, keyStroke);
  }

  public void setAcceleratorShiftControlKey(final int keyCode) {
    int modifiers = InputEvent.SHIFT_MASK;
    if (OS.isMac()) {
      modifiers |= InputEvent.META_MASK;
    } else {
      modifiers |= InputEvent.CTRL_MASK;
    }
    setAcceleratorKey(KeyStroke.getKeyStroke(keyCode, modifiers));
  }

  protected void setCheckBox(final boolean checkBox) {
    this.checkBox = checkBox;
  }

  public void setEnableCheck(final EnableCheck enableCheck) {
    Property.removeListener(this.enableCheck, "enabled", this.enabledListener);
    this.enableCheck = enableCheck;
    if (this.enableCheck != null) {
      Property.addListener(this.enableCheck, "enabled", this.enabledListener);
      final boolean enabled = enableCheck.isEnabled();
      firePropertyChange("enabled", !enabled, enabled);
    }
  }

  public void setIcon(final Icon icon) {
    putValue(SMALL_ICON, icon);
  }

  public void setMnemonicKey(final int key) {
    putValue(MNEMONIC_KEY, key);
  }

  public void setName(final String name) {
    putValue(NAME, name);
  }

  public void setToolTip(final CharSequence toolTip) {
    if (toolTip == null) {
      setToolTip(null);
    } else {
      setToolTip(toolTip.toString());
    }
  }

  public void setToolTip(final String toolTip) {
    putValue(SHORT_DESCRIPTION, toolTip);
  }

  @Override
  public String toString() {
    String name = getName();
    if (name == null) {
      name = getToolTip();
      if (name == null) {
        return super.toString();
      }
    }
    return name;
  }
}
