package com.revolsys.raster.io.format.tiff;

import com.revolsys.io.AbstractIoFactory;
import com.revolsys.raster.GeoreferencedImage;
import com.revolsys.raster.GeoreferencedImageReadFactory;
import com.revolsys.raster.GeoreferencedImageWriter;
import com.revolsys.raster.GeoreferencedImageWriterFactory;
import com.revolsys.raster.ImageIoGeoreferencedImageWriter;
import com.revolsys.spring.resource.Resource;

public class TiffImageFactory extends AbstractIoFactory
  implements GeoreferencedImageReadFactory, GeoreferencedImageWriterFactory {

  public TiffImageFactory() {
    super("TIFF/GeoTIFF");
    addMediaTypeAndFileExtension("image/tiff", "tif");
    addMediaTypeAndFileExtension("image/tiff", "tiff");
  }

  @Override
  public GeoreferencedImage loadImage(final Resource resource) {
    return new TiffImage(resource);
  }

  @Override
  public GeoreferencedImageWriter newGeoreferencedImageWriter(final Resource resource) {
    return new ImageIoGeoreferencedImageWriter(resource, "TIFF");
  }

}
