package com.revolsys.swing.map.layer.record.table.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.ListSelectionModel;

import com.revolsys.collection.list.ListByIndexIterator;
import com.revolsys.collection.list.Lists;
import com.revolsys.record.query.Condition;
import com.revolsys.record.query.Query;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.parallel.Invoke;
import com.revolsys.util.Property;

public abstract class ModeAbstractCached implements TableRecordsMode {
  private final String key;

  private final AtomicLong refreshIndex = new AtomicLong(Long.MIN_VALUE + 1);

  private List<LayerRecord> records = new ArrayList<>();

  private int recordCount;

  private final RecordLayerTableModel model;

  private final List<PropertyChangeListener> listeners = new ArrayList<>();

  private long lastRefreshIndex = Long.MIN_VALUE;

  private LayerRecord currentRecord;

  private int currentRowIndex;

  private ListSelectionModel selectionModel;

  public ModeAbstractCached(final String key, final RecordLayerTableModel model) {
    this.key = key;
    this.model = model;
  }

  @Override
  public void activate() {
    this.selectionModel = newSelectionModel(this.model);
    final AbstractRecordLayer layer = getLayer();
    final PropertyChangeListener recordFieldListener = this::recordFieldChanged;
    layer.addPropertyChangeListener(recordFieldListener);
    addListeners( //
      Property.addListenerNewValueSource(layer, AbstractRecordLayer.RECORD_UPDATED,
        this::recordUpdated), //
      recordFieldListener);
  }

  protected void addCachedRecord(final LayerRecord record) {
    if (record != null) {
      addCachedRecords(Collections.singletonList(record));
    }
  }

  protected void addCachedRecords(final Iterable<? extends LayerRecord> records) {
    if (records != null) {
      final int fromIndex = this.records.size();
      int addCount = 0;
      for (final LayerRecord record : records) {
        if (canAddCachedRecord(record)) {
          final int index = record.addTo(this.records);
          if (index != -1) {
            addCount++;
          }
        }
      }
      if (addCount > 0) {
        clearCurrentRecord();
        setRecordCount(this.recordCount + addCount);
        this.model.fireTableRowsInserted(fromIndex, fromIndex + addCount - 1);
      }
    }
  }

  protected void addListeners(final PropertyChangeListener... listeners) {
    Lists.addAll(this.listeners, listeners);
  }

  protected boolean canAddCachedRecord(final LayerRecord record) {
    return true;
  }

  protected boolean canRefreshFinish(final long index) {
    if (index >= this.lastRefreshIndex) {
      this.lastRefreshIndex = index;
      return true;
    } else {
      return false;
    }
  }

  protected void clearCurrentRecord() {
    this.currentRecord = null;
    this.currentRowIndex = -1;
  }

  @Override
  public void deactivate() {
    for (final Object source : Arrays.asList(this.model, getLayer())) {
      for (final PropertyChangeListener listener : this.listeners) {
        Property.removeListener(source, listener);
      }
    }
    clearCurrentRecord();
    this.listeners.clear();
    this.recordCount = 0;
    this.records = new ArrayList<>();
    this.selectionModel = null;
  }

  @Override
  public void exportRecords(final Query query, final Object target) {
    final Condition filter = query.getWhereCondition();
    final Map<String, Boolean> orderBy = query.getOrderBy();
    final AbstractRecordLayer layer = getLayer();
    final Iterable<LayerRecord> records = new ListByIndexIterator<>(this.records);
    layer.exportRecords(records, filter, orderBy, target);
  }

  protected void fireRecordUpdated(final int index) {
    clearCurrentRecord();
    this.model.fireTableRowsUpdated(index, index);
  }

  protected void fireTableDataChanged() {
    clearCurrentRecord();
    this.model.fireTableDataChanged();
  }

  protected Condition getFilter() {
    return this.model.getFilter();
  }

  protected Query getFilterQuery() {
    return this.model.getFilterQuery();
  }

  @Override
  public String getKey() {
    return this.key;
  }

  public AbstractRecordLayer getLayer() {
    return this.model.getLayer();
  }

  @Override
  public final LayerRecord getRecord(final int rowIndex) {
    LayerRecord record = null;
    if (rowIndex >= 0) {
      if (rowIndex == this.currentRowIndex && this.currentRecord != null) {
        record = this.currentRecord;
      } else {
        record = getRecordDo(rowIndex);
        this.currentRecord = record;
        this.currentRowIndex = rowIndex;
      }
    }
    return record;
  }

  @Override
  public int getRecordCount() {
    return this.recordCount;
  }

  protected LayerRecord getRecordDo(final int index) {
    final List<LayerRecord> records = this.records;
    synchronized (records) {
      if (index >= 0 && index < records.size()) {
        return records.get(index);
      } else {
        return null;
      }
    }
  }

  protected List<LayerRecord> getRecordsForCache() {
    return Collections.emptyList();
  }

  protected long getRefreshIndex() {
    return this.refreshIndex.get();
  }

  protected long getRefreshIndexNext() {
    return this.refreshIndex.incrementAndGet();
  }

  @Override
  public final ListSelectionModel getSelectionModel() {
    return this.selectionModel;
  }

  protected RecordLayerTableModel getTableModel() {
    return this.model;
  }

  private int indexOf(final LayerRecord record) {
    if (record == null) {
      return -1;
    } else {
      return record.indexOf(this.records);
    }
  }

  protected ListSelectionModel newSelectionModel(final RecordLayerTableModel tableModel) {
    return new RecordLayerListSelectionModel(tableModel);
  }

  private void recordFieldChanged(final LayerRecord record, final String fieldName,
    final Object value) {
    final int rowIndex = indexOf(record);
    if (rowIndex != -1) {
      final RecordLayerTableModel model = getTableModel();
      final int fieldIndex = model.getColumnFieldIndex(fieldName);
      if (fieldIndex == -1) {
        repaint();
      } else {
        model.fireTableCellUpdated(rowIndex, fieldIndex);
      }
    }
  }

  private void recordFieldChanged(final PropertyChangeEvent event) {
    final Object source = event.getSource();
    if (source instanceof LayerRecord) {
      final String propertyName = event.getPropertyName();
      final Object newValue = event.getNewValue();
      recordFieldChanged((LayerRecord)source, propertyName, newValue);
    }
  }

  protected void recordsDeleted(final List<LayerRecord> records) {
    Invoke.later(() -> {
      boolean deleted = false;
      for (final LayerRecord record : records) {
        if (record.getLayer().isDeleted(record)) {
          if (removeCachedRecord(record)) {
            deleted = true;
          }
        }
      }
      if (deleted) {
        repaint();
      }
    });
  }

  protected void recordUpdated(final LayerRecord record) {
    repaint();
  }

  @Override
  public void refresh() {
    Invoke.later(() -> {
      final long refreshIndex = getRefreshIndexNext();
      clearCurrentRecord();
      refresh(refreshIndex);
    });
  }

  public void refresh(final long refreshIndex) {
    final Supplier<List<LayerRecord>> backgroundTask = this::getRecordsForCache;

    final Consumer<List<LayerRecord>> doneTask = (records) -> {
      if (canRefreshFinish(refreshIndex)) {
        this.recordCount = 0; // Set to 0 to avoid array index exceptions
        this.records = records;
        this.recordCount = records.size();
        fireTableDataChanged();
      }
    };

    Invoke.background("Refresh table records", backgroundTask, doneTask);
  }

  protected boolean removeCachedRecord(final LayerRecord record) {
    final int index = record.removeFrom(this.records);
    if (index == -1) {
      return false;
    } else {
      clearCurrentRecord();
      setRecordCount(this.recordCount - 1);
      this.model.fireTableRowsDeleted(index, index);
      return true;
    }
  }

  protected boolean removeCachedRecords(final Iterable<? extends LayerRecord> records) {
    boolean removed = false;
    for (final LayerRecord record : records) {
      removed |= removeCachedRecord(record);
    }
    return removed;
  }

  public void repaint() {
    this.model.repaint();
  }

  protected void setRecordCount(final int recordCount) {
    final int oldValue = getRecordCount();
    this.recordCount = recordCount;
    this.model.firePropertyChange("rowCount", oldValue, getRecordCount());
  }
}
