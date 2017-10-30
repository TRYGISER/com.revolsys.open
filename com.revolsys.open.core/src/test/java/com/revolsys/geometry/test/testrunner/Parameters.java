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
package com.revolsys.geometry.test.testrunner;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import junit.framework.Assert;

/**
 * Parameters passed to a main method (also known as "command-line arguments").
 * Arguments are recognized only if they are of the form -key or -key:value
 *
 * @version 1.7
 */
public class Parameters {
  private static List allowedKeys = new Vector();

  private static List arguments = null;

  private static Parameters instance = null;

  private static List requiredKeys = new Vector();

  /**
   * Returns the singleton. Be sure to call #setParameters first.
   */
  public static Parameters getInstance() {
    Assert.assertTrue(arguments != null);
    if (instance == null) {
      instance = new Parameters();
    }
    return instance;
  }

  /**
   * Sets the command-line arguments. reqdKeys may be set to null if there
   * are no required command-line arguments. Same with optionalKeys.
   */
  public static void setParameters(final String[] args, final String[] reqdKeys,
    final String[] optionalKeys) {
    arguments = Arrays.asList(args);
    if (reqdKeys != null) {
      requiredKeys = StringUtil.toLowerCase(Arrays.asList(reqdKeys));
    }
    if (reqdKeys != null) {
      allowedKeys.addAll(StringUtil.toLowerCase(Arrays.asList(reqdKeys)));
    }
    if (optionalKeys != null) {
      allowedKeys.addAll(StringUtil.toLowerCase(Arrays.asList(optionalKeys)));
    }
  }

  // //////////////////////////////////////////////////////////////////////////////
  private final Hashtable hashtable = new Hashtable();

  /**
   * Creates a Parameters object for the given main-method arguments.
   */
  private Parameters() {
    for (final Iterator i = arguments.iterator(); i.hasNext();) {
      String arg = (String)i.next();
      arg = arg.toLowerCase();
      if (!arg.startsWith("-")) {
        throw new IllegalArgumentException("Command-line argument does not start with '-': " + arg);
      }
      final int colonIndex = arg.indexOf(":");
      String key;
      String value;
      if (colonIndex >= 0) {
        key = arg.substring(1, colonIndex);
        value = arg.substring(colonIndex + 1);
      } else {
        key = arg.substring(1);
        value = "";
      }
      if (!allowedKeys.contains(key)) {
        throw new IllegalArgumentException("Unrecognized command-line argument: " + arg.substring(1)
          + ". Valid arguments are: " + StringUtil.toCommaDelimitedString(allowedKeys));
      }
      this.hashtable.put(key, value);
    }
    for (final Iterator i = requiredKeys.iterator(); i.hasNext();) {
      final String requiredKey = (String)i.next();
      if (!this.hashtable.containsKey(requiredKey)) {
        throw new IllegalArgumentException(
          "Required command-line argument is missing: " + requiredKey);
      }
    }
  }

  /**
   * Returns true if key is one of the parameters. Case-insensitive.
   */
  public boolean contains(final String key) {
    Assert.assertTrue(allowedKeys.contains(key.toLowerCase()));
    return this.hashtable.containsKey(key.toLowerCase());
  }

  /**
   * Returns the value of the specified parameter, or null if there is no such key. Case-insensitive.
   */
  public String get(final String key) {
    Assert.assertTrue(allowedKeys.contains(key.toLowerCase()));
    return (String)this.hashtable.get(key.toLowerCase());
  }
}
