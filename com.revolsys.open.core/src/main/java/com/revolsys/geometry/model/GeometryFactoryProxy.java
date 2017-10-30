package com.revolsys.geometry.model;

import com.revolsys.geometry.cs.CoordinateSystem;
import com.revolsys.geometry.cs.projection.CoordinatesOperation;
import com.revolsys.geometry.cs.projection.ProjectionFactory;

public interface GeometryFactoryProxy {
  default BoundingBox convertBoundingBox(final BoundingBoxProxy boundingBoxProxy) {
    if (boundingBoxProxy != null) {
      final BoundingBox boundingBox = boundingBoxProxy.getBoundingBox();
      if (boundingBox != null) {

        final GeometryFactory geometryFactory = getGeometryFactory();
        if (geometryFactory != null) {
          return boundingBox.convert(geometryFactory);
        }
      }
      return boundingBox;
    }
    return null;
  }

  default <G extends Geometry> G convertGeometry(final G geometry) {
    if (geometry != null) {
      final GeometryFactory geometryFactory = getGeometryFactory();
      if (geometryFactory != null) {
        return geometry.convertGeometry(geometryFactory);
      }
    }
    return geometry;
  }

  default <G extends Geometry> G convertGeometry(final G geometry, final int axisCount) {
    if (geometry != null) {
      final GeometryFactory geometryFactory = getGeometryFactory();
      if (geometryFactory != null) {
        return geometry.convertGeometry(geometryFactory, axisCount);
      }
    }
    return geometry;
  }

  default CoordinatesOperation getCoordinatesOperation(final GeometryFactory geometryFactory) {
    if (geometryFactory == null) {
      return null;
    } else {
      final int coordinateSystemId = geometryFactory.getCoordinateSystemId();
      final int coordinateSystemIdThis = getCoordinateSystemId();
      if (coordinateSystemId == coordinateSystemIdThis) {
        return null;
      } else if (coordinateSystemId == 0 || coordinateSystemIdThis == 0) {
        return null;
      } else {
        final CoordinateSystem coordinateSystemThis = getCoordinateSystem();
        final CoordinateSystem coordinateSystem = geometryFactory.getCoordinateSystem();
        if (coordinateSystem == coordinateSystemThis) {
          return null;
        } else if (coordinateSystem == null || coordinateSystemThis == null) {
          return null;
        } else if (coordinateSystem.equals(coordinateSystemThis)) {
          return null;
        } else {
          return ProjectionFactory.getCoordinatesOperation(coordinateSystemThis, coordinateSystem);
        }
      }
    }
  }

  default CoordinateSystem getCoordinateSystem() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    if (geometryFactory == null) {
      return null;
    } else {
      return geometryFactory.getCoordinateSystem();
    }
  }

  default int getCoordinateSystemId() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    if (geometryFactory == null) {
      return 0;
    } else {
      return geometryFactory.getCoordinateSystemId();
    }
  }

  default String getCoordinateSystemName() {
    final CoordinateSystem coordinateSystem = getCoordinateSystem();
    if (coordinateSystem == null) {
      return "Unknown";
    } else {
      return coordinateSystem.getCoordinateSystemName();
    }
  }

  default GeometryFactory getGeometryFactory() {
    return GeometryFactory.DEFAULT_3D;
  }

  default GeometryFactory getNonZeroGeometryFactory(GeometryFactory geometryFactory) {
    final GeometryFactory geometryFactoryThis = getGeometryFactory();
    if (geometryFactory == null) {
      return geometryFactoryThis;
    } else {
      final int srid = geometryFactory.getCoordinateSystemId();
      if (srid == 0) {
        final int geometrySrid = geometryFactoryThis.getCoordinateSystemId();
        if (geometrySrid != 0) {
          geometryFactory = geometryFactory.convertSrid(geometrySrid);
        }
      }
      return geometryFactory;
    }
  }

  default boolean isSameCoordinateSystem(final GeometryFactory geometryFactory) {
    final GeometryFactory geometryFactory2 = getGeometryFactory();
    if (geometryFactory == null || geometryFactory2 == null) {
      return false;
    } else {
      return geometryFactory.isSameCoordinateSystem(geometryFactory2);
    }
  }

  default boolean isSameCoordinateSystem(final GeometryFactoryProxy proxy) {
    if (proxy == null) {
      return false;
    } else {
      final GeometryFactory geometryFactory = proxy.getGeometryFactory();
      return isSameCoordinateSystem(geometryFactory);
    }
  }
}
