package com.revolsys.io.esri.map.rest;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.revolsys.gis.cs.BoundingBox;
import com.revolsys.io.esri.map.rest.map.LayerDescription;
import com.revolsys.io.esri.map.rest.map.LevelOfDetail;
import com.revolsys.io.esri.map.rest.map.TableDescription;
import com.revolsys.io.esri.map.rest.map.TileInfo;
import com.revolsys.io.esri.map.rest.map.TimeInfo;

public class MapServer extends Service {

  protected MapServer() {
    super("MapServer");
  }

  public BoundingBox getBoundingBox(final int zoomLevel, final int tileX,
    final int tileY) {
    final TileInfo tileInfo = getTileInfo();

    final double originX = tileInfo.getOriginX();
    final double tileWidth = tileInfo.getModelWidth(zoomLevel);
    final double x1 = originX + tileWidth * tileX;
    final double x2 = x1 + tileWidth;

    final double originY = tileInfo.getOriginY();
    final double tileHeight = tileInfo.getModelHeight(zoomLevel);
    final double y1 = originY - tileHeight * tileY;
    final double y2 = y1 - tileHeight;

    return new BoundingBox(tileInfo.getSpatialReference(), x1, y1, x2, y2);
  }

  public String getCapabilities() {
    return getValue("capabilities");
  }

  public String getCopyrightText() {
    return getValue("copyrightText");
  }

  public String getDescription() {
    return getValue("description");
  }

  public Map<String, String> getDocumentInfo() {
    return getValue("documentInfo");
  }

  public BoundingBox getFullExtent() {
    return getBoundingBox("fullExtent");
  }

  public BoundingBox getInitialExtent() {
    return getBoundingBox("initialExtent");
  }

  public List<LayerDescription> getLayers() {
    return getList(LayerDescription.class, "layers");
  }

  public String getMapName() {
    return getValue("mapName");
  }

  public Boolean getSingleFusedMapCache() {
    return getValue("singleFusedMapCache");
  }

  public String getSupportedImageFormatTypes() {
    return getValue("supportedImageFormatTypes");
  }

  public List<TableDescription> getTables() {
    return getList(TableDescription.class, "tables");
  }

  public Image getTileImage(final int zoomLevel, final int tileX,
    final int tileY) {
    final String url = getTileUrl(zoomLevel, tileX, tileY);
    try {
      final URLConnection connection = new URL(url).openConnection();
      final InputStream in = connection.getInputStream();
      return ImageIO.read(in);
    } catch (final IOException e) {
      throw new RuntimeException("Unable to download image from: " + url, e);
    }
  }

  public TileInfo getTileInfo() {
    return getObject(TileInfo.class, "tileInfo");
  }

  public String getTileUrl(final int zoomLevel, final int tileX, final int tileY) {
    return getServiceUrl() + getPath() + "/tile/" + zoomLevel + "/" + tileY
      + "/" + tileX;
  }

  public int getTileX(final int zoomLevel, final double x) {
    final TileInfo tileInfo = getTileInfo();
    final double modelTileSize = tileInfo.getModelWidth(zoomLevel);
    final double originX = tileInfo.getOriginX();
    final double deltaX = x - originX;
    final double ratio = deltaX / modelTileSize;
    int tileX = (int)Math.floor(ratio);
    if (tileX > 0 && ratio - tileX < 0.0001) {
      tileX--;
    }
    return tileX;
  }

  public int getTileY(final int zoomLevel, final double y) {
    final TileInfo tileInfo = getTileInfo();
    final double modelTileSize = tileInfo.getModelHeight(zoomLevel);

    final double originY = tileInfo.getOriginY();
    final double deltaY = originY - y;

    final double ratio = deltaY / modelTileSize;
    final int tileY = (int)Math.floor(ratio);
    return tileY;
  }

  public TimeInfo getTimeInfo() {
    return getObject(TimeInfo.class, "timeInfo");
  }

  public String getUnits() {
    return getValue("units");
  }

  public int getZoomLevel(final double metresPerPixel) {
    final TileInfo tileInfo = getTileInfo();
    final List<LevelOfDetail> levelOfDetails = tileInfo.getLevelOfDetails();
    final Integer minLevel = levelOfDetails.get(0).getLevel();
    for (final LevelOfDetail levelOfDetail : levelOfDetails) {
      final double zoomLevelMetresPerPixel = levelOfDetail.getResolution();
      if (metresPerPixel > zoomLevelMetresPerPixel) {
        final Integer level = levelOfDetail.getLevel();
        return Math.max(level, minLevel);
      }
    }
    return levelOfDetails.get(levelOfDetails.size() - 1).getLevel();
  }
}
