package com.revolsys.gis.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.revolsys.util.WrappedException;

public class LittleEndianRandomAccessFile implements EndianInputOutput {

  private final RandomAccessFile randomFile;

  public LittleEndianRandomAccessFile(final File file, final String mode) {
    try {
      this.randomFile = new RandomAccessFile(file, mode);
    } catch (final FileNotFoundException e) {
      throw new WrappedException(e);
    }
  }

  public LittleEndianRandomAccessFile(final String name, final String mode) {
    try {
      this.randomFile = new RandomAccessFile(name, mode);
    } catch (final FileNotFoundException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public void close() {
    try {
      this.randomFile.close();
    } catch (final IOException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public void flush() {
  }

  @Override
  public long getFilePointer() {
    try {
      return this.randomFile.getFilePointer();
    } catch (final IOException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public long length() throws IOException {
    return this.randomFile.length();
  }

  @Override
  public int read() throws IOException {
    return this.randomFile.read();
  }

  @Override
  public int read(final byte[] buf) throws IOException {
    return this.randomFile.read(buf);
  }

  @Override
  public double readDouble() throws IOException {
    return this.randomFile.readDouble();
  }

  @Override
  public int readInt() throws IOException {
    return this.randomFile.readInt();
  }

  @Override
  public double readLEDouble() throws IOException {
    final long value = readLELong();
    return Double.longBitsToDouble(value);
  }

  @Override
  public float readLEFloat() throws IOException {
    final int value = readLEInt();
    return Float.intBitsToFloat(value);
  }

  @Override
  public int readLEInt() throws IOException {
    final int b1 = read();
    final int b2 = read();
    final int b3 = read();
    final int b4 = read();
    final int value = (b4 << 24) + (b3 << 16) + (b2 << 8) + b1;

    return value;
  }

  @Override
  public long readLELong() throws IOException {
    long value = 0;
    for (int shiftBy = 0; shiftBy < 64; shiftBy += 8) {
      value |= (long)(read() & 0xff) << shiftBy;
    }
    return value;
  }

  @Override
  public short readLEShort() throws IOException {
    final int b1 = read();
    final int b2 = read();
    final int value = (b2 << 8) + b1;
    return (short)value;
  }

  @Override
  public long readLong() throws IOException {
    return this.randomFile.readLong();
  }

  @Override
  public short readShort() throws IOException {
    return this.randomFile.readShort();
  }

  @Override
  public void seek(final long index) {
    try {
      this.randomFile.seek(index);
    } catch (final IOException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public void setLength(final long length) throws IOException {
    try {
      this.randomFile.setLength(length);
    } catch (final IOException e) {
      throw new WrappedException(e);
    }

  }

  @Override
  public int skipBytes(final int i) throws IOException {
    return this.randomFile.skipBytes(i);
  }

  @Override
  public void write(final byte[] b) {
    try {
      this.randomFile.write(b);
    } catch (final IOException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public void write(final byte[] b, final int off, final int len) {
    try {
      this.randomFile.write(b, off, len);
    } catch (final IOException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public void write(final int b) {
    try {
      this.randomFile.write(b);
    } catch (final IOException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public final void writeBytes(final String s) {
    try {
      this.randomFile.writeBytes(s);
    } catch (final IOException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public final void writeDouble(final double v) {
    try {
      this.randomFile.writeDouble(v);
    } catch (final IOException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public final void writeFloat(final float v) {
    try {
      this.randomFile.writeFloat(v);
    } catch (final IOException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public final void writeInt(final int v) {
    try {
      this.randomFile.writeInt(v);
    } catch (final IOException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public void writeLEDouble(final double d) {
    final long l = Double.doubleToLongBits(d);
    writeLELong(l);
  }

  @Override
  public void writeLEFloat(final float f) {
    final int i = Float.floatToIntBits(f);
    writeLEInt(i);
  }

  @Override
  public void writeLEInt(final int i) {
    write(i & 0xFF);
    write(i >>> 8 & 0xFF);
    write(i >>> 16 & 0xFF);
    write(i >>> 24 & 0xFF);
  }

  @Override
  public void writeLELong(final long l) {
    write((int)l & 0xFF);
    write((int)(l >>> 8) & 0xFF);
    write((int)(l >>> 16) & 0xFF);
    write((int)(l >>> 24) & 0xFF);
    write((int)(l >>> 32) & 0xFF);
    write((int)(l >>> 40) & 0xFF);
    write((int)(l >>> 48) & 0xFF);
    write((int)(l >>> 56) & 0xFF);
  }

  @Override
  public void writeLEShort(final short s) {
    write(s & 0xFF);
    write(s >>> 8 & 0xFF);
  }

  @Override
  public final void writeLong(final long v) {
    try {
      this.randomFile.writeLong(v);
    } catch (final IOException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public void writeShort(final short s) {
    write(s >>> 8 & 0xFF);
    write(s & 0xFF);
  }
}
