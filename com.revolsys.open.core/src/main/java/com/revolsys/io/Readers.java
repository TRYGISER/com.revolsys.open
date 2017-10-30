package com.revolsys.io;

import java.io.IOException;
import java.io.Reader;

import com.revolsys.util.Exceptions;

public interface Readers {
  static double readDouble(final Reader reader) throws IOException {
    int digitCount = 0;
    long number = 0;
    boolean negative = false;
    double decimalDivisor = -1;
    for (int character = reader.read(); character != -1; character = reader.read()) {
      if (Character.isWhitespace(character)) {
        if (digitCount > 0) {
          break;
        }
      } else if (character == '.') {
        if (decimalDivisor == -1) {
          decimalDivisor = 1;
        } else {
          throw new IllegalStateException("Cannot have two '.' characters in a number");
        }
      } else if (character == '-') {
        if (digitCount == 0 && !negative) {
          negative = true;
        } else {
          throw new IllegalStateException("Cannot have two '-' characters in a number");
        }
      } else if (character >= '0' && character <= '9') {
        digitCount++;
        if (digitCount < 19) {
          number = number * 10 + character - '0';
          if (decimalDivisor != -1) {
            decimalDivisor *= 10;
          }
        }
      } else {
        throw new IllegalStateException("Cannot have a '" + character + "' character in a number");
      }
    }
    if (digitCount == 0) {
      return Double.NaN;
    } else {
      double doubleNumber;
      if (decimalDivisor > 1) {
        doubleNumber = number / decimalDivisor;
      } else {
        doubleNumber = number;
      }
      if (negative) {
        return -doubleNumber;
      } else {
        return doubleNumber;
      }
    }
  }

  static float readFloat(final Reader reader) throws IOException {
    int digitCount = 0;
    long number = 0;
    boolean negative = false;
    float decimalDivisor = -1;
    for (int character = reader.read(); character != -1; character = reader.read()) {
      if (Character.isWhitespace(character)) {
        if (digitCount > 0) {
          break;
        }
      } else if (character == '.') {
        if (decimalDivisor == -1) {
          decimalDivisor = 1;
        } else {
          throw new IllegalStateException("Cannot have two '.' characters in a number");
        }
      } else if (character == '-') {
        if (digitCount == 0 && !negative) {
          negative = true;
        } else {
          throw new IllegalStateException("Cannot have two '-' characters in a number");
        }
      } else if (character >= '0' && character <= '9') {
        digitCount++;
        if (digitCount < 19) {
          number = number * 10 + character - '0';
          if (decimalDivisor != -1) {
            decimalDivisor *= 10;
          }
        }
      } else {
        throw new IllegalStateException("Cannot have a '" + character + "' character in a number");
      }
    }
    if (digitCount == 0) {
      return Float.NaN;
    } else {
      float floatNumber;
      if (decimalDivisor > 1) {
        floatNumber = number / decimalDivisor;
      } else {
        floatNumber = number;
      }
      if (negative) {
        return -floatNumber;
      } else {
        return floatNumber;
      }
    }
  }

  public static int readInteger(final Reader reader) throws IOException {
    int digitCount = 0;
    int number = 0;
    boolean negative = false;
    for (int character = reader.read(); character != -1; character = reader.read()) {
      if (Character.isWhitespace(character)) {
        if (digitCount > 0) {
          break;
        }
      } else if (character == '-') {
        if (digitCount == 0 && !negative) {
          negative = true;
        } else {
          throw new IllegalStateException("Cannot have two '-' characters in a number");
        }
      } else if (character >= '0' && character <= '9') {
        number = number * 10 + character - '0';
        digitCount++;
      } else {
        throw new IllegalStateException("Cannot have a '" + character + "' character in a number");
      }
    }
    if (digitCount == 0) {
      throw new IllegalStateException("Reader does not contain a number");
    } else if (negative) {
      return -number;
    } else {
      return number;
    }
  }

  static String readKeyword(final Reader reader) {
    try {
      final StringBuilder string = new StringBuilder();
      for (int character = reader.read(); character != -1; character = reader.read()) {
        if (Character.isWhitespace(character)) {
          if (string.length() > 0) {
            break;
          }
        } else {
          string.append((char)character);
        }
      }
      return string.toString();
    } catch (final IOException e) {
      throw Exceptions.wrap(e);
    }
  }
}
