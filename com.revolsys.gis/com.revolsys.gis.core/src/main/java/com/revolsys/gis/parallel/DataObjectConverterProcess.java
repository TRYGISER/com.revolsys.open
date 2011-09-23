package com.revolsys.gis.parallel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

import com.revolsys.filter.Filter;
import com.revolsys.gis.converter.FilterDataObjectConverter;
import com.revolsys.gis.converter.SimpleDataObjectConveter;
import com.revolsys.gis.converter.process.CopyValues;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectMetaDataFactory;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.process.BaseInOutProcess;

public class DataObjectConverterProcess extends
  BaseInOutProcess<DataObject, DataObject> {
  private static final Logger LOG = LoggerFactory.getLogger(DataObjectConverterProcess.class);

  private Converter<DataObject, DataObject> defaultConverter;

  private Map<QName, Collection<FilterDataObjectConverter>> typeFilterConverterMap = new LinkedHashMap<QName, Collection<FilterDataObjectConverter>>();

  private Map<QName, Converter<DataObject, DataObject>> typeConverterMap = new HashMap<QName, Converter<DataObject, DataObject>>();

  private DataObjectMetaDataFactory targetMetaDataFactory;

  private Map<Object, Map<String, Object>> simpleMapping;

  public void addTypeConverter(final QName typeName,
    final Converter<DataObject, DataObject> converter) {
    typeConverterMap.put(typeName, converter);
  }

  public void addTypeFilterConverter(final QName typeName,
    final FilterDataObjectConverter filterConverter) {

    Collection<FilterDataObjectConverter> converters = typeFilterConverterMap.get(typeName);
    if (converters == null) {
      converters = new ArrayList<FilterDataObjectConverter>();
      typeFilterConverterMap.put(typeName, converters);
    }
    converters.add(filterConverter);
  }

  public Converter<DataObject, DataObject> getDefaultConverter() {
    return defaultConverter;
  }

  public Map<QName, Collection<FilterDataObjectConverter>> getFilterTypeConverterMap() {
    return typeFilterConverterMap;
  }

  public DataObjectMetaDataFactory getTargetMetaDataFactory() {
    return targetMetaDataFactory;
  }

  public Map<QName, Converter<DataObject, DataObject>> getTypeConverterMap() {
    return typeConverterMap;
  }

  @Override
  protected void process(final Channel<DataObject> in,
    final Channel<DataObject> out, final DataObject sourceObject) {
    DataObject targetObject = convert(sourceObject);
    if (targetObject != null) {
      out.write(targetObject);
    }
  }

  protected DataObject convert(final DataObject source) {
    int matchCount = 0;
    final DataObjectMetaData sourceMetaData = source.getMetaData();
    final QName sourceTypeName = sourceMetaData.getName();
    final Collection<FilterDataObjectConverter> converters = typeFilterConverterMap.get(sourceTypeName);
    DataObject target = null;
    if (converters != null && !converters.isEmpty()) {
      for (final FilterDataObjectConverter filterConverter : converters) {
        final Filter<DataObject> filter = filterConverter.getFilter();
        if (filter.accept(source)) {
          final Converter<DataObject, DataObject> converter = filterConverter.getConverter();
          target = converter.convert(source);
          matchCount++;
        }
      }
      if (matchCount == 1) {
        return target;
      }
    }
    if (matchCount == 0) {
      final Converter<DataObject, DataObject> typeConveter = typeConverterMap.get(sourceTypeName);
      if (typeConveter != null) {
        target = typeConveter.convert(source);
        return target;
      } else if (defaultConverter == null) {
        return convertObjectWithNoConverter(source);
      } else {
        return defaultConverter.convert(source);

      }
    } else {
      final StringBuffer sb = new StringBuffer("Multiple conveters found: \n  ");
      for (final FilterDataObjectConverter filterConverter : converters) {
        final Filter<DataObject> filter = filterConverter.getFilter();
        if (filter.accept(source)) {
          sb.append(filter.toString());
          sb.append("\n  ");
        }
      }
      sb.append(source);
      LOG.error(sb.toString());
      return null;
    }
  }

  protected DataObject convertObjectWithNoConverter(final DataObject source) {
    LOG.error("No converter found for: " + source);
    return null;
  }

  public void setDefaultConverter(
    final Converter<DataObject, DataObject> defaultConverter) {
    this.defaultConverter = defaultConverter;
  }

  protected void preRun(final Channel<DataObject> in,
    final Channel<DataObject> out) {
    if (simpleMapping != null) {
      for (final Entry<Object, Map<String, Object>> entry : simpleMapping.entrySet()) {
        final Object key = entry.getKey();
        QName sourceTypeName;
        if (key instanceof QName) {
          sourceTypeName = (QName)key;
        } else {
          sourceTypeName = QName.valueOf(key.toString());
        }
        final Map<String, Object> map = entry.getValue();
        final Object targetName = map.get("typeName");
        QName targetTypeName;
        if (key instanceof QName) {
          targetTypeName = (QName)targetName;
        } else {
          targetTypeName = QName.valueOf(targetName.toString());
        }
        @SuppressWarnings("unchecked")
        Map<String, String> attributeMapping = (Map<String, String>)map.get("attributeMapping");

        final DataObjectMetaData targetMetaData = targetMetaDataFactory.getMetaData(targetTypeName);
        final SimpleDataObjectConveter converter = new SimpleDataObjectConveter(
          targetMetaData);
        converter.addProcessor(new CopyValues(attributeMapping));
        addTypeConverter(sourceTypeName, converter);
      }
    }
  }

  public void setSimpleMapping(
    final Map<Object, Map<String, Object>> simpleMapping) {
    this.simpleMapping = simpleMapping;
  }

  public void setTargetMetaDataFactory(
    final DataObjectMetaDataFactory targetMetaDataFactory) {
    this.targetMetaDataFactory = targetMetaDataFactory;
  }

  public void setTypeConverterMap(
    final Map<QName, Converter<DataObject, DataObject>> typeConverterMap) {
    this.typeConverterMap = typeConverterMap;
  }

  public void setTypeFilterConverterMap(
    final Map<QName, Collection<FilterDataObjectConverter>> typeConverterMap) {
    this.typeFilterConverterMap = typeConverterMap;
  }
}
