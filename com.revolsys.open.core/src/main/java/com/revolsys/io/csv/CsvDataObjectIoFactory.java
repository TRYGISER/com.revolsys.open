package com.revolsys.io.csv;

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;

import com.revolsys.gis.data.io.AbstractDataObjectIoFactory;
import com.revolsys.gis.data.io.DataObjectIteratorReader;
import com.revolsys.gis.data.io.DataObjectReader;
import com.revolsys.gis.data.io.DataObjectStore;
import com.revolsys.gis.data.io.DataObjectStoreFactory;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectFactory;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.io.DirectoryDataObjectStore;
import com.revolsys.io.Writer;
import com.revolsys.spring.SpringUtil;

public class CsvDataObjectIoFactory extends AbstractDataObjectIoFactory
  implements DataObjectStoreFactory {
  public CsvDataObjectIoFactory() {
    super(CsvConstants.DESCRIPTION, false, true, true);
    addMediaTypeAndFileExtension(CsvConstants.MEDIA_TYPE,
      CsvConstants.FILE_EXTENSION);
  }

  @Override
  public DataObjectReader createDataObjectReader(final Resource resource,
    final DataObjectFactory dataObjectFactory) {
    final CsvDataObjectIterator iterator = new CsvDataObjectIterator(resource,
      dataObjectFactory);
    return new DataObjectIteratorReader(iterator);
  }

  @Override
  public DataObjectStore createDataObjectStore(
    final Map<String, ? extends Object> connectionProperties) {
    final String url = (String)connectionProperties.get("url");
    final Resource resource = SpringUtil.getResource(url);
    final File directory = SpringUtil.getFile(resource);
    return new DirectoryDataObjectStore(directory, "csv");
  }

  @Override
  public Writer<DataObject> createDataObjectWriter(final String baseName,
    final DataObjectMetaData metaData, final OutputStream outputStream,
    final Charset charset) {
    final OutputStreamWriter writer = new OutputStreamWriter(outputStream,
      charset);

    return new CsvDataObjectWriter(metaData, writer);
  }

  @Override
  public Class<? extends DataObjectStore> getDataObjectStoreInterfaceClass(
    final Map<String, ? extends Object> connectionProperties) {
    return DataObjectStore.class;
  }

  @Override
  public List<String> getUrlPatterns() {
    // TODO Auto-generated method stub
    return null;
  }
}