package com.revolsys.raster;

import com.revolsys.io.IoFactory;
import com.revolsys.io.ReadIoFactory;
import com.revolsys.spring.resource.Resource;

public interface GeoreferencedImageReadFactory extends ReadIoFactory {
  static GeoreferencedImage loadGeoreferencedImage(final Resource resource) {
    final GeoreferencedImageReadFactory factory = IoFactory
      .factory(GeoreferencedImageReadFactory.class, resource);
    if (factory == null) {
      return null;
    } else {
      final GeoreferencedImage reader = factory.loadImage(resource);
      return reader;
    }
  }

  @Override
  default boolean isReadFromZipFileSupported() {
    return true;
  }

  GeoreferencedImage loadImage(Resource resource);
}
