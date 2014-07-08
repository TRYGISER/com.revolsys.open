package com.revolsys.gis.esri.gdb.file;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;

import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.io.DataObjectStoreSchema;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectMetaDataImpl;
import com.revolsys.gis.data.model.types.DataTypes;

public class FeatureDatasetTest {

  @Test
  public void testCreateGeodatabase() throws Exception {
    final String path = "/test/Point";
    final DataObjectMetaDataImpl newMetaData = new DataObjectMetaDataImpl(path);
    newMetaData.addAttribute("id", DataTypes.INT, false);
    newMetaData.addAttribute("name", DataTypes.STRING, 255, false);
    newMetaData.addAttribute("geometry", DataTypes.POINT, true);
    newMetaData.setIdAttributeName("id");
    final GeometryFactory geometryFactory = GeometryFactory.getFactory(4326);
    newMetaData.setGeometryFactory(geometryFactory);

    final String datasetName = "target/Create.gdb";
    FileGdbDataObjectStore dataStore = FileGdbDataObjectStoreFactory.create(new File(
      datasetName));
    try {
      dataStore.setCreateMissingTables(true);
      dataStore.setCreateMissingDataStore(true);
      dataStore.initialize();
      dataStore.setDefaultSchema("test");
      Assert.assertEquals("Initial Schema Size", 1, dataStore.getSchemas()
        .size());
      final DataObjectMetaData metaData = dataStore.getMetaData(newMetaData);
      Assert.assertNotNull("Created Metadata", metaData);

      final DataObject object = dataStore.create(newMetaData);
      object.setIdValue(1);
      object.setValue("name", "Paul Austin");
      object.setGeometryValue(geometryFactory.createPoint(-122, 150));
      dataStore.insert(object);
      for (final DataObject object2 : dataStore.query(path)) {
        System.out.println(object2);
      }
      dataStore.close();

      dataStore = FileGdbDataObjectStoreFactory.create(new File(datasetName));
      dataStore.initialize();
      dataStore.setDefaultSchema("test");
      final DataObjectStoreSchema schema = dataStore.getSchema("test");
      for (final DataObjectMetaData metaData2 : schema.getTypes()) {
        System.out.println(metaData2);
      }
    } finally {
      dataStore.deleteGeodatabase();
    }
  }
}
