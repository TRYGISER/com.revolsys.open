package com.revolsys.format.directory;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.revolsys.collection.AbstractIterator;
import com.revolsys.data.io.RecordIo;
import com.revolsys.data.io.RecordReader;
import com.revolsys.data.query.Query;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.AbstractRecordStore;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.data.record.schema.RecordStore;
import com.revolsys.data.record.schema.RecordStoreSchema;
import com.revolsys.data.record.schema.RecordStoreSchemaElement;
import com.revolsys.io.ObjectWithProperties;
import com.revolsys.io.Path;
import com.revolsys.io.Writer;
import com.revolsys.io.filter.ExtensionFilenameFilter;
import com.revolsys.spring.SpringUtil;

public class DirectoryRecordStore extends AbstractRecordStore {

  private boolean createMissingTables = true;

  private final Map<String, Writer<Record>> writers = new HashMap<>();

  private File directory;

  private List<String> fileExtensions;

  private Writer<Record> writer;

  private boolean createMissingRecordStore = true;

  private final Map<RecordDefinition, Resource> resourcesByRecordDefinition = new HashMap<>();

  private final Map<Resource, String> typePathByResource = new HashMap<>();

  public DirectoryRecordStore(final File directory,
    final Collection<String> fileExtensions) {
    this.directory = directory;
    this.fileExtensions = new ArrayList<>(fileExtensions);
  }

  public DirectoryRecordStore(final File directory,
    final String... fileExtensions) {
    this(directory, Arrays.asList(fileExtensions));
  }

  @Override
  public void close() {
    directory = null;
    if (writers != null) {
      for (final Writer<Record> writer : writers.values()) {
        writer.close();
      }
      writers.clear();
    }
    if (writer != null) {
      writer.close();
      writer = null;
    }
    super.close();
  }

  @Override
  public AbstractIterator<Record> createIterator(final Query query,
    final Map<String, Object> properties) {
    final String path = query.getTypeName();
    final RecordReader reader = query(path);
    reader.setProperties(properties);
    return new RecordReaderQueryIterator(reader, query);
  }

  @Override
  public Writer<Record> createWriter() {
    return new DirectoryRecordStoreWriter(this);
  }

  @Override
  public void delete(final Record record) {
    final RecordDefinition recordDefinition = record.getRecordDefinition();
    final RecordStore recordStore = recordDefinition.getRecordStore();
    if (recordStore == this) {
      throw new UnsupportedOperationException("Deleting records not supported");
    }
  }

  public File getDirectory() {
    return directory;
  }

  public String getFileExtension() {
    return getFileExtensions().get(0);
  }

  public List<String> getFileExtensions() {
    return fileExtensions;
  }

  @Override
  public RecordDefinition getRecordDefinition(
    final RecordDefinition recordDefinition) {
    final RecordDefinition storeRecordDefinition = super.getRecordDefinition(recordDefinition);
    if (storeRecordDefinition == null && createMissingTables) {
      final String typePath = recordDefinition.getPath();
      final String schemaPath = Path.getPath(typePath);
      RecordStoreSchema schema = getSchema(schemaPath);
      if (schema == null && createMissingTables) {
        final RecordStoreSchema rootSchema = getRootSchema();
        schema = rootSchema.createSchema(schemaPath);
      }
      final File schemaDirectory = new File(directory, schemaPath);
      if (!schemaDirectory.exists()) {
        schemaDirectory.mkdirs();
      }
      final RecordDefinitionImpl newRecordDefinition = new RecordDefinitionImpl(
        schema, typePath);
      for (final FieldDefinition field : recordDefinition.getFields()) {
        final FieldDefinition newField = new FieldDefinition(field);
        newRecordDefinition.addField(newField);
      }
      schema.addElement(newRecordDefinition);
      return newRecordDefinition;
    }
    return storeRecordDefinition;
  }

  protected Resource getResource(final String path) {
    final RecordDefinition recordDefinition = getRecordDefinition(path);
    return getResource(path, recordDefinition);
  }

  protected Resource getResource(final String path,
    final RecordDefinition recordDefinition) {
    if (recordDefinition == null) {
      throw new IllegalArgumentException("Table does not exist " + path);
    }
    final Resource resource = resourcesByRecordDefinition.get(recordDefinition);
    if (resource == null) {
      throw new IllegalArgumentException("File does not exist for " + path);
    }
    return resource;
  }

  @Override
  public int getRowCount(final Query query) {
    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized Writer<Record> getWriter() {
    if (writer == null && directory != null) {
      writer = new DirectoryRecordStoreWriter(this);
    }
    return writer;
  }

  @PostConstruct
  @Override
  public void initialize() {
    if (!directory.exists()) {
      directory.mkdirs();
    }
    super.initialize();
  }

  @Override
  public synchronized void insert(final Record record) {
    final RecordDefinition recordDefinition = record.getRecordDefinition();
    final String typePath = recordDefinition.getPath();
    Writer<Record> writer = writers.get(typePath);
    if (writer == null) {
      final String schemaName = Path.getPath(typePath);
      final File subDirectory = new File(getDirectory(), schemaName);
      final String fileExtension = getFileExtension();
      final File file = new File(subDirectory, recordDefinition.getName() + "."
        + fileExtension);
      final Resource resource = new FileSystemResource(file);
      writer = RecordIo.recordWriter(recordDefinition, resource);
      if (writer instanceof ObjectWithProperties) {
        final ObjectWithProperties properties = writer;
        properties.setProperties(getProperties());
      }
      writers.put(typePath, writer);
    }
    writer.write(record);
    addStatistic("Insert", record);
  }

  public boolean isCreateMissingRecordStore() {
    return createMissingRecordStore;
  }

  public boolean isCreateMissingTables() {
    return createMissingTables;
  }

  protected RecordDefinition loadRecordDefinition(
    final RecordStoreSchema schema, final String schemaName,
    final Resource resource) {
    try (
      RecordReader recordReader = RecordIo.recordReader(resource)) {
      final String typePath = Path.toPath(schemaName,
        SpringUtil.getBaseName(resource));
      recordReader.setProperty("schema", schema);
      recordReader.setProperty("typePath", typePath);
      final RecordDefinition recordDefinition = recordReader.getRecordDefinition();
      if (recordDefinition != null) {
        resourcesByRecordDefinition.put(recordDefinition, resource);
        typePathByResource.put(resource, typePath);
      }
      return recordDefinition;
    }
  }

  @Override
  public RecordReader query(final String path) {
    final RecordDefinition recordDefinition = getRecordDefinition(path);
    final Resource resource = getResource(path, recordDefinition);
    final RecordReader reader = RecordIo.recordReader(resource);
    if (reader == null) {
      throw new IllegalArgumentException("Cannot find reader for: " + path);
    } else {
      final String typePath = typePathByResource.get(resource);
      reader.setProperty("schema", recordDefinition.getSchema());
      reader.setProperty("typePath", typePath);
      return reader;
    }
  }

  @Override
  protected Map<String, RecordStoreSchemaElement> refreshSchemaElements(
    final RecordStoreSchema schema) {
    final Map<String, RecordStoreSchemaElement> elements = new TreeMap<>();
    final String schemaPath = schema.getPath();
    final File subDirectory;
    if (schemaPath.equals("/")) {
      subDirectory = directory;
    } else {
      subDirectory = new File(directory, schemaPath);
    }
    final FileFilter filter = new ExtensionFilenameFilter(fileExtensions);
    final File[] files = subDirectory.listFiles();
    if (files != null) {
      for (final File file : files) {
        if (filter.accept(file)) {
          final FileSystemResource resource = new FileSystemResource(file);
          final RecordDefinition recordDefinition = loadRecordDefinition(
            schema, schemaPath, resource);
          elements.put(recordDefinition.getPath().toUpperCase(),
            recordDefinition);
        } else if (file.isDirectory()) {
          String childSchemaPath = file.getName();
          if (schemaPath.equals("/")) {
            childSchemaPath = "/" + childSchemaPath;
          } else {
            childSchemaPath = schemaPath + "/" + childSchemaPath;
          }
          RecordStoreSchema childSchema = schema.getSchema(childSchemaPath);
          if (childSchema == null) {
            childSchema = new RecordStoreSchema(schema, childSchemaPath);
          } else {
            if (!childSchema.isInitialized()) {
              childSchema.refresh();
            }
          }
          elements.put(childSchemaPath.toUpperCase(), childSchema);
        }
      }
    }
    return elements;
  }

  public void setCreateMissingRecordStore(final boolean createMissingRecordStore) {
    this.createMissingRecordStore = createMissingRecordStore;
  }

  public void setCreateMissingTables(final boolean createMissingTables) {
    this.createMissingTables = createMissingTables;
  }

  public void setDirectory(final File directory) {
    this.directory = directory;
  }

  protected void setFileExtensions(final List<String> fileExtensions) {
    this.fileExtensions = fileExtensions;
  }

  protected void superDelete(final Record record) {
    super.delete(record);
  }

  protected void superUpdate(final Record record) {
    super.update(record);
  }

  @Override
  public String toString() {
    final String fileExtension = getFileExtension();
    return fileExtension + " " + directory;
  }

  @Override
  public void update(final Record record) {
    final RecordDefinition recordDefinition = record.getRecordDefinition();
    final RecordStore recordStore = recordDefinition.getRecordStore();
    if (recordStore == this) {
      switch (record.getState()) {
        case Deleted:
        break;
        case Persisted:
        break;
        case Modified:
          throw new UnsupportedOperationException();
        default:
          insert(record);
        break;
      }
    } else {
      insert(record);
    }
  }
}
