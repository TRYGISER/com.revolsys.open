package com.revolsys.elevation.cloud.las;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.revolsys.collection.map.Maps;
import com.revolsys.geometry.cs.Area;
import com.revolsys.geometry.cs.Authority;
import com.revolsys.geometry.cs.Axis;
import com.revolsys.geometry.cs.CoordinateSystem;
import com.revolsys.geometry.cs.GeographicCoordinateSystem;
import com.revolsys.geometry.cs.LinearUnit;
import com.revolsys.geometry.cs.ProjectedCoordinateSystem;
import com.revolsys.geometry.cs.Projection;
import com.revolsys.geometry.cs.ProjectionParameterNames;
import com.revolsys.geometry.cs.epsg.EpsgCoordinateSystems;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.io.endian.EndianInput;
import com.revolsys.io.endian.EndianInputStream;
import com.revolsys.io.endian.EndianOutput;
import com.revolsys.io.endian.EndianOutputStream;
import com.revolsys.raster.io.format.tiff.TiffImage;
import com.revolsys.util.Exceptions;
import com.revolsys.util.Pair;

public class LasProjection {
  private static final int LASF_PROJECTION_TIFF_GEO_ASCII_PARAMS = 34737;

  private static final int LASF_PROJECTION_TIFF_GEO_KEY_DIRECTORY_TAG = 34735;

  private static final int LASF_PROJECTION_TIFF_GEO_DOUBLE_PARAMS = 34736;

  private static final String LASF_PROJECTION = "LASF_Projection";

  private static final int LASF_PROJECTION_WKT_COORDINATE_SYSTEM = 2112;

  @SuppressWarnings("unused")
  public static Object convertGeoTiffProjection(final LasPointCloud lasPointCloud,
    final byte[] bytes) {
    try {
      final List<Double> doubleParams = new ArrayList<>();
      {
        final LasVariableLengthRecord doubleParamsProperty = lasPointCloud
          .getLasProperty(new Pair<>(LASF_PROJECTION, LASF_PROJECTION_TIFF_GEO_DOUBLE_PARAMS));
        if (doubleParamsProperty != null) {
          final byte[] doubleParamBytes = doubleParamsProperty.getBytes();
          final ByteBuffer buffer = ByteBuffer.wrap(doubleParamBytes);
          buffer.order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < doubleParamBytes.length / 8; i++) {
            final double value = buffer.getDouble();
            doubleParams.add(value);
          }
        }
      }
      byte[] asciiParamsBytes;
      {
        final LasVariableLengthRecord asciiParamsProperty = lasPointCloud
          .getLasProperty(new Pair<>(LASF_PROJECTION, LASF_PROJECTION_TIFF_GEO_ASCII_PARAMS));
        if (asciiParamsProperty == null) {
          asciiParamsBytes = new byte[0];
        } else {
          asciiParamsBytes = asciiParamsProperty.getBytes();
        }
      }
      final Map<Integer, Object> properties = new LinkedHashMap<>();
      try (
        final EndianInput in = new EndianInputStream(new ByteArrayInputStream(bytes))) {
        final int keyDirectoryVersion = in.readLEUnsignedShort();
        final int keyRevision = in.readLEUnsignedShort();
        final int minorRevision = in.readLEUnsignedShort();
        final int numberOfKeys = in.readLEUnsignedShort();
        for (int i = 0; i < numberOfKeys; i++) {
          final int keyId = in.readLEUnsignedShort();
          final int tagLocation = in.readLEUnsignedShort();
          final int count = in.readLEUnsignedShort();
          final int offset = in.readLEUnsignedShort();
          if (tagLocation == 0) {
            properties.put(keyId, offset);
          } else if (tagLocation == LASF_PROJECTION_TIFF_GEO_DOUBLE_PARAMS) {
            final double value = doubleParams.get(offset);
            properties.put(keyId, value);
          } else if (tagLocation == LASF_PROJECTION_TIFF_GEO_ASCII_PARAMS) {
            final String value = new String(asciiParamsBytes, offset, count,
              StandardCharsets.US_ASCII);
            properties.put(keyId, value);
          }
        }
      }
      GeometryFactory geometryFactory = GeometryFactory.DEFAULT_3D;
      final double[] scaleFactors = new double[] {
        1.0 / lasPointCloud.getResolutionX(), 1.0 / lasPointCloud.getResolutionZ()
      };
      int coordinateSystemId = Maps.getInteger(properties, TiffImage.PROJECTED_COORDINATE_SYSTEM_ID,
        0);
      if (coordinateSystemId == 0) {
        coordinateSystemId = Maps.getInteger(properties, TiffImage.GEOGRAPHIC_COORDINATE_SYSTEM_ID,
          0);
        if (coordinateSystemId != 0) {
          geometryFactory = GeometryFactory.fixed(coordinateSystemId, 3, scaleFactors);
        }
      } else if (coordinateSystemId <= 0 || coordinateSystemId == 32767) {
        final int geoSrid = Maps.getInteger(properties, TiffImage.GEOGRAPHIC_COORDINATE_SYSTEM_ID,
          0);
        if (geoSrid != 0) {
          if (geoSrid > 0 && geoSrid < 32767) {
            final GeographicCoordinateSystem geographicCoordinateSystem = EpsgCoordinateSystems
              .getCoordinateSystem(geoSrid);
            final String name = "unknown";
            final Projection projection = TiffImage.getProjection(properties);
            final Area area = null;

            final Map<String, Object> parameters = new LinkedHashMap<>();
            TiffImage.addDoubleParameter(parameters, ProjectionParameterNames.STANDARD_PARALLEL_1,
              properties, TiffImage.STANDARD_PARALLEL_1_KEY);
            TiffImage.addDoubleParameter(parameters, ProjectionParameterNames.STANDARD_PARALLEL_2,
              properties, TiffImage.STANDARD_PARALLEL_2_KEY);
            TiffImage.addDoubleParameter(parameters, ProjectionParameterNames.LONGITUDE_OF_CENTER,
              properties, TiffImage.LONGITUDE_OF_CENTER_2_KEY);
            TiffImage.addDoubleParameter(parameters, ProjectionParameterNames.LATITUDE_OF_CENTER,
              properties, TiffImage.LATITUDE_OF_CENTER_2_KEY);
            TiffImage.addDoubleParameter(parameters, ProjectionParameterNames.FALSE_EASTING,
              properties, TiffImage.FALSE_EASTING_KEY);
            TiffImage.addDoubleParameter(parameters, ProjectionParameterNames.FALSE_NORTHING,
              properties, TiffImage.FALSE_NORTHING_KEY);

            final LinearUnit linearUnit = TiffImage.getLinearUnit(properties);
            final List<Axis> axis = null;
            final Authority authority = null;
            final ProjectedCoordinateSystem coordinateSystem = new ProjectedCoordinateSystem(
              coordinateSystemId, name, geographicCoordinateSystem, area, projection, parameters,
              linearUnit, axis, authority, false);
            final CoordinateSystem epsgCoordinateSystem = EpsgCoordinateSystems
              .getCoordinateSystem(coordinateSystem);
            geometryFactory = GeometryFactory.fixed(epsgCoordinateSystem.getCoordinateSystemId(), 3,
              scaleFactors);
          }
        }
      } else {
        geometryFactory = GeometryFactory.fixed(coordinateSystemId, 3, scaleFactors);
      }
      lasPointCloud.setGeometryFactoryInternal(geometryFactory);
      return geometryFactory;
    } catch (final IOException e) {
      throw Exceptions.wrap(e);
    }
  }

  public static void init(
    final Map<Pair<String, Integer>, BiFunction<LasPointCloud, byte[], Object>> vlrfactory) {
    vlrfactory.put(new Pair<>(LASF_PROJECTION, LASF_PROJECTION_TIFF_GEO_KEY_DIRECTORY_TAG),
      LasProjection::convertGeoTiffProjection);
  }

  protected static void setGeometryFactory(final LasPointCloud pointCloud,
    final GeometryFactory geometryFactory) {
    pointCloud.removeLasProperties(LASF_PROJECTION);
    final CoordinateSystem coordinateSystem = geometryFactory.getCoordinateSystem();
    if (coordinateSystem != null) {
      final LasPointFormat pointFormat = pointCloud.getPointFormat();
      if (pointFormat.getId() <= 5) {
        final int coordinateSystemId = coordinateSystem.getCoordinateSystemId();
        int keyId;
        if (coordinateSystem instanceof ProjectedCoordinateSystem) {
          keyId = TiffImage.PROJECTED_COORDINATE_SYSTEM_ID;
        } else {
          keyId = TiffImage.GEOGRAPHIC_COORDINATE_SYSTEM_ID;
        }

        final ByteArrayOutputStream byteOut = new ByteArrayOutputStream(1024);
        try (
          final EndianOutput out = new EndianOutputStream(byteOut)) {
          out.writeLEUnsignedShort(1);
          out.writeLEUnsignedShort(1);
          out.writeLEUnsignedShort(0);
          out.writeLEUnsignedShort(1);
          {
            out.writeLEUnsignedShort(keyId);
            out.writeLEUnsignedShort(0);
            out.writeLEUnsignedShort(1);
            out.writeLEUnsignedShort(coordinateSystemId);
          }
        }
        final byte[] bytes = byteOut.toByteArray();
        final LasVariableLengthRecord property = new LasVariableLengthRecord(LASF_PROJECTION,
          LASF_PROJECTION_TIFF_GEO_KEY_DIRECTORY_TAG, "TIFF GeoKeyDirectoryTag", bytes,
          geometryFactory);
        pointCloud.addProperty(property);
      } else {
        final String wkt = EpsgCoordinateSystems.toWkt(coordinateSystem);
        final byte[] stringBytes = wkt.getBytes(StandardCharsets.UTF_8);
        final byte[] bytes = new byte[stringBytes.length + 1];
        System.arraycopy(stringBytes, 0, bytes, 0, stringBytes.length);
        final LasVariableLengthRecord property = new LasVariableLengthRecord(LASF_PROJECTION,
          LASF_PROJECTION_WKT_COORDINATE_SYSTEM, "WKT", bytes, geometryFactory);
        pointCloud.addProperty(property);
      }
    }
  }

}
