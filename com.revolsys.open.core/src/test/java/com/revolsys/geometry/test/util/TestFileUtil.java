/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 *
 * Copyright (C) 2001 Vivid Solutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, contact:
 *
 *     Vivid Solutions
 *     Suite #1A
 *     2328 Government Street
 *     Victoria BC  V8T 5G5
 *     Canada
 *
 *     (250)385-6040
 *     www.vividsolutions.com
 */
package com.revolsys.geometry.test.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Vector;

/**
 * Useful file utilities.
 *
 * @version 1.7
 */
public class TestFileUtil {
  public static final String EXTENSION_SEPARATOR = ".";

  /**
   * Copies the source file to the destination filename.
   * Posted by Mark Thornton <mthorn@cix.compulink.co.uk> on Usenet.
   */
  public static void copyFile(final File source, final File destination) throws IOException {
    final RandomAccessFile out = new RandomAccessFile(destination, "rw");
    // Tell the OS in advance how big the file will be. This may reduce
    // fragmentation
    out.setLength(source.length());
    // copy the content
    final FileInputStream in = new FileInputStream(source);
    final byte[] buffer = new byte[16384];
    while (true) {
      final int n = in.read(buffer);
      if (n == -1) {
        break;
      }
      out.write(buffer, 0, n);
    }
    in.close();
    out.close();
  }

  /**
   * Deletes the files in the directory, but does not remove the directory.
   */
  public static void deleteFiles(final String directoryName) {
    final File dir = new File(directoryName);
    final File[] files = dir.listFiles();
    for (final File file : files) {
      file.delete();
    }
  }

  /**
   * Returns true if the given directory exists.
   */
  public static boolean directoryExists(final String directoryName) {
    final File directory = new File(directoryName);
    return directory.exists();
  }

  public static String getFileNameExtension(final String path) {
    return com.revolsys.io.FileUtil.getFileNameExtension(path);
  }

  /**
   * Returns a List of the String's in the text file, one per line.
   */
  public static List getContents(final String textFileName)
    throws FileNotFoundException, IOException {
    final List contents = new Vector();
    final FileReader fileReader = new FileReader(textFileName);
    final BufferedReader bufferedReader = new BufferedReader(fileReader);
    String line = bufferedReader.readLine();
    while (line != null) {
      contents.add(line);
      line = bufferedReader.readLine();
    }
    return contents;
  }

  public static String name(final String path) {
    final File file = new File(path);
    return file.getName();
  }

  /**
   * Gets the contents of a text file as a single String
   * @param file
   * @return text file contents
   * @throws IOException
   */
  public static String readText(final File file) throws IOException {
    String thisLine;
    final StringBuilder stringBuilder = new StringBuilder();

    try (
      final FileInputStream fin = new FileInputStream(file);
      final BufferedReader br = new BufferedReader(new InputStreamReader(fin));) {
      while ((thisLine = br.readLine()) != null) {
        stringBuilder.append(thisLine + "\r\n");
      }
      final String result = stringBuilder.toString();
      return result;
    }
  }

  public static String readText(final String filename) throws IOException {
    return readText(new File(filename));
  }

  /**
   * Saves the String with the given filename
   */
  public static void setContents(final String textFileName, final String contents)
    throws IOException {
    final FileWriter fileWriter = new FileWriter(textFileName, false);
    final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
    bufferedWriter.write(contents);
    bufferedWriter.flush();
    bufferedWriter.close();
    fileWriter.close();
  }
}
