package com.revolsys.record.io.format.gpx;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;

import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.LineString;
import com.revolsys.geometry.model.Point;
import com.revolsys.geometry.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.properties.BaseObjectWithProperties;
import com.revolsys.record.Record;
import com.revolsys.record.RecordFactory;
import com.revolsys.record.io.RecordReader;
import com.revolsys.record.io.format.xml.StaxReader;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.spring.resource.Resource;
import com.revolsys.util.Dates;
import com.revolsys.util.MathUtil;

public class GpxIterator extends BaseObjectWithProperties
  implements Iterator<Record>, RecordReader {

  private static final Logger LOG = Logger.getLogger(GpxIterator.class);

  private String baseName;

  private Record currentRecord;

  private File file;

  private final GeometryFactory geometryFactory = GeometryFactory.floating3(4326);

  private boolean hasNext = true;

  private final StaxReader in;

  private int index = 0;

  private boolean loadNextObject = true;

  private final Queue<Record> objects = new LinkedList<>();

  private RecordFactory recordFactory;

  private String schemaName = GpxConstants.GPX_NS_URI;

  private String typePath;

  public GpxIterator(final File file) throws IOException, XMLStreamException {
    this(new FileReader(file));
  }

  public GpxIterator(final Reader in) throws IOException, XMLStreamException {
    this(StaxReader.newXmlReader(in));
  }

  public GpxIterator(final Reader in, final RecordFactory recordFactory, final String path) {
    this(StaxReader.newXmlReader(in));
    this.recordFactory = recordFactory;
    this.typePath = path;
  }

  public GpxIterator(final Resource resource, final RecordFactory recordFactory, final String path)
    throws IOException {
    this(StaxReader.newXmlReader(resource));
    this.recordFactory = recordFactory;
    this.typePath = path;
    this.baseName = resource.getBaseName();
  }

  public GpxIterator(final StaxReader in) {
    this.in = in;
    try {
      in.skipToStartElement();
      skipMetaData();
    } catch (final XMLStreamException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  @Override
  public void close() {
    if (this.in != null) {
      this.in.close();
    }
  }

  @Override
  public RecordDefinition getRecordDefinition() {
    return GpxConstants.GPX_TYPE;
  }

  public String getSchemaName() {
    return this.schemaName;
  }

  @Override
  public boolean hasNext() {
    if (!this.hasNext) {
      return false;
    } else if (this.loadNextObject) {
      return loadNextRecord();
    } else {
      return true;
    }
  }

  @Override
  public Iterator<Record> iterator() {
    return this;
  }

  protected boolean loadNextRecord() {
    try {
      do {
        this.currentRecord = parseRecord();
      } while (this.currentRecord != null && this.typePath != null
        && !this.currentRecord.getRecordDefinition().getPath().equals(this.typePath));
      this.loadNextObject = false;
      if (this.currentRecord == null) {
        close();
        this.hasNext = false;
      }
      return this.hasNext;
    } catch (final XMLStreamException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  @Override
  public Record next() {
    if (hasNext()) {
      this.loadNextObject = true;
      return this.currentRecord;
    } else {
      throw new NoSuchElementException();
    }
  }

  protected Object parseAttribute(final Record record) {
    final String fieldName = this.in.getLocalName();
    final String stringValue = this.in.getElementText();
    Object value;
    if (stringValue == null) {
      value = null;
    } else if (fieldName.equals("time")) {
      value = Dates.getDate("yyyy-MM-dd'T'HH:mm:ss'Z'", stringValue);
    } else {
      value = stringValue;
    }
    if (value != null) {
      record.setValue(fieldName, value);
    }
    return value;
  }

  protected Record parsePoint(final String featureType, final double index)
    throws XMLStreamException {
    final Record record = this.recordFactory.newRecord(GpxConstants.GPX_TYPE);
    record.setValue("dataset_name", this.baseName);
    record.setValue("index", index);
    record.setValue("feature_type", featureType);
    final double lat = Double.parseDouble(this.in.getAttributeValue("", "lat"));
    final double lon = Double.parseDouble(this.in.getAttributeValue("", "lon"));
    double elevation = Double.NaN;

    while (this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      if (this.in.getName().equals(GpxConstants.EXTENSION_ELEMENT)) {
        this.in.skipSubTree();
      } else if (this.in.getName().equals(GpxConstants.ELEVATION_ELEMENT)) {
        elevation = Double.parseDouble(this.in.getElementText());
      } else {
        parseAttribute(record);
      }
    }

    Point point = null;
    if (Double.isNaN(elevation)) {
      point = this.geometryFactory.point(lon, lat);
    } else {
      point = this.geometryFactory.point(lon, lat, elevation);
    }

    record.setValue("location", point);
    return record;
  }

  private Record parseRecord() throws XMLStreamException {
    if (!this.objects.isEmpty()) {
      return this.objects.remove();
    } else {
      if (this.in.getEventType() != XMLStreamConstants.START_ELEMENT) {
        this.in.skipToStartElement();
      }
      while (this.in.getEventType() == XMLStreamConstants.START_ELEMENT) {
        final QName name = this.in.getName();
        if (name.equals(GpxConstants.WAYPOINT_ELEMENT)) {
          return parseWaypoint();
        } else if (name.equals(GpxConstants.TRACK_ELEMENT)) {
          return parseTrack();
        } else if (name.equals(GpxConstants.ROUTE_ELEMENT)) {
          return parseRoute();
        } else {
          this.in.skipSubTree();
          this.in.nextTag();
        }
      }
      return null;
    }
  }

  private Record parseRoute() throws XMLStreamException {
    this.index++;
    final Record record = this.recordFactory.newRecord(GpxConstants.GPX_TYPE);
    record.setValue("dataset_name", this.baseName);
    record.setValue("index", this.index);
    record.setValue("feature_type", "rte");
    final List<Record> pointObjects = new ArrayList<>();
    int axisCount = 2;
    while (this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      if (this.in.getName().equals(GpxConstants.EXTENSION_ELEMENT)) {
        this.in.skipSubTree();
      } else if (this.in.getName().equals(GpxConstants.ROUTE_POINT_ELEMENT)) {
        final double pointIndex = this.index + (pointObjects.size() + 1.0) / 10000;
        final Record pointObject = parseRoutPoint(pointIndex);
        pointObjects.add(pointObject);
        final Point point = pointObject.getGeometry();
        axisCount = Math.max(axisCount, point.getAxisCount());
      } else {
        parseAttribute(record);
      }
    }
    final int vertexCount = pointObjects.size();
    final double[] coordinates = new double[vertexCount * axisCount];
    for (int i = 0; i < vertexCount; i++) {
      final Record pointObject = pointObjects.get(i);
      final Point point = pointObject.getGeometry();
      CoordinatesListUtil.setCoordinates(coordinates, axisCount, i, point);
    }
    final LineString line;
    if (vertexCount > 1) {
      line = this.geometryFactory.lineString(axisCount, coordinates);
    } else {
      line = this.geometryFactory.lineString();
    }

    record.setGeometryValue(line);
    this.objects.addAll(pointObjects);
    return record;
  }

  private Record parseRoutPoint(final double index) throws XMLStreamException {
    final String featureType = "rtept";
    return parsePoint(featureType, index);
  }

  private Record parseTrack() throws XMLStreamException {
    this.index++;
    final Record record = this.recordFactory.newRecord(GpxConstants.GPX_TYPE);
    record.setValue("dataset_name", this.baseName);
    record.setValue("index", this.index);
    record.setValue("feature_type", "trk");
    int axisCount = 2;
    final List<Geometry> parts = new ArrayList<>();
    while (this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      if (this.in.getName().equals(GpxConstants.EXTENSION_ELEMENT)) {
        this.in.skipSubTree();
      } else if (this.in.getName().equals(GpxConstants.TRACK_SEGMENT_ELEMENT)) {
        final Geometry part = parseTrackSegment();
        parts.add(part);
        if (part.getAxisCount() > axisCount) {
          axisCount = part.getAxisCount();
        }
      } else {
        parseAttribute(record);
      }
    }
    final Geometry geometry = this.geometryFactory.convertAxisCount(axisCount).geometry(parts);
    record.setGeometryValue(geometry);
    return record;
  }

  private int parseTrackPoint(final List<Double> points) throws XMLStreamException {
    final String lonText = this.in.getAttributeValue("", "lon");
    final double lon = Double.parseDouble(lonText);
    points.add(lon);

    final String latText = this.in.getAttributeValue("", "lat");
    final double lat = Double.parseDouble(latText);
    points.add(lat);

    int axisCount = 2;

    double z = Double.NaN;
    double m = Double.NaN;
    while (this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      if (this.in.getName().equals(GpxConstants.EXTENSION_ELEMENT)
        || this.in.getName().equals(GpxConstants.TRACK_SEGMENT_ELEMENT)) {
        this.in.skipSubTree();
      } else {
        if (this.in.getName().equals(GpxConstants.ELEVATION_ELEMENT)) {
          final String elevationText = this.in.getElementText();
          final double elevation = Double.parseDouble(elevationText);
          z = elevation;
          if (axisCount < 3) {
            axisCount = 3;
          }
        } else if (this.in.getName().equals(GpxConstants.TIME_ELEMENT)) {
          final String dateText = this.in.getElementText();
          final Calendar calendar = Dates.getIsoCalendar(dateText);
          final long time = calendar.getTimeInMillis();
          m = time;
          if (axisCount < 4) {
            axisCount = 4;
          }
        } else {
          // TODO decide if we want to handle the metadata on a track point
          this.in.skipSubTree();
        }
      }
    }
    points.add(z);
    points.add(m);
    return axisCount;
  }

  private Geometry parseTrackSegment() throws XMLStreamException {
    final List<Double> coordinates = new ArrayList<>();
    int axisCount = 2;
    while (this.in.nextTag() == XMLStreamConstants.START_ELEMENT) {
      final int pointAxisCount = parseTrackPoint(coordinates);
      axisCount = Math.max(axisCount, pointAxisCount);
    }
    if (coordinates.size() == axisCount) {
      return this.geometryFactory.convertAxisCount(axisCount)
        .point(MathUtil.toDoubleArray(coordinates));
    } else {
      return this.geometryFactory.convertAxisCount(axisCount).lineString(4,
        MathUtil.toDoubleArray(coordinates));
    }
  }

  private Record parseWaypoint() throws XMLStreamException {
    this.index++;
    final String featureType = "wpt";
    return parsePoint(featureType, this.index);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  public void setSchemaName(final String schemaName) {
    this.schemaName = schemaName;
  }

  public void skipMetaData() throws XMLStreamException {
    this.in.requireLocalName(GpxConstants.GPX_ELEMENT);
    this.in.skipToStartElement();
    if (this.in.getName().equals(GpxConstants.METADATA_ELEMENT)) {
      this.in.skipSubTree();
      this.in.skipToStartElement();
    }
  }

  @Override
  public String toString() {
    return this.file.getAbsolutePath();
  }

}
