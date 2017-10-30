package com.revolsys.record.io.format.gml.type;

import com.revolsys.datatype.DataType;
import com.revolsys.datatype.DataTypes;
import com.revolsys.record.io.format.xml.XmlWriter;

public class SimpleFieldType extends AbstractGmlFieldType {

  public static final SimpleFieldType OBJECT = new SimpleFieldType(DataTypes.OBJECT);

  public SimpleFieldType(final DataType dataType) {
    super(dataType, "xs:" + dataType.getName());
  }

  @Override
  protected void writeValueText(final XmlWriter out, final Object value) {
    out.text(value);
  }
}
