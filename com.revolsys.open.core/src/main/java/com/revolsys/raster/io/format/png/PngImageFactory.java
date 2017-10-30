package com.revolsys.raster.io.format.png;

import com.revolsys.io.AbstractIoFactory;
import com.revolsys.raster.GeoreferencedImage;
import com.revolsys.raster.GeoreferencedImageReadFactory;
import com.revolsys.spring.resource.Resource;

public class PngImageFactory extends AbstractIoFactory implements GeoreferencedImageReadFactory {

  public PngImageFactory() {
    super("PNG");
    addMediaTypeAndFileExtension("image/png", "png");
  }

  @Override
  public GeoreferencedImage loadImage(final Resource resource) {
    return new PngImage(resource);
  }

}
