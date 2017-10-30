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
package com.revolsys.geometry.test.function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class DoubleKeyMap {
  private final Map topMap = new TreeMap();

  public Object get(final Object key1, final Object key2) {
    final Map keyMap = (Map)this.topMap.get(key1);
    if (keyMap == null) {
      return null;
    }
    return keyMap.get(key2);
  }

  public Set keySet() {
    return this.topMap.keySet();
  }

  public Set keySet(final Object key) {
    final Map keyMap = (Map)this.topMap.get(key);
    if (keyMap == null) {
      return new TreeSet();
    }
    return keyMap.keySet();
  }

  private Map newKeyMap(final Object key1) {
    final Map map = new TreeMap();
    this.topMap.put(key1, map);
    return map;
  }

  public void put(final Object key1, final Object key2, final Object value) {
    Map keyMap = (Map)this.topMap.get(key1);
    if (keyMap == null) {
      keyMap = newKeyMap(key1);
    }
    keyMap.put(key2, value);
  }

  public Collection values(final Object key1) {
    final Map keyMap = (Map)this.topMap.get(key1);
    if (keyMap == null) {
      return new ArrayList();
    }
    return keyMap.values();
  }
}
