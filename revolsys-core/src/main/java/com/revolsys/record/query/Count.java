package com.revolsys.record.query;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.revolsys.record.schema.RecordStore;

public class Count extends AbstractUnaryQueryValue {

  public Count(final CharSequence name) {
    this(new Column(name));
  }

  public Count(final QueryValue queryValue) {
    super(queryValue);
  }

  @Override
  public void appendDefaultSql(final Query query, final RecordStore recordStore,
    final StringBuilder buffer) {
    buffer.append("count(");
    super.appendDefaultSql(query, recordStore, buffer);
    buffer.append(")");
  }

  @Override
  public Count clone() {
    return (Count)super.clone();
  }

  @Override
  public int getFieldIndex() {
    final QueryValue value = getValue();
    return value.getFieldIndex();
  }

  @Override
  public Object getValueFromResultSet(final ResultSet resultSet, final ColumnIndexes indexes,
    final boolean internStrings) throws SQLException {
    final long value = resultSet.getLong(indexes.incrementAndGet());
    if (resultSet.wasNull()) {
      return null;
    } else {
      return Long.valueOf(value);
    }
  }

  @Override
  public String toString() {
    final StringBuilder buffer = new StringBuilder();
    buffer.append("COUNT(");
    buffer.append(super.toString());
    buffer.append(")");
    return buffer.toString();
  }
}
