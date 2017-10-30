package com.revolsys.record.query;

import java.util.function.Predicate;

import com.revolsys.record.Record;
import com.revolsys.util.Emptyable;
import com.revolsys.util.Property;

public abstract class Condition implements QueryValue, Predicate<Record>, Emptyable {

  public static final AcceptAllCondition ALL = new AcceptAllCondition();

  public Condition and(final Condition condition) {
    if (Property.isEmpty(condition)) {
      return this;
    } else if (Property.isEmpty(this)) {
      return condition;
    } else {
      return new And(this, condition);
    }
  }

  @Override
  public Condition clone() {
    try {
      return (Condition)super.clone();
    } catch (final CloneNotSupportedException e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(final Record record) {
    final Boolean value = test(record);
    return (V)value;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  public Condition not() {
    if (Property.isEmpty(this)) {
      return this;
    } else {
      return new Not(this);
    }
  }

  public Condition or(final Condition condition) {
    if (Property.isEmpty(condition)) {
      return this;
    } else if (Property.isEmpty(this)) {
      return condition;
    } else {
      return new Or(this, condition);
    }
  }

  @Override
  public boolean test(final Record record) {
    throw new UnsupportedOperationException("Cannot filter using " + toString());
  }

  @Override
  public String toFormattedString() {
    return toString();
  }
}
