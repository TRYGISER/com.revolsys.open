package com.revolsys.record.io;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import com.revolsys.io.AbstractIoFactoryWithCoordinateSystem;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordStore;
import com.revolsys.spring.resource.Resource;

public class RecordStoreRecordAndGeometryWriterFactory extends AbstractIoFactoryWithCoordinateSystem
  implements RecordWriterFactory {

  public RecordStoreRecordAndGeometryWriterFactory(final String name, final String mediaType,
    final boolean geometrySupported, final boolean customAttributionSupported,
    final Iterable<String> fileExtensions) {
    super(name);
    for (final String fileExtension : fileExtensions) {
      addMediaTypeAndFileExtension(mediaType, fileExtension);
    }
  }

  public RecordStoreRecordAndGeometryWriterFactory(final String name, final String mediaType,
    final boolean geometrySupported, final boolean customAttributionSupported,
    final String... fileExtensions) {
    this(name, mediaType, geometrySupported, customAttributionSupported,
      Arrays.asList(fileExtensions));
  }

  @Override
  public RecordWriter newRecordWriter(final RecordDefinition recordDefinition,
    final Resource resource) {
    final File file = resource.getFile();
    final RecordStore recordStore = RecordStore.newRecordStore(file);
    if (recordStore == null) {
      return null;
    } else {
      recordStore.initialize();
      return new RecordStoreRecordWriter(recordStore, recordDefinition);
    }
  }

  @Override
  public RecordWriter newRecordWriter(final String baseName,
    final RecordDefinition recordDefinition, final OutputStream outputStream,
    final Charset charset) {
    throw new UnsupportedOperationException("Writing to a stream not currently supported");
  }
}
