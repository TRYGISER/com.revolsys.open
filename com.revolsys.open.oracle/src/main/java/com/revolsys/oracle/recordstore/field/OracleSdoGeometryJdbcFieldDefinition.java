package com.revolsys.oracle.recordstore.field;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.revolsys.datatype.DataType;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.ClockDirection;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.LineString;
import com.revolsys.geometry.model.Lineal;
import com.revolsys.geometry.model.LinearRing;
import com.revolsys.geometry.model.Point;
import com.revolsys.geometry.model.Polygon;
import com.revolsys.geometry.model.Polygonal;
import com.revolsys.geometry.model.Punctual;
import com.revolsys.geometry.model.vertex.Vertex;
import com.revolsys.geometry.operation.valid.CoordinateInfiniteError;
import com.revolsys.geometry.operation.valid.CoordinateNaNError;
import com.revolsys.jdbc.JdbcUtils;
import com.revolsys.jdbc.field.JdbcFieldDefinition;
import com.revolsys.record.Record;
import com.revolsys.record.property.FieldProperties;
import com.revolsys.util.Property;
import com.revolsys.util.number.Numbers;

public class OracleSdoGeometryJdbcFieldDefinition extends JdbcFieldDefinition {

  private static final int[] LINESTRING_ELEM_INFO = new int[] {
    1, 2, 1
  };

  private static final String MDSYS_SDO_GEOMETRY = "MDSYS.SDO_GEOMETRY";

  private static final String MDSYS_SDO_POINT_TYPE = "MDSYS.SDO_POINT_TYPE";

  private static final int[] RECTANGLE_ELEM_INFO = new int[] {
    1, 1003, 3
  };

  private static final double NAN_VALUE = 0;

  private final int axisCount;

  private final GeometryFactory geometryFactory;

  private final int oracleSrid;

  public OracleSdoGeometryJdbcFieldDefinition(final String dbName, final String name,
    final DataType type, final int sqlType, final boolean required, final String description,
    final Map<String, Object> properties, final GeometryFactory geometryFactory,
    final int axisCount, final int oracleSrid) {
    super(dbName, name, type, sqlType, 0, 0, required, description, properties);
    this.geometryFactory = geometryFactory;
    this.axisCount = axisCount;
    this.oracleSrid = oracleSrid;
    setProperty(FieldProperties.GEOMETRY_FACTORY, geometryFactory);
  }

  @Override
  public void addColumnName(final StringBuilder sql, final String tablePrefix) {
    sql.append(tablePrefix);
    sql.append(".GEOMETRY.SDO_GTYPE, ");
    sql.append(tablePrefix);
    sql.append(".GEOMETRY.SDO_POINT.X, ");
    sql.append(tablePrefix);
    sql.append(".GEOMETRY.SDO_POINT.Y, ");
    sql.append(tablePrefix);
    sql.append(".GEOMETRY.SDO_POINT.Z, ");
    sql.append(tablePrefix);
    sql.append(".GEOMETRY.SDO_ELEM_INFO, ");
    sql.append(tablePrefix);
    sql.append(".GEOMETRY.SDO_ORDINATES");
  }

  @Override
  public OracleSdoGeometryJdbcFieldDefinition clone() {
    return new OracleSdoGeometryJdbcFieldDefinition(getDbName(), getName(), getDataType(),
      getSqlType(), isRequired(), getDescription(), getProperties(), this.geometryFactory,
      this.axisCount, this.oracleSrid);
  }

  @Override
  public int setFieldValueFromResultSet(final ResultSet resultSet, final int columnIndex,
    final Record record) throws SQLException {
    Geometry value;
    final int geometryType = resultSet.getInt(columnIndex);
    if (!resultSet.wasNull()) {
      final int axisCount = geometryType / 1000;
      switch (geometryType % 1000) {
        case 1:
          value = toPoint(resultSet, columnIndex, axisCount);
        break;
        case 2:
          value = toLineString(resultSet, columnIndex, axisCount);
        break;
        case 3:
          value = toPolygon(resultSet, columnIndex, axisCount);
        break;
        case 5:
          value = toPunctual(resultSet, columnIndex, axisCount);
        break;
        case 6:
          value = toLineal(resultSet, columnIndex, axisCount);
        break;
        case 7:
          value = toPolygonal(resultSet, columnIndex, axisCount);
        break;
        default:
          throw new IllegalArgumentException("Unsupported geometry type " + geometryType);
      }
      record.setValue(getIndex(), value);
    }
    return columnIndex + 6;
  }

  @Override
  public int setInsertPreparedStatementValue(final PreparedStatement statement,
    final int parameterIndex, final Record record) throws SQLException {
    final String name = getName();
    final Object value = record.getValue(name);
    if (Property.isEmpty(value)) {
      statement.setNull(parameterIndex, Types.STRUCT, "SDO_GEOMETRY");
    } else {
      final Connection connection = statement.getConnection();
      final Struct oracleValue = toSdoGeometry(connection, value, this.axisCount);
      statement.setObject(parameterIndex, oracleValue);
    }
    return parameterIndex + 1;
  }

  @Override
  public int setPreparedStatementValue(final PreparedStatement statement, final int parameterIndex,
    final Object value) throws SQLException {
    if (Property.isEmpty(value)) {
      statement.setNull(parameterIndex, Types.STRUCT, "SDO_GEOMETRY");
    } else {
      final Connection connection = statement.getConnection();
      final Struct oracleValue = toSdoGeometry(connection, value, 2);
      statement.setObject(parameterIndex, oracleValue);
    }
    return parameterIndex + 1;
  }

  private Lineal toLineal(final ResultSet resultSet, final int columnIndex, final int axisCount)
    throws SQLException {
    final List<LineString> lines = new ArrayList<>();

    final BigDecimal[] elemInfo = JdbcUtils.getBigDecimalArray(resultSet, columnIndex + 4);
    final BigDecimal[] coordinatesArray = JdbcUtils.getBigDecimalArray(resultSet, columnIndex + 5);

    for (int i = 0; i < elemInfo.length; i += 3) {
      final int offset = elemInfo[i].intValue();
      final int type = elemInfo[i + 1].intValue();
      final int interpretation = elemInfo[i + 2].intValue();
      int length;
      if (i + 3 < elemInfo.length) {
        final long nextOffset = elemInfo[i + 3].intValue();
        length = (int)(nextOffset - offset);
      } else {
        length = coordinatesArray.length - offset + 1;
      }
      if (interpretation == 1) {
        final double[] coordinates = Numbers.toDoubleArray(coordinatesArray, offset - 1, length);
        final LineString points = this.geometryFactory.lineString(axisCount, coordinates);
        lines.add(points);
      } else {
        throw new IllegalArgumentException(
          "Unsupported geometry type " + type + " interpretation " + interpretation);
      }
    }
    if (lines.size() == 1) {
      return lines.get(0);
    } else {
      return this.geometryFactory.lineal(lines);
    }
  }

  private LineString toLineString(final ResultSet resultSet, final int columnIndex,
    final int axisCount) throws SQLException {
    final int index = columnIndex + 5;
    final BigDecimal[] coordinates = JdbcUtils.getBigDecimalArray(resultSet, index);
    return this.geometryFactory.lineString(axisCount, coordinates);
  }

  private Point toPoint(final ResultSet resultSet, final int columnIndex, final int axisCount)
    throws SQLException {
    final double x = resultSet.getDouble(columnIndex + 1);
    final double y = resultSet.getDouble(columnIndex + 2);
    if (axisCount == 2) {
      return this.geometryFactory.point(x, y);
    } else {
      final double z = resultSet.getDouble(columnIndex + 3);
      return this.geometryFactory.point(x, y, z);
    }
  }

  private Polygon toPolygon(final ResultSet resultSet, final int columnIndex, final int axisCount)
    throws SQLException {
    final BigDecimal[] elemInfo = JdbcUtils.getBigDecimalArray(resultSet, columnIndex + 4);
    final BigDecimal[] coordinatesArray = JdbcUtils.getBigDecimalArray(resultSet, columnIndex + 5);

    final List<LinearRing> rings = new ArrayList<>();
    int numInteriorRings = 0;

    for (int i = 0; i < elemInfo.length; i += 3) {
      final int offset = elemInfo[i].intValue();
      final long type = elemInfo[i + 1].longValue();
      final long interpretation = elemInfo[i + 2].longValue();
      int length;
      if (i + 3 < elemInfo.length) {
        final long nextOffset = elemInfo[i + 3].longValue();
        length = (int)(nextOffset - offset);
      } else {
        length = coordinatesArray.length - offset + 1;
      }
      if (interpretation == 1) {
        final double[] coordinates = Numbers.toDoubleArray(coordinatesArray, offset - 1, length);
        final LinearRing ring = this.geometryFactory.linearRing(axisCount, coordinates);

        switch ((int)type) {
          case 1003:
            if (rings.isEmpty()) {
              rings.add(ring);
            } else {
              throw new IllegalArgumentException("Cannot have two exterior rings on a geometry");
            }
          break;
          case 2003:
            if (numInteriorRings == rings.size()) {
              throw new IllegalArgumentException("Too many interior rings");
            } else {
              numInteriorRings++;
              rings.add(ring);
            }
          break;

          default:
            throw new IllegalArgumentException("Unsupported geometry type " + type);
        }
      } else {
        throw new IllegalArgumentException(
          "Unsupported geometry type " + type + " interpretation " + interpretation);
      }
    }
    final Polygon polygon = this.geometryFactory.polygon(rings);
    return polygon;
  }

  private Polygonal toPolygonal(final ResultSet resultSet, final int columnIndex,
    final int axisCount) throws SQLException {
    final List<Polygon> polygons = new ArrayList<>();

    final BigDecimal[] elemInfo = JdbcUtils.getBigDecimalArray(resultSet, columnIndex + 4);
    final BigDecimal[] coordinatesArray = JdbcUtils.getBigDecimalArray(resultSet, columnIndex + 5);
    final int coordinateCount = coordinatesArray.length;

    List<LinearRing> rings = Collections.emptyList();

    for (int i = 0; i < elemInfo.length; i += 3) {
      final int offset = elemInfo[i].intValue();
      final long type = elemInfo[i + 1].longValue();
      final long interpretation = elemInfo[i + 2].longValue();
      int length;
      if (i + 3 < elemInfo.length) {
        final long nextOffset = elemInfo[i + 3].longValue();
        length = (int)(nextOffset - offset);
      } else {
        length = coordinateCount + 1 - offset;
      }
      if (interpretation == 1) {
        final double[] coordinates = Numbers.toDoubleArray(coordinatesArray, offset - 1, length);
        final LinearRing ring = this.geometryFactory.linearRing(axisCount, coordinates);

        switch ((int)type) {
          case 1003:
            if (!rings.isEmpty()) {
              final Polygon polygon = this.geometryFactory.polygon(rings);
              polygons.add(polygon);
            }
            rings = new ArrayList<>();
            rings.add(ring);

          break;
          case 2003:
            rings.add(ring);
          break;

          default:
            throw new IllegalArgumentException("Unsupported geometry type " + type);
        }
      } else {
        throw new IllegalArgumentException(
          "Unsupported geometry type " + type + " interpretation " + interpretation);
      }
    }
    if (!rings.isEmpty()) {
      final Polygon polygon = this.geometryFactory.polygon(rings);
      polygons.add(polygon);
    }

    if (polygons.size() == 1) {
      return polygons.get(0);
    } else {
      return this.geometryFactory.polygonal(polygons);
    }
  }

  private Punctual toPunctual(final ResultSet resultSet, final int columnIndex, final int axisCount)
    throws SQLException {
    final BigDecimal[] coordinatesArray = JdbcUtils.getBigDecimalArray(resultSet, columnIndex + 5);
    final int vertexCount = coordinatesArray.length / axisCount;
    if (vertexCount == 1) {
      final double[] coordinates = new double[axisCount];
      for (int i = 0; i < axisCount; i++) {
        coordinates[i] = coordinatesArray[i].doubleValue();
      }
      final Point point = this.geometryFactory.point(coordinates);
      return point;
    } else {
      final Point[] points = new Point[vertexCount];
      int coordinateIndex = 0;
      for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
        final double[] coordinates = new double[axisCount];
        for (int i = 0; i < axisCount; i++) {
          coordinates[i] = coordinatesArray[coordinateIndex++].doubleValue();
        }
        final Point point = this.geometryFactory.point(coordinates);
        points[vertexIndex] = point;
      }

      return this.geometryFactory.punctual(points);
    }
  }

  private int toSdoAddPolygon(int offset, final int[] elemInfo, int elemIndex, final int axisCount,
    final double[] coordinates, final Polygon polygon) {
    final LinearRing shell = polygon.getShell();
    offset = toSodAddPolygonRing(offset, elemInfo, elemIndex, 1003, axisCount, coordinates,
      ClockDirection.COUNTER_CLOCKWISE, shell);
    for (final LinearRing hole : polygon.holes()) {
      elemIndex += 3;
      offset = toSodAddPolygonRing(offset, elemInfo, elemIndex, 2003, axisCount, coordinates,
        ClockDirection.CLOCKWISE, hole);
    }
    return offset;
  }

  private Struct toSdoGeometry(final Connection connection, final int geometryType,
    final Struct pointStruct, final int[] elemInfo, final double... coordinates)
    throws SQLException {
    return JdbcUtils.struct(connection, MDSYS_SDO_GEOMETRY, geometryType, this.oracleSrid,
      pointStruct, elemInfo, coordinates);
  }

  private Struct toSdoGeometry(final Connection connection, final Object object,
    final int axisCount) throws SQLException {
    if (object instanceof Geometry) {
      Geometry geometry = (Geometry)object;
      geometry = geometry.newGeometry(this.geometryFactory);
      if (object instanceof Polygon) {
        final Polygon polygon = (Polygon)geometry;
        return toSdoPolygon(connection, polygon, axisCount);
      } else if (object instanceof LineString) {
        final LineString lineString = (LineString)geometry;
        return toSdoLineString(connection, lineString, axisCount);
      } else if (object instanceof Point) {
        final Point point = (Point)geometry;
        return toSdoPoint(connection, point, axisCount);
      } else if (object instanceof Punctual) {
        final Punctual punctual = (Punctual)geometry;
        return toSdoMultiPoint(connection, punctual, axisCount);
      } else if (object instanceof Lineal) {
        final Lineal lineal = (Lineal)geometry;
        return toSdoMultiLineString(connection, lineal, axisCount);
      } else if (object instanceof Polygonal) {
        final Polygonal polygonal = (Polygonal)geometry;
        return toSdoMultiPolygon(connection, polygonal, axisCount);
      }
    } else if (object instanceof BoundingBox) {
      BoundingBox boundingBox = (BoundingBox)object;
      boundingBox = boundingBox.convert(this.geometryFactory, 2);
      final double minX = boundingBox.getMinX();
      final double minY = boundingBox.getMinY();
      final double maxX = boundingBox.getMaxX();
      final double maxY = boundingBox.getMaxY();
      return toSdoGeometry(connection, 3, null, RECTANGLE_ELEM_INFO, minX, minY, maxX, maxY);
    }
    throw new IllegalArgumentException("Unable to convert to SDO_GEOMETRY " + object.getClass());
  }

  private Struct toSdoLineString(final Connection connection, final LineString line,
    final int axisCount) throws SQLException {
    final int geometryType = axisCount * 1000 + 2;
    final int vertexCount = line.getVertexCount();
    final double[] coordinates = new double[vertexCount * axisCount];
    line.copyCoordinates(axisCount, NAN_VALUE, coordinates, 0);
    return toSdoGeometry(connection, geometryType, null, LINESTRING_ELEM_INFO, coordinates);
  }

  private Struct toSdoMultiLineString(final Connection connection, final Lineal lineal,
    final int axisCount) throws SQLException {
    final int geometryType = axisCount * 1000 + 6;

    final int geometryCount = lineal.getGeometryCount();
    final int[] elemInfo = new int[geometryCount * 3];

    final int vertexCount = lineal.getVertexCount();
    final int coordinateCount = vertexCount * axisCount;
    final double[] coordinates = new double[coordinateCount];
    int offset = 0;
    int elemIndex = 0;
    for (final LineString line : lineal.lineStrings()) {
      elemInfo[elemIndex++] = offset + 1;
      elemInfo[elemIndex++] = 2;
      elemInfo[elemIndex++] = 1;
      offset = line.copyCoordinates(axisCount, NAN_VALUE, coordinates, offset);
    }
    return toSdoGeometry(connection, geometryType, null, elemInfo, coordinates);
  }

  private Struct toSdoMultiPoint(final Connection connection, final Punctual punctual,
    final int axisCount) throws SQLException {
    final int geometryType = axisCount * 1000 + 5;

    final int geometryCount = punctual.getGeometryCount();
    final int[] elemInfo = new int[] {
      1, 1, geometryCount
    };

    final double[] coordinates = new double[geometryCount * axisCount];
    int i = 0;
    for (final Point point : punctual.points()) {
      for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
        final double value = point.getCoordinate(axisIndex);
        if (Double.isNaN(value)) {
          coordinates[i] = NAN_VALUE;
        } else {
          coordinates[i] = value;
        }
        i++;
      }
    }
    return toSdoGeometry(connection, geometryType, null, elemInfo, coordinates);
  }

  private Struct toSdoMultiPolygon(final Connection connection, final Polygonal polygonal,
    final int axisCount) throws SQLException {
    final int geometryType = axisCount * 1000 + 7;

    final int vertexCount = polygonal.getVertexCount();
    final int coordinateCount = vertexCount * axisCount;
    final double[] coordinates = new double[coordinateCount];

    int ringCount = 0;
    for (final Polygon polygon : polygonal.polygons()) {
      ringCount += polygon.getRingCount();
    }
    final int[] elemInfo = new int[ringCount * 3];

    int offset = 0;
    int elemIndex = 0;
    for (final Polygon polygon : polygonal.polygons()) {
      offset = toSdoAddPolygon(offset, elemInfo, elemIndex, axisCount, coordinates, polygon);
      elemIndex += 3 * polygon.getRingCount();
    }
    return toSdoGeometry(connection, geometryType, null, elemInfo, coordinates);
  }

  private Struct toSdoPoint(final Connection connection, final Point point, final int axisCount)
    throws SQLException {
    final double x = point.getX();
    final double y = point.getY();
    validateCoordinate(point, 0, x);
    validateCoordinate(point, 1, y);
    Double z = null;
    int geometryType = 1;
    if (axisCount > 2) {
      geometryType = 3001;
      z = point.getZ();
      if (Double.isNaN(z)) {
        z = null;
      }
    }
    final Struct pointStruct = JdbcUtils.struct(connection, MDSYS_SDO_POINT_TYPE, x, y, z);
    return toSdoGeometry(connection, geometryType, pointStruct, null, null);
  }

  private Struct toSdoPolygon(final Connection connection, final Polygon polygon,
    final int axisCount) throws SQLException {
    final int geometryType = axisCount * 1000 + 3;

    final int ringCount = polygon.getRingCount();
    final int[] elemInfo = new int[ringCount * 3];

    final int vertexCount = polygon.getVertexCount();
    final int coordinateCount = vertexCount * axisCount;
    final double[] coordinates = new double[coordinateCount];

    toSdoAddPolygon(0, elemInfo, 0, axisCount, coordinates, polygon);
    return toSdoGeometry(connection, geometryType, null, elemInfo, coordinates);

  }

  private int toSodAddPolygonRing(int offset, final int[] elemInfo, final int elemIndex,
    final int elemType, final int axisCount, final double[] coordinates,
    final ClockDirection expectedRingOrientation, final LinearRing ring) {
    elemInfo[elemIndex] = offset + 1;
    elemInfo[elemIndex + 1] = elemType; // Exterior counter clockwise
    elemInfo[elemIndex + 2] = 1;
    final ClockDirection ringOrientation = ring.getClockDirection();
    if (ringOrientation == expectedRingOrientation) {
      offset = ring.copyCoordinates(axisCount, NAN_VALUE, coordinates, offset);
    } else {
      offset = ring.copyCoordinatesReverse(axisCount, NAN_VALUE, coordinates, offset);
    }
    return offset;
  }

  protected void validateCoordinate(final Point point, final int axisIndex,
    final double coordinate) {
    if (!Double.isFinite(coordinate)) {
      final Vertex vertex = point.getVertex(axisIndex);
      if (Double.isNaN(coordinate)) {
        throw new CoordinateNaNError(vertex, axisIndex);
      } else {
        throw new CoordinateInfiniteError(vertex, axisIndex);
      }
    }
  }
}
