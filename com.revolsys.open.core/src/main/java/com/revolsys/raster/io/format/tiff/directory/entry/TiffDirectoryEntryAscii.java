package com.revolsys.raster.io.format.tiff.directory.entry;

import com.revolsys.io.channels.ChannelReader;
import com.revolsys.raster.io.format.tiff.TiffDirectory;
import com.revolsys.raster.io.format.tiff.code.TiffFieldType;
import com.revolsys.raster.io.format.tiff.code.TiffTag;

public class TiffDirectoryEntryAscii extends AbstractTiffDirectoryEntry<String> {

  public TiffDirectoryEntryAscii(final TiffTag tag, final TiffDirectory directory,
    final ChannelReader in) {
    super(tag, directory, in);
  }

  @Override
  public String getString() {
    return this.value;
  }

  @Override
  public TiffFieldType getType() {
    return TiffFieldType.ASCII;
  }

  @Override
  protected String loadValueDo(final ChannelReader in, final int count) {
    return in.getUsAsciiString(count);
  }
}
