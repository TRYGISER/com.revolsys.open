package com.revolsys.data.io;

import java.util.ArrayList;
import java.util.List;

import com.revolsys.data.query.Query;
import com.revolsys.data.query.functions.F;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.Attribute;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.io.Reader;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.parallel.process.AbstractProcess;

public class RecordStoreQueryTask extends AbstractProcess {

  private final RecordStore recordStore;

  private final BoundingBox boundingBox;

  private List<Record> objects;

  private final String path;

  public RecordStoreQueryTask(final RecordStore recordStore, final String path,
    final BoundingBox boundingBox) {
    this.recordStore = recordStore;
    this.path = path;
    this.boundingBox = boundingBox;
  }

  public void cancel() {
    objects = null;
  }

  @Override
  public String getBeanName() {
    return getClass().getName();
  }

  @Override
  public void run() {
    objects = new ArrayList<Record>();
    final RecordDefinition recordDefinition = recordStore.getRecordDefinition(path);
    final Query query = new Query(recordDefinition);
    final Attribute geometryAttribute = recordDefinition.getGeometryAttribute();
    query.setWhereCondition(F.envelopeIntersects(geometryAttribute, boundingBox));
    try (
      final Reader<Record> reader = recordStore.query(query)) {
      for (final Record object : reader) {
        try {
          objects.add(object);
        } catch (final NullPointerException e) {
          return;
        }
      }
    }
  }

  @Override
  public void setBeanName(final String name) {
  }
}