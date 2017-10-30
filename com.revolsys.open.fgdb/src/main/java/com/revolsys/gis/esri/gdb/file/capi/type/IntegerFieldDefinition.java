package com.revolsys.gis.esri.gdb.file.capi.type;

import com.revolsys.datatype.DataTypes;
import com.revolsys.gis.esri.gdb.file.FileGdbRecordStore;
import com.revolsys.gis.esri.gdb.file.capi.swig.Row;
import com.revolsys.record.Record;
import com.revolsys.record.io.format.esri.gdb.xml.model.Field;
import com.revolsys.util.Booleans;
import com.revolsys.util.Property;

public class IntegerFieldDefinition extends AbstractFileGdbFieldDefinition {
  public IntegerFieldDefinition(final Field field) {
    super(field.getName(), DataTypes.INT,
      Booleans.getBoolean(field.getRequired()) || !field.isIsNullable());
  }

  @Override
  public int getMaxStringLength() {
    return 11;
  }

  @Override
  public Object getValue(final Row row) {
    final String name = getName();
    final FileGdbRecordStore recordStore = getRecordStore();
    if (recordStore.isNull(row, name)) {
      return null;
    } else {
      synchronized (getSync()) {
        return row.getInteger(name);
      }
    }
  }

  @Override
  public void setValue(final Record record, final Row row, final Object value) {
    final String name = getName();
    if (value == null) {
      setNull(row);
    } else if (value instanceof Number) {
      final Number number = (Number)value;
      final int intValue = number.intValue();
      synchronized (getSync()) {
        row.setInteger(name, intValue);
      }
    } else {
      final String string = value.toString().trim();
      if (Property.hasValue(string)) {
        final int intValue = Integer.parseInt(string);
        synchronized (getSync()) {
          row.setInteger(name, intValue);
        }
      } else {
        if (isRequired()) {
          throw new IllegalArgumentException(name + " is required and cannot be null");
        } else {
          row.setNull(name);
        }
      }
    }
  }

}
