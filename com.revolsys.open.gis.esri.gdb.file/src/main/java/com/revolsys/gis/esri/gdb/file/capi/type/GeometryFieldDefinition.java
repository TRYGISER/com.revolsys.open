package com.revolsys.gis.esri.gdb.file.capi.type;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import com.revolsys.converter.string.BooleanStringConverter;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.property.FieldProperties;
import com.revolsys.data.types.DataType;
import com.revolsys.data.types.DataTypes;
import com.revolsys.gis.esri.gdb.file.CapiFileGdbRecordStore;
import com.revolsys.gis.esri.gdb.file.capi.swig.Row;
import com.revolsys.gis.io.EndianInputStream;
import com.revolsys.gis.io.EndianOutput;
import com.revolsys.gis.io.EndianOutputStream;
import com.revolsys.io.EndianInput;
import com.revolsys.io.FileUtil;
import com.revolsys.io.esri.gdb.xml.model.Field;
import com.revolsys.io.esri.gdb.xml.model.GeometryDef;
import com.revolsys.io.esri.gdb.xml.model.SpatialReference;
import com.revolsys.io.esri.gdb.xml.model.enums.GeometryType;
import com.revolsys.io.shp.ShapefileConstants;
import com.revolsys.io.shp.ShapefileGeometryUtil;
import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.GeometryFactory;

public class GeometryFieldDefinition extends AbstractFileGdbFieldDefinition {

  public static final Map<GeometryType, DataType> GEOMETRY_TYPE_DATA_TYPE_MAP = new LinkedHashMap<GeometryType, DataType>();

  static {
    GEOMETRY_TYPE_DATA_TYPE_MAP.put(GeometryType.esriGeometryPoint,
      DataTypes.POINT);
    GEOMETRY_TYPE_DATA_TYPE_MAP.put(GeometryType.esriGeometryMultipoint,
      DataTypes.MULTI_POINT);
    GEOMETRY_TYPE_DATA_TYPE_MAP.put(GeometryType.esriGeometryPolyline,
      DataTypes.MULTI_LINE_STRING);
    GEOMETRY_TYPE_DATA_TYPE_MAP.put(GeometryType.esriGeometryPolygon,
      DataTypes.MULTI_POLYGON);
  }

  private static final ShapefileGeometryUtil SHP_UTIL = new ShapefileGeometryUtil(
    false);

  private GeometryFactory geometryFactory = GeometryFactory.floating3();

  private Method readMethod;

  private Method writeMethod;

  public GeometryFieldDefinition(final Field field) {
    super(field.getName(), DataTypes.GEOMETRY,
      BooleanStringConverter.getBoolean(field.getRequired())
        || !field.isIsNullable());
    final GeometryDef geometryDef = field.getGeometryDef();
    if (geometryDef == null) {
      throw new IllegalArgumentException(
        "Field definition does not include a geometry definition");

    } else {
      final SpatialReference spatialReference = geometryDef.getSpatialReference();
      if (spatialReference == null) {
        throw new IllegalArgumentException(
          "Field definition does not include a spatial reference");
      } else {
        final GeometryType geometryType = geometryDef.getGeometryType();
        final DataType dataType = GEOMETRY_TYPE_DATA_TYPE_MAP.get(geometryType);
        setType(dataType);
        this.geometryFactory = spatialReference.getGeometryFactory();
        if (geometryFactory == null) {
          throw new IllegalArgumentException(
            "Field definition does not include a valid coordinate system "
              + spatialReference.getLatestWKID());
        }

        int axisCount = 2;
        final boolean hasZ = geometryDef.isHasZ();
        if (hasZ) {
          axisCount = 3;
        }
        final boolean hasM = geometryDef.isHasM();
        if (hasM) {
          axisCount = 4;
        }
        if (axisCount != geometryFactory.getAxisCount()) {
          final int srid = geometryFactory.getSrid();
          final double scaleXY = geometryFactory.getScaleXY();
          final double scaleZ = geometryFactory.getScaleZ();
          geometryFactory = GeometryFactory.fixed(srid, axisCount,
            scaleXY, scaleZ);
        }
        setProperty(FieldProperties.GEOMETRY_FACTORY, geometryFactory);

        final String geometryTypeKey = dataType.toString() + hasZ + hasM;
        readMethod = ShapefileGeometryUtil.getReadMethod(geometryTypeKey);
        if (readMethod == null) {
          throw new IllegalArgumentException(
            "No read method for geometry type " + geometryTypeKey);
        }
        writeMethod = ShapefileGeometryUtil.getWriteMethod(geometryTypeKey);
        if (writeMethod == null) {
          throw new IllegalArgumentException(
            "No write method for geometry type " + geometryTypeKey);
        }
      }

    }
  }

  @Override
  public int getMaxStringLength() {
    return 40;
  }

  @Override
  public Object getValue(final Row row) {
    final String name = getName();
    final CapiFileGdbRecordStore recordStore = getRecordStore();
    if (recordStore.isNull(row, name)) {
      return null;
    } else {
      final byte[] buffer;
      synchronized (recordStore) {
        buffer = row.getGeometry();
      }
      final ByteArrayInputStream byteIn = new ByteArrayInputStream(buffer);
      final EndianInput in = new EndianInputStream(byteIn);
      try {
        final int type = in.readLEInt();
        if (type == 0) {
          final DataType dataType = getType();
          if (DataTypes.POINT.equals(dataType)) {
            return geometryFactory.point();
          } else if (DataTypes.MULTI_POINT.equals(dataType)) {
            return geometryFactory.multiPoint();
          } else if (DataTypes.LINE_STRING.equals(dataType)) {
            return geometryFactory.lineString();
          } else if (DataTypes.MULTI_LINE_STRING.equals(dataType)) {
            return geometryFactory.multiLineString();
          } else if (DataTypes.POLYGON.equals(dataType)) {
            return geometryFactory.polygon();
          } else if (DataTypes.MULTI_POLYGON.equals(dataType)) {
            return geometryFactory.multiPolygon();
          } else {
            return null;
          }
        } else {
          final Geometry geometry = SHP_UTIL.read(readMethod, geometryFactory,
            in, -1);
          return geometry;
        }
      } catch (final IOException e) {
        throw new RuntimeException(e);
      } finally {
        FileUtil.closeSilent(in);
      }
    }
  }

  @Override
  public Object setValue(final Record object, final Row row,
    final Object value) {
    final String name = getName();
    if (value == null) {
      if (isRequired()) {
        throw new IllegalArgumentException(name
          + " is required and cannot be null");
      } else {
        getRecordStore().setNull(row, name);
      }
      return null;
    } else if (value instanceof Geometry) {
      final Geometry geometry = (Geometry)value;
      final Geometry projectedGeometry = geometry.convert(geometryFactory);
      final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      final EndianOutput out = new EndianOutputStream(byteOut);
      if (geometry.isEmpty()) {
        try {
          out.writeLEInt(ShapefileConstants.NULL_SHAPE);
        } catch (final IOException e) {
        }
      } else {
        SHP_UTIL.write(writeMethod, out, projectedGeometry);
      }
      final byte[] bytes = byteOut.toByteArray();
      synchronized (getRecordStore()) {
        row.setGeometry(bytes);
      }
      return bytes;
    } else {
      throw new IllegalArgumentException("Expecting a " + Geometry.class
        + " not a " + value.getClass() + "=" + value);
    }
  }
}