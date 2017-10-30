package com.revolsys.swing.map.layer.record;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.SwingWorker;

import com.revolsys.collection.iterator.Iterators;
import com.revolsys.collection.map.MapEx;
import com.revolsys.collection.map.Maps;
import com.revolsys.geometry.cs.CoordinateSystem;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.identifier.Identifier;
import com.revolsys.io.BaseCloseable;
import com.revolsys.io.PathName;
import com.revolsys.io.Writer;
import com.revolsys.logging.Logs;
import com.revolsys.predicate.Predicates;
import com.revolsys.record.Record;
import com.revolsys.record.RecordFactory;
import com.revolsys.record.RecordState;
import com.revolsys.record.Records;
import com.revolsys.record.code.CodeTable;
import com.revolsys.record.io.RecordReader;
import com.revolsys.record.io.RecordStoreConnectionManager;
import com.revolsys.record.query.Condition;
import com.revolsys.record.query.In;
import com.revolsys.record.query.Q;
import com.revolsys.record.query.Query;
import com.revolsys.record.query.functions.F;
import com.revolsys.record.query.functions.WithinDistance;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordStore;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.component.BasePanel;
import com.revolsys.swing.component.ValueField;
import com.revolsys.swing.layout.GroupLayouts;
import com.revolsys.swing.map.layer.record.table.model.RecordSaveErrors;
import com.revolsys.swing.parallel.Invoke;
import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;
import com.revolsys.util.Label;
import com.revolsys.util.Property;
import com.revolsys.util.count.LabelCountMap;

public class RecordStoreLayer extends AbstractRecordLayer {
  private BoundingBox loadedBoundingBox = BoundingBox.empty();

  private BoundingBox loadingBoundingBox = BoundingBox.empty();

  private SwingWorker<List<LayerRecord>, Void> loadingWorker;

  /**
   * Caches of sets of {@link Record#getIdentifier()} for different purposes (e.g. selected records, deleted records).
   * Each cache has a separate cacheId. The cache id is recommended to be a private variable to prevent modification
   * of that cache.
   */
  private Map<Label, Set<Identifier>> recordIdentifiersByCacheId = new WeakHashMap<>();

  /** Cache of records from {@link Record#getIdentifier()} to {@link Record}. */
  private Map<Identifier, RecordStoreLayerRecord> recordsByIdentifier = new WeakHashMap<>();

  private RecordStore recordStore;

  private PathName typePath;

  public RecordStoreLayer() {
    this("recordStoreLayer");
  }

  public RecordStoreLayer(final Map<String, ? extends Object> properties) {
    this();
    setProperties(properties);
  }

  public RecordStoreLayer(final RecordStore recordStore, final PathName typePath,
    final boolean exists) {
    this();
    this.recordStore = recordStore;
    setExists(exists);

    setTypePath(typePath);
  }

  protected RecordStoreLayer(final String type) {
    super(type);
  }

  protected void addCachedRecord(final Identifier identifier, final LayerRecord record) {
    synchronized (this.recordsByIdentifier) {
      this.recordsByIdentifier.put(identifier, (RecordStoreLayerRecord)record);
    }
  }

  @Override
  protected boolean addRecordToCache(final Label cacheId, final LayerRecord record) {
    if (isLayerRecord(record)) {
      if (record.getState() == RecordState.DELETED && !isDeleted(record)) {
      } else {
        final Identifier identifier = record.getIdentifier();
        if (identifier == null) {
          return super.addRecordToCache(cacheId, record);
        } else {
          synchronized (getSync()) {
            if (!(record instanceof AbstractProxyLayerRecord)) {
              getCachedRecord(identifier, record, false);
            }
            return Maps.addToSet(this.recordIdentifiersByCacheId, cacheId, identifier);
          }
        }
      }
    }
    return false;
  }

  protected void cancelLoading(final BoundingBox loadedBoundingBox) {
    synchronized (getSync()) {
      if (loadedBoundingBox == this.loadingBoundingBox) {
        firePropertyChange("loaded", false, true);
        this.loadedBoundingBox = BoundingBox.empty();
        this.loadingBoundingBox = BoundingBox.empty();
        this.loadingWorker = null;
      }
    }
  }

  protected Set<Identifier> cleanCachedRecordIds() {
    final Set<Identifier> identifiers = new HashSet<>();
    for (final Set<Identifier> recordIds : this.recordIdentifiersByCacheId.values()) {
      if (recordIds != null) {
        identifiers.addAll(recordIds);
      }
    }
    addProxiedRecordIdsToCollection(identifiers);
    return identifiers;
  }

  /**
  * Remove any cached records that are currently not used.
  */
  @Override
  protected void cleanCachedRecords() {
    synchronized (getSync()) {
      super.cleanCachedRecords();
      final Set<Identifier> identifiers = cleanCachedRecordIds();
      synchronized (this.recordsByIdentifier) {
        this.recordsByIdentifier.keySet().retainAll(identifiers);
      }
    }
  }

  @Override
  public void clearCachedRecords(final Label cacheId) {
    synchronized (getSync()) {
      super.clearCachedRecords(cacheId);
      this.recordIdentifiersByCacheId.remove(cacheId);
    }
  }

  @Override
  public RecordStoreLayer clone() {
    final RecordStoreLayer clone = (RecordStoreLayer)super.clone();
    clone.recordIdentifiersByCacheId = new WeakHashMap<>();
    clone.loadedBoundingBox = BoundingBox.empty();
    clone.loadingBoundingBox = BoundingBox.empty();
    clone.loadingWorker = null;
    clone.recordsByIdentifier = new WeakHashMap<>();
    return clone;
  }

  @Override
  public void delete() {
    super.delete();
    if (this.recordStore != null) {
      final Map<String, String> connectionProperties = getProperty("connection");
      if (connectionProperties != null) {
        final Map<String, Object> config = new HashMap<>();
        config.put("connection", connectionProperties);
        RecordStoreConnectionManager.releaseRecordStore(config);
      }
      this.recordStore = null;
    }
    final SwingWorker<List<LayerRecord>, Void> loadingWorker = this.loadingWorker;
    this.recordIdentifiersByCacheId.clear();
    this.loadedBoundingBox = BoundingBox.empty();
    this.loadingBoundingBox = BoundingBox.empty();
    this.loadingWorker = null;
    this.recordsByIdentifier.clear();
    if (loadingWorker != null) {
      loadingWorker.cancel(true);
    }
  }

  @Override
  protected boolean deleteRecordDo(final LayerRecord record) {
    final Identifier identifier = record.getIdentifier();
    final boolean result = super.deleteRecordDo(record);
    removeFromRecordIdToRecordMap(identifier);
    return result;
  }

  @Override
  protected void forEachRecord(Query query, final Consumer<? super LayerRecord> consumer) {
    if (isExists()) {
      try {
        final RecordStore recordStore = getRecordStore();
        if (recordStore != null && query != null) {
          final Predicate<Record> filter = query.getWhereCondition();
          final Map<String, Boolean> orderBy = query.getOrderBy();

          final List<LayerRecord> changedRecords = new ArrayList<>();
          changedRecords.addAll(getRecordsNew());
          changedRecords.addAll(getRecordsModified());
          Records.filterAndSort(changedRecords, filter, orderBy);
          final Iterator<LayerRecord> changedIterator = changedRecords.iterator();
          LayerRecord currentChangedRecord = Iterators.next(changedIterator);

          final RecordDefinition internalRecordDefinition = getInternalRecordDefinition();
          query = query.newQuery(internalRecordDefinition);
          final Comparator<Record> comparator = Records.newComparatorOrderBy(orderBy);
          try (
            final BaseCloseable booleanValueCloseable = eventsDisabled();
            Transaction transaction = recordStore.newTransaction(Propagation.REQUIRES_NEW);
            final RecordReader reader = newRecordStoreRecordReader(query);) {
            for (LayerRecord record : reader.<LayerRecord> i()) {
              boolean write = true;
              final Identifier identifier = getId(record);
              if (identifier != null) {
                final LayerRecord cachedRecord = this.recordsByIdentifier.get(identifier);
                if (cachedRecord != null) {
                  record = cachedRecord;
                  if (record.isChanged() || isDeleted(record)) {
                    write = false;
                  }
                }
              }
              if (!isDeleted(record) && write) {
                while (currentChangedRecord != null
                  && comparator.compare(currentChangedRecord, record) < 0) {
                  consumer.accept(currentChangedRecord);
                  currentChangedRecord = Iterators.next(changedIterator);
                }
                consumer.accept(record);
              }
            }
            while (currentChangedRecord != null) {
              consumer.accept(currentChangedRecord);
              currentChangedRecord = Iterators.next(changedIterator);
            }
          }
        }
      } catch (final RuntimeException e) {
        Logs.error(this, "Error executing query: " + query, e);
        throw e;
      }
    }
  }

  public <R extends LayerRecord> void forEachRecordsPersisted(final Query query,
    final Consumer<? super R> consumer) {
    if (query != null && isExists()) {
      final RecordStore recordStore = getRecordStore();
      if (recordStore != null) {
        try (
          final BaseCloseable booleanValueCloseable = eventsDisabled();
          Transaction transaction = recordStore.newTransaction(Propagation.REQUIRES_NEW);
          final RecordReader reader = newRecordStoreRecordReader(query);) {
          final LabelCountMap labelCountMap = query.getProperty("statistics");
          for (final LayerRecord record : reader.<LayerRecord> i()) {
            final Identifier identifier = getId(record);
            R proxyRecord = null;
            if (identifier == null) {
              proxyRecord = newProxyLayerRecord(record);
            } else {
              synchronized (getSync()) {
                final LayerRecord cachedRecord = getCachedRecord(identifier, record, true);
                if (!cachedRecord.isDeleted()) {
                  proxyRecord = newProxyLayerRecord(identifier);
                }
              }
            }
            if (proxyRecord != null) {
              consumer.accept(proxyRecord);
              if (labelCountMap != null) {
                labelCountMap.addCount(record);
              }
            }
          }
        }
      }
    }
  }

  @Override
  public BoundingBox getBoundingBox() {
    if (hasGeometryField()) {
      final CoordinateSystem coordinateSystem = getCoordinateSystem();
      if (coordinateSystem != null) {
        return coordinateSystem.getAreaBoundingBox();
      }
    }
    return BoundingBox.empty();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected <R extends LayerRecord> R getCachedRecord(final Identifier identifier) {
    final RecordDefinition recordDefinition = getInternalRecordDefinition();
    final List<String> idFieldNames = recordDefinition.getIdFieldNames();
    if (idFieldNames.isEmpty()) {
      Logs.error(this, this.typePath + " does not have a primary key");
      return null;
    } else {
      synchronized (this.recordsByIdentifier) {
        LayerRecord record = this.recordsByIdentifier.get(identifier);
        if (record == null) {
          final Condition where = getCachedRecordQuery(idFieldNames, identifier);
          final Query query = new Query(recordDefinition, where);
          final RecordStore recordStore = this.recordStore;
          if (recordStore != null) {
            try (
              Transaction transaction = recordStore.newTransaction(Propagation.REQUIRED);
              RecordReader reader = newRecordStoreRecordReader(query)) {
              record = reader.getFirst();
              if (record != null) {
                addCachedRecord(identifier, record);
              }
            }
          }
        }
        return (R)record;
      }
    }
  }

  /**
   * Get the record from the cache if it exists, otherwise add this record to the cache
   *
   * @param identifier
   * @param record
   */
  private LayerRecord getCachedRecord(final Identifier identifier, final LayerRecord record,
    final boolean updateRecord) {
    assert !(record instanceof AbstractProxyLayerRecord);
    synchronized (this.recordsByIdentifier) {
      final RecordStoreLayerRecord cachedRecord = this.recordsByIdentifier.get(identifier);
      if (cachedRecord == null) {
        addCachedRecord(identifier, record);
        return record;
      } else {
        if (updateRecord) {
          cachedRecord.refreshFromRecordStore(record);
        }
        return cachedRecord;
      }
    }
  }

  protected Condition getCachedRecordQuery(final List<String> idFieldNames,
    final Identifier identifier) {
    return Q.equalId(idFieldNames, identifier);
  }

  @Override
  protected Set<Label> getCacheIdsDo(final LayerRecord record) {
    final Set<Label> cacheIds = super.getCacheIdsDo(record);
    final Identifier identifier = record.getIdentifier();
    if (identifier != null) {
      for (final Entry<Label, Set<Identifier>> entry : this.recordIdentifiersByCacheId.entrySet()) {
        final Label cacheId = entry.getKey();
        if (!cacheIds.contains(cacheId)) {
          final Collection<Identifier> identifiers = entry.getValue();
          if (identifiers.contains(identifier)) {
            cacheIds.add(cacheId);
          }
        }
      }
    }
    return cacheIds;
  }

  public FieldDefinition getGeometryField() {
    final RecordDefinition recordDefinition = getRecordDefinition();
    if (recordDefinition == null) {
      return null;
    } else {
      return recordDefinition.getGeometryField();
    }
  }

  protected Identifier getId(final LayerRecord record) {
    if (isLayerRecord(record)) {
      return record.getIdentifier();
    } else {
      return null;
    }
  }

  protected RecordDefinition getInternalRecordDefinition() {
    return getRecordDefinition();
  }

  public BoundingBox getLoadingBoundingBox() {
    return this.loadingBoundingBox;
  }

  @Override
  public PathName getPathName() {
    return this.typePath;
  }

  @Override
  public LayerRecord getRecordById(final Identifier identifier) {
    final LayerRecord record = getCachedRecord(identifier);
    if (record == null) {
      return record;
    } else {
      return newProxyLayerRecord(identifier);
    }
  }

  @Override
  public int getRecordCount(final Query query) {
    int count = 0;
    count += Predicates.count(getRecordsNew(), query.getWhereCondition());
    count += getRecordCountChangeModified(query);
    count += getRecordCountPersisted(query);
    count -= Predicates.count(getRecordsDeleted(), query.getWhereCondition());
    return count;
  }

  @Override
  protected int getRecordCountCached(final Label cacheId) {
    int count = super.getRecordCountCached(cacheId);
    final Set<Identifier> identifiers = this.recordIdentifiersByCacheId.get(cacheId);
    if (identifiers != null) {
      count += identifiers.size();
    }
    return count;
  }

  /**
   * Get the count of the modified records where the original record did not match the filter but
   * the modified record does.
   * @param query
   * @return
   */
  protected int getRecordCountChangeModified(final Query query) {
    final Condition filter = query.getWhereCondition();
    if (filter.isEmpty()) {
      return 0;
    } else {
      int count = 0;
      for (final LayerRecord record : getRecordsModified()) {
        final Record originalRecord = record.getOriginalRecord();
        final boolean modifiedMatches = filter.test(record);
        final boolean originalMatches = filter.test(originalRecord);
        if (modifiedMatches) {
          if (!originalMatches) {
            count++;
          }
        } else {
          if (originalMatches) {
            count--;
          }
        }
      }
      return count;
    }
  }

  @Override
  public int getRecordCountPersisted() {
    if (isExists()) {
      final RecordDefinition recordDefinition = getRecordDefinition();
      final Query query = new Query(recordDefinition);
      return getRecordCountPersisted(query);
    }
    return 0;
  }

  @Override
  public int getRecordCountPersisted(final Query query) {
    if (isExists()) {
      final RecordStore recordStore = getRecordStore();
      if (recordStore != null) {
        try (
          Transaction transaction = recordStore.newTransaction(Propagation.REQUIRES_NEW)) {
          return recordStore.getRecordCount(query);
        }
      }
    }
    return 0;
  }

  protected RecordDefinition getRecordDefinition(final PathName typePath) {
    if (typePath != null) {
      final RecordStore recordStore = getRecordStore();
      if (recordStore != null) {
        return recordStore.getRecordDefinition(typePath);
      }
    }
    return null;
  }

  @SuppressWarnings({
    "unchecked", "rawtypes"
  })
  @Override
  public <R extends LayerRecord> List<R> getRecords(BoundingBox boundingBox) {
    if (hasGeometryField()) {
      boundingBox = convertBoundingBox(boundingBox);
      if (Property.hasValue(boundingBox)) {
        try (
          final BaseCloseable booleanValueCloseable = eventsDisabled()) {
          final BoundingBox queryBoundingBox = convertBoundingBox(boundingBox);
          boolean covers;
          synchronized (getSync()) {
            covers = this.loadedBoundingBox.covers(queryBoundingBox);
          }
          if (covers) {
            final LayerRecordQuadTree index = getIndex();
            return (List)index.queryIntersects(queryBoundingBox);
          } else {
            final List<R> records = (List)getRecordsPersisted(queryBoundingBox);
            return records;
          }
        }
      }
    }
    return Collections.emptyList();
  }

  @Override
  public <R extends LayerRecord> List<R> getRecords(final Geometry geometry,
    final double distance) {
    if (Property.isEmpty(geometry) || !hasGeometryField()) {
      return Collections.emptyList();
    } else {
      final RecordDefinition recordDefinition = getRecordDefinition();
      final FieldDefinition geometryField = getGeometryField();
      final WithinDistance where = F.dWithin(geometryField, geometry, distance);
      final Query query = new Query(recordDefinition, where);
      return getRecords(query);
    }
  }

  @Override
  public List<LayerRecord> getRecordsBackground(BoundingBox boundingBox) {
    if (hasGeometryField()) {
      boundingBox = convertBoundingBox(boundingBox);
      if (Property.hasValue(boundingBox)) {
        synchronized (getSync()) {
          final BoundingBox loadBoundingBox = boundingBox.expandPercent(0.2);
          if (!this.loadedBoundingBox.covers(boundingBox)
            && !this.loadingBoundingBox.covers(boundingBox)) {
            if (this.loadingWorker != null) {
              this.loadingWorker.cancel(true);
            }
            this.loadingBoundingBox = loadBoundingBox;
            this.loadingWorker = newLoadingWorker(loadBoundingBox);
            Invoke.worker(this.loadingWorker);
          }
        }
        final LayerRecordQuadTree index = getIndex();

        final List<LayerRecord> records = index.queryIntersects(boundingBox);
        return records;
      }
    }
    return Collections.emptyList();
  }

  @Override
  public List<LayerRecord> getRecordsCached(final Label cacheId) {
    synchronized (getSync()) {
      final List<LayerRecord> records = super.getRecordsCached(cacheId);
      final Set<Identifier> recordIds = this.recordIdentifiersByCacheId.get(cacheId);
      if (recordIds != null) {
        for (final Identifier recordId : recordIds) {
          final LayerRecord record = getRecordById(recordId);
          if (record != null) {
            records.add(record);
          }
        }
      }
      return records;
    }
  }

  protected List<LayerRecord> getRecordsPersisted(final BoundingBox boundingBox) {
    final RecordDefinition recordDefinition = getInternalRecordDefinition();
    final Query query = Query.intersects(recordDefinition, boundingBox);
    return getRecords(query);
  }

  @Override
  public <R extends LayerRecord> List<R> getRecordsPersisted(final Query query) {
    final List<R> records = new ArrayList<>();
    final Consumer<R> consumer = records::add;
    forEachRecordsPersisted(query, consumer);
    return records;
  }

  @Override
  public RecordStore getRecordStore() {
    return this.recordStore;
  }

  @Override
  protected boolean initializeDo() {
    RecordStore recordStore = this.recordStore;
    if (recordStore == null) {
      final Map<String, String> connectionProperties = getProperty("connection");
      if (connectionProperties == null) {
        Logs.error(this,
          "A record store layer requires a connection entry with a name or url, username, and password: "
            + getPath());
        return false;
      } else {
        final Map<String, Object> config = new HashMap<>();
        config.put("connection", connectionProperties);
        recordStore = RecordStoreConnectionManager.getRecordStore(config);

        if (recordStore == null) {
          Logs.error(this, "Unable to create record store for layer: " + getPath());
          return false;
        } else {
          try {
            recordStore.initialize();
          } catch (final Throwable e) {
            throw new RuntimeException("Unable to iniaitlize record store for layer " + getPath(),
              e);
          }

          setRecordStore(recordStore);
        }
      }
    }
    final PathName typePath = getPathName();
    RecordDefinition recordDefinition = getRecordDefinition();
    if (recordDefinition == null) {
      recordDefinition = getRecordDefinition(typePath);
      if (recordDefinition == null) {
        Logs.error(this, "Cannot find table " + typePath + " for layer " + getPath());
        return false;
      } else {
        setRecordDefinition(recordDefinition);
      }
    }
    initRecordMenu();
    return true;
  }

  @Override
  public boolean isLayerRecord(final Record record) {
    if (record instanceof LayerRecord) {
      final LayerRecord layerRecord = (LayerRecord)record;
      if (layerRecord.getLayer() == this) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isRecordCached(final Label cacheId, final LayerRecord record) {
    if (isLayerRecord(record)) {
      synchronized (getSync()) {
        final Identifier identifier = record.getIdentifier();
        if (identifier != null) {
          if (Maps.collectionContains(this.recordIdentifiersByCacheId, cacheId, identifier)) {
            return true;
          }
        }
        return super.isRecordCached(cacheId, record);
      }
    }
    return false;
  }

  protected Query newBoundingBoxQuery(BoundingBox boundingBox) {
    final RecordDefinition recordDefinition = getInternalRecordDefinition();
    final FieldDefinition geometryField = recordDefinition.getGeometryField();
    boundingBox = convertBoundingBox(boundingBox);
    if (geometryField == null || Property.isEmpty(boundingBox)) {
      return null;
    } else {
      Query query = getQuery();
      query = query.newQuery(recordDefinition);
      query.and(F.envelopeIntersects(geometryField, boundingBox));
      return query;
    }
  }

  @Override
  public LayerRecord newLayerRecord(final Map<String, ? extends Object> values) {
    if (!isReadOnly() && isEditable() && isCanAddRecords()) {

      final RecordDefinition recordDefinition = getRecordDefinition();
      final ArrayLayerRecord newRecord = new RecordStoreLayerRecord(this);
      if (values != null) {
        newRecord.setState(RecordState.INITIALIZING);
        final List<FieldDefinition> idFields = recordDefinition.getIdFields();
        for (final FieldDefinition fieldDefinition : recordDefinition.getFields()) {
          if (!idFields.contains(fieldDefinition)) {
            final String fieldName = fieldDefinition.getName();
            final Object value = values.get(fieldName);
            fieldDefinition.setValue(newRecord, value);
          }
        }
        newRecord.setState(RecordState.NEW);
      }

      addRecordToCache(getCacheIdNew(), newRecord);
      if (isEventsEnabled()) {
        cleanCachedRecords();
      }
      final LayerRecord proxyRecord = new NewProxyLayerRecord(this, newRecord);
      fireRecordInserted(proxyRecord);
      return proxyRecord;
    } else {
      return null;
    }
  }

  @Override
  protected LayerRecord newLayerRecord(final RecordDefinition recordDefinition) {
    final PathName layerTypePath = getPathName();
    if (recordDefinition.getPathName().equals(layerTypePath)) {
      return new RecordStoreLayerRecord(this);
    } else {
      throw new IllegalArgumentException("Cannot create records for " + recordDefinition);
    }
  }

  protected LoadingWorker newLoadingWorker(final BoundingBox boundingBox) {
    return new LoadingWorker(this, boundingBox);
  }

  @Override
  protected ValueField newPropertiesTabGeneralPanelSource(final BasePanel parent) {
    final ValueField panel = super.newPropertiesTabGeneralPanelSource(parent);
    final Map<String, String> connectionProperties = getProperty("connection");
    String connectionName = null;
    String url = null;
    String user = null;
    if (isExists()) {
      final RecordStore recordStore = getRecordStore();
      url = recordStore.getUrl();
      user = recordStore.getUsername();
    }
    if (connectionProperties != null) {
      connectionName = connectionProperties.get("name");
      if (!isExists()) {
        url = connectionProperties.get("url");
        user = connectionProperties.get("user");
      }
    }
    if (connectionName != null) {
      SwingUtil.addLabelledReadOnlyTextField(panel, "Record Store Name", connectionName);
    }
    if (url != null) {
      SwingUtil.addLabelledReadOnlyTextField(panel, "Record Store URL", url);
    }
    if (user != null) {
      SwingUtil.addLabelledReadOnlyTextField(panel, "Record Store Username", user);
    }
    SwingUtil.addLabelledReadOnlyTextField(panel, "Type Path", this.typePath);

    GroupLayouts.makeColumns(panel, 2, true);
    return panel;
  }

  @SuppressWarnings("unchecked")
  protected <V extends LayerRecord> V newProxyLayerRecord(final Identifier identifier) {
    return (V)new IdentifierProxyLayerRecord(this, identifier);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <R extends LayerRecord> R newProxyLayerRecord(LayerRecord record) {
    if (record instanceof AbstractProxyLayerRecord) {
      // Already a proxy
    } else if (RecordState.NEW.equals(record.getState())) {
      record = new NewProxyLayerRecord(this, record);
    } else {
      final Identifier identifier = record.getIdentifier();
      addCachedRecord(identifier, record);
      record = newProxyLayerRecord(identifier);
    }
    return (R)record;
  }

  protected RecordReader newRecordStoreRecordReader(final Query query) {
    final RecordStore recordStore = getRecordStore();
    if (recordStore == null) {
      return RecordReader.empty();
    } else {
      final RecordFactory<LayerRecord> recordFactory = getRecordFactory();
      query.setRecordFactory(recordFactory);
      return recordStore.getRecords(query);
    }
  }

  @Override
  protected boolean postSaveDeletedRecord(final LayerRecord record) {
    final boolean deleted = super.postSaveDeletedRecord(record);
    if (deleted) {
      removeRecordFromCache(this.getCacheIdDeleted(), record);
    }
    return deleted;
  }

  protected void preDeleteRecord(final LayerRecord record) {
  }

  @Override
  protected void refreshDo() {
    synchronized (getSync()) {
      if (this.loadingWorker != null) {
        this.loadingWorker.cancel(true);
      }
      this.loadedBoundingBox = BoundingBox.empty();
      this.loadingBoundingBox = this.loadedBoundingBox;
      super.refreshDo();
    }
    final RecordStore recordStore = getRecordStore();
    final PathName pathName = getPathName();
    final CodeTable codeTable = recordStore.getCodeTable(pathName);
    if (codeTable != null) {
      codeTable.refresh();
    }
    final List<Identifier> identifiers = new ArrayList<>();
    synchronized (this.recordsByIdentifier) {
      identifiers.addAll(this.recordsByIdentifier.keySet());
    }
    if (!identifiers.isEmpty()) {
      identifiers.sort(Identifier.comparator());
      final RecordDefinition recordDefinition = recordStore.getRecordDefinition(pathName);
      final String idFieldName = recordDefinition.getIdFieldName();
      final int pageSize = 999;
      final int identifierCount = identifiers.size();
      for (int i = 0; i < identifiers.size(); i += pageSize) {
        final List<Identifier> queryIdentifiers = identifiers.subList(i,
          Math.min(identifierCount, i + pageSize));
        final In in = Q.in(idFieldName, queryIdentifiers);
        final Query query = new Query(recordDefinition, in);
        try (
          Transaction transaction = recordStore.newTransaction(Propagation.REQUIRES_NEW);
          RecordReader reader = recordStore.getRecords(query)) {
          for (final Record record : reader) {
            final Identifier identifier = record.getIdentifier();
            final RecordStoreLayerRecord cachedRecord = this.recordsByIdentifier.get(identifier);
            if (cachedRecord != null) {
              cachedRecord.refreshFromRecordStore(record);
            }
          }
        }
      }
    }
  }

  private void removeFromRecordIdToRecordMap(final Identifier identifier) {
    synchronized (this.recordsByIdentifier) {
      this.recordsByIdentifier.remove(identifier);
    }
  }

  @Override
  protected boolean removeRecordFromCache(final Label cacheId, final LayerRecord record) {
    boolean removed = false;
    if (isLayerRecord(record)) {
      final Identifier identifier = record.getIdentifier();
      if (identifier != null) {
        synchronized (getSync()) {
          removed = Maps.removeFromSet(this.recordIdentifiersByCacheId, cacheId, identifier);
        }
      }
    }
    removed |= super.removeRecordFromCache(cacheId, record);
    return removed;
  }

  @Override
  protected boolean removeRecordFromCache(final LayerRecord record) {
    boolean removed = false;
    if (isLayerRecord(record)) {
      synchronized (getSync()) {
        final Identifier identifier = record.getIdentifier();
        if (identifier != null) {
          for (final Iterator<Set<Identifier>> iterator = this.recordIdentifiersByCacheId.values()
            .iterator(); iterator.hasNext();) {
            final Set<Identifier> identifiers = iterator.next();
            identifiers.remove(identifier);
            if (identifiers.isEmpty()) {
              iterator.remove();
            }
          }
        }
        removed |= super.removeRecordFromCache(record);
      }
    }
    return removed;
  }

  @Override
  public void revertChanges(final LayerRecord record) {
    removeRecordFromCache(this.getCacheIdDeleted(), record);
    super.revertChanges(record);
  }

  @Override
  protected boolean saveChangesDo(final RecordSaveErrors errors, final LayerRecord record) {
    boolean deleted = super.isDeleted(record);

    if (isExists()) {
      if (this.recordStore != null) {
        final RecordStore recordStore = getRecordStore();
        try (
          Transaction transaction = recordStore.newTransaction(Propagation.REQUIRES_NEW)) {
          try {
            Identifier identifier = record.getIdentifier();
            try (
              final Writer<Record> writer = recordStore.newRecordWriter()) {
              if (isRecordCached(getCacheIdDeleted(), record) || super.isDeleted(record)) {
                preDeleteRecord(record);
                record.setState(RecordState.DELETED);
                writeDelete(writer, record);
                deleted = true;
              } else {
                final RecordDefinition recordDefinition = getRecordDefinition();
                if (super.isNew(record)) {
                  final List<String> idFieldNames = recordDefinition.getIdFieldNames();
                  if (identifier == null && !idFieldNames.isEmpty()) {
                    identifier = recordStore.newPrimaryIdentifier(this.typePath);
                    if (identifier != null) {
                      identifier.setIdentifier(record, idFieldNames);
                    }
                  }
                }
                final int fieldCount = recordDefinition.getFieldCount();
                for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                  record.validateField(fieldIndex);
                }
                if (super.isModified(record)) {
                  writeUpdate(writer, record);
                } else if (super.isNew(record)) {
                  writer.write(record);
                }
              }
            }
            if (!deleted) {
              record.setState(RecordState.PERSISTED);
            }
            removeFromRecordIdToRecordMap(identifier);
            return true;
          } catch (final Throwable e) {
            throw transaction.setRollbackOnly(e);
          }
        }
      }
      if (deleted) {
        firePropertyChange(RECORD_DELETED_PERSISTED, null, record.newRecordProxy());
      }
    }
    return false;
  }

  protected void setIndexRecords(final BoundingBox loadedBoundingBox,
    final List<LayerRecord> records) {
    synchronized (getSync()) {
      if (loadedBoundingBox == this.loadingBoundingBox) {
        setIndexRecords(records);
        firePropertyChange("loaded", false, true);
        this.loadedBoundingBox = this.loadingBoundingBox;
        this.loadingBoundingBox = BoundingBox.empty();
        this.loadingWorker = null;
      }
    }
    firePropertyChange("refresh", false, true);
  }

  @Override
  public void setProperty(final String name, final Object value) {
    if ("typePath".equals(name)) {
      super.setProperty(name, PathName.newPathName(value));
    } else {
      super.setProperty(name, value);
    }
  }

  public void setRecordsToCache(final Label cacheId,
    final Collection<? extends LayerRecord> records) {
    synchronized (getSync()) {
      this.recordIdentifiersByCacheId.put(cacheId, new HashSet<>());
      addRecordsToCache(cacheId, records);
    }
  }

  protected void setRecordStore(final RecordStore recordStore) {
    this.recordStore = recordStore;
  }

  public void setTypePath(final PathName typePath) {
    this.typePath = typePath;
    if (this.typePath != null) {
      if (!Property.hasValue(getName())) {
        setName(this.typePath.getName());
      }
    }
    if (isExists()) {
      final RecordDefinition recordDefinition = getRecordDefinition(typePath);
      setRecordDefinition(recordDefinition);
    }
  }

  @Override
  public void showForm(final LayerRecord record, final String fieldName) {
    if (record != null) {
      final Identifier identifier = getId(record);
      if (identifier != null) {
        addRecordToCache(getCacheIdForm(), record);
      }
      final LayerRecord proxyRecord = record.newRecordProxy();
      super.showForm(proxyRecord, fieldName);
    }
  }

  @Override
  public MapEx toMap() {
    final MapEx map = super.toMap();
    addToMap(map, "typePath", this.typePath);
    return map;
  }

  protected void writeDelete(final Writer<Record> writer, final LayerRecord record) {
    writer.write(record);
  }

  protected void writeUpdate(final Writer<Record> writer, final LayerRecord record) {
    writer.write(record);
  }

}
