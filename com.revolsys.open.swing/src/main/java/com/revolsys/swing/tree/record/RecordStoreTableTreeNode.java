package com.revolsys.swing.tree.record;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.revolsys.famfamfam.silk.SilkIconLoader;
import com.revolsys.io.PathUtil;
import com.revolsys.swing.map.layer.AbstractLayer;
import com.revolsys.swing.map.layer.Project;
import com.revolsys.swing.map.layer.record.RecordStoreLayer;
import com.revolsys.swing.menu.MenuFactory;
import com.revolsys.swing.tree.BaseTree;
import com.revolsys.swing.tree.model.node.LazyLoadTreeNode;
import com.revolsys.util.CaseConverter;

public class RecordStoreTableTreeNode extends LazyLoadTreeNode {

  public static final ImageIcon ICON_TABLE = SilkIconLoader.getIcon("table");

  public static Map<String, Icon> ICONS_GEOMETRY = new HashMap<String, Icon>();

  private static final MenuFactory MENU = new MenuFactory();

  static {
    for (final String geometryType : Arrays.asList("Point", "MultiPoint",
      "LineString", "MultiLineString", "Polygon", "MultiPolygon")) {
      ICONS_GEOMETRY.put(geometryType,
        SilkIconLoader.getIcon("table_" + geometryType.toLowerCase()));
    }

    MENU.addMenuItemTitleIcon("default", "Add Layer", "map_add", null,
      RecordStoreTableTreeNode.class, "addLayer");

  }

  public static void addLayer() {
    final RecordStoreTableTreeNode node = BaseTree.getMouseClickItem();
    final String typePath = node.getTypePath();
    final RecordStoreSchemaTreeNode schemaNode = node.getParentNode();
    final Map<String, Object> connection = schemaNode.getRecordStoreConnectionMap();
    final Map<String, Object> layerConfig = new LinkedHashMap<String, Object>();
    layerConfig.put("type", "recordStore");
    layerConfig.put("name", node.getName());
    layerConfig.put("connection", connection);
    layerConfig.put("typePath", typePath);
    final AbstractLayer layer = RecordStoreLayer.create(layerConfig);
    Project.get().add(layer);
    // TODO different layer groups?
  }

  public RecordStoreTableTreeNode(
    final RecordStoreSchemaTreeNode parent, final String typePath,
    final String geometryType) {
    super(parent, typePath);
    if (geometryType == null) {
      setType("Data Table");
    } else {
      setType("Data Table (" + CaseConverter.toCapitalizedWords(geometryType)
        + ")");
    }
    setAllowsChildren(false);
    setParent(parent);

    final String name = PathUtil.getName(typePath);
    setName(name);

    Icon icon = ICONS_GEOMETRY.get(geometryType);
    if (icon == null) {
      icon = ICON_TABLE;
    }
    setIcon(icon);
  }

  @Override
  public MenuFactory getMenu() {
    return MENU;
  }

  public String getTypePath() {
    return (String)getUserObject();
  }
}