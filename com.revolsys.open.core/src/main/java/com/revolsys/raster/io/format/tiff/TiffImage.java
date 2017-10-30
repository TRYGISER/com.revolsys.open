package com.revolsys.raster.io.format.tiff;

import java.awt.image.RenderedImage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;

import org.libtiff.jai.codec.XTIFF;
import org.libtiff.jai.codec.XTIFFDirectory;
import org.libtiff.jai.codec.XTIFFField;
import org.libtiff.jai.codecimpl.XTIFFCodec;
import org.libtiff.jai.operator.XTIFFDescriptor;

import com.revolsys.collection.map.IntHashMap;
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
import com.revolsys.geometry.cs.epsg.EpsgAuthority;
import com.revolsys.geometry.cs.epsg.EpsgCoordinateSystems;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.logging.Logs;
import com.revolsys.raster.JaiGeoreferencedImage;
import com.revolsys.spring.resource.Resource;
import com.sun.media.jai.codec.ImageCodec;

@SuppressWarnings("deprecation")
public class TiffImage extends JaiGeoreferencedImage {
  /** ProjFalseEastingGeoKey (3082) */
  public static final int FALSE_EASTING_KEY = 3082;

  /** ProjFalseNorthingGeoKey (3083) */
  public static final int FALSE_NORTHING_KEY = 3083;

  /** GeographicTypeGeoKey (2048) */
  public static final int GEOGRAPHIC_COORDINATE_SYSTEM_ID = 2048;

  /** ProjNatOriginLatGeoKey (3081) */
  public static final int LATITUDE_OF_CENTER_2_KEY = 3081;

  /** ProjLinearUnitsGeoKey (3076) */
  public static final int LINEAR_UNIT_ID = 3076;

  /** ProjNatOriginLongGeoKey (3080) */
  public static final int LONGITUDE_OF_CENTER_2_KEY = 3080;

  /** ProjectedCSTypeGeoKey (3072) */
  public static final int PROJECTED_COORDINATE_SYSTEM_ID = 3072;

  /** ProjCoordTransGeoKey (3075) */
  public static final int PROJECTION_ID = 3075;

  private static IntHashMap<String> PROJECTION_NAMES = new IntHashMap<>();

  /** ProjStdParallel1GeoKey (3078) */
  public static final int STANDARD_PARALLEL_1_KEY = 3078;

  /** ProjStdParallel2GeoKey (3079) */
  public static final int STANDARD_PARALLEL_2_KEY = 3079;

  public static final int TAG_X_RESOLUTION = 282;

  public static final int TAG_Y_RESOLUTION = 283;

  static {
    try {
      final OperationRegistry reg = JAI.getDefaultInstance().getOperationRegistry();
      ImageCodec.unregisterCodec("tiff");
      reg.unregisterOperationDescriptor("tiff");

      ImageCodec.registerCodec(new XTIFFCodec());
      final XTIFFDescriptor descriptor = new XTIFFDescriptor();

      reg.registerDescriptor(descriptor);

    } catch (final Throwable t) {
    }
  }

  static {
    PROJECTION_NAMES.put(1, "Transverse_Mercator");
    // CT_TransvMercator_Modified_Alaska = 2
    // CT_ObliqueMercator = 3
    // CT_ObliqueMercator_Laborde = 4
    // CT_ObliqueMercator_Rosenmund = 5
    // CT_ObliqueMercator_Spherical = 6
    PROJECTION_NAMES.put(7, "Mercator");
    PROJECTION_NAMES.put(8, "Lambert_Conic_Conformal_(2SP)");
    PROJECTION_NAMES.put(9, "Lambert_Conic_Conformal_(1SP)");
    // CT_LambertAzimEqualArea = 10
    PROJECTION_NAMES.put(11, "Albers_Equal_Area");
    // CT_AzimuthalEquidistant = 12
    // CT_EquidistantConic = 13
    // CT_Stereographic = 14
    // CT_PolarStereographic = 15
    // CT_ObliqueStereographic = 16
    // CT_Equirectangular = 17
    // CT_CassiniSoldner = 18
    // CT_Gnomonic = 19
    // CT_MillerCylindrical = 20
    // CT_Orthographic = 21
    // CT_Polyconic = 22
    // CT_Robinson = 23
    // CT_Sinusoidal = 24
    // CT_VanDerGrinten = 25
    // CT_NewZealandMapGrid = 26
    // CT_TransvMercator_SouthOriented= 27

    // registerCoordinatesProjection("Popular_Visualisation_Pseudo_Mercator",
    // WebMercator.class);
    // registerCoordinatesProjection("Mercator_(1SP)", Mercator1SP.class);
    // registerCoordinatesProjection("Mercator_(2SP)", Mercator2SP.class);
    // registerCoordinatesProjection("Mercator_(1SP)_(Spherical)",
    // Mercator1SPSpherical.class);
    // registerCoordinatesProjection("Lambert_Conic_Conformal_(2SP_Belgium)",
    // LambertConicConformal.class);
  }

  public static void addDoubleParameter(final Map<String, Object> parameters, final String name,
    final Map<Integer, Object> geoKeys, final int key) {
    final Double value = Maps.getDouble(geoKeys, key);
    if (value != null) {
      parameters.put(name, value);
    }
  }

  private static void addGeoKey(final Map<Integer, Object> geoKeys, final XTIFFDirectory dir,
    final int keyId, final int tiffTag, final int valueCount, final int valueOrOffset) {
    int type = XTIFFField.TIFF_SHORT;
    Object value = null;
    if (tiffTag > 0) {
      // Values are in another tag:
      final XTIFFField values = dir.getField(tiffTag);
      if (values != null) {
        type = values.getType();
        if (type == XTIFFField.TIFF_ASCII) {
          final String string = values.getAsString(0).substring(valueOrOffset,
            valueOrOffset + valueCount - 1);
          value = string;
        } else if (type == XTIFFField.TIFF_DOUBLE) {
          final double number = values.getAsDouble(valueOrOffset);
          value = number;
        }
      } else {
        throw new IllegalArgumentException("GeoTIFF tag not found");
      }
    } else {
      // value is SHORT, stored in valueOrOffset
      type = XTIFFField.TIFF_SHORT;
      value = (short)valueOrOffset;
    }

    geoKeys.put(keyId, value);
  }

  private static Map<Integer, Object> getGeoKeys(final XTIFFDirectory dir) {
    final Map<Integer, Object> geoKeys = new LinkedHashMap<>();

    final XTIFFField geoKeyTag = dir.getField(XTIFF.TIFFTAG_GEO_KEY_DIRECTORY);

    if (geoKeyTag != null) {
      final char[] keys = geoKeyTag.getAsChars();
      for (int i = 4; i < keys.length; i += 4) {
        final int keyId = keys[i];
        final int tiffTag = keys[i + 1];
        final int valueCount = keys[i + 2];
        final int valueOrOffset = keys[i + 3];
        addGeoKey(geoKeys, dir, keyId, tiffTag, valueCount, valueOrOffset);
      }

    }
    return geoKeys;
  }

  public static LinearUnit getLinearUnit(final Map<Integer, Object> geoKeys) {
    final int linearUnitId = Maps.getInteger(geoKeys, LINEAR_UNIT_ID, 0);
    return EpsgCoordinateSystems.getLinearUnit(linearUnitId);
  }

  public static Projection getProjection(final Map<Integer, Object> geoKeys) {
    final int projectionId = Maps.getInteger(geoKeys, PROJECTION_ID, 0);
    final String projectionName = PROJECTION_NAMES.get(projectionId);
    if (projectionName == null) {
      return null;
    } else {
      final Authority projectionAuthority = new EpsgAuthority(projectionId);
      final Projection projection = new Projection(projectionName, projectionAuthority);
      return projection;
    }
  }

  public TiffImage(final Resource imageResource) {
    super(imageResource);
  }

  private double getFieldAsDouble(final XTIFFDirectory directory, final int fieldIndex,
    final double defaultValue) {
    final XTIFFField field = directory.getField(fieldIndex);
    if (field == null) {
      return defaultValue;
    } else {
      return field.getAsDouble(0);
    }
  }

  @Override
  public String getWorldFileExtension() {
    return "tfw";
  }

  private boolean loadGeoTiffMetaData(final XTIFFDirectory directory) {
    try {
      final int xResolution = (int)getFieldAsDouble(directory, TAG_X_RESOLUTION, 1);
      final int yResolution = (int)getFieldAsDouble(directory, TAG_Y_RESOLUTION, 1);
      setDpi(xResolution, yResolution);
    } catch (final Throwable e) {
      Logs.error(this, e);
    }
    GeometryFactory geometryFactory = null;
    final Map<Integer, Object> geoKeys = getGeoKeys(directory);
    int coordinateSystemId = Maps.getInteger(geoKeys, PROJECTED_COORDINATE_SYSTEM_ID, 0);
    if (coordinateSystemId == 0) {
      coordinateSystemId = Maps.getInteger(geoKeys, GEOGRAPHIC_COORDINATE_SYSTEM_ID, 0);
      if (coordinateSystemId != 0) {
        geometryFactory = GeometryFactory.floating(coordinateSystemId, 2);
      }
    } else if (coordinateSystemId <= 0 || coordinateSystemId == 32767) {
      final int geoSrid = Maps.getInteger(geoKeys, GEOGRAPHIC_COORDINATE_SYSTEM_ID, 0);
      if (geoSrid != 0) {
        if (geoSrid > 0 && geoSrid < 32767) {
          final GeographicCoordinateSystem geographicCoordinateSystem = EpsgCoordinateSystems
            .getCoordinateSystem(geoSrid);
          final String name = "unknown";
          final Projection projection = getProjection(geoKeys);
          final Area area = null;

          final Map<String, Object> parameters = new LinkedHashMap<>();
          addDoubleParameter(parameters, ProjectionParameterNames.STANDARD_PARALLEL_1, geoKeys,
            STANDARD_PARALLEL_1_KEY);
          addDoubleParameter(parameters, ProjectionParameterNames.STANDARD_PARALLEL_2, geoKeys,
            STANDARD_PARALLEL_2_KEY);
          addDoubleParameter(parameters, ProjectionParameterNames.LONGITUDE_OF_CENTER, geoKeys,
            LONGITUDE_OF_CENTER_2_KEY);
          addDoubleParameter(parameters, ProjectionParameterNames.LATITUDE_OF_CENTER, geoKeys,
            LATITUDE_OF_CENTER_2_KEY);
          addDoubleParameter(parameters, ProjectionParameterNames.FALSE_EASTING, geoKeys,
            FALSE_EASTING_KEY);
          addDoubleParameter(parameters, ProjectionParameterNames.FALSE_NORTHING, geoKeys,
            FALSE_NORTHING_KEY);

          final LinearUnit linearUnit = getLinearUnit(geoKeys);
          final List<Axis> axis = null;
          final Authority authority = null;
          final ProjectedCoordinateSystem coordinateSystem = new ProjectedCoordinateSystem(
            coordinateSystemId, name, geographicCoordinateSystem, area, projection, parameters,
            linearUnit, axis, authority, false);
          final CoordinateSystem epsgCoordinateSystem = EpsgCoordinateSystems
            .getCoordinateSystem(coordinateSystem);
          geometryFactory = GeometryFactory.floating(epsgCoordinateSystem.getCoordinateSystemId(),
            2);
        }
      }
    } else {
      geometryFactory = GeometryFactory.floating(coordinateSystemId, 2);
    }
    if (geometryFactory != null) {
      setGeometryFactory(geometryFactory);
    }

    final XTIFFField tiePoints = directory.getField(XTIFF.TIFFTAG_GEO_TIEPOINTS);
    if (tiePoints == null) {
      final XTIFFField geoTransform = directory.getField(XTIFF.TIFFTAG_GEO_TRANS_MATRIX);
      if (geoTransform == null) {
        return false;
      } else {
        final double x1 = geoTransform.getAsDouble(3);
        final double y1 = geoTransform.getAsDouble(7);
        final double pixelWidth = geoTransform.getAsDouble(0);
        final double pixelHeight = geoTransform.getAsDouble(5);
        final double xRotation = geoTransform.getAsDouble(4);
        final double yRotation = geoTransform.getAsDouble(1);
        setResolution(pixelWidth);
        // TODO rotation
        setBoundingBox(x1, y1, pixelWidth, pixelHeight);
        return true;
      }
    } else {
      final XTIFFField pixelScale = directory.getField(XTIFF.TIFFTAG_GEO_PIXEL_SCALE);
      if (pixelScale == null) {
        return false;
      } else {
        final double rasterXOffset = tiePoints.getAsDouble(0);
        final double rasterYOffset = tiePoints.getAsDouble(1);
        if (rasterXOffset != 0 && rasterYOffset != 0) {
          // These should be 0, not sure what to do if they are not
          throw new IllegalArgumentException(
            "Exepectig 0 for the raster x,y tie points in a GeoTIFF");
        }
        // double rasterZOffset = fieldModelTiePoints.getAsDouble(2);
        // setTopLeftRasterPoint(new PointDouble(
        // rasterXOffset,
        // rasterYOffset));

        // Top left corner of image in model coordinates
        final double x1 = tiePoints.getAsDouble(3);
        final double y1 = tiePoints.getAsDouble(4);
        // double modelZOffset = fieldModelTiePoints.getAsDouble(5);
        // setTopLeftModelPoint(new PointDouble(
        // modelXOffset,
        // modelYOffset));
        final double pixelWidth = pixelScale.getAsDouble(0);
        final double pixelHeight = pixelScale.getAsDouble(1);
        setResolution(pixelWidth);
        setBoundingBox(x1, y1, pixelWidth, -pixelHeight);
        return true;
      }
    }
  }

  @Override
  protected void loadMetaDataFromImage() {
    final RenderedImage image = getRenderedImage();
    final Object tiffDirectory = image.getProperty("tiff.directory");
    if (tiffDirectory == null) {
      throw new IllegalArgumentException("This is not a (geo)tiff file. Missing TIFF directory.");
    } else {
      if (!(tiffDirectory instanceof XTIFFDirectory)
        || !loadGeoTiffMetaData((XTIFFDirectory)tiffDirectory)) {
      }
    }
  }
}
