package com.revolsys.io.connection;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.revolsys.beans.PropertyChangeSupportProxy;
import com.revolsys.collection.map.MapEx;
import com.revolsys.collection.map.Maps;
import com.revolsys.io.FileUtil;
import com.revolsys.record.io.format.json.Json;
import com.revolsys.spring.resource.FileSystemResource;
import com.revolsys.spring.resource.PathResource;
import com.revolsys.spring.resource.Resource;
import com.revolsys.util.Property;

public abstract class AbstractConnectionRegistry<C extends Connection>
  implements ConnectionRegistry<C>, PropertyChangeListener {

  private ConnectionRegistryManager<ConnectionRegistry<C>> connectionManager;

  private final Map<String, String> connectionNames = new TreeMap<>();

  private Map<String, C> connections;

  private File directory;

  private final String fileExtension;

  private final String name;

  private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  private boolean readOnly;

  private boolean visible = true;

  public AbstractConnectionRegistry(
    final ConnectionRegistryManager<? extends ConnectionRegistry<C>> connectionManager,
    final String name, final boolean visible, final boolean readOnly,
    final Resource directoryResource, final String fileExtension) {
    this.name = name;
    this.fileExtension = fileExtension;
    setConnectionManager(connectionManager);
    setVisible(visible);
    setReadOnly(readOnly);
    setDirectory(directoryResource);
    init();
  }

  protected synchronized void addConnection(final String name, final C connection) {
    if (connection != null && name != null) {
      final String lowerName = name.toLowerCase();
      final C existingConnection = this.connections.get(lowerName);
      removeConnection(existingConnection);
      this.connectionNames.put(lowerName, name);
      this.connections.put(lowerName, connection);
      if (connection instanceof PropertyChangeSupportProxy) {
        final PropertyChangeSupportProxy proxy = (PropertyChangeSupportProxy)connection;
        final PropertyChangeSupport propertyChangeSupport = proxy.getPropertyChangeSupport();
        if (propertyChangeSupport != null) {
          propertyChangeSupport.addPropertyChangeListener(this);
        }
      }
      final int index = getConnectionIndex(name);
      this.propertyChangeSupport.fireIndexedPropertyChange("connections", index, null, connection);
      this.propertyChangeSupport.fireIndexedPropertyChange("children", index, null, connection);
    }
  }

  @Override
  public C getConnection(final String connectionName) {
    if (Property.hasValue(connectionName)) {
      return this.connections.get(connectionName.toLowerCase());
    } else {
      return null;
    }
  }

  protected File getConnectionFile(final Connection connection, final boolean useOriginalFile) {
    File connectionFile = null;
    if (useOriginalFile) {
      connectionFile = connection.getConnectionFile();
    }
    if (connectionFile == null) {
      final String connectionName = connection.getName();
      connectionFile = getConnectionFile(connectionName);
    }
    return connectionFile;
  }

  protected File getConnectionFile(final String name) {
    if (Property.hasValue(name)) {
      if (!this.directory.exists()) {
        if (isReadOnly()) {
          return null;
        } else if (!this.directory.mkdirs()) {
          return null;
        }
      }
      final String fileName = FileUtil.toSafeName(name) + "." + this.fileExtension;
      final File file = new File(this.directory, fileName);
      return file;
    } else {
      return null;
    }
  }

  protected int getConnectionIndex(final String name) {
    final String lowerName = name.toLowerCase();
    final int index = new ArrayList<>(this.connectionNames.keySet()).indexOf(lowerName);
    return index;
  }

  @Override
  public ConnectionRegistryManager<ConnectionRegistry<C>> getConnectionManager() {
    return this.connectionManager;
  }

  public synchronized String getConnectionName(final C connection) {
    for (final Entry<String, C> entry : this.connections.entrySet()) {
      if (entry.getValue() == connection) {
        final String lowerName = entry.getKey();
        return this.connectionNames.get(lowerName);
      }
    }
    return null;
  }

  protected String getConnectionName(final MapEx config, final File connectionFile,
    final boolean requireUniqueNames) {
    String name = config.getString("name");
    if (connectionFile != null && !Property.hasValue(name)) {
      name = FileUtil.getBaseName(connectionFile);
    }
    if (requireUniqueNames) {
      name = getUniqueName(name);
    }
    config.put("name", name);
    return name;
  }

  @Override
  public List<String> getConnectionNames() {
    final List<String> names = new ArrayList<>(this.connectionNames.values());
    return names;
  }

  @Override
  public List<C> getConnections() {
    return new ArrayList<>(this.connections.values());
  }

  public File getDirectory() {
    return this.directory;
  }

  @Override
  public String getFileExtension() {
    return this.fileExtension;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public PropertyChangeSupport getPropertyChangeSupport() {
    return this.propertyChangeSupport;
  }

  protected String getUniqueName(String name) {
    int i = 1;
    String newName = name;
    while (getConnection(newName) != null) {
      newName = name + i;
      i++;
    }
    name = newName;
    return name;
  }

  @Override
  public void importConnection(final File file) {
    if (file != null && file.isFile()) {
      loadConnection(file, true);
    }
  }

  protected synchronized void init() {
    this.connections = new TreeMap<>();
    initDo();
  }

  protected void initDo() {
    if (this.directory != null && this.directory.isDirectory()) {
      for (final File connectionFile : FileUtil.getFilesByExtension(this.directory,
        this.fileExtension, "rgobject")) {
        loadConnection(connectionFile, false);
      }
    }
  }

  @Override
  public boolean isReadOnly() {
    return this.readOnly;
  }

  @Override
  public boolean isVisible() {
    return this.visible;
  }

  protected abstract C loadConnection(final File connectionFile, boolean importConnection);

  @Override
  public C newConnection(final Map<String, ? extends Object> connectionParameters) {
    final String name = Maps.getString(connectionParameters, "name");
    final File file = getConnectionFile(name);
    if (file != null && (!file.exists() || file.canRead())) {
      Json.writeMap(connectionParameters, file, true);
      return loadConnection(file, false);
    }
    return null;
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    this.propertyChangeSupport.firePropertyChange(event);
  }

  @Override
  public boolean removeConnection(final Connection connection) {
    if (connection == null) {
      return false;
    } else {
      final String name = connection.getName();
      return removeConnection(name, connection);
    }
  }

  public boolean removeConnection(final String name) {
    final C connection = getConnection(name);
    return removeConnection(connection);
  }

  protected synchronized boolean removeConnection(final String name, final Connection connection) {
    if (connection != null && name != null) {
      final String lowerName = name.toLowerCase();
      final C existingConnection = this.connections.get(lowerName);
      if (existingConnection == connection) {
        final int index = getConnectionIndex(name);
        this.connectionNames.remove(lowerName);
        this.connections.remove(lowerName);
        if (connection instanceof PropertyChangeSupportProxy) {
          final PropertyChangeSupportProxy proxy = (PropertyChangeSupportProxy)connection;
          final PropertyChangeSupport propertyChangeSupport = proxy.getPropertyChangeSupport();
          if (propertyChangeSupport != null) {
            propertyChangeSupport.removePropertyChangeListener(this);
          }
        }
        this.propertyChangeSupport.fireIndexedPropertyChange("connections", index, connection,
          null);
        this.propertyChangeSupport.fireIndexedPropertyChange("children", index, connection, null);
        if (this.directory != null && !this.readOnly) {
          final File file = existingConnection.getConnectionFile();
          FileUtil.deleteDirectory(file);
        }
        return true;
      }
    }
    return false;
  }

  public void save() {
    saveDo(true);
  }

  public void saveAs(final Resource directory) {
    setDirectory(directory);
    saveDo(false);
  }

  public void saveAs(final Resource parentDirectory, final String directoryName) {
    final Resource connectionsDirectory = parentDirectory.newChildResource(directoryName);
    saveAs(connectionsDirectory);
  }

  private void saveDo(final boolean useOriginalFile) {
    for (final Connection connection : this.connections.values()) {
      final File connectionFile = getConnectionFile(connection, useOriginalFile);
      final String name = connection.getName();
      if (Property.hasValue(name)) {
        connection.writeToFile(connectionFile);
      } else {
        throw new IllegalArgumentException("Connection must have a name");
      }
    }
  }

  @Override
  public void setConnectionManager(
    final ConnectionRegistryManager<? extends ConnectionRegistry<C>> connectionManager) {
    if (this.connectionManager != connectionManager) {
      if (this.connectionManager != null) {
        this.propertyChangeSupport.removePropertyChangeListener(connectionManager);
      }
      this.connectionManager = (ConnectionRegistryManager)connectionManager;
      if (connectionManager != null) {
        this.propertyChangeSupport.addPropertyChangeListener(connectionManager);
      }
    }
  }

  protected void setDirectory(final Resource directoryResource) {
    if (directoryResource instanceof FileSystemResource) {
      final FileSystemResource fileResource = (FileSystemResource)directoryResource;
      final File directory = fileResource.getFile();
      boolean readOnly = isReadOnly();
      if (!readOnly) {
        if (directoryResource.exists()) {
          readOnly = !directory.canWrite();
        } else if (directory.mkdirs()) {
          readOnly = false;
        } else {
          readOnly = true;
        }
      }
      setReadOnly(readOnly);
      this.directory = directory;
    } else if (directoryResource instanceof PathResource) {
      final PathResource pathResource = (PathResource)directoryResource;
      final File directory = pathResource.getFile();
      boolean readOnly = isReadOnly();
      if (!readOnly) {
        if (directoryResource.exists()) {
          readOnly = !directory.canWrite();
        } else if (directory.mkdirs()) {
          readOnly = false;
        } else {
          readOnly = true;
        }
      }
      setReadOnly(readOnly);
      this.directory = directory;
    } else {
      setReadOnly(true);
      this.directory = null;
    }
  }

  public void setReadOnly(final boolean readOnly) {
    if (this.isReadOnly() && !readOnly) {
      throw new IllegalArgumentException("Cannot make a read only registry not read only");
    }
    this.readOnly = readOnly;
  }

  public void setVisible(final boolean visible) {
    this.visible = visible;
  }

  @Override
  public String toString() {
    return getName();
  }
}
