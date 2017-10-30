// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
// http://code.google.com/p/protobuf/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.revolsys.io;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;

/**
 * This class is used internally by the Protocol Buffer library and generated
 * message implementations.  It is public only because those generated messages
 * do not reside in the {@code protobuf} package.  Others should not use this
 * class directly.
 *
 * This class contains constants and helper functions useful for dealing with
 * the Protocol Buffer wire format.
 *
 * @author kenton@google.com Kenton Varda
 */
public final class WireFormat {
  /**
   * Lite equivalent to {@link Descriptors.FieldDescriptor.Type}.  This is
   * only here to support the lite runtime and should not be used by users.
   */
  public enum FieldType {
    BOOL(JavaType.BOOLEAN, WIRETYPE_VARINT), BYTES(JavaType.BYTE_STRING,
      WIRETYPE_LENGTH_DELIMITED) {
      @Override
      public boolean isPackable() {
        return false;
      }
    },
    DOUBLE(JavaType.DOUBLE, WIRETYPE_FIXED64), ENUM(JavaType.ENUM, WIRETYPE_VARINT), FIXED32(
      JavaType.INT, WIRETYPE_FIXED32), FIXED64(JavaType.LONG, WIRETYPE_FIXED64), FLOAT(
        JavaType.FLOAT, WIRETYPE_FIXED32), GROUP(JavaType.MESSAGE, WIRETYPE_START_GROUP) {
          @Override
          public boolean isPackable() {
            return false;
          }
        },
    INT32(JavaType.INT, WIRETYPE_VARINT), INT64(JavaType.LONG,
      WIRETYPE_VARINT), MESSAGE(JavaType.MESSAGE, WIRETYPE_LENGTH_DELIMITED) {
        @Override
        public boolean isPackable() {
          return false;
        }
      },
    SFIXED32(JavaType.INT, WIRETYPE_FIXED32), SFIXED64(JavaType.LONG, WIRETYPE_FIXED64), SINT32(
      JavaType.INT, WIRETYPE_VARINT), SINT64(JavaType.LONG,
        WIRETYPE_VARINT), STRING(JavaType.STRING, WIRETYPE_LENGTH_DELIMITED) {
          @Override
          public boolean isPackable() {
            return false;
          }
        },
    UINT32(JavaType.INT, WIRETYPE_VARINT), UINT64(JavaType.LONG, WIRETYPE_VARINT);

    private final JavaType javaType;

    private final int wireType;

    FieldType(final JavaType javaType, final int wireType) {
      this.javaType = javaType;
      this.wireType = wireType;
    }

    public JavaType getJavaType() {
      return this.javaType;
    }

    public int getWireType() {
      return this.wireType;
    }

    public boolean isPackable() {
      return true;
    }
  }

  /**
   * Lite equivalent to {@link Descriptors.FieldDescriptor.JavaType}.  This is
   * only here to support the lite runtime and should not be used by users.
   */
  public enum JavaType {
    BOOLEAN(false), BYTE_STRING(ByteString.EMPTY), DOUBLE(0D), ENUM(null), FLOAT(0F), INT(0), LONG(
      0L), MESSAGE(null), STRING("");

    private final Object defaultDefault;

    JavaType(final Object defaultDefault) {
      this.defaultDefault = defaultDefault;
    }

    /**
     * The default default value for fields of this type, if it's a primitive
     * type.
     */
    Object getDefaultDefault() {
      return this.defaultDefault;
    }
  }

  public static final int WIRETYPE_END_GROUP = 4;

  public static final int WIRETYPE_FIXED32 = 5;

  public static final int WIRETYPE_FIXED64 = 1;

  public static final int WIRETYPE_LENGTH_DELIMITED = 2;

  public static final int WIRETYPE_START_GROUP = 3;

  public static final int WIRETYPE_VARINT = 0;

  // Field numbers for fields in MessageSet wire format.
  static final int MESSAGE_SET_ITEM = 1;

  static final int MESSAGE_SET_ITEM_END_TAG = makeTag(MESSAGE_SET_ITEM, WIRETYPE_END_GROUP);

  // Tag numbers.
  static final int MESSAGE_SET_ITEM_TAG = makeTag(MESSAGE_SET_ITEM, WIRETYPE_START_GROUP);

  static final int MESSAGE_SET_MESSAGE = 3;

  static final int MESSAGE_SET_MESSAGE_TAG = makeTag(MESSAGE_SET_MESSAGE,
    WIRETYPE_LENGTH_DELIMITED);

  static final int MESSAGE_SET_TYPE_ID = 2;

  static final int MESSAGE_SET_TYPE_ID_TAG = makeTag(MESSAGE_SET_TYPE_ID, WIRETYPE_VARINT);

  static final int TAG_TYPE_BITS = 3;

  static final int TAG_TYPE_MASK = (1 << TAG_TYPE_BITS) - 1;

  /** Given a tag value, determines the field number (the upper 29 bits). */
  public static int getTagFieldNumber(final int tag) {
    return tag >>> TAG_TYPE_BITS;
  }

  /** Given a tag value, determines the wire type (the lower 3 bits). */
  static int getTagWireType(final int tag) {
    return tag & TAG_TYPE_MASK;
  }

  /** Makes a tag value given a field number and wire type. */
  static int makeTag(final int fieldNumber, final int wireType) {
    return fieldNumber << TAG_TYPE_BITS | wireType;
  }

  // Do not allow instantiation.
  private WireFormat() {
  }
}
