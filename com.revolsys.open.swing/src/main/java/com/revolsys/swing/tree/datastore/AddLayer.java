package com.revolsys.swing.tree.datastore;

import java.awt.event.ActionEvent;
import java.util.List;

import com.revolsys.gis.data.io.DataObjectStore;
import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.swing.action.I18nAction;
import com.revolsys.swing.map.layer.Layer;
import com.revolsys.swing.map.layer.dataobject.DataObjectStoreLayer;
import com.revolsys.swing.map.util.LayerUtil;
import com.revolsys.swing.tree.TreeUtil;

public class AddLayer extends I18nAction {
  private static final long serialVersionUID = -4761946240388324500L;

  public AddLayer() {
    super("Add Layer");
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    final Object source = e.getSource();

    final List<DataObjectMetaData> types = TreeUtil.getSelectedNodes(source,
      DataObjectMetaData.class);
    for (final DataObjectMetaData metaData : types) {
      final String typePath = metaData.getPath();
      final Attribute geometryAttribute = metaData.getGeometryAttribute();
      if (geometryAttribute != null) {
        final DataObjectStore dataObjectStore = metaData.getDataObjectStore();
        final Layer layer = new DataObjectStoreLayer(dataObjectStore, typePath);
        LayerUtil.addLayer(layer);
      }
    }
  }

}
