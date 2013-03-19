package com.revolsys.swing.map.util;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CLocation;
import bibliothek.gui.dock.common.DefaultSingleCDockable;
import bibliothek.gui.dock.common.SingleCDockable;
import bibliothek.gui.dock.common.event.CDockableStateListener;
import bibliothek.gui.dock.common.intern.CControlAccess;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.common.mode.ExtendedMode;

import com.revolsys.gis.cs.BoundingBox;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.io.AbstractDataObjectReaderFactory;
import com.revolsys.gis.data.io.DataObjectReader;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.io.FileUtil;
import com.revolsys.io.json.JsonMapIoFactory;
import com.revolsys.swing.DockingFramesUtil;
import com.revolsys.swing.map.MapPanel;
import com.revolsys.swing.map.form.DataObjectLayerFormFactory;
import com.revolsys.swing.map.layer.Layer;
import com.revolsys.swing.map.layer.LayerFactory;
import com.revolsys.swing.map.layer.LayerGroup;
import com.revolsys.swing.map.layer.Project;
import com.revolsys.swing.map.layer.arcgisrest.ArcGisServerRestLayer;
import com.revolsys.swing.map.layer.bing.BingLayer;
import com.revolsys.swing.map.layer.dataobject.DataObjectLayer;
import com.revolsys.swing.map.layer.dataobject.DataObjectListLayer;
import com.revolsys.swing.map.layer.dataobject.DataObjectStoreLayer;
import com.revolsys.swing.map.layer.geonames.GeoNamesBoundingBoxLayerWorker;
import com.revolsys.swing.map.layer.grid.GridLayer;
import com.revolsys.swing.map.layer.wikipedia.WikipediaBoundingBoxLayerWorker;
import com.revolsys.swing.map.table.DataObjectLayerTableModel;
import com.revolsys.swing.map.table.DataObjectListLayerTableModel;
import com.revolsys.swing.map.table.LayerTablePanelFactory;
import com.revolsys.swing.tree.ObjectTree;
import com.vividsolutions.jts.geom.Geometry;

public class LayerUtil {

  private static final Map<String, LayerFactory<?>> LAYER_FACTORIES = new HashMap<String, LayerFactory<?>>();

  private static final Map<Class<? extends Layer>, LayerTablePanelFactory> LAYER_TABLE_FACTORIES = new HashMap<Class<? extends Layer>, LayerTablePanelFactory>();

  static {
    addLayerFactory(DataObjectStoreLayer.FACTORY);
    addLayerFactory(ArcGisServerRestLayer.FACTORY);
    addLayerFactory(BingLayer.FACTORY);
    addLayerFactory(GridLayer.FACTORY);
    addLayerFactory(WikipediaBoundingBoxLayerWorker.FACTORY);
    addLayerFactory(GeoNamesBoundingBoxLayerWorker.FACTORY);

    addLayerTablePanelFactory(DataObjectLayerTableModel.FACTORY);
    addLayerTablePanelFactory(DataObjectListLayerTableModel.FACTORY);
  }

  public static void addLayerFactory(LayerFactory<?> factory) {
    String typeName = factory.getTypeName();
    LAYER_FACTORIES.put(typeName, factory);
  }

  public static void zoomToLayerSelected() {
    final Layer layer = ObjectTree.getMouseClickItem();
    if (layer != null) {
      Project project = layer.getProject();
      GeometryFactory geometryFactory = project.getGeometryFactory();
      BoundingBox boundingBox = layer.getSelectedBoundingBox()
        .convert(geometryFactory)
        .expandPercent(0.1);
      project.setViewBoundingBox(boundingBox);
    }
  }

  public static void zoomToLayer() {
    final Layer layer = ObjectTree.getMouseClickItem();
    if (layer != null) {
      Project project = layer.getProject();
      GeometryFactory geometryFactory = project.getGeometryFactory();
      BoundingBox boundingBox = layer.getBoundingBox()
        .convert(geometryFactory)
        .expandPercent(0.1);
      project.setViewBoundingBox(boundingBox);
    }
  }

  public static void showViewAttributes() {
    final Layer layer = ObjectTree.getMouseClickItem();
    if (layer != null) {
      DefaultSingleCDockable dockable;
      synchronized (layer) {
        dockable = layer.getProperty("TableView");
      }
      if (dockable == null) {
        Project project = layer.getProject();

        Component component = LayerUtil.getLayerTablePanel(layer);
        String id = layer.getClass().getName() + "." + layer.getId();
        dockable = DockingFramesUtil.addDockable(project,
          MapPanel.MAP_TABLE_WORKING_AREA, id, layer.getName(), component);

        dockable.setCloseable(true);
        layer.setProperty("TableView", dockable);
        dockable.addCDockableStateListener(new CDockableStateListener() {
          @Override
          public void extendedModeChanged(final CDockable dockable,
            final ExtendedMode mode) {
          }

          @Override
          public void visibilityChanged(final CDockable dockable) {
            final boolean visible = dockable.isVisible();
            if (!visible) {
              dockable.getControl()
                .getOwner()
                .remove((SingleCDockable)dockable);
              synchronized (layer) {
                layer.setProperty("TableView", null);
              }
            }
          }
        });
      }

      dockable.toFront();
    }
  }

  public static void addLayerTablePanelFactory(LayerTablePanelFactory factory) {
    Class<? extends Layer> layerClass = factory.getLayerClass();
    LAYER_TABLE_FACTORIES.put(layerClass, factory);
  }

  @SuppressWarnings("unchecked")
  public static <T extends Layer> T getLayer(
    final Map<String, Object> properties) {
    if (properties != null) {
      final String typeName = (String)properties.get("type");
      LayerFactory<?> layerFactory = getLayerFactory(typeName);
      if (layerFactory == null) {
        LoggerFactory.getLogger(LayerUtil.class).error(
          "No layer factory for " + typeName);
      } else {
        return (T)layerFactory.createLayer(properties);
      }
    }
    return null;
  }

  public static LayerFactory<?> getLayerFactory(final String typeName) {
    return LAYER_FACTORIES.get(typeName);
  }

  public static LayerTablePanelFactory getLayerTablePanelFactory(
    final Class<?> layerClass) {
    if (layerClass == null) {
      return null;
    } else {
      LayerTablePanelFactory factory = LAYER_TABLE_FACTORIES.get(layerClass);
      if (factory == null) {
        Class<?> superclass = layerClass.getSuperclass();
        factory = getLayerTablePanelFactory(superclass);
        if (factory == null) {
          Class<?>[] interfaces = layerClass.getInterfaces();
          for (Class<?> interfaceClass : interfaces) {
            factory = getLayerTablePanelFactory(interfaceClass);
            if (factory != null) {
              return factory;
            }
          }
        }
      }
      return factory;
    }
  }

  public static void loadLayer(final LayerGroup group, final File file) {
    try {
      final Map<String, Object> properties = JsonMapIoFactory.toMap(file);
      final Layer layer = getLayer(properties);
      if (layer != null) {
        group.add(layer);
      }
    } catch (Throwable t) {
      LoggerFactory.getLogger(LayerUtil.class).error(
        "Cannot load layer from " + file, t);
    }
  }

  public static void loadLayerGroup(final LayerGroup parent,
    final File directory) {
    for (final File file : directory.listFiles()) {
      final String name = file.getName();
      if (file.isDirectory()) {
        final LayerGroup group = parent.addLayerGroup(name);
        loadLayerGroup(group, file);
      } else {
        final String fileExtension = FileUtil.getFileNameExtension(file);
        if (fileExtension.equals("rglayer")) {
          loadLayer(parent, file);
        }
      }
    }
  }

  public static Component getLayerTablePanel(Layer layer) {
    Class<? extends Layer> layerClass = layer.getClass();
    LayerTablePanelFactory factory = getLayerTablePanelFactory(layerClass);
    if (factory != null) {
      return factory.createPanel(layer);
    }
    return null;
  }

  public static void openFile(File file) {
    Project project = Project.get();
    if (project != null) {
      LayerGroup firstGroup = project.getLayerGroups().get(0);
      String extension = FileUtil.getFileNameExtension(file);
      if ("rgmap".equals(extension)) {
        loadLayerGroup(firstGroup, file);
      } else if ("rglayer".equals(extension)) {
        loadLayer(firstGroup, file);
      } else {
        DataObjectReader reader = AbstractDataObjectReaderFactory.dataObjectReader(new FileSystemResource(
          file));
        try {
          DataObjectMetaData metaData = reader.getMetaData();
          GeometryFactory geometryFactory = metaData.getGeometryFactory();
          BoundingBox boundingBox = new BoundingBox(geometryFactory);
          DataObjectListLayer layer = new DataObjectListLayer(metaData);
          for (DataObject object : reader) {
            Geometry geometry = object.getGeometryValue();
            boundingBox.expandToInclude(geometry);
            layer.add(object);
          }
          layer.setBoundingBox(boundingBox);
          firstGroup.add(layer);
        } finally {
          reader.close();
        }
      }
    }

  }

  public static void openFiles(List<File> files) {
    for (File file : files) {
      openFile(file);
    }
  }

  private static Map<DataObject, DefaultSingleCDockable> forms = new HashMap<DataObject, DefaultSingleCDockable>();

  public static void showForm(DataObjectLayer layer, final DataObject object) {
    synchronized (forms) {

      DefaultSingleCDockable dockable = forms.get(object);
      if (dockable == null) {
        Project project = layer.getProject();
        if (project == null) {
          return;
        } else {
          DataObjectMetaData metaData = layer.getMetaData();
          Object id = object.getIdValue();
          String dockableId = metaData.getInstanceId() + "-" + id;
          Component form = DataObjectLayerFormFactory.createFormComponent(
            layer, object);
          dockable = DockingFramesUtil.addDockable(project,
            MapPanel.MAP_TABLE_WORKING_AREA, dockableId, metaData.getTypeName()
              + " (#" + id +")", form);
          Dimension size = form.getPreferredSize();
          dockable.setLocation(CLocation.external(50, 50, size.width +20,
            size.height+60));
          forms.put(object, dockable);
          dockable.addCDockableStateListener(new CDockableStateListener() {

            @Override
            public void visibilityChanged(CDockable dockable) {
              final boolean visible = dockable.isVisible();
              if (!visible) {
                CControlAccess controlAccess = dockable.getControl();
                if (controlAccess != null) {
                  CControl owner = controlAccess.getOwner();
                  if (owner != null) {
                    owner.remove((SingleCDockable)dockable);
                  }
                }
                synchronized (forms) {
                  forms.remove(object);
                }
              }
            }

            @Override
            public void extendedModeChanged(CDockable dockable,
              ExtendedMode mode) {

            }
          });
        }
      }
      dockable.setCloseable(true);
      dockable.toFront();
    }

  }
}
