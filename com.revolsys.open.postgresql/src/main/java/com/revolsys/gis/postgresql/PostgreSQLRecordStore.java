package com.revolsys.gis.postgresql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.postgresql.jdbc2.AbstractJdbc2Connection;

import com.revolsys.collection.ResultPager;
import com.revolsys.collection.iterator.AbstractIterator;
import com.revolsys.datatype.DataTypes;
import com.revolsys.gis.postgresql.type.PostgreSQLBoundingBoxWrapper;
import com.revolsys.gis.postgresql.type.PostgreSQLGeometryFieldAdder;
import com.revolsys.gis.postgresql.type.PostgreSQLGeometryWrapper;
import com.revolsys.gis.postgresql.type.PostgreSQLJdbcBlobFieldDefinition;
import com.revolsys.gis.postgresql.type.PostgreSQLOidFiedDefinition;
import com.revolsys.gis.postgresql.type.PostgreSQLTidWrapper;
import com.revolsys.identifier.Identifier;
import com.revolsys.io.PathName;
import com.revolsys.jdbc.JdbcConnection;
import com.revolsys.jdbc.JdbcUtils;
import com.revolsys.jdbc.field.JdbcFieldAdder;
import com.revolsys.jdbc.field.JdbcFieldDefinition;
import com.revolsys.jdbc.field.JdbcFieldFactory;
import com.revolsys.jdbc.field.JdbcFieldFactoryAdder;
import com.revolsys.jdbc.io.AbstractJdbcRecordStore;
import com.revolsys.jdbc.io.RecordStoreIteratorFactory;
import com.revolsys.record.ArrayRecord;
import com.revolsys.record.Record;
import com.revolsys.record.RecordFactory;
import com.revolsys.record.property.ShortNameProperty;
import com.revolsys.record.query.Query;
import com.revolsys.record.query.QueryValue;
import com.revolsys.record.query.functions.EnvelopeIntersects;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordDefinitionImpl;
import com.revolsys.record.schema.RecordStore;
import com.revolsys.util.Property;

public class PostgreSQLRecordStore extends AbstractJdbcRecordStore {

  public static final List<String> POSTGRESQL_INTERNAL_SCHEMAS = Arrays.asList("information_schema",
    "pg_catalog", "pg_toast_temp_1");

  private static final AbstractIterator<Record> newPostgreSQLIterator(final RecordStore recordStore,
    final Query query, final Map<String, Object> properties) {
    return new PostgreSQLJdbcQueryIterator((PostgreSQLRecordStore)recordStore, query, properties);
  }

  private boolean useSchemaSequencePrefix = true;

  public PostgreSQLRecordStore() {
    this(ArrayRecord.FACTORY);
  }

  public PostgreSQLRecordStore(final DataSource dataSource) {
    super(dataSource);
    initSettings();
  }

  public PostgreSQLRecordStore(final PostgreSQL databaseFactory,
    final Map<String, ? extends Object> connectionProperties) {
    super(databaseFactory, connectionProperties);
    initSettings();
  }

  public PostgreSQLRecordStore(final RecordFactory<? extends Record> recordFactory) {
    super(recordFactory);
    initSettings();
  }

  public PostgreSQLRecordStore(final RecordFactory<? extends Record> recordFactory,
    final DataSource dataSource) {
    this(recordFactory);
    setDataSource(dataSource);
  }

  @Override
  protected JdbcFieldDefinition addField(final RecordDefinitionImpl recordDefinition,
    final String dbColumnName, final String name, final String dataType, final int sqlType,
    final int length, final int scale, final boolean required, final String description) {
    final JdbcFieldDefinition field = super.addField(recordDefinition, dbColumnName, name, dataType,
      sqlType, length, scale, required, description);
    if (!dbColumnName.matches("[a-z_]")) {
      field.setQuoteName(true);
    }
    return field;
  }

  protected void addFieldAdder(final String sqlTypeName, final JdbcFieldFactory fieldFactory) {
    final JdbcFieldFactoryAdder fieldAdder = new JdbcFieldFactoryAdder(fieldFactory);
    addFieldAdder(sqlTypeName, fieldAdder);
  }

  @Override
  public void appendQueryValue(final Query query, final StringBuilder sql,
    final QueryValue queryValue) {
    if (queryValue instanceof EnvelopeIntersects) {
      final EnvelopeIntersects envelopeIntersects = (EnvelopeIntersects)queryValue;
      final QueryValue boundingBox1Value = envelopeIntersects.getBoundingBox1Value();
      if (boundingBox1Value == null) {
        sql.append("NULL");
      } else {
        boundingBox1Value.appendSql(query, this, sql);
      }
      sql.append(" && ");
      final QueryValue boundingBox2Value = envelopeIntersects.getBoundingBox2Value();
      if (boundingBox2Value == null) {
        sql.append("NULL");
      } else {
        boundingBox2Value.appendSql(query, this, sql);
      }
    } else {
      super.appendQueryValue(query, sql, queryValue);
    }
  }

  @Override
  public String getGeneratePrimaryKeySql(final RecordDefinition recordDefinition) {
    final String sequenceName = getSequenceName(recordDefinition);
    return "nextval('" + sequenceName + "')";
  }

  @Override
  public JdbcConnection getJdbcConnection() {
    return getJdbcConnection(false);
  }

  @Override
  public JdbcConnection getJdbcConnection(final boolean autoCommit) {
    final DataSource dataSource = getDataSource();
    final Connection connection = JdbcUtils.getConnection(dataSource);
    try {
      final AbstractJdbc2Connection pgConnection = connection.unwrap(AbstractJdbc2Connection.class);
      pgConnection.addDataType("geometry", PostgreSQLGeometryWrapper.class);
      pgConnection.addDataType("box2d", PostgreSQLBoundingBoxWrapper.class);
      pgConnection.addDataType("box3d", PostgreSQLBoundingBoxWrapper.class);
      pgConnection.addDataType("tid", PostgreSQLTidWrapper.class);
    } catch (final SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return new JdbcConnection(connection, dataSource, autoCommit);
  }

  @Override
  public Identifier getNextPrimaryKey(final RecordDefinition recordDefinition) {
    final String sequenceName = getSequenceName(recordDefinition);
    return getNextPrimaryKey(sequenceName);
  }

  @Override
  public Identifier getNextPrimaryKey(final String sequenceName) {
    final String sql = "SELECT nextval(?)";
    return Identifier.newIdentifier(JdbcUtils.selectLong(this, sql, sequenceName));
  }

  @Override
  public String getRecordStoreType() {
    return "PostgreSQL";
  }

  @Override
  public String getSequenceName(final RecordDefinition recordDefinition) {
    final PathName typePath = recordDefinition.getPathName();
    final PathName schemaPath = typePath.getParent();
    final String dbSchemaName = getDatabaseSchemaName(schemaPath);
    final String shortName = ShortNameProperty.getShortName(recordDefinition);
    final String sequenceName;
    if (Property.hasValue(shortName)) {
      if (this.useSchemaSequencePrefix) {
        sequenceName = dbSchemaName + "." + shortName.toLowerCase() + "_seq";
      } else {
        sequenceName = shortName.toLowerCase() + "_seq";
      }
    } else {
      final String tableName = getDatabaseTableName(typePath);
      final String idFieldName = recordDefinition.getIdFieldName().toLowerCase();
      if (this.useSchemaSequencePrefix) {
        sequenceName = dbSchemaName + "." + tableName + "_" + idFieldName + "_seq";
      } else {
        sequenceName = tableName + "_" + idFieldName + "_seq";
      }
    }
    return sequenceName;

  }

  @Override
  @PostConstruct
  public void initialize() {
    super.initialize();
    final JdbcFieldAdder numberFieldAdder = new JdbcFieldAdder(DataTypes.DECIMAL);
    addFieldAdder("numeric", numberFieldAdder);

    final JdbcFieldAdder stringFieldAdder = new JdbcFieldAdder(DataTypes.STRING);
    addFieldAdder("varchar", stringFieldAdder);
    addFieldAdder("text", stringFieldAdder);
    addFieldAdder("name", stringFieldAdder);
    addFieldAdder("bpchar", stringFieldAdder);

    final JdbcFieldAdder longFieldAdder = new JdbcFieldAdder(DataTypes.LONG);
    addFieldAdder("int8", longFieldAdder);
    addFieldAdder("bigint", longFieldAdder);
    addFieldAdder("bigserial", longFieldAdder);
    addFieldAdder("serial8", longFieldAdder);

    final JdbcFieldAdder intFieldAdder = new JdbcFieldAdder(DataTypes.INT);
    addFieldAdder("int4", intFieldAdder);
    addFieldAdder("integer", intFieldAdder);
    addFieldAdder("serial", intFieldAdder);
    addFieldAdder("serial4", intFieldAdder);

    final JdbcFieldAdder shortFieldAdder = new JdbcFieldAdder(DataTypes.SHORT);
    addFieldAdder("int2", shortFieldAdder);
    addFieldAdder("smallint", shortFieldAdder);

    final JdbcFieldAdder floatFieldAdder = new JdbcFieldAdder(DataTypes.FLOAT);
    addFieldAdder("float4", floatFieldAdder);

    final JdbcFieldAdder doubleFieldAdder = new JdbcFieldAdder(DataTypes.DOUBLE);
    addFieldAdder("float8", doubleFieldAdder);
    addFieldAdder("double precision", doubleFieldAdder);

    addFieldAdder("date", new JdbcFieldAdder(DataTypes.DATE_TIME));

    addFieldAdder("bool", new JdbcFieldAdder(DataTypes.BOOLEAN));

    addFieldAdder("oid", PostgreSQLJdbcBlobFieldDefinition::new);

    final JdbcFieldAdder geometryFieldAdder = new PostgreSQLGeometryFieldAdder(this);
    addFieldAdder("geometry", geometryFieldAdder);
    setPrimaryKeySql("SELECT t.relname \"TABLE_NAME\", c.attname \"COLUMN_NAME\"" //
      + " FROM pg_namespace s" //
      + " join pg_class t on t.relnamespace = s.oid" //
      + " join pg_index i on i.indrelid = t.oid " //
      + " join pg_attribute c on c.attrelid = t.oid" //
      + " WHERE s.nspname = ? AND c.attnum = any(i.indkey) AND i.indisprimary");
    setPrimaryKeyTableCondition(" AND r.relname = ?");
    setSchemaPermissionsSql("select distinct t.table_schema as \"SCHEMA_NAME\" "
      + "from information_schema.role_table_grants t  "
      + "where (t.grantee  in (current_user, 'PUBLIC') or "
      + "t.grantee in (select role_name from information_schema.applicable_roles r where r.grantee = current_user)) and "
      + "privilege_type IN ('SELECT', 'INSERT','UPDATE','DELETE') ");
    setSchemaTablePermissionsSql(
      "select distinct t.table_schema as \"SCHEMA_NAME\", t.table_name, t.privilege_type as \"PRIVILEGE\", d.description as \"REMARKS\" from information_schema.role_table_grants t join pg_namespace n on t.table_schema = n.nspname join pg_class c on (n.oid = c.relnamespace AND t.table_name = c.relname) left join pg_description d on d.objoid = c.oid "
        + "where t.table_schema = ? and "
        + "(t.grantee  in (current_user, 'PUBLIC') or t.grantee in (select role_name from information_schema.applicable_roles r where r.grantee = current_user)) AND "
        + "privilege_type IN ('SELECT', 'INSERT','UPDATE','DELETE') "
        + "order by t.table_schema, t.table_name, t.privilege_type");
  }

  protected void initSettings() {
    setIteratorFactory(
      new RecordStoreIteratorFactory(PostgreSQLRecordStore::newPostgreSQLIterator));
  }

  @Override
  public boolean isSchemaExcluded(final String schemaName) {
    return POSTGRESQL_INTERNAL_SCHEMAS.contains(schemaName);
  }

  public boolean isUseSchemaSequencePrefix() {
    return this.useSchemaSequencePrefix;
  }

  @Override
  protected JdbcFieldDefinition newRowIdFieldDefinition() {
    return new PostgreSQLOidFiedDefinition();
  }

  @Override
  public ResultPager<Record> page(final Query query) {
    return new PostgreSQLJdbcQueryResultPager(this, getProperties(), query);
  }

  public void setUseSchemaSequencePrefix(final boolean useSchemaSequencePrefix) {
    this.useSchemaSequencePrefix = useSchemaSequencePrefix;
  }
}
