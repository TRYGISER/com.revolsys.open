package com.revolsys.geometry.test.model;

import org.junit.Test;

public class MultiLineStringTest {

  @Test
  public void testFromFile() {
    TestUtil.doTestGeometry(getClass(), "MultiLineString.csv");
  }
}
