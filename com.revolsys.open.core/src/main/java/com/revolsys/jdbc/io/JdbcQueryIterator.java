package com.revolsys.jdbc.io;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.PreDestroy;

import com.revolsys.collection.iterator.AbstractIterator;
import com.revolsys.io.FileUtil;
import com.revolsys.jdbc.JdbcConnection;
import com.revolsys.jdbc.JdbcUtils;
import com.revolsys.jdbc.field.JdbcFieldDefinition;
import com.revolsys.record.Record;
import com.revolsys.record.RecordFactory;
import com.revolsys.record.RecordState;
import com.revolsys.record.io.RecordReader;
import com.revolsys.record.query.Query;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordDefinitionImpl;
import com.revolsys.util.Booleans;
import com.revolsys.util.count.LabelCountMap;

public class JdbcQueryIterator extends AbstractIterator<Record> implements RecordReader {
  public static Record getNextRecord(final JdbcRecordStore recordStore,
    final RecordDefinition recordDefinition, final List<FieldDefinition> fields,
    final RecordFactory<Record> recordFactory, final ResultSet resultSet) {
    final Record record = recordFactory.newRecord(recordDefinition);
    if (record != null) {
      record.setState(RecordState.INITIALIZING);
      int columnIndex = 1;
      for (final FieldDefinition field : fields) {
        final JdbcFieldDefinition jdbcField = (JdbcFieldDefinition)field;
        try {
          columnIndex = jdbcField.setFieldValueFromResultSet(resultSet, columnIndex, record);
        } catch (final SQLException e) {
          throw new RuntimeException(
            "Unable to get value " + (columnIndex + 1) + " from result set", e);
        }
      }
      record.setState(RecordState.PERSISTED);
      recordStore.addStatistic("query", record);
    }
    return record;
  }

  public static ResultSet getResultSet(final RecordDefinition recordDefinition,
    final PreparedStatement statement, final Query query) throws SQLException {
    JdbcUtils.setPreparedStatementParameters(statement, query);

    return statement.executeQuery();
  }

  private JdbcConnection connection;

  private final int currentQueryIndex = -1;

  private final int fetchSize = 10;

  private List<FieldDefinition> fields = new ArrayList<>();

  private List<Query> queries;

  private Query query;

  private RecordDefinition recordDefinition;

  private RecordFactory<Record> recordFactory;

  private JdbcRecordStore recordStore;

  private ResultSet resultSet;

  private PreparedStatement statement;

  private LabelCountMap labelCountMap;

  public JdbcQueryIterator(final JdbcRecordStore recordStore, final Query query,
    final Map<String, Object> properties) {
    super();

    final boolean autoCommit = Booleans.getBoolean(properties.get("autoCommit"));
    this.connection = recordStore.getJdbcConnection(autoCommit);
    this.recordFactory = query.getRecordFactory();
    if (this.recordFactory == null) {
      this.recordFactory = recordStore.getRecordFactory();
    }
    this.recordStore = recordStore;
    this.query = query;
    this.labelCountMap = query.getStatistics();
    if (this.labelCountMap == null) {
      this.labelCountMap = (LabelCountMap)properties.get(LabelCountMap.class.getName());
    }
  }

  @Override
  @PreDestroy
  public void closeDo() {
    JdbcUtils.close(this.statement, this.resultSet);
    FileUtil.closeSilent(this.connection);
    this.fields = null;
    this.connection = null;
    this.recordFactory = null;
    this.recordStore = null;
    this.recordDefinition = null;
    this.queries = null;
    this.query = null;
    this.resultSet = null;
    this.statement = null;
    this.labelCountMap = null;
  }

  protected String getErrorMessage() {
    if (this.queries == null) {
      return null;
    } else {
      return this.queries.get(this.currentQueryIndex).getSql();
    }
  }

  @Override
  protected Record getNext() throws NoSuchElementException {
    try {
      if (this.resultSet != null && this.resultSet.next() && !this.query.isCancelled()) {
        final Record record = getNextRecord(this.recordStore, this.recordDefinition, this.fields,
          this.recordFactory, this.resultSet);
        if (this.labelCountMap != null) {
          this.labelCountMap.addCount(record);
        }
        return record;
      } else {
        close();
        throw new NoSuchElementException();
      }
    } catch (final SQLException e) {
      close();
      throw new RuntimeException(getErrorMessage(), e);
    } catch (final RuntimeException e) {
      close();
      throw e;
    } catch (final Error e) {
      close();
      throw e;
    }
  }

  @Override
  public RecordDefinition getRecordDefinition() {
    if (this.recordDefinition == null) {
      hasNext();
    }
    return this.recordDefinition;
  }

  @Override
  public JdbcRecordStore getRecordStore() {
    return this.recordStore;
  }

  protected ResultSet getResultSet() {
    final String tableName = this.query.getTypeName();
    this.recordDefinition = this.query.getRecordDefinition();
    if (this.recordDefinition == null) {
      if (tableName != null) {
        this.recordDefinition = this.recordStore.getRecordDefinition(tableName);
        this.query.setRecordDefinition(this.recordDefinition);
      }
    }
    final String sql = getSql(this.query);
    try {
      this.statement = this.connection.prepareStatement(sql);
      this.statement.setFetchSize(this.fetchSize);

      this.resultSet = getResultSet(this.recordDefinition, this.statement, this.query);
      final ResultSetMetaData resultSetMetaData = this.resultSet.getMetaData();

      if (this.recordDefinition == null) {
        this.recordDefinition = this.recordStore.getRecordDefinition(tableName, resultSetMetaData);
      }
      final List<String> fieldNames = new ArrayList<>(this.query.getFieldNames());
      if (fieldNames.isEmpty()) {
        this.fields.addAll(this.recordDefinition.getFields());
      } else {
        for (String fieldName : fieldNames) {
          if (fieldName.equals("*")) {
            this.fields.addAll(this.recordDefinition.getFields());
          } else {
            if (fieldName.endsWith("\"")) {
              final int index = fieldName.indexOf('"');
              if (index > 0 && fieldName.charAt(index - 1) == ' ') {
                fieldName = fieldName.substring(index + 1, fieldName.length() - 1);
              }
            }
            final FieldDefinition field = this.recordDefinition.getField(fieldName);
            if (field != null) {
              this.fields.add(field);
            }
          }
        }
      }

      final String typePath = this.query.getTypeNameAlias();
      if (typePath != null) {
        final RecordDefinitionImpl newRecordDefinition = ((RecordDefinitionImpl)this.recordDefinition)
          .rename(typePath);
        this.recordDefinition = newRecordDefinition;
      }
    } catch (final SQLException e) {
      JdbcUtils.close(this.statement, this.resultSet);
      throw this.connection.getException("Execute Query", sql, e);
    }
    return this.resultSet;
  }

  protected String getSql(final Query query) {
    return JdbcUtils.getSelectSql(query);
  }

  @Override
  protected void initDo() {
    this.resultSet = getResultSet();
  }

  protected void setQuery(final Query query) {
    this.query = query;
  }

}
