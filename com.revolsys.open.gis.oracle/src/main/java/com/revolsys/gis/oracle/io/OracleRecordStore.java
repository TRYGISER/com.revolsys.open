package com.revolsys.gis.oracle.io;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.revolsys.collection.AbstractIterator;
import com.revolsys.collection.IntHashMap;
import com.revolsys.collection.ResultPager;
import com.revolsys.data.query.Column;
import com.revolsys.data.query.Query;
import com.revolsys.data.query.QueryValue;
import com.revolsys.data.query.Value;
import com.revolsys.data.query.functions.EnvelopeIntersects;
import com.revolsys.data.query.functions.GeometryEqual2d;
import com.revolsys.data.query.functions.WithinDistance;
import com.revolsys.data.record.ArrayRecordFactory;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordFactory;
import com.revolsys.data.record.property.ShortNameProperty;
import com.revolsys.data.record.schema.Attribute;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.types.DataTypes;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.WktCsParser;
import com.revolsys.gis.cs.epsg.EpsgCoordinateSystems;
import com.revolsys.gis.oracle.esri.ArcSdeBinaryGeometryRecordStoreExtension;
import com.revolsys.gis.oracle.esri.ArcSdeStGeometryAttribute;
import com.revolsys.gis.oracle.esri.ArcSdeStGeometryRecordStoreExtension;
import com.revolsys.io.PathUtil;
import com.revolsys.jdbc.JdbcUtils;
import com.revolsys.jdbc.attribute.JdbcAttributeAdder;
import com.revolsys.jdbc.io.AbstractJdbcRecordStore;
import com.revolsys.jdbc.io.RecordStoreIteratorFactory;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.GeometryFactory;

public class OracleRecordStore extends AbstractJdbcRecordStore {
  private boolean initialized;

  public static final List<String> ORACLE_INTERNAL_SCHEMAS = Arrays.asList(
    "ANONYMOUS", "APEX_030200", "AURORA$JIS$UTILITY$",
    "AURORA$ORB$UNAUTHENTICATED", "AWR_STAGE", "CSMIG", "CTXSYS", "DBSNMP",
    "DEMO", "DIP", "DMSYS", "DSSYS", "EXFSYS", "LBACSYS", "MDSYS", "OLAPSYS",
    "ORACLE_OCM", "ORDDATA", "ORDPLUGINS", "ORDSYS", "OSE$HTTP$ADMIN", "OUTLN",
    "PERFSTAT", "SDE", "SYS", "SYSTEM", "TRACESVR", "TSMSYS", "WMSYS", "XDB");

  private boolean useSchemaSequencePrefix = true;

  private final IntHashMap<CoordinateSystem> oracleCoordinateSystems = new IntHashMap<>();

  public OracleRecordStore() {
    this(new ArrayRecordFactory());
  }

  public OracleRecordStore(final RecordFactory recordFactory) {
    super(recordFactory);
    initSettings();
  }

  public OracleRecordStore(final RecordFactory recordFactory,
    final DataSource dataSource) {
    this(recordFactory);
    setDataSource(dataSource);
  }

  public OracleRecordStore(final DataSource dataSource) {
    super(dataSource);
    initSettings();
  }

  public OracleRecordStore(final OracleDatabaseFactory databaseFactory,
    final Map<String, ? extends Object> connectionProperties) {
    super(databaseFactory);
    setConnectionProperties(connectionProperties);
    final DataSource dataSource = databaseFactory.createDataSource(connectionProperties);
    setDataSource(dataSource);
    initSettings();

  }

  private void appendEnvelopeIntersects(final Query query,
    final StringBuffer sql, final EnvelopeIntersects envelopeIntersects) {
    final Attribute geometryAttribute = query.getGeometryAttribute();

    if (geometryAttribute instanceof OracleSdoGeometryJdbcAttribute) {
      sql.append("SDO_RELATE(");
      final QueryValue boundingBox1Value = envelopeIntersects.getBoundingBox1Value();
      if (boundingBox1Value == null) {
        sql.append("NULL");
      } else {
        boundingBox1Value.appendSql(query, this, sql);
      }
      sql.append(",");
      final QueryValue boundingBox2Value = envelopeIntersects.getBoundingBox2Value();
      if (boundingBox2Value == null) {
        sql.append("NULL");
      } else {
        boundingBox2Value.appendSql(query, this, sql);
      }
      sql.append(",'mask=ANYINTERACT querytype=WINDOW') = 'TRUE'");
    } else if (geometryAttribute instanceof ArcSdeStGeometryAttribute) {
      sql.append("SDE.ST_ENVINTERSECTS(");
      final QueryValue boundingBox1Value = envelopeIntersects.getBoundingBox1Value();
      if (boundingBox1Value == null) {
        sql.append("NULL");
      } else {
        boundingBox1Value.appendSql(query, this, sql);
      }
      sql.append(",");
      final QueryValue boundingBox2Value = envelopeIntersects.getBoundingBox2Value();
      if (boundingBox2Value == null) {
        sql.append("NULL");
      } else {
        boundingBox2Value.appendSql(query, this, sql);
      }
      sql.append(") = 1");
    } else {
      throw new IllegalArgumentException("Unknown geometry attribute type "
        + geometryAttribute.getClass());
    }
  }

  private void appendGeometryEqual2d(final Query query, final StringBuffer sql,
    final GeometryEqual2d equals) {
    final Attribute geometryAttribute = query.getGeometryAttribute();

    if (geometryAttribute instanceof OracleSdoGeometryJdbcAttribute) {
      sql.append("MDSYS.SDO_EQUALS(");
      final QueryValue geometry1Value = equals.getGeometry1Value();
      if (geometry1Value == null) {
        sql.append("NULL");
      } else {
        geometry1Value.appendSql(query, this, sql);
      }
      sql.append(",");
      final QueryValue geometry2Value = equals.getGeometry2Value();
      if (geometry2Value == null) {
        sql.append("NULL");
      } else {
        geometry2Value.appendSql(query, this, sql);
      }
      sql.append(") = 'TRUE'");
    } else if (geometryAttribute instanceof ArcSdeStGeometryAttribute) {
      sql.append("SDE.ST_EQUALS(");
      final QueryValue geometry1Value = equals.getGeometry1Value();
      if (geometry1Value == null) {
        sql.append("NULL");
      } else {
        geometry1Value.appendSql(query, this, sql);
      }
      sql.append(",");
      final QueryValue geometry2Value = equals.getGeometry2Value();
      if (geometry2Value == null) {
        sql.append("NULL");
      } else {
        geometry2Value.appendSql(query, this, sql);
      }
      sql.append(") = 1");
    } else {
      throw new IllegalArgumentException("Unknown geometry attribute type "
        + geometryAttribute.getClass());
    }
  }

  @Override
  public void appendQueryValue(final Query query, final StringBuffer sql,
    final QueryValue queryValue) {
    if (queryValue instanceof GeometryEqual2d) {
      appendGeometryEqual2d(query, sql, (GeometryEqual2d)queryValue);
    } else if (queryValue instanceof EnvelopeIntersects) {
      appendEnvelopeIntersects(query, sql, (EnvelopeIntersects)queryValue);
    } else if (queryValue instanceof WithinDistance) {
      appendWithinDistance(query, sql, (WithinDistance)queryValue);
    } else {
      super.appendQueryValue(query, sql, queryValue);
    }
  }

  private void appendWithinDistance(final Query query, final StringBuffer sql,
    final WithinDistance withinDistance) {
    final Attribute geometryAttribute = query.getGeometryAttribute();
    if (geometryAttribute instanceof OracleSdoGeometryJdbcAttribute) {
      sql.append("MDSYS.SDO_WITHIN_DISTANCE(");
      final QueryValue geometry1Value = withinDistance.getGeometry1Value();
      if (geometry1Value == null) {
        sql.append("NULL");
      } else {
        geometry1Value.appendSql(query, this, sql);
      }
      sql.append(", ");
      final QueryValue geometry2Value = withinDistance.getGeometry2Value();
      if (geometry2Value == null) {
        sql.append("NULL");
      } else {
        geometry2Value.appendSql(query, this, sql);
      }
      sql.append(",'distance = ");
      final QueryValue distanceValue = withinDistance.getDistanceValue();
      if (distanceValue == null) {
        sql.append("0");
      } else {
        distanceValue.appendSql(query, this, sql);
      }
      sql.append("') = 'TRUE'");
    } else if (geometryAttribute instanceof ArcSdeStGeometryAttribute) {
      final Column column = (Column)withinDistance.getGeometry1Value();
      final GeometryFactory geometryFactory = column.getAttribute()
        .getRecordDefinition()
        .getGeometryFactory();
      final Value geometry2Value = (Value)withinDistance.getGeometry2Value();
      final Value distanceValue = (Value)withinDistance.getDistanceValue();
      final Number distance = (Number)distanceValue.getValue();
      final Object geometryObject = geometry2Value.getValue();
      BoundingBox boundingBox;
      if (geometryObject instanceof BoundingBox) {
        boundingBox = (BoundingBox)geometryObject;
      } else if (geometryObject instanceof Geometry) {
        final Geometry geometry = (Geometry)geometryObject;
        boundingBox = geometry.getBoundingBox();
      } else {
        boundingBox = geometryFactory.boundingBox();
      }
      boundingBox = boundingBox.expand(distance.doubleValue());
      boundingBox = boundingBox.convert(geometryFactory);
      sql.append("(SDE.ST_ENVINTERSECTS(");
      column.appendSql(query, this, sql);
      sql.append(",");
      sql.append(boundingBox.getMinX());
      sql.append(",");
      sql.append(boundingBox.getMinY());
      sql.append(",");
      sql.append(boundingBox.getMaxX());
      sql.append(",");
      sql.append(boundingBox.getMaxY());
      sql.append(") = 1 AND SDE.ST_DISTANCE(");
      column.appendSql(query, this, sql);
      sql.append(", ");
      geometry2Value.appendSql(query, this, sql);
      sql.append(") <= ");
      distanceValue.appendSql(query, this, sql);
      sql.append(")");
    } else {
      throw new IllegalArgumentException("Unknown geometry attribute type "
        + geometryAttribute.getClass());
    }
  }

  public AbstractIterator<Record> createOracleIterator(
    final OracleRecordStore recordStore, final Query query,
    final Map<String, Object> properties) {
    return new OracleJdbcQueryIterator(recordStore, query, properties);
  }

  public synchronized CoordinateSystem getCoordinateSystem(final int oracleSrid) {
    CoordinateSystem coordinateSystem = oracleCoordinateSystems.get(oracleSrid);
    if (coordinateSystem == null) {
      try {
        final Map<String, Object> result = JdbcUtils.selectMap(getDataSource(),
          "SELECT * FROM MDSYS.SDO_CS_SRS WHERE SRID = ?", oracleSrid);
        if (result == null) {
          coordinateSystem = EpsgCoordinateSystems.getCoordinateSystem(oracleSrid);
        } else {
          final String wkt = (String)result.get("WKTEXT");
          coordinateSystem = WktCsParser.read(wkt);
          coordinateSystem = EpsgCoordinateSystems.getCoordinateSystem(coordinateSystem);
        }
      } catch (final Throwable e) {
        LoggerFactory.getLogger(getClass()).error(
          "Unable to load coordinate system: " + oracleSrid, e);
        return null;
      }
      oracleCoordinateSystems.put(oracleSrid, coordinateSystem);
    }
    return coordinateSystem;
  }

  @Override
  public String getGeneratePrimaryKeySql(final RecordDefinition recordDefinition) {
    final String sequenceName = getSequenceName(recordDefinition);
    return sequenceName + ".NEXTVAL";
  }

  public GeometryFactory getGeometryFactory(final int oracleSrid,
    final int axisCount, final double... scales) {
    final CoordinateSystem coordinateSystem = getCoordinateSystem(oracleSrid);
    if (coordinateSystem == null) {
      return GeometryFactory.fixed(0, axisCount, scales);
    } else {
      final int srid = coordinateSystem.getId();
      if (srid <= 0) {
        return GeometryFactory.fixed(coordinateSystem, axisCount, scales);
      } else {
        return GeometryFactory.fixed(srid, axisCount, scales);
      }
    }
  }

  @Override
  public Object getNextPrimaryKey(final RecordDefinition recordDefinition) {
    final String sequenceName = getSequenceName(recordDefinition);
    return getNextPrimaryKey(sequenceName);
  }

  @Override
  public Object getNextPrimaryKey(final String sequenceName) {
    final String sql = "SELECT " + sequenceName + ".NEXTVAL FROM SYS.DUAL";
    try {
      return JdbcUtils.selectLong(getDataSource(), getConnection(), sql);
    } catch (final SQLException e) {
      throw new IllegalArgumentException(
        "Cannot create ID for " + sequenceName, e);
    }
  }

  public String getSequenceName(final RecordDefinition recordDefinition) {
    if (recordDefinition == null) {
      return null;
    } else {
      final String typePath = recordDefinition.getPath();
      final String schema = getDatabaseSchemaName(PathUtil.getPath(typePath));
      final String shortName = ShortNameProperty.getShortName(recordDefinition);
      final String sequenceName;
      if (StringUtils.hasText(shortName)) {
        if (useSchemaSequencePrefix) {
          sequenceName = schema + "." + shortName.toLowerCase() + "_SEQ";
        } else {
          sequenceName = shortName.toLowerCase() + "_SEQ";
        }
      } else {
        final String tableName = getDatabaseTableName(typePath);
        if (useSchemaSequencePrefix) {
          sequenceName = schema + "." + tableName + "_SEQ";
        } else {
          sequenceName = tableName + "_SEQ";
        }
      }
      return sequenceName;
    }
  }

  @Override
  @PostConstruct
  public void initialize() {
    super.initialize();
    if (!initialized) {
      initialized = true;
      final JdbcAttributeAdder attributeAdder = new JdbcAttributeAdder();
      addAttributeAdder("NUMBER", attributeAdder);

      addAttributeAdder("CHAR", attributeAdder);
      addAttributeAdder("NCHAR", attributeAdder);
      addAttributeAdder("VARCHAR", attributeAdder);
      addAttributeAdder("VARCHAR2", attributeAdder);
      addAttributeAdder("NVARCHAR2", new JdbcAttributeAdder(DataTypes.STRING));
      addAttributeAdder("LONG", attributeAdder);
      addAttributeAdder("CLOB", attributeAdder);
      addAttributeAdder("NCLOB", attributeAdder);

      addAttributeAdder("DATE", attributeAdder);
      addAttributeAdder("TIMESTAMP", attributeAdder);

      final OracleSdoGeometryAttributeAdder sdoGeometryAttributeAdder = new OracleSdoGeometryAttributeAdder(
        this, getDataSource());
      addAttributeAdder("SDO_GEOMETRY", sdoGeometryAttributeAdder);
      addAttributeAdder("MDSYS.SDO_GEOMETRY", sdoGeometryAttributeAdder);

      final OracleBlobAttributeAdder blobAdder = new OracleBlobAttributeAdder();
      addAttributeAdder("BLOB", blobAdder);

      final OracleClobAttributeAdder clobAdder = new OracleClobAttributeAdder();
      addAttributeAdder("CLOB", clobAdder);
      setPrimaryKeySql("SELECT distinct cols.table_name, cols.column_name FROM all_constraints cons, all_cons_columns cols WHERE cons.constraint_type = 'P' AND cons.constraint_name = cols.constraint_name AND cons.owner = cols.owner AND cons.owner =?");

      setSchemaPermissionsSql("select distinct p.owner \"SCHEMA_NAME\" "
        + "from ALL_TAB_PRIVS_RECD P "
        + "where p.privilege in ('SELECT', 'INSERT', 'UPDATE', 'DELETE') union all select USER \"SCHEMA_NAME\" from DUAL");
      setTablePermissionsSql("select distinct p.owner \"SCHEMA_NAME\", p.table_name, p.privilege, comments \"REMARKS\" "
        + "from ALL_TAB_PRIVS_RECD P "
        + "join all_tab_comments C on (p.owner = c.owner and p.table_name = c.table_name) "
        + "where p.owner = ? and c.table_type in ('TABLE', 'VIEW') and p.privilege in ('SELECT', 'INSERT', 'UPDATE', 'DELETE') ");

      addDataStoreExtension(new ArcSdeStGeometryRecordStoreExtension());
      addDataStoreExtension(new ArcSdeBinaryGeometryRecordStoreExtension());

    }
  }

  private void initSettings() {
    setExcludeTablePatterns(".*\\$");
    setSqlPrefix("BEGIN ");
    setSqlSuffix(";END;");
    setIteratorFactory(new RecordStoreIteratorFactory(this,
      "createOracleIterator"));
  }

  @Override
  public boolean isSchemaExcluded(final String schemaName) {
    return ORACLE_INTERNAL_SCHEMAS.contains(schemaName);
  }

  public boolean isUseSchemaSequencePrefix() {
    return useSchemaSequencePrefix;
  }

  @Override
  public ResultPager<Record> page(final Query query) {
    return new OracleJdbcQueryResultPager(this, getProperties(), query);
  }

  public void setUseSchemaSequencePrefix(final boolean useSchemaSequencePrefix) {
    this.useSchemaSequencePrefix = useSchemaSequencePrefix;
  }

}
