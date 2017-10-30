package com.revolsys.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface Buffers {

  static long getLEUnsignedInt(final ByteBuffer buffer) throws IOException {
    final long b1 = buffer.get();
    final long b2 = buffer.get();
    final long b3 = buffer.get();
    final long b4 = buffer.get();
    final long value = (b4 << 24) + (b3 << 16) + (b2 << 8) + b1;
    return value;
  }

  static long getLEUnsignedLong(final ByteBuffer buffer) throws IOException {
    long value = 0;
    for (int shiftBy = 0; shiftBy < 64; shiftBy += 8) {
      value |= (long)(buffer.get() & 0xff) << shiftBy;
    }
    return value;
  }

  static int getLEUnsignedShort(final ByteBuffer buffer) throws IOException {
    final int ch1 = buffer.get();
    final int ch2 = buffer.get();
    return (ch1 << 0) + (ch2 << 8);
  }

  static String getString(final ByteBuffer buffer, final int byteCount, final Charset charset)
    throws IOException {
    final byte[] bytes = new byte[byteCount];
    buffer.get(bytes);
    int i = 0;
    for (; i < bytes.length; i++) {
      final byte character = bytes[i];
      if (character == 0) {
        return new String(bytes, 0, i, charset);
      }
    }
    return new String(bytes, 0, i, charset);
  }

  static short getUnsignedByte(final ByteBuffer buffer) throws IOException {
    final int ch = buffer.get();
    return (short)ch;
  }

  static String getUsAsciiString(final ByteBuffer buffer, final int byteCount) throws IOException {
    return getString(buffer, byteCount, StandardCharsets.US_ASCII);
  }

  static void putDouble(final ByteBuffer buffer, final double value, final double scale) {
    if (Double.isFinite(value)) {
      final int intX = (int)Math.round(value * scale);
      buffer.putInt(intX);
    } else {
      buffer.putInt(Integer.MIN_VALUE);
    }
  }

  static int readAll(final ReadableByteChannel channel, final ByteBuffer buffer)
    throws IOException {
    do {
      final int readCount = channel.read(buffer);
      if (readCount == -1) {
        if (buffer.position() == 0) {
          return -1;
        }
      }
    } while (buffer.hasRemaining());
    final int position = buffer.position();
    buffer.flip();
    return position;
  }

  static int skipBytes(final ReadableByteChannel channel, final ByteBuffer buffer, int count)
    throws IOException {
    while (count > 0) {
      buffer.clear();
      if (count < buffer.capacity()) {
        buffer.limit(count);
      }

      final int readCount = channel.read(buffer);
      if (readCount == -1) {
        return -1;
      } else {
        count -= readCount;
      }
    }
    final int position = buffer.position();
    buffer.flip();
    return position;
  }

  static void writeAll(final WritableByteChannel out, final ByteBuffer buffer) throws IOException {
    buffer.flip();
    while (buffer.hasRemaining()) {
      out.write(buffer);
    }
    buffer.clear();
  }
}
