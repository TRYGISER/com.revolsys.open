package com.revolsys.swing.map.layer.arcgisrest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.revolsys.gis.cs.BoundingBox;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.io.esri.map.rest.ArcGisServerRestClient;
import com.revolsys.io.esri.map.rest.MapServer;
import com.revolsys.io.esri.map.rest.map.TileInfo;
import com.revolsys.io.map.MapObjectFactory;
import com.revolsys.io.map.MapSerializerUtil;
import com.revolsys.swing.map.Viewport2D;
import com.revolsys.swing.map.layer.AbstractTiledImageLayer;
import com.revolsys.swing.map.layer.InvokeMethodMapObjectFactory;
import com.revolsys.swing.map.layer.MapTile;

public class ArcGisServerRestLayer extends AbstractTiledImageLayer {

  public static final MapObjectFactory FACTORY = new InvokeMethodMapObjectFactory(
    "arcgisServerRest", "Arc GIS Server REST", ArcGisServerRestLayer.class,
    "create");

  public static ArcGisServerRestLayer create(
    final Map<String, Object> properties) {
    return new ArcGisServerRestLayer(properties);
  }

  private MapServer mapServer;

  private GeometryFactory geometryFactory;

  private String url;

  public ArcGisServerRestLayer(final Map<String, Object> properties) {
    super(properties);
    setType("arcgisServerRest");
  }

  @Override
  protected boolean doInitialize() {
    this.url = getProperty("url");
    try {
      this.mapServer = ArcGisServerRestClient.getMapServer(url);
      final TileInfo tileInfo = this.mapServer.getTileInfo();
      this.geometryFactory = tileInfo.getSpatialReference();
      return true;
    } catch (final Throwable e) {
      throw new RuntimeException("Error connecting to ArcGIS rest server "
        + url, e);
    }
  }

  @Override
  public BoundingBox getBoundingBox() {
    final MapServer mapServer = getMapServer();
    if (mapServer == null) {
      return new BoundingBox();
    } else {
      return mapServer.getFullExtent();
    }
  }

  public synchronized MapServer getMapServer() {
    return this.mapServer;
  }

  @Override
  public List<MapTile> getOverlappingMapTiles(final Viewport2D viewport) {
    final MapServer mapServer = getMapServer();
    final List<MapTile> tiles = new ArrayList<MapTile>();
    if (!isHasError()) {
      try {
        final double metresPerPixel = viewport.getUnitsPerPixel();
        final int zoomLevel = mapServer.getZoomLevel(metresPerPixel);
        final double resolution = getResolution(viewport);
        if (resolution > 0) {
          final BoundingBox viewBoundingBox = viewport.getBoundingBox();
          final BoundingBox maxBoundingBox = getBoundingBox();
          final BoundingBox boundingBox = viewBoundingBox.convert(
            this.geometryFactory).intersection(maxBoundingBox);
          final double minX = boundingBox.getMinX();
          final double minY = boundingBox.getMinY();
          final double maxX = boundingBox.getMaxX();
          final double maxY = boundingBox.getMaxY();

          // Tiles start at the North-West corner of the map
          final int minTileX = mapServer.getTileX(zoomLevel, minX);
          final int minTileY = mapServer.getTileY(zoomLevel, maxY);
          final int maxTileX = mapServer.getTileX(zoomLevel, maxX);
          final int maxTileY = mapServer.getTileY(zoomLevel, minY);

          for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
            for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
              final ArcGisServerRestMapTile tile = new ArcGisServerRestMapTile(
                this, zoomLevel, resolution, tileX, tileY);
              tiles.add(tile);
            }
          }
        }
      } catch (final Throwable e) {
        setError(e);
      }
    }
    return tiles;
  }

  @Override
  public double getResolution(final Viewport2D viewport) {
    final MapServer mapServer = getMapServer();
    if (mapServer == null) {
      return 0;
    } else {
      final double metresPerPixel = viewport.getUnitsPerPixel();
      final int zoomLevel = mapServer.getZoomLevel(metresPerPixel);
      return mapServer.getResolution(zoomLevel);
    }
  }

  @Override
  public Map<String, Object> toMap() {
    final Map<String, Object> map = super.toMap();
    MapSerializerUtil.add(map, "url", this.url);
    return map;
  }

}
