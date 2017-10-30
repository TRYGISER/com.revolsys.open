package com.revolsys.oracle.recordstore.esri;

import com.revolsys.datatype.DataType;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.io.PathName;
import com.revolsys.jdbc.field.JdbcFieldAdder;
import com.revolsys.jdbc.io.AbstractJdbcRecordStore;
import com.revolsys.logging.Logs;
import com.revolsys.record.property.FieldProperties;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordDefinitionImpl;
import com.revolsys.record.schema.RecordStoreSchema;

public class ArcSdeStGeometryFieldAdder extends JdbcFieldAdder {
  private final AbstractJdbcRecordStore recordStore;

  public ArcSdeStGeometryFieldAdder(final AbstractJdbcRecordStore recordStore) {
    this.recordStore = recordStore;

  }

  @Override
  public FieldDefinition addField(final AbstractJdbcRecordStore recordStore,
    final RecordDefinitionImpl recordDefinition, final String dbName, final String name,
    final String dataTypeName, final int sqlType, final int length, final int scale,
    final boolean required, final String description) {
    final RecordStoreSchema schema = recordDefinition.getSchema();
    final PathName typePath = recordDefinition.getPathName();
    final String owner = this.recordStore.getDatabaseSchemaName(schema);
    final String tableName = this.recordStore.getDatabaseTableName(typePath);
    final String columnName = name.toUpperCase();
    final int esriSrid = JdbcFieldAdder.getIntegerColumnProperty(schema, typePath, columnName,
      ArcSdeConstants.ESRI_SRID_PROPERTY);
    if (esriSrid == -1) {
      Logs.error(this,
        "Column not registered in SDE.ST_GEOMETRY table " + owner + "." + tableName + "." + name);
    }
    final int axisCount = JdbcFieldAdder.getIntegerColumnProperty(schema, typePath, columnName,
      JdbcFieldAdder.AXIS_COUNT);
    if (axisCount == -1) {
      Logs.error(this,
        "Column not found in SDE.GEOMETRY_COLUMNS table " + owner + "." + tableName + "." + name);
    }
    final DataType dataType = JdbcFieldAdder.getColumnProperty(schema, typePath, columnName,
      JdbcFieldAdder.GEOMETRY_TYPE);
    if (dataType == null) {
      Logs.error(this,
        "Column not found in SDE.GEOMETRY_COLUMNS table " + owner + "." + tableName + "." + name);
    }

    final ArcSdeSpatialReference spatialReference = JdbcFieldAdder.getColumnProperty(schema,
      typePath, columnName, ArcSdeConstants.SPATIAL_REFERENCE);

    final GeometryFactory geometryFactory = JdbcFieldAdder.getColumnProperty(schema, typePath,
      columnName, JdbcFieldAdder.GEOMETRY_FACTORY);

    final FieldDefinition fieldDefinition = new ArcSdeStGeometryFieldDefinition(dbName, name,
      dataType, required, description, null, spatialReference, axisCount);

    recordDefinition.addField(fieldDefinition);
    fieldDefinition.setProperty(FieldProperties.GEOMETRY_FACTORY, geometryFactory);
    return fieldDefinition;
  }

}
