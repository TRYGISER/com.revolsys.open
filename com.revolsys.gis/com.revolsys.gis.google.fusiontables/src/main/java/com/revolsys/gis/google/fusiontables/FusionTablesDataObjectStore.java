package com.revolsys.gis.google.fusiontables;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.GoogleTransport;
import com.google.api.client.googleapis.auth.clientlogin.ClientLogin;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UrlEncodedContent;
import com.revolsys.collection.ThreadSharedAttributes;
import com.revolsys.csv.CsvMapIoFactory;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.cs.projection.GeometryProjectionUtil;
import com.revolsys.gis.data.io.AbstractDataObjectStore;
import com.revolsys.gis.data.io.DataObjectReader;
import com.revolsys.gis.data.io.DataObjectStoreSchema;
import com.revolsys.gis.data.io.Reader;
import com.revolsys.gis.data.model.ArrayDataObjectFactory;
import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.AttributeProperties;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectMetaDataImpl;
import com.revolsys.gis.data.model.DataObjectState;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.gis.data.model.types.DataTypes;
import com.revolsys.gis.google.fusiontables.attribute.DateTimeAttribute;
import com.revolsys.gis.google.fusiontables.attribute.GeometryAttribute;
import com.revolsys.gis.google.fusiontables.attribute.NumberAttribute;
import com.revolsys.gis.google.fusiontables.attribute.StringAttribute;
import com.revolsys.io.FileUtil;
import com.revolsys.io.MapReader;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class FusionTablesDataObjectStore extends AbstractDataObjectStore {
  private static final Map<String, DataType> DATA_TYPES;

  static {
    final Map<String, DataType> dataTypes = new HashMap<String, DataType>();
    dataTypes.put("string", DataTypes.STRING);
    dataTypes.put("number", DataTypes.DECIMAL);
    dataTypes.put("location", DataTypes.GEOMETRY);
    dataTypes.put("datetime", DataTypes.DATE_TIME);

    DATA_TYPES = Collections.unmodifiableMap(dataTypes);
  }

  private static final String SERVICE_URL = "http://www.google.com/fusiontables/api/query";

  public static void addColumnNames(final StringBuffer sql,
    final DataObjectMetaData metaData) {
    final List<String> attributeNames = metaData.getAttributeNames();
    for (int i = 0; i < attributeNames.size(); i++) {
      if (i > 0) {
        sql.append(", ");
      }
      final String attributeName = attributeNames.get(i);
      if (attributeName.equals("rowid")) {
        sql.append(attributeName);
      } else {
        sql.append("'");
        sql.append(attributeName);
        sql.append("'");
      }
      // final Attribute attribute = metaData.getAttribute(attributeName);
      // if (attribute instanceof JdbcAttribute) {
      // final JdbcAttribute jdbcAttribute = (JdbcAttribute)attribute;
      // jdbcAttribute.addColumnName(sql, tablePrefix);
      // }
    }
  }

  private String username = "api.user@revolsys.com";

  private String password = "Tdnmatm1";

  private final Map<QName, String> typeLoadSql = new HashMap<QName, String>();

  private final Map<String, QName> tableIdTypeNameMap = new HashMap<String, QName>();

  private final Map<QName, String> typeNameTableIdMap = new HashMap<QName, String>();

  private String label;

  public FusionTablesDataObjectStore() {
    super(new ArrayDataObjectFactory());
  }

  private DataObjectReader createDataObjectReader(
    final DataObjectMetaData metaData, final String sql) {
    try {
      final HttpResponse response = executeQuery(sql);
      final InputStream in = response.getContent();
      final DataObjectReader reader = new FusionTablesDataObjectReader(
        metaData, in);
      return reader;
    } catch (final IOException e) {
      throw new RuntimeException("Unable to invoke query " + sql, e);
    }
  }

  private MapReader createMapReader(final String sql) {
    try {
      final HttpResponse response = executeQuery(sql);
      final InputStream in = response.getContent();
      final MapReader reader = new CsvMapIoFactory().createMapReader(in);
      return reader;
    } catch (final IOException e) {
      throw new RuntimeException("Unable to invoke query " + sql, e);
    }
  }

  public void createTable(final DataObjectMetaData metaData) {
    final StringBuffer sql = new StringBuffer("CREATE TABLE ");
    sql.append(metaData.getName().getLocalPart());
    sql.append(" (");
    for (int i = 0; i < metaData.getAttributeCount(); i++) {
      if (i > 0) {
        sql.append(", ");
      }
      sql.append(metaData.getAttributeName(i));
      sql.append(": ");
      final DataType dataType = metaData.getAttributeType(i);
      final Class<?> attributeClass = dataType.getJavaClass();
      if (Geometry.class.isAssignableFrom(attributeClass)) {
        sql.append("LOCATION");
      } else if (Number.class.isAssignableFrom(attributeClass)) {
        sql.append("NUMBER");
      } else if (Date.class.isAssignableFrom(attributeClass)) {
        sql.append("DATETIME");
      } else {
        sql.append("STRING");
      }
    }
    sql.append(" )");
  }

  public FusionTablesDataObjectWriter createWriter() {
    final FusionTablesDataObjectWriter writer = new FusionTablesDataObjectWriter(
      this);
    return writer;
  }

  @Override
  public void delete(final DataObject object) {
    if (object.getState() == DataObjectState.Persisted) {
      object.setState(DataObjectState.Deleted);
      getWriter().write(object);
    }
  }

  @Override
  public void deleteAll(final Collection<DataObject> objects) {
    for (final DataObject object : objects) {
      delete(object);
    }
  }

  private HttpResponse executeQuery(final String sql) {
    try {
      final HttpTransport transport = GoogleTransport.create();
      final GoogleHeaders headers = (GoogleHeaders)transport.defaultHeaders;
      headers.setApplicationName("fusiontables");
      headers.gdataVersion = "2";

      final ClientLogin authenticator = new ClientLogin();
      authenticator.authTokenType = "fusiontables";
      authenticator.username = username;
      authenticator.password = password;
      authenticator.authenticate().setAuthorizationHeader(transport);

      final HttpRequest request = transport.buildGetRequest();
      final GenericUrl url = new GenericUrl(SERVICE_URL);
      url.set("sql", sql);
      request.url = url;
      final HttpResponse response = request.execute();
      return response;
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeException("Unable to encode query " + sql, e);
    } catch (final IOException e) {
      throw new RuntimeException("Unable to invoke query " + sql, e);
    }
  }

  protected HttpResponse executePostQuery(final String sql) {
    try {
      final HttpTransport transport = GoogleTransport.create();
      final GoogleHeaders headers = (GoogleHeaders)transport.defaultHeaders;
      headers.setApplicationName("fusiontables");
      headers.gdataVersion = "2";

      final ClientLogin authenticator = new ClientLogin();
      authenticator.authTokenType = "fusiontables";
      authenticator.username = username;
      authenticator.password = password;
      authenticator.authenticate().setAuthorizationHeader(transport);
      final HttpRequest request = transport.buildPostRequest();
      final GenericUrl url = new GenericUrl(SERVICE_URL);
      request.url = url;

      Map<String, Object> data = new LinkedHashMap<String, Object>();
      data.put("sql", sql);

      UrlEncodedContent content = new UrlEncodedContent();
      content.data = data;
      request.content = content;
      final HttpResponse response = request.execute();
      return response;
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeException("Unable to encode query " + sql, e);
      } catch (final IOException e) {
      throw new RuntimeException("Unable to invoke query " + sql, e);
    }
  }

  private List<Attribute> getAttributes(final String tableId) {
    final List<Attribute> attributes = new ArrayList<Attribute>();
    final MapReader reader = createMapReader("DESCRIBE " + tableId);
    attributes.add(new NumberAttribute("rowid"));
    for (final Map<String, Object> map : reader) {
      final String name = (String)map.get("name");
      final String type = (String)map.get("type");
      DataType dataType = DATA_TYPES.get(type);
      if (dataType == null) {
        dataType = DataTypes.STRING;
      }
      final Attribute attribute;
      if (dataType.equals(DataTypes.DECIMAL)) {
        attribute = new NumberAttribute(name);
      } else if (dataType.equals(DataTypes.DATE_TIME)) {
        attribute = new DateTimeAttribute(name);
      } else if (dataType.equals(DataTypes.GEOMETRY)) {
        attribute = new GeometryAttribute(name);
        attribute.setProperty(AttributeProperties.GEOMETRY_FACTORY,
          GeometryFactory.getFactory(4326));
      } else {
        attribute = new StringAttribute(name);
      }
      attributes.add(attribute);
    }
    return attributes;
  }

  protected String getLoadSql(final QName typeName) {
    String sql = typeLoadSql.get(typeName);
    if (sql == null) {
      final DataObjectMetaData metaData = getMetaData(typeName);
      if (metaData == null) {
        return null;
      } else {
        if (metaData.getIdAttributeIndex() == -1) {
          throw new IllegalArgumentException(typeName
            + " does not have a primary key");
        }

        final String idAttributeName = metaData.getIdAttributeName();

        final StringBuffer sqlBuffer = new StringBuffer();
        final String tableId = getTableId(typeName);
        sqlBuffer.append("SELECT ");
        addColumnNames(sqlBuffer, metaData);
        sqlBuffer.append(" FROM " + tableId);
        // JdbcQuery.addColumnsAndTableName(sqlBuffer, metaData, "T");
        sqlBuffer.append(" WHERE ");

        sqlBuffer.append(idAttributeName);
        sqlBuffer.append(" = ");

        sql = sqlBuffer.toString();
        typeLoadSql.put(typeName, sql);
      }
    }
    return sql;
  }

  public String getPassword() {
    return password;
  }

  @SuppressWarnings("unchecked")
  private <T> T getSharedAttribute(final String name) {
    final Map<String, Object> sharedAttributes = getSharedAttributes();
    final T value = (T)sharedAttributes.get(name);
    return value;
  }

  private synchronized Map<String, Object> getSharedAttributes() {
    Map<String, Object> sharedAttributes = ThreadSharedAttributes.getAttribute(this);
    if (sharedAttributes == null) {
      sharedAttributes = new HashMap<String, Object>();
      ThreadSharedAttributes.setAttribute(this, sharedAttributes);
    }
    return sharedAttributes;
  }

  public String getTableId(final QName typeName) {
    return typeNameTableIdMap.get(typeName);
  }

  public String getUsername() {
    return username;
  }

  public synchronized FusionTablesDataObjectWriter getWriter() {
    FusionTablesDataObjectWriter writer = getSharedAttribute("writer");
    if (writer == null) {
      writer = createWriter();
      setSharedAttribute("writer", writer);
    }
    return writer;
  }

  @PostConstruct
  public void init() throws HttpResponseException, IOException {
  }

  @Override
  public void insert(final DataObject object) {
    getWriter().write(object);
  }

  @Override
  public void insertAll(final Collection<DataObject> objects) {
    for (final DataObject object : objects) {
      insert(object);
    }
  }

  @Override
  public DataObject load(final QName typeName, final Object id) {
    final String sql = getLoadSql(typeName);
    if (sql == null) {
      return null;
    } else {
      final DataObjectMetaData metaData = getMetaData(typeName);
      final DataObjectReader reader = createDataObjectReader(metaData, sql
        + "'" + id.toString().replaceAll("'", "''") + "'");
      try {
        final Iterator<DataObject> iterator = reader.iterator();
        if (iterator.hasNext()) {
          final DataObject object = iterator.next();
          return object;
        } else {
          return null;
        }
      } finally {
        reader.close();
      }
    }
  }

  @Override
  protected void loadSchemaDataObjectMetaData(
    final DataObjectStoreSchema schema,
    final Map<QName, DataObjectMetaData> metaDataMap) {
    final List<QName> typeNames = new ArrayList<QName>();
    final String namespace = schema.getName();
    final MapReader reader = createMapReader("SHOW TABLES");
    for (final Map<String, Object> map : reader) {
      final String tableId = (String)map.get("table id");
      final String tableName = (String)map.get("name");
      final QName typeName = new QName(namespace, tableName);
      tableIdTypeNameMap.put(tableId, typeName);
      typeNameTableIdMap.put(typeName, tableId);
      typeNames.add(typeName);
    }
    for (final QName typeName : typeNames) {
      final String tableId = getTableId(typeName);
      final List<Attribute> attributes = getAttributes(tableId);
      final DataObjectMetaDataImpl metaData = new DataObjectMetaDataImpl(
        typeName, attributes);
      metaData.setIdAttributeName("rowid");
      metaDataMap.put(typeName, metaData);
    }
  }

  @Override
  protected void loadSchemas(final Map<String, DataObjectStoreSchema> schemaMap) {
    schemaMap.put("", new DataObjectStoreSchema(this, ""));
  }

  public Reader<DataObject> query(final QName typeName) {
    final String where = null;
    return query(typeName, where);
  }

  public Reader<DataObject> query(final QName typeName, final Envelope envelope) {
    final DataObjectMetaData metaData = getMetaData(typeName);
    final String where = "ST_INTERSECTS(" + metaData.getGeometryAttributeName()
      + ", RECTANGLE(LATLNG(" + envelope.getMinY() + "," + envelope.getMinX()
      + "), LATLNG(" + envelope.getMaxY() + "," + envelope.getMaxX() + ")))";
    return query(typeName, where);
  }

  public Reader<DataObject> query(final QName typeName, final Geometry geometry) {
    final Geometry projectedGeometry = GeometryProjectionUtil.perform(geometry,
      4326);
    return query(typeName, projectedGeometry.getEnvelopeInternal());
  }

  private Reader<DataObject> query(final QName typeName, final String where) {
    final DataObjectMetaData metaData = getMetaData(typeName);
    final StringBuffer sql = new StringBuffer();
    sql.append("SELECT ");
    addColumnNames(sql, metaData);
    sql.append(" FROM ");
    final String tableId = getTableId(typeName);
    sql.append(tableId);
    if (where != null) {
      sql.append(" WHERE ");
      sql.append(where);
    }
    final String sqlString = sql.toString();
    return createDataObjectReader(metaData, sqlString);
  }

  public DataObject query(final QName typeName, final String queryString,
    final Object... arguments) {
    // TODO Auto-generated method stub
    return null;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  private void setSharedAttribute(final String name, final Object value) {
    final Map<String, Object> sharedAttributes = getSharedAttributes();
    sharedAttributes.put(name, value);
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  @Override
  public String toString() {
    if (label == null) {
      return super.toString();
    } else {
      return label;
    }
  }

  @Override
  public void update(final DataObject object) {
    final FusionTablesDataObjectWriter writer = getWriter();
    writer.write(object);
  }
}
