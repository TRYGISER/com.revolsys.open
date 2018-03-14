package com.revolsys.geometry.test.old.index;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
  KdTreeTest.class, //
  QuadtreeTest.class, //
  STRtreeTest.class
})
public class TreeTestSuite {

}