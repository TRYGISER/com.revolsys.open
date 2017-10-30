package com.revolsys.swing.map.layer;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import com.revolsys.beans.PropertyChangeSupportProxy;
import com.revolsys.collection.Child;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactoryProxy;
import com.revolsys.io.map.MapSerializer;
import com.revolsys.properties.ObjectWithProperties;
import com.revolsys.swing.component.TabbedValuePanel;
import com.revolsys.swing.map.MapPanel;
import com.revolsys.swing.map.Viewport2D;

public interface Layer
  extends GeometryFactoryProxy, PropertyChangeSupportProxy, ObjectWithProperties,
  PropertyChangeListener, Comparable<Layer>, MapSerializer, Child<LayerGroup>, Cloneable {

  void delete();

  BoundingBox getBoundingBox();

  BoundingBox getBoundingBox(boolean visibleLayersOnly);

  Collection<Class<?>> getChildClasses();

  Icon getIcon();

  long getId();

  LayerGroup getLayerGroup();

  default MapPanel getMapPanel() {
    final LayerGroup project = getProject();
    if (project == null) {
      return null;
    } else {
      return project.getProperty(MapPanel.MAP_PANEL);
    }
  }

  long getMaximumScale();

  long getMinimumScale();

  String getName();

  /**
   * Get the path from the root project. The name of the layer group at the root is not included.
   * @return
   */
  String getPath();

  List<Layer> getPathList();

  Project getProject();

  <L extends LayerRenderer<? extends Layer>> L getRenderer();

  BoundingBox getSelectedBoundingBox();

  String getType();

  default Viewport2D getViewport() {
    final MapPanel mapPanel = getMapPanel();
    if (mapPanel == null) {
      return null;
    } else {
      return mapPanel.getViewport();
    }
  }

  void initialize();

  boolean isClonable();

  boolean isDeleted();

  boolean isEditable();

  boolean isEditable(double scale);

  boolean isExists();

  default boolean isHasChanges() {
    return false;
  }

  default boolean isHasGeometry() {
    return true;
  }

  boolean isHasSelectedRecords();

  boolean isInitialized();

  boolean isOpen();

  boolean isQueryable();

  boolean isQuerySupported();

  boolean isReadOnly();

  boolean isSelectable();

  boolean isSelectable(double scale);

  boolean isSelectSupported();

  boolean isVisible();

  boolean isVisible(double scale);

  TabbedValuePanel newPropertiesPanel();

  void refresh();

  void refreshAll();

  boolean saveChanges();

  boolean saveSettings(Path directory);

  void setEditable(boolean editable);

  void setLayerGroup(LayerGroup layerGroup);

  /**
   * Set the maximum scale. This is the scale that if you zoom in to a more
   * detailed scale than the maximum scale the layer will not be visible. This
   * is inverse from the logical definition of maximum. If scale < maximumScale
   * it will not be shown.
   */
  void setMaximumScale(long maximumScale);

  /**
   * Set the minimum scale. This is the scale that if you zoom out to a less
   * detailed scale than the minimum scale the layer will not be visible. This
   * is inverse from the logical definition of minimum. If scale > minimumScale
   * it will not be shown.
   */
  void setMinimumScale(long minimumScale);

  void setName(String name);

  void setOpen(boolean open);

  void setQueryable(boolean b);

  void setReadOnly(boolean readOnly);

  void setRenderer(final LayerRenderer<? extends Layer> renderer);

  void setSelectable(boolean selectable);

  void setVisible(boolean visible);

  void showProperties();

  void showProperties(String tabName);

  void showRendererProperties(final LayerRenderer<?> renderer);

  void showTableView();

  <C extends Component> C showTableView(Map<String, Object> config);
}
