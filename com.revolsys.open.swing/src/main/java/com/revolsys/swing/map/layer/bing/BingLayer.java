package com.revolsys.swing.map.layer.bing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.revolsys.collection.map.MapEx;
import com.revolsys.datatype.DataType;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.logging.Logs;
import com.revolsys.parallel.ExecutorServiceFactory;
import com.revolsys.swing.Icons;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.component.ValueField;
import com.revolsys.swing.field.ComboBox;
import com.revolsys.swing.layout.GroupLayouts;
import com.revolsys.swing.map.Viewport2D;
import com.revolsys.swing.map.layer.AbstractTiledImageLayer;
import com.revolsys.swing.map.layer.BaseMapLayerGroup;
import com.revolsys.swing.map.layer.MapTile;
import com.revolsys.swing.menu.MenuFactory;
import com.revolsys.swing.menu.Menus;
import com.revolsys.util.CaseConverter;

public class BingLayer extends AbstractTiledImageLayer {
  public static final GeometryFactory GEOMETRY_FACTORY = GeometryFactory.floating3(4326);

  private static final BoundingBox MAX_BOUNDING_BOX = GEOMETRY_FACTORY.newBoundingBox(-180, -85,
    180, 85);

  private static void actionAddLayer(final BaseMapLayerGroup parent) {
    final ValueField dialog = new ValueField();
    dialog.setTitle("Add Bing Layer");
    dialog.setIconImage("bing");

    SwingUtil.addLabel(dialog, "Imagery Set");
    final ComboBox<ImagerySet> imagerySetField = ComboBox.newComboBox("imagerySet", ImagerySet.Road,
      ImagerySet.Aerial, ImagerySet.AerialWithLabels, ImagerySet.CollinsBart,
      ImagerySet.OrdnanceSurvey);
    dialog.add(imagerySetField);

    GroupLayouts.makeColumns(dialog, 2, true, true);

    dialog.setSaveAction(() -> {
      final BingLayer layer = new BingLayer();
      final ImagerySet imagerySet = imagerySetField.getSelectedItem();
      layer.setImagerySet(imagerySet);
      layer.setVisible(true);
      parent.addLayer(layer);
    });

    dialog.showDialog();
  }

  public static void mapObjectFactoryInit() {
    final MenuFactory baseMapsMenu = MenuFactory.getMenu(BaseMapLayerGroup.class);

    Menus.addMenuItem(baseMapsMenu, "group", "Add Bing Layer", "bing", BingLayer::actionAddLayer,
      false);
  }

  private BingClient client;

  private ImagerySet imagerySet = ImagerySet.Road;

  private MapLayer mapLayer;

  private BingLayer() {
    super("bing");
    setIcon(Icons.getIcon("bing"));
  }

  public BingLayer(final Map<String, Object> properties) {
    this();
    setProperties(properties);
  }

  @Override
  public boolean equals(final Object other) {
    if (other instanceof BingLayer) {
      final BingLayer layer = (BingLayer)other;
      if (DataType.equal(layer.getImagerySet(), getImagerySet())) {
        if (DataType.equal(layer.getMapLayer(), getMapLayer())) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public BoundingBox getBoundingBox() {
    return MAX_BOUNDING_BOX;
  }

  public BingClient getClient() {
    return this.client;
  }

  public ImagerySet getImagerySet() {
    return this.imagerySet;
  }

  public MapLayer getMapLayer() {
    return this.mapLayer;
  }

  @Override
  public List<MapTile> getOverlappingMapTiles(final Viewport2D viewport) {
    final List<MapTile> tiles = new ArrayList<>();
    try {
      final double metresPerPixel = viewport.getUnitsPerPixel();
      final int zoomLevel = this.client.getZoomLevel(metresPerPixel);
      final double resolution = getResolution(viewport);
      final BoundingBox geographicBoundingBox = viewport.getBoundingBox()
        .convert(GEOMETRY_FACTORY)
        .intersection(MAX_BOUNDING_BOX);
      final double minX = geographicBoundingBox.getMinX();
      final double minY = geographicBoundingBox.getMinY();
      final double maxX = geographicBoundingBox.getMaxX();
      final double maxY = geographicBoundingBox.getMaxY();

      // Tiles start at the North-West corner of the map
      final int minTileY = this.client.getTileY(zoomLevel, maxY);
      final int maxTileY = this.client.getTileY(zoomLevel, minY);
      final int minTileX = this.client.getTileX(zoomLevel, minX);
      final int maxTileX = this.client.getTileX(zoomLevel, maxX);

      for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
        for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
          final BingMapTile tile = new BingMapTile(this, zoomLevel, resolution, tileX, tileY);
          tiles.add(tile);
        }
      }

    } catch (final Throwable e) {
      Logs.error(this, "Error getting tile envelopes", e);
    }
    return tiles;
  }

  @Override
  public double getResolution(final Viewport2D viewport) {
    final double metresPerPixel = viewport.getUnitsPerPixel();
    final int zoomLevel = this.client.getZoomLevel(metresPerPixel);
    return this.client.getResolution(zoomLevel);
  }

  @Override
  protected boolean initializeDo() {
    final String bingMapsKey = getProperty("bingMapsKey");
    this.client = new BingClient(bingMapsKey);
    return true;
  }

  public void setClient(final BingClient client) {
    this.client = client;
    ExecutorServiceFactory.getExecutorService().execute(this::initialize);
  }

  public void setImagerySet(final ImagerySet imagerySet) {
    this.imagerySet = imagerySet;
    if (getName() == null) {
      setName("Bing " + CaseConverter.toCapitalizedWords(imagerySet.toString()));
    }
  }

  public void setMapLayer(final MapLayer mapLayer) {
    this.mapLayer = mapLayer;
  }

  @Override
  public MapEx toMap() {
    final MapEx map = super.toMap();
    addToMap(map, "imagerySet", this.imagerySet);
    addToMap(map, "mapLayer", this.mapLayer);
    return map;
  }
}
