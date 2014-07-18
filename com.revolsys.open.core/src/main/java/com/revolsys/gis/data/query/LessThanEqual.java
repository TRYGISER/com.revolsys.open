package com.revolsys.gis.data.query;

import java.util.Map;

import com.revolsys.gis.data.model.Attribute;
import com.revolsys.util.CompareUtil;

public class LessThanEqual extends BinaryCondition {

  public static LessThanEqual lessThanOrEqual(final Attribute attribute,
    final Object value) {
    final String name = attribute.getName();
    final Value valueCondition = new Value(attribute, value);
    return lessThanOrEqual(name, valueCondition);
  }

  public static LessThanEqual lessThanOrEqual(final String name,
    final Object value) {
    final Value valueCondition = new Value(value);
    return lessThanOrEqual(name, valueCondition);
  }

  public static LessThanEqual lessThanOrEqual(final String name,
    final QueryValue right) {
    final Column column = new Column(name);
    return new LessThanEqual(column, right);
  }

  public LessThanEqual(final QueryValue left, final QueryValue right) {
    super(left, "<=", right);
  }

  public LessThanEqual(final String name, final Object value) {
    super(name, "<=", value);
  }

  @Override
  public boolean accept(final Map<String, Object> record) {
    final QueryValue left = getLeft();
    final Object value1 = left.getValue(record);

    final QueryValue right = getRight();
    final Object value2 = right.getValue(record);

    return CompareUtil.compare(value1, value2) <= 0;
  }

  @Override
  public LessThanEqual clone() {
    return (LessThanEqual)super.clone();
  }

}