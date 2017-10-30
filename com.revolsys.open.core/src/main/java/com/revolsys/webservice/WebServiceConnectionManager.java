package com.revolsys.webservice;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.revolsys.io.connection.AbstractConnectionRegistryManager;
import com.revolsys.spring.resource.FileSystemResource;
import com.revolsys.spring.resource.Resource;
import com.revolsys.util.OS;

public class WebServiceConnectionManager
  extends AbstractConnectionRegistryManager<WebServiceConnectionRegistry, WebServiceConnection> {
  public static final String WEB_SERVICES = "Web Services";

  private static final WebServiceConnectionManager INSTANCE;

  static {
    INSTANCE = new WebServiceConnectionManager();
    final File webServicesDirectory = OS
      .getApplicationDataDirectory("com.revolsys.gis/Web Services");
    INSTANCE.addConnectionRegistry("User", new FileSystemResource(webServicesDirectory));
  }

  private static Function<WebServiceConnection, Boolean> invalidWebServiceFunction;

  @SuppressWarnings("rawtypes")
  private static Function<String, WebService> missingWebServiceFunction;

  public static WebServiceConnectionManager get() {
    return INSTANCE;
  }

  public static Function<WebServiceConnection, Boolean> getInvalidWebServiceFunction() {
    return WebServiceConnectionManager.invalidWebServiceFunction;
  }

  @SuppressWarnings({
    "unchecked"
  })
  public static <W extends WebService<?>> W getWebService(final String name) {
    final WebServiceConnectionManager connectionManager = get();
    final List<WebServiceConnectionRegistry> registries = new ArrayList<>();
    registries.addAll(connectionManager.getConnectionRegistries());
    final WebServiceConnectionRegistry threadRegistry = WebServiceConnectionRegistry.getForThread();
    if (threadRegistry != null) {
      registries.add(threadRegistry);
    }
    Collections.reverse(registries);
    for (final WebServiceConnectionRegistry registry : registries) {
      final WebServiceConnection webServiceConnection = registry.getConnection(name);
      if (webServiceConnection != null) {
        return webServiceConnection.getWebService();
      }
    }
    if (missingWebServiceFunction == null) {
      return null;
    } else {
      return (W)missingWebServiceFunction.apply(name);
    }
  }

  public static void setInvalidWebServiceFunction(
    final Function<WebServiceConnection, Boolean> invalidWebServiceFunction) {
    WebServiceConnectionManager.invalidWebServiceFunction = invalidWebServiceFunction;
  }

  @SuppressWarnings("rawtypes")
  public static void setMissingWebServiceFunction(
    final Function<String, WebService> missingWebServiceFunction) {
    WebServiceConnectionManager.missingWebServiceFunction = missingWebServiceFunction;
  }

  public WebServiceConnectionManager() {
    super(WEB_SERVICES);
  }

  public WebServiceConnectionRegistry addConnectionRegistry(final String name,
    final boolean visible) {
    final WebServiceConnectionRegistry registry = new WebServiceConnectionRegistry(this, name,
      visible);
    addConnectionRegistry(registry);
    return registry;
  }

  public WebServiceConnectionRegistry addConnectionRegistry(final String name,
    final Resource recordStoresDirectory) {
    final WebServiceConnectionRegistry registry = new WebServiceConnectionRegistry(this, name,
      recordStoresDirectory);
    addConnectionRegistry(registry);
    return registry;
  }

  @Override
  public String getIconName() {
    return "folder:world";
  }
}
