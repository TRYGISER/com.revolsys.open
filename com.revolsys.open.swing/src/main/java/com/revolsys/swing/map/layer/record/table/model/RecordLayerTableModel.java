package com.revolsys.swing.map.layer.record.table.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JMenuItem;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SortOrder;

import com.revolsys.beans.PropertyChangeSupportProxy;
import com.revolsys.collection.CollectionUtil;
import com.revolsys.collection.list.Lists;
import com.revolsys.datatype.DataType;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.record.Record;
import com.revolsys.record.Records;
import com.revolsys.record.query.Condition;
import com.revolsys.record.query.Query;
import com.revolsys.record.query.functions.F;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.swing.EventQueue;
import com.revolsys.swing.action.RunnableAction;
import com.revolsys.swing.listener.EventQueueRunnableListener;
import com.revolsys.swing.map.layer.Project;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.map.layer.record.LayerRecordMenu;
import com.revolsys.swing.map.layer.record.table.RecordLayerTable;
import com.revolsys.swing.menu.BaseJPopupMenu;
import com.revolsys.swing.parallel.Invoke;
import com.revolsys.swing.table.BaseJTable;
import com.revolsys.swing.table.SortableTableModel;
import com.revolsys.swing.table.record.filter.RecordRowPredicateRowFilter;
import com.revolsys.swing.table.record.model.RecordRowTableModel;
import com.revolsys.util.Property;

public class RecordLayerTableModel extends RecordRowTableModel
  implements SortableTableModel, PropertyChangeSupportProxy {
  private static final ModeEmpty MODE_EMPTY = new ModeEmpty();

  public static final String MODE_RECORDS_ALL = "all";

  public static final String MODE_RECORDS_CHANGED = "edits";

  public static final String MODE_RECORDS_SELECTED = "selected";

  private static final long serialVersionUID = 1L;

  public static RecordLayerTable newTable(final AbstractRecordLayer layer) {
    return newTable(layer, layer.getFieldNamesSet());
  }

  public static RecordLayerTable newTable(final AbstractRecordLayer layer,
    final Collection<String> fieldNames) {
    final RecordDefinition recordDefinition = layer.getRecordDefinition();
    if (recordDefinition == null) {
      return null;
    } else {
      final RecordLayerTableModel model = new RecordLayerTableModel(layer, fieldNames);
      final RecordLayerTable table = new RecordLayerTable(model);

      model.selectionChangedListener = EventQueue.addPropertyChange(layer, "hasSelectedRecords",
        () -> selectionChanged(table, model));
      return table;
    }
  }

  public static RecordLayerTable newTable(final AbstractRecordLayer layer,
    final String... fieldNames) {
    return newTable(layer, Arrays.asList(fieldNames));
  }

  public static final void selectionChanged(final RecordLayerTable table,
    final RecordLayerTableModel tableModel) {
    table.repaint();
  }

  private TableRecordsMode tableRecordsMode;

  private final Map<String, TableRecordsMode> tableRecordsModeByKey = new LinkedHashMap<>();

  private Condition filter = Condition.ALL;

  private boolean filterByBoundingBox;

  private final LinkedList<Condition> filterHistory = new LinkedList<>();

  private final AbstractRecordLayer layer;

  private Map<String, Boolean> orderBy;

  private Comparator<Record> orderByComparatorIdentifier = null;

  private RowFilter<RecordRowTableModel, Integer> rowFilterCondition = null;

  private EventQueueRunnableListener selectionChangedListener;

  private final Object sync = new Object();

  private boolean useRecordMenu = true;

  public RecordLayerTableModel(final AbstractRecordLayer layer,
    final Collection<String> fieldNames) {
    super(layer.getRecordDefinition());
    this.layer = layer;
    setFieldNames(fieldNames);
    setEditable(true);
    setReadOnlyFieldNames(layer.getUserReadOnlyFieldNames());
    final String idFieldName = getRecordDefinition().getIdFieldName();
    setSortOrder(idFieldName);

    addFieldFilterMode(new ModeAllPaged(this));
    addFieldFilterMode(new ModeChanged(this));
    addFieldFilterMode(new ModeSelected(this));
  }

  protected void addFieldFilterMode(final TableRecordsMode tableRecordsMode) {
    final String key = tableRecordsMode.getKey();
    this.tableRecordsModeByKey.put(key, tableRecordsMode);
  }

  @Override
  public void dispose() {
    getTable().setSelectionModel(null);
    Property.removeListener(this.layer, "hasSelectedRecords", this.selectionChangedListener);
    final TableRecordsMode tableRecordsMode = getTableRecordsMode();
    if (tableRecordsMode != null) {
      tableRecordsMode.deactivate();
    }
    this.selectionChangedListener = null;
    super.dispose();
  }

  public void exportRecords(final Object target) {
    final TableRecordsMode tableRecordsMode = getTableRecordsMode();
    if (tableRecordsMode != null) {
      final Query query = getFilterQuery();
      tableRecordsMode.exportRecords(query, target);
    }
  }

  public List<TableRecordsMode> getFieldFilterModes() {
    return Lists.toArray(this.tableRecordsModeByKey.values());
  }

  public Condition getFilter() {
    return this.filter;
  }

  public LinkedList<Condition> getFilterHistory() {
    return this.filterHistory;
  }

  protected Query getFilterQuery() {
    final Query query = this.layer.getQuery();
    final Condition filter = getFilter();
    query.and(filter);
    query.setOrderBy(this.orderBy);
    if (this.filterByBoundingBox) {
      final Project project = this.layer.getProject();
      final BoundingBox viewBoundingBox = project.getViewBoundingBox();
      final RecordDefinition recordDefinition = this.layer.getRecordDefinition();
      final FieldDefinition geometryField = recordDefinition.getGeometryField();
      if (geometryField != null) {
        query.and(F.envelopeIntersects(geometryField, viewBoundingBox));
      }
    }
    return query;
  }

  public String getGeometryFilterMode() {
    if (this.filterByBoundingBox) {
      return "boundingBox";
    }
    return "all";
  }

  public AbstractRecordLayer getLayer() {
    return this.layer;
  }

  @Override
  public BaseJPopupMenu getMenu(final int rowIndex, final int columnIndex) {
    final LayerRecord record = getRecord(rowIndex);
    if (record != null) {
      final AbstractRecordLayer layer = getLayer();
      if (layer != null) {
        LayerRecordMenu.setEventRecord(record);
        if (isUseRecordMenu()) {
          final LayerRecordMenu menu = record.getMenu();

          final BaseJPopupMenu popupMenu = menu.newJPopupMenu();
          popupMenu.addSeparator();
          final RecordLayerTable table = getTable();
          final boolean cellEditable = isCellEditable(rowIndex, columnIndex);

          final Object value = getValueAt(rowIndex, columnIndex);

          final boolean canCopy = Property.hasValue(value);
          if (cellEditable) {
            final JMenuItem cutMenu = RunnableAction.newMenuItem("Cut Field Value", "cut",
              table::cutFieldValue);
            cutMenu.setEnabled(canCopy);
            popupMenu.add(cutMenu);
          }

          final JMenuItem copyMenu = RunnableAction.newMenuItem("Copy Field Value", "page_copy",
            table::copyFieldValue);
          copyMenu.setEnabled(canCopy);
          popupMenu.add(copyMenu);

          if (cellEditable) {
            popupMenu.add(RunnableAction.newMenuItem("Paste Field Value", "paste_plain",
              table::pasteFieldValue));
          }
          return popupMenu;
        } else {
          return super.getMenu().newJPopupMenu();
        }
      }
    }
    return null;
  }

  public Map<String, Boolean> getOrderBy() {
    return this.orderBy;
  }

  public Comparator<Record> getOrderByComparatorIdentifier() {
    return this.orderByComparatorIdentifier;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V extends Record> V getRecord(final int rowIndex) {
    LayerRecord record = null;
    if (rowIndex >= 0) {
      final TableRecordsMode tableRecordsMode = getTableRecordsMode();
      if (tableRecordsMode != null) {
        record = tableRecordsMode.getRecord(rowIndex);
      }
    }
    return (V)record;
  }

  @Override
  public final int getRowCount() {
    final TableRecordsMode tableRecordsMode = getTableRecordsMode();
    if (tableRecordsMode == null) {
      return 0;
    } else {
      return tableRecordsMode.getRecordCount();
    }
  }

  public RowFilter<RecordRowTableModel, Integer> getRowFilter() {
    if (isSortable()) {
      return this.rowFilterCondition;
    } else {
      return null;
    }
  }

  public ListSelectionModel getSelectionModel() {
    final TableRecordsMode tableRecordsMode = getTableRecordsMode();
    if (tableRecordsMode == null) {
      return null;
    } else {
      return tableRecordsMode.getSelectionModel();
    }
  }

  @Override
  public RecordLayerTable getTable() {
    return (RecordLayerTable)super.getTable();
  }

  public TableRecordsMode getTableRecordsMode() {
    if (this.tableRecordsMode == null || !this.tableRecordsMode.isEnabled()) {
      setTableRecordsMode(CollectionUtil.get(this.tableRecordsModeByKey.values(), 0));
    }
    return this.tableRecordsMode;
  }

  public TableRecordsMode getTableRecordsMode(final String key) {
    return this.tableRecordsModeByKey.get(key);
  }

  public String getTypeName() {
    return getRecordDefinition().getPath();
  }

  @Override
  protected boolean isCellEditable(final int rowIndex, final int columnIndex, final Record record) {
    final AbstractRecordLayer layer = getLayer();
    final LayerRecord layerRecord = (LayerRecord)record;
    if (layer.isDeleted(layerRecord)) {
      return false;
    } else if (layer.isCanEditRecords() || layer.isNew(layerRecord) && layer.isCanAddRecords()) {
      return super.isCellEditable(rowIndex, columnIndex, record);
    } else {
      return false;
    }
  }

  public boolean isDeleted(final int rowIndex) {
    final LayerRecord record = getRecord(rowIndex);
    if (record != null) {
      final AbstractRecordLayer layer = getLayer();
      if (layer != null) {
        return layer.isDeleted(record);
      }
    }
    return false;
  }

  @Override
  public boolean isEditable() {
    return super.isEditable() && this.layer.isEditable() && this.layer.isCanEditRecords();
  }

  public boolean isFilterByBoundingBox() {
    return this.filterByBoundingBox;
  }

  public boolean isFilterByBoundingBoxSupported() {
    if (this.tableRecordsMode == null) {
      return true;
    }
    return this.tableRecordsMode.isFilterByBoundingBoxSupported();
  }

  public boolean isHasFilter() {
    return this.filter != null && !this.filter.isEmpty();
  }

  public boolean isHasFilterHistory() {
    return !this.filterHistory.isEmpty();
  }

  @Override
  public boolean isSelected(final boolean selected, final int rowIndex, final int columnIndex) {
    final LayerRecord record = getRecord(rowIndex);
    return this.layer.isSelected(record);
  }

  public boolean isSortable() {
    final TableRecordsMode tableRecordsMode = getTableRecordsMode();
    if (tableRecordsMode == null) {
      return false;
    } else {
      return tableRecordsMode.isSortable();
    }
  }

  public boolean isUseRecordMenu() {
    return this.useRecordMenu;
  }

  public void refresh() {
    final TableRecordsMode tableRecordsMode = getTableRecordsMode();
    if (tableRecordsMode != null) {
      tableRecordsMode.refresh();
    }
  }

  protected void repaint() {
    final RecordLayerTable table = getTable();
    if (table != null) {
      table.repaint();
    }
  }

  @Override
  public void setFieldNames(final Collection<String> fieldNames) {
    final List<String> fieldTitles = new ArrayList<>();
    for (final String fieldName : fieldNames) {
      final String fieldTitle = this.layer.getFieldTitle(fieldName);
      fieldTitles.add(fieldTitle);
    }
    super.setFieldNamesAndTitles(fieldNames, fieldTitles);
  }

  public void setFieldNames(final String... fieldNames) {
    setFieldNames(Arrays.asList(fieldNames));
  }

  public void setFieldNamesSetName(final String fieldNamesSetName) {
    this.layer.setFieldNamesSetName(fieldNamesSetName);
    final List<String> fieldNamesSet = this.layer.getFieldNamesSet();
    setFieldNames(fieldNamesSet);
  }

  public void setFilter(final Condition filter) {
    Invoke.later(() -> {
      final Condition filter2;
      if (filter == null) {
        filter2 = Condition.ALL;
      } else {
        filter2 = filter;
      }
      if (!DataType.equal(filter2, this.filter)) {
        final Object oldValue = this.filter;
        this.filter = filter2;
        if (Property.isEmpty(filter2)) {
          this.rowFilterCondition = null;
        } else {
          this.rowFilterCondition = new RecordRowPredicateRowFilter(filter2);
          if (!DataType.equal(oldValue, filter2)) {
            this.filterHistory.remove(filter2);
            this.filterHistory.addFirst(filter2);
            while (this.filterHistory.size() > 20) {
              this.filterHistory.removeLast();
            }
            firePropertyChange("hasFilterHistory", false, true);
          }
        }
        if (isSortable()) {
          final RecordLayerTable table = getTable();
          table.setRowFilter(this.rowFilterCondition);
        } else {
          refresh();
        }
        firePropertyChange("filter", oldValue, this.filter);
        final boolean hasFilter = isHasFilter();
        firePropertyChange("hasFilter", !hasFilter, hasFilter);
      }
    });
  }

  public void setFilterByBoundingBox(boolean filterByBoundingBox) {
    final String geometryFilterMode = getGeometryFilterMode();
    final String oldValue = geometryFilterMode;
    if (!this.tableRecordsMode.isFilterByBoundingBoxSupported()) {
      filterByBoundingBox = false;
    }
    if (this.filterByBoundingBox != filterByBoundingBox) {
      this.filterByBoundingBox = filterByBoundingBox;
      refresh();
    }
    firePropertyChange("geometryFilterMode", oldValue, geometryFilterMode);
  }

  public String setGeometryFilterMode(final String mode) {
    final boolean filterByBoundingBox = "boundingBox".equals(mode);
    setFilterByBoundingBox(filterByBoundingBox);
    return getGeometryFilterMode();
  }

  public void setOrderBy(final Map<String, Boolean> orderBy) {
    setOrderByInternal(orderBy);
    final Map<Integer, SortOrder> sortedColumns = new LinkedHashMap<>();
    for (final Entry<String, Boolean> entry : orderBy.entrySet()) {
      if (orderBy != null) {
        final String fieldName = entry.getKey();
        final Boolean order = entry.getValue();
        final int index = getColumnFieldIndex(fieldName);
        if (index != -1) {
          SortOrder sortOrder;
          if (order) {
            sortOrder = SortOrder.ASCENDING;
          } else {
            sortOrder = SortOrder.DESCENDING;
          }
          sortedColumns.put(index, sortOrder);
        }
      }
    }
    setSortedColumns(sortedColumns);
  }

  private void setOrderByInternal(final Map<String, Boolean> orderBy) {
    if (Property.hasValue(orderBy)) {
      this.orderBy = orderBy;
      this.orderByComparatorIdentifier = Records.newComparatorOrderByIdentifier(orderBy);
    } else {
      this.orderBy = Collections.emptyMap();
      this.orderByComparatorIdentifier = null;
    }
  }

  @Override
  public SortOrder setSortOrder(final int columnIndex) {
    final SortOrder sortOrder = super.setSortOrder(columnIndex);
    final String fieldName = getColumnFieldName(columnIndex);
    if (Property.hasValue(fieldName)) {
      Map<String, Boolean> orderBy;
      if (sortOrder == SortOrder.ASCENDING) {
        orderBy = Collections.singletonMap(fieldName, true);
      } else if (sortOrder == SortOrder.DESCENDING) {
        orderBy = Collections.singletonMap(fieldName, false);
      } else {
        orderBy = Collections.singletonMap(fieldName, true);
      }
      if (this.sync == null) {
        setOrderByInternal(orderBy);
      } else {
        setOrderByInternal(orderBy);
        refresh();
      }
    }
    return sortOrder;
  }

  @Override
  public SortOrder setSortOrder(final int columnIndex, final SortOrder sortOrder) {
    super.setSortOrder(columnIndex, sortOrder);
    final String fieldName = getColumnFieldName(columnIndex);
    if (Property.hasValue(fieldName)) {
      Map<String, Boolean> orderBy;
      if (sortOrder == SortOrder.ASCENDING) {
        orderBy = Collections.singletonMap(fieldName, true);
      } else if (sortOrder == SortOrder.DESCENDING) {
        orderBy = Collections.singletonMap(fieldName, false);
      } else {
        orderBy = Collections.singletonMap(fieldName, true);
      }
      if (this.sync == null) {
        setOrderByInternal(orderBy);
      } else {
        setOrderByInternal(orderBy);
        refresh();
      }
    }
    return sortOrder;
  }

  @Override
  public void setTable(final BaseJTable table) {
    super.setTable(table);
    final ListSelectionModel selectionModel = getSelectionModel();
    table.setSelectionModel(selectionModel);
  }

  public void setTableRecordsMode(final TableRecordsMode tableRecordsMode) {
    Invoke.later(() -> {
      final TableRecordsMode oldMode = this.tableRecordsMode;
      final RecordLayerTable table = getTable();
      if (table != null && tableRecordsMode != null && tableRecordsMode != oldMode) {
        if (oldMode != null) {
          oldMode.deactivate();
        }
        final String oldGeometryFilterMode = getGeometryFilterMode();
        this.tableRecordsMode = MODE_EMPTY;
        fireTableDataChanged();
        table.setSortable(false);
        table.setSelectionModel(null);
        table.setRowFilter(null);

        tableRecordsMode.activate();

        final ListSelectionModel selectionModel = tableRecordsMode.getSelectionModel();
        table.setSelectionModel(selectionModel);

        final boolean sortable = tableRecordsMode.isSortable();
        table.setSortable(sortable);

        final RowFilter<RecordRowTableModel, Integer> rowFilter = getRowFilter();
        table.setRowFilter(rowFilter);

        final boolean filterByBoundingBoxSupported = tableRecordsMode
          .isFilterByBoundingBoxSupported();
        if (!filterByBoundingBoxSupported) {
          this.filterByBoundingBox = false;
        }
        this.tableRecordsMode = tableRecordsMode;

        refresh();
        firePropertyChange("tableRecordsMode", oldMode, this.tableRecordsMode);
        firePropertyChange("geometryFilterMode", oldGeometryFilterMode, getGeometryFilterMode());
        firePropertyChange("filterByBoundingBox", !this.filterByBoundingBox,
          this.filterByBoundingBox);
        firePropertyChange("filterByBoundingBoxSupported", !filterByBoundingBoxSupported,
          filterByBoundingBoxSupported);
      }
    });
  }

  public void setUseRecordMenu(final boolean useRecordMenu) {
    this.useRecordMenu = useRecordMenu;
  }

  @Override
  public String toDisplayValueInternal(final int rowIndex, final int fieldIndex,
    final Object objectValue) {
    if (objectValue == null) {
      final String fieldName = getColumnFieldName(fieldIndex);
      final RecordDefinition recordDefinition = getRecordDefinition();
      final List<String> idFieldNames = recordDefinition.getIdFieldNames();
      if (idFieldNames.contains(fieldName)) {
        return "NEW";
      }
    }
    return super.toDisplayValueInternal(rowIndex, fieldIndex, objectValue);
  }
}
