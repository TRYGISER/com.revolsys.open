/*
 * Copyright 2004-2005 Revolution Systems Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.revolsys.util;

import java.math.BigDecimal;

/**
 * The MathUtil class is a utility class for handling integer, percent and
 * currency BigDecimal values.
 * 
 * @author Paul Austin
 */
public final class MathUtil {
  public static final int BYTES_IN_DOUBLE = 8;

  public static final int BYTES_IN_INT = 4;

  public static final int BYTES_IN_LONG = 8;

  public static final int BYTES_IN_SHORT = 2;

  /** The number of cents in a dollar. */
  public static final BigDecimal CURRENCY_CENTS_PER_DOLLAR = getInteger(100);

  /** The scale for currency numbers. */
  public static final int CURRENCY_SCALE = 2;

  /** A 0 currency. */
  public static final BigDecimal CURRENCY0 = getCurrency(0);

  /** The scale for integer numbers. */
  public static final int INTEGER_SCALE = 0;

  /** A 0 integer. */
  public static final BigDecimal INTEGER0 = getInteger(0);

  /** The scale for percent numbers. */
  public static final int PERCENT_SCALE = 4;

  /** A 0 percent. */
  public static final BigDecimal PERCENT0 = getPercent(0);

  /** A 1000 percent. */
  public static final BigDecimal PERCENT100 = getPercent(1);

  /**
   * Calculate the angle of a coordinates
   * 
   * @param x The x coordinate.
   * @param y The y coordinate.
   * @return The distance.
   */
  public static double angle(
    final double x,
    final double y) {
    final double angle = Math.atan2(y, x);
    return angle;
  }

  /**
   * Calculate the angle between two coordinates.
   * 
   * @param x1 The first x coordinate.
   * @param y1 The first y coordinate.
   * @param x2 The second x coordinate.
   * @param y2 The second y coordinate.
   * @return The distance.
   */
  public static double angle(
    final double x1,
    final double y1,
    final double x2,
    final double y2) {
    final double dx = x2 - x1;
    final double dy = y2 - y1;

    final double angle = Math.atan2(dy, dx);
    return angle;
  }

  /**
   * Calculate the angle between three coordinates.
   * 
   * @param x1 The first x coordinate.
   * @param y1 The first y coordinate.
   * @param x2 The second x coordinate.
   * @param y2 The second y coordinate.
   * @param x3 The third x coordinate.
   * @param y3 The third y coordinate.
   * @return The distance.
   */
  public static double angle(
    final double x1,
    final double y1,
    final double x2,
    final double y2,
    final double x3,
    final double y3) {
    final double angle1 = angle(x2, y2, x1, y1);
    final double angle2 = angle(x2, y2, x3, y3);
    return angleDiff(angle1, angle2);
  }

  public static double angleDegrees(
    final double x1,
    final double y1,
    final double x2,
    final double y2) {
    final double width = x2 - x1;
    final double height = y2 - y1;
    if (width == 0) {
      if (height < 0) {
        return 270;
      } else {
        return 90;
      }
    } else if (height == 0) {
      if (width < 0) {
        return 180;
      } else {
        return 0;
      }
    }
    final double arctan = Math.atan(height / width);
    double degrees = Math.toDegrees(arctan);
    if (width < 0) {
      degrees = 180 + degrees;
    } else {
      degrees = (360 + degrees) % 360;
    }
    return degrees;
  }

  public static double angleDiff(
    final double ang1,
    final double ang2) {
    double delAngle;

    if (ang1 < ang2) {
      delAngle = ang2 - ang1;
    } else {
      delAngle = ang1 - ang2;
    }

    if (delAngle > Math.PI) {
      delAngle = (2 * Math.PI) - delAngle;
    }

    return delAngle;
  }

  public static double angleDiff(
    final double angle1,
    final double angle2,
    final boolean clockwise) {
    if (clockwise) {
      if (angle2 < angle1) {
        final double angle = angle2 + Math.PI * 2 - angle1;
        return angle;
      } else {
        final double angle = angle2 - angle1;
        return angle;
      }
    } else {
      if (angle1 < angle2) {
        final double angle = angle1 + Math.PI * 2 - angle2;
        return angle;
      } else {
        final double angle = angle1 - angle2;
        return angle;
      }
    }
  }

  public static double angleDiffDegrees(
    final double a,
    final double b) {
    final double largest = Math.max(a, b);
    final double smallest = Math.min(a, b);
    double diff = largest - smallest;
    if (diff > 180) {
      diff = 360 - diff;
    }
    return diff;
  }

  /**
   * Convert a BigDecimal amount to a currency string prefixed by the "$" sign.
   * 
   * @param amount The BigDecimal amount.
   * @return The currency String
   */
  public static String currencyToString(
    final BigDecimal amount) {
    if (amount != null) {
      return "$" + getCurrency(amount);
    } else {
      return null;
    }
  }

  /**
   * Calculate the distance between two coordinates.
   * 
   * @param x1 The first x coordinate.
   * @param y1 The first y coordinate.
   * @param x2 The second x coordinate.
   * @param y2 The second y coordinate.
   * @return The distance.
   */
  public static double distance(
    final double x1,
    final double y1,
    final double x2,
    final double y2) {
    final double dx = x2 - x1;
    final double dy = y2 - y1;

    final double distance = Math.sqrt(dx * dx + dy * dy);
    return distance;
  }

  /**
   * Divide two currency amounts, setting the scale to {@link #CURRENCY_SCALE}
   * and rounding 1/2 u
   * 
   * @param left The left operand.
   * @param right The right operand.
   * @return The new amount.
   */
  public static BigDecimal divideCurrency(
    final BigDecimal left,
    final BigDecimal right) {
    return left.divide(right, CURRENCY_SCALE, BigDecimal.ROUND_HALF_UP);
  }

  /**
   * Divide two percent amounts, setting the scale to {@link #CURRENCY_SCALE}
   * and rounding 1/2 u
   * 
   * @param left The left operand.
   * @param right The right operand.
   * @return The new amount.
   */
  public static BigDecimal dividePercent(
    final BigDecimal left,
    final BigDecimal right) {
    return left.divide(right, PERCENT_SCALE, BigDecimal.ROUND_HALF_UP);
  }

  /**
   * Divide two percent amounts, setting the scale to {@link #CURRENCY_SCALE}
   * and rounding 1/2 u
   * 
   * @param left The left operand.
   * @param right The right operand.
   * @return The new amount.
   */
  public static BigDecimal dividePercent(
    final double left,
    final double right) {
    return dividePercent(new BigDecimal(left), new BigDecimal(right));
  }

  /**
   * Divide two percent amounts, setting the scale to {@link #CURRENCY_SCALE}
   * and rounding 1/2 u
   * 
   * @param left The left operand.
   * @param right The right operand.
   * @return The new amount.
   */
  public static BigDecimal dividePercent(
    final double left,
    final int right) {
    return dividePercent(new BigDecimal(left), new BigDecimal(right));
  }

  /**
   * Convert a BigDecimal amount into a currency BigDecimal.
   * 
   * @param amount The ammount.
   * @return The currency.
   */
  public static BigDecimal getCurrency(
    final BigDecimal amount) {
    if (amount != null) {
      return amount.setScale(CURRENCY_SCALE, BigDecimal.ROUND_HALF_UP);
    } else {
      return null;
    }
  }

  /**
   * Convert a double amount into a currency BigDecimal.
   * 
   * @param amount The ammount.
   * @return The currency.
   */
  public static BigDecimal getCurrency(
    final double amount) {
    return getCurrency(new BigDecimal(amount));
  }

  /**
   * Convert a BigDecimal into an ineteger BigDecimal.
   * 
   * @param value The BigDecimal value.
   * @return The ineteger BigDecimal.
   */
  public static BigDecimal getInteger(
    final BigDecimal value) {
    if (value != null) {
      return value.setScale(INTEGER_SCALE, BigDecimal.ROUND_DOWN);
    } else {
      return null;
    }
  }

  /**
   * Convert a int into an ineteger BigDecimal.
   * 
   * @param value The int value.
   * @return The ineteger BigDecimal.
   */
  public static BigDecimal getInteger(
    final int value) {
    return getInteger(new BigDecimal((double)value));
  }

  /**
   * Convert a BigDecimal decimal percent (e.g. 0.5 is 50%) into an percent
   * BigDecimal.
   * 
   * @param decimalPercent The decimal percent value.
   * @return The currency.
   */
  public static BigDecimal getPercent(
    final BigDecimal decimalPercent) {
    if (decimalPercent != null) {
      return decimalPercent.setScale(PERCENT_SCALE, BigDecimal.ROUND_HALF_UP);
    } else {
      return null;
    }
  }

  /**
   * Convert a double decimal percent (e.g. 0.5 is 50%) into an percent
   * BigDecimal.
   * 
   * @param decimalPercent The decimal percent value.
   * @return The currency.
   */
  public static BigDecimal getPercent(
    final double decimalPercent) {
    return getPercent(new BigDecimal(decimalPercent));
  }

  /**
   * Convert a String decimal percent (e.g. 0.5 is 50%) into an percent
   * BigDecimal.
   * 
   * @param decimalPercent The decimal percent value.
   * @return The currency.
   */
  public static BigDecimal getPercent(
    final String decimalPercent) {
    return getPercent(new BigDecimal(decimalPercent));
  }

  /**
   * Convert a BigDecimal integer to a string.
   * 
   * @param integer The BigDecimal integer.
   * @return The integer String
   */
  public static String integerToString(
    final BigDecimal integer) {
    return getInteger(integer).toString();
  }

  public static double midpoint(
    final double d1,
    final double d2) {
    return d1 + (d2 - d1) / 2;
  }

  /**
   * Convert a BigDecimal decimal percent to a percent string suffixed by the
   * "%" sign.
   * 
   * @param decimalPercent The BigDecimal percent.
   * @return The percent String
   */
  public static String percentToString(
    final BigDecimal decimalPercent) {
    return percentToString(decimalPercent, PERCENT_SCALE);
  }

  /**
   * Convert a BigDecimal decimal percent to a percent string suffixed by the
   * "%" sign with the specified number of decimal places.
   * 
   * @param decimalPercent The BigDecimal percent.
   * @param scale The number of decimal places to show.
   * @return The percent String
   */
  public static String percentToString(
    final BigDecimal decimalPercent,
    final int scale) {
    if (decimalPercent != null) {
      return decimalPercent.multiply(new BigDecimal(100)).setScale(scale,
        BigDecimal.ROUND_HALF_UP)
        + "%";
    } else {
      return null;
    }
  }

  public static double pointLineDistance(
    final double x,
    final double y,
    final double x1,
    final double y1,
    final double x2,
    final double y2) {
    // if start==end, then use pt distance
    if (x1 == x2 && y1 == y2) {
      return distance(x, y, x1, y1);
    }

    // otherwise use comgraphics.algorithms Frequently Asked Questions method
    /*
     * (1) AC dot AB r = --------- ||AB||^2 r has the following meaning: r=0 P =
     * A r=1 P = B r<0 P is on the backward extension of AB r>1 P is on the
     * forward extension of AB 0<r<1 P is interior to AB
     */

    final double r = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1))
      / ((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));

    if (r <= 0.0) {
      return distance(x, y, x1, y1);
    }
    if (r >= 1.0) {
      return distance(x, y, x2, y2);
    }

    /*
     * (2) (Ay-Cy)(Bx-Ax)-(Ax-Cx)(By-Ay) s = ----------------------------- L^2
     * Then the distance from C to P = |s|*L.
     */

    final double s = ((y1 - y) * (x2 - x1) - (x1 - x) * (y2 - y1))
      / ((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));

    return Math.abs(s)
      * Math.sqrt(((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)));
  }

  /**
   * Construct a new MathUtil.
   */
  private MathUtil() {
  }
}
