package com.revolsys.io.xml;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.revolsys.converter.string.BooleanStringConverter;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectMetaDataProperties;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.io.AbstractWriter;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoConstants;
import com.revolsys.io.NamedObject;
import com.revolsys.util.Property;

public class XmlDataObjectWriter extends AbstractWriter<DataObject> {

  private final DataObjectMetaData metaData;

  private XmlWriter out;

  private boolean startAttribute;

  private boolean singleObject;

  private boolean opened;

  private boolean writeNulls;

  public XmlDataObjectWriter(final DataObjectMetaData metaData,
    final java.io.Writer out) {
    this.metaData = metaData;
    if (out instanceof XmlWriter) {
      this.out = (XmlWriter)out;
    } else {
      this.out = new XmlWriter(out);
    }
  }

  /**
   * Closes the underlying reader.
   */
  @Override
  public void close() {
    if (this.out != null) {
      try {
        if (this.opened) {
          if (!this.singleObject) {
            this.out.endTag();
          }
          this.out.endDocument();
        }
      } finally {
        FileUtil.closeSilent(this.out);
        this.out = null;
      }
    }
  }

  @Override
  public void flush() {
    this.out.flush();
  }

  public boolean isWriteNulls() {
    return this.writeNulls;
  }

  private void list(final List<? extends Object> list) {
    for (final Object value : list) {
      if (value instanceof Map) {
        final Map<String, ?> map = (Map<String, ?>)value;
        map(map);
      } else if (value instanceof List) {
        final List<?> subList = (List<?>)value;
        list(subList);
      } else {
        this.out.startTag(new QName("item"));
        this.out.text(value);
        this.out.endTag();
      }
    }
  }

  private void map(final Map<String, ? extends Object> values) {
    if (values instanceof NamedObject) {
      final NamedObject namedObject = (NamedObject)values;
      this.out.startTag(new QName(namedObject.getName()));
    } else {
      this.out.startTag(new QName("item"));
    }

    for (final Entry<String, ? extends Object> field : values.entrySet()) {
      final Object key = field.getKey();
      final Object value = field.getValue();
      final QName tagName = new QName(key.toString());
      if (value instanceof Map) {
        final Map<String, ?> map = (Map<String, ?>)value;
        this.out.startTag(tagName);
        map(map);
        this.out.endTag();
      } else if (value instanceof List) {
        final List<?> list = (List<?>)value;
        this.out.startTag(tagName);
        list(list);
        this.out.endTag();
      } else {
        this.out.nillableElement(tagName, value);
      }
    }
    this.out.endTag();
  }

  @Override
  public void setProperty(final String name, final Object value) {
    super.setProperty(name, value);
    if (name.equals(IoConstants.INDENT_PROPERTY)) {
      this.out.setIndent((Boolean)value);
    } else if (IoConstants.WRITE_NULLS_PROPERTY.equals(name)) {
      this.writeNulls = BooleanStringConverter.isTrue(value);
    }
  }

  public void setWriteNulls(final boolean writeNulls) {
    this.writeNulls = writeNulls;
  }

  @Override
  public String toString() {
    return this.metaData.getPath().toString();
  }

  @Override
  public void write(final DataObject object) {
    if (!this.opened) {
      writeHeader();
    }
    QName qualifiedName = this.metaData.getProperty(DataObjectMetaDataProperties.QUALIFIED_NAME);
    if (qualifiedName == null) {
      qualifiedName = new QName(this.metaData.getTypeName());
    }

    this.out.startTag(qualifiedName);

    final int attributeCount = this.metaData.getAttributeCount();
    for (int i = 0; i < attributeCount; i++) {
      final String name = this.metaData.getAttributeName(i);
      final Object value = object.getValue(i);
      if (Property.hasValue(value) || this.writeNulls) {
        final QName tagName = new QName(name);
        if (value instanceof Map) {
          @SuppressWarnings("unchecked")
          final Map<String, ?> map = (Map<String, ?>)value;
          this.out.startTag(tagName);
          map(map);
          this.out.endTag();
        } else if (value instanceof List) {
          final List<?> list = (List<?>)value;
          this.out.startTag(tagName);
          list(list);
          this.out.endTag();
        } else {
          final DataType dataType = this.metaData.getAttributeType(i);
          final String string = StringConverterRegistry.toString(dataType,
            value);
          this.out.nillableElement(tagName, string);
        }
      }
    }
    this.out.endTag();
  }

  private void writeHeader() {
    this.out.startDocument("UTF-8", "1.0");
    this.singleObject = Boolean.TRUE.equals(getProperty(IoConstants.SINGLE_OBJECT_PROPERTY));
    if (!this.singleObject) {
      this.out.startTag(new QName("items"));
    }
    this.opened = true;
  }
}