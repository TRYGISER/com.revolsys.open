package com.revolsys.io.connection;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.revolsys.beans.PropertyChangeSupportProxy;
import com.revolsys.collection.NameProxy;
import com.revolsys.collection.Parent;

public interface ConnectionRegistry<C extends Connection>
  extends PropertyChangeSupportProxy, Parent<C>, NameProxy {
  @Override
  default List<C> getChildren() {
    return getConnections();
  }

  C getConnection(final String connectionName);

  ConnectionRegistryManager<ConnectionRegistry<C>> getConnectionManager();

  List<String> getConnectionNames();

  List<C> getConnections();

  String getFileExtension();

  void importConnection(File file);

  default boolean isEditable() {
    return !isReadOnly();
  }

  boolean isReadOnly();

  boolean isVisible();

  C newConnection(Map<String, ? extends Object> connectionParameters);

  boolean removeConnection(Connection connection);

  void setConnectionManager(
    ConnectionRegistryManager<? extends ConnectionRegistry<C>> connectionManager);
}
