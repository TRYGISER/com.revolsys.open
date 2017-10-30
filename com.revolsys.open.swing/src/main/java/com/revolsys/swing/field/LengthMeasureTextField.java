package com.revolsys.swing.field;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.measure.Measure;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.revolsys.datatype.DataType;
import com.revolsys.datatype.DataTypes;
import com.revolsys.swing.EventQueue;
import com.revolsys.swing.component.ValueField;
import com.revolsys.swing.layout.GroupLayouts;
import com.revolsys.swing.listener.EventQueueRunnableListener;
import com.revolsys.swing.listener.WeakFocusListener;

public class LengthMeasureTextField extends ValueField implements ItemListener {

  private static final long serialVersionUID = 6402788548005557723L;

  private static final Map<Unit<Length>, String> UNITS = new LinkedHashMap<>();

  static {
    UNITS.put(NonSI.PIXEL, "Pixel");
    UNITS.put(SI.METRE, "Metre");
    UNITS.put(SI.KILOMETRE, "Kilometre");
    UNITS.put(NonSI.FOOT, "Foot");
    UNITS.put(NonSI.MILE, "Mile");
  }

  private Number number;

  private Unit<Length> unit;

  private final ComboBox<Unit<Length>> unitField;

  private final NumberTextField valueField;

  public LengthMeasureTextField(final Measure<Length> value, final Unit<Length> unit) {
    this(null, value, unit);
  }

  public LengthMeasureTextField(final String fieldName, final Measure<Length> value) {
    this(fieldName, value, value.getUnit());
  }

  public LengthMeasureTextField(final String fieldName, final Measure<Length> value,
    final Unit<Length> unit) {
    super(fieldName, value);
    setOpaque(false);
    this.valueField = new NumberTextField(fieldName, DataTypes.DOUBLE, 6, 2);
    if (value == null) {
      this.number = 0;
      if (unit == null) {
        this.unit = NonSI.PIXEL;
      } else {
        this.unit = unit;
      }
    } else {
      this.number = value.getValue();
      this.unit = value.getUnit();
    }
    this.valueField.setFieldValue(this.number);
    final EventQueueRunnableListener updateNumberListener = EventQueue.addAction(this.valueField,
      () -> updateNumber());
    this.valueField.addFocusListener(new WeakFocusListener(updateNumberListener));
    add(this.valueField);
    this.valueField.addActionListener(updateNumberListener);

    final Set<Unit<Length>> units = UNITS.keySet();
    this.unitField = ComboBox.newComboBox("unit", units, UNITS::get);
    this.unitField.setMinimumSize(new Dimension(100, 20));
    this.unitField.addItemListener(this);
    this.unitField.setSelectedItem(this.unit);
    add(this.unitField);
    GroupLayouts.makeColumns(this, 2, true);
  }

  public Measure<Length> getLength() {
    return Measure.valueOf(this.number.doubleValue(), this.unit);
  }

  public Number getNumber() {
    final String text = this.valueField.getText();
    if (text == null) {
      return 0.0;
    } else {
      return Double.parseDouble(text);
    }
  }

  public Unit<Length> getUnit() {
    return this.unit;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void itemStateChanged(final ItemEvent e) {
    if (e.getSource() == this.unitField && e.getStateChange() == ItemEvent.SELECTED) {
      final Object selectedItem = this.unitField.getSelectedItem();
      if (selectedItem instanceof Unit<?>) {
        setUnit((Unit<Length>)selectedItem);
      }
    }
  }

  @Override
  public void save() {
    final Unit<Length> selectedItem = this.unitField.getSelectedItem();
    setUnit(selectedItem);
    updateNumber();
  }

  @Override
  public void setEditable(final boolean enabled) {
    this.valueField.setEditable(enabled);
    this.unitField.setEditable(enabled);
  }

  @Override
  public boolean setFieldValue(final Object value) {
    final boolean updated = super.setFieldValue(value);
    final Measure<Length> fieldValue = getFieldValue();
    setNumber(fieldValue.getValue());
    setUnit(fieldValue.getUnit());
    return updated;
  }

  public void setNumber(final Number value) {
    final Object oldValue = this.number;
    this.number = value.doubleValue();
    this.valueField.setFieldValue(value);
    if (!DataType.equal(oldValue, this.number)) {
      firePropertyChange("number", oldValue, this.number);
      setFieldValue(Measure.valueOf(this.number.doubleValue(), this.unit));
    }
  }

  public void setText(final CharSequence text) {
    if (text == null) {
      this.valueField.setText(null);
    } else {
      this.valueField.setText(text.toString());
    }
  }

  public void setUnit(final Unit<Length> unit) {
    final Object oldValue = this.unit;
    this.unit = unit;
    this.unitField.setSelectedItem(this.unit);
    if (!DataType.equal(oldValue, this.unit)) {
      firePropertyChange("unit", oldValue, this.unit);
      setFieldValue(Measure.valueOf(this.number.doubleValue(), unit));
    }
  }

  @Override
  public void updateFieldValue() {
    final Unit<Length> selectedItem = this.unitField.getSelectedItem();
    setUnit(selectedItem);
    updateNumber();
  }

  public void updateNumber() {
    this.valueField.updateFieldValue();
    final Object value = this.valueField.getFieldValue();
    if (value instanceof Number) {
      final Number number = (Number)value;
      setNumber(number);
    } else {
      setNumber(0);
    }
  }
}
