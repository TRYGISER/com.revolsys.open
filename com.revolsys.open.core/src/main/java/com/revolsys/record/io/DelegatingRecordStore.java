package com.revolsys.record.io;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.transaction.PlatformTransactionManager;

import com.revolsys.collection.ResultPager;
import com.revolsys.collection.map.MapEx;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.identifier.Identifier;
import com.revolsys.io.PathName;
import com.revolsys.record.Record;
import com.revolsys.record.RecordFactory;
import com.revolsys.record.code.CodeTable;
import com.revolsys.record.property.RecordDefinitionProperty;
import com.revolsys.record.query.Query;
import com.revolsys.record.schema.AbstractRecordStore;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordStoreSchema;
import com.revolsys.util.count.CategoryLabelCountMap;
import com.revolsys.util.count.LabelCountMap;

public class DelegatingRecordStore extends AbstractRecordStore {
  private final AbstractRecordStore recordStore;

  public DelegatingRecordStore(final AbstractRecordStore recordStore) {
    this.recordStore = recordStore;
  }

  @Override
  public void addCodeTable(final CodeTable codeTable) {
    this.recordStore.addCodeTable(codeTable);
  }

  @Override
  public void addCodeTable(final String columnName, final CodeTable codeTable) {
    this.recordStore.addCodeTable(columnName, codeTable);
  }

  @Override
  public void addCodeTables(final Collection<CodeTable> codeTables) {
    this.recordStore.addCodeTables(codeTables);
  }

  @Override
  public void addStatistic(final String statisticName, final Record record) {
    this.recordStore.addStatistic(statisticName, record);
  }

  @Override
  public void addStatistic(final String statisticName, final String typePath, final int count) {
    this.recordStore.addStatistic(statisticName, typePath, count);
  }

  @Override
  public void clearProperties() {
    this.recordStore.clearProperties();
  }

  @Override
  @PreDestroy
  public void close() {
    this.recordStore.close();
  }

  @Override
  public boolean deleteRecord(final Record record) {
    return this.recordStore.deleteRecord(record);
  }

  @Override
  public boolean equals(final Object value) {
    return this.recordStore.equals(value);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V extends CodeTable> V getCodeTable(final PathName typePath) {
    return (V)this.recordStore.getCodeTable(typePath);
  }

  @Override
  public CodeTable getCodeTableByFieldName(final CharSequence columnName) {
    return this.recordStore.getCodeTableByFieldName(columnName);
  }

  @Override
  public Map<String, CodeTable> getCodeTableByFieldNameMap() {
    return this.recordStore.getCodeTableByFieldNameMap();
  }

  @Override
  public Map<String, List<String>> getCodeTableColumNames() {
    return this.recordStore.getCodeTableColumNames();
  }

  @Override
  public MapEx getConnectionProperties() {
    return this.recordStore.getConnectionProperties();
  }

  @Override
  public GeometryFactory getGeometryFactory() {
    return this.recordStore.getGeometryFactory();
  }

  @Override
  public String getLabel() {
    return this.recordStore.getLabel();
  }

  @Override
  public MapEx getProperties() {
    return this.recordStore.getProperties();
  }

  @Override
  public <C> C getProperty(final String name) {
    return this.recordStore.getProperty(name);
  }

  @Override
  public <C> C getProperty(final String name, final C defaultValue) {
    return this.recordStore.getProperty(name, defaultValue);
  }

  @Override
  public int getRecordCount(final Query query) {
    return this.recordStore.getRecordCount(query);
  }

  @Override
  public RecordDefinition getRecordDefinition(final RecordDefinition recordDefinition) {
    return this.recordStore.getRecordDefinition(recordDefinition);
  }

  @Override
  public RecordFactory<Record> getRecordFactory() {
    return this.recordStore.getRecordFactory();
  }

  public AbstractRecordStore getRecordStore() {
    return this.recordStore;
  }

  @Override
  public String getRecordStoreType() {
    return this.recordStore.getRecordStoreType();
  }

  @Override
  public RecordStoreSchema getRootSchema() {
    return this.recordStore.getRootSchema();
  }

  @Override
  public CategoryLabelCountMap getStatistics() {
    return this.recordStore.getStatistics();
  }

  @Override
  public LabelCountMap getStatistics(final String name) {
    return this.recordStore.getStatistics(name);
  }

  @Override
  public PlatformTransactionManager getTransactionManager() {
    return this.recordStore.getTransactionManager();
  }

  @Override
  public int hashCode() {
    return this.recordStore.hashCode();
  }

  @Override
  @PostConstruct
  public void initialize() {
    this.recordStore.initialize();
  }

  @Override
  public void insertRecord(final Record record) {
    this.recordStore.insertRecord(record);
  }

  @Override
  public boolean isEditable(final PathName typePath) {
    return this.recordStore.isEditable(typePath);
  }

  @Override
  public Identifier newPrimaryIdentifier(final PathName typePath) {
    return this.recordStore.newPrimaryIdentifier(typePath);
  }

  @Override
  public Query newQuery(final String typePath, final String whereClause,
    final BoundingBox boundingBox) {
    return this.recordStore.newQuery(typePath, whereClause, boundingBox);
  }

  @Override
  public Record newRecord(final PathName typePath) {
    return this.recordStore.newRecord(typePath);
  }

  @Override
  public Record newRecord(final RecordDefinition recordDefinition) {
    return this.recordStore.newRecord(recordDefinition);
  }

  @Override
  public RecordStoreQueryReader newRecordReader() {
    return this.recordStore.newRecordReader();
  }

  @Override
  public RecordWriter newRecordWriter() {
    return this.recordStore.newRecordWriter();
  }

  @Override
  public ResultPager<Record> page(final Query query) {
    return this.recordStore.page(query);
  }

  @Override
  public void removeProperty(final String propertyName) {
    this.recordStore.removeProperty(propertyName);
  }

  @Override
  public void setCodeTableColumNames(final Map<String, List<String>> domainColumNames) {
    this.recordStore.setCodeTableColumNames(domainColumNames);
  }

  @Override
  public void setCommonRecordDefinitionProperties(
    final List<RecordDefinitionProperty> commonRecordDefinitionProperties) {
    this.recordStore.setCommonRecordDefinitionProperties(commonRecordDefinitionProperties);
  }

  @Override
  public void setGeometryFactory(final GeometryFactory geometryFactory) {
    this.recordStore.setGeometryFactory(geometryFactory);
  }

  @Override
  public void setLabel(final String label) {
    this.recordStore.setLabel(label);
  }

  @Override
  public void setProperties(final Map<String, ? extends Object> properties) {
    this.recordStore.setProperties(properties);
  }

  @Override
  public void setProperty(final String name, final Object value) {
    this.recordStore.setProperty(name, value);
  }

  @Override
  public void setPropertySoft(final String name, final Object value) {
    this.recordStore.setPropertySoft(name, value);
  }

  @Override
  public void setPropertyWeak(final String name, final Object value) {
    this.recordStore.setPropertyWeak(name, value);
  }

  @Override
  public void setRecordFactory(final RecordFactory<? extends Record> recordFactory) {
    this.recordStore.setRecordFactory(recordFactory);
  }

  @Override
  public void setTypeRecordDefinitionProperties(
    final Map<String, List<RecordDefinitionProperty>> typeRecordDefinitionProperties) {
    this.recordStore.setTypeRecordDefinitionProperties(typeRecordDefinitionProperties);
  }

  @Override
  public String toString() {
    return this.recordStore.toString();
  }

  @Override
  public void updateRecord(final Record record) {
    this.recordStore.updateRecord(record);
  }

}
