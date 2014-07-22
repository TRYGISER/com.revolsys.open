package com.revolsys.gis.web.rest.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.io.DataObjectReader;
import com.revolsys.gis.data.io.DataObjectReaderFactory;
import com.revolsys.gis.data.io.DataObjectWriterFactory;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoConstants;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.io.Reader;
import com.revolsys.io.Writer;
import com.revolsys.io.kml.Kml22Constants;
import com.revolsys.spring.InputStreamResource;
import com.revolsys.ui.web.rest.converter.AbstractHttpMessageConverter;
import com.revolsys.ui.web.utils.HttpServletUtils;
import com.vividsolutions.jts.geom.Geometry;

public class DataObjectReaderHttpMessageConverter extends
AbstractHttpMessageConverter<DataObjectReader> {

  private List<String> requestAttributeNames = Arrays.asList(
    IoConstants.SINGLE_OBJECT_PROPERTY, Kml22Constants.STYLE_URL_PROPERTY,
    Kml22Constants.LOOK_AT_POINT_PROPERTY,
    Kml22Constants.LOOK_AT_RANGE_PROPERTY,
    Kml22Constants.LOOK_AT_MIN_RANGE_PROPERTY,
    Kml22Constants.LOOK_AT_MAX_RANGE_PROPERTY, IoConstants.JSONP_PROPERTY,
    IoConstants.TITLE_PROPERTY, IoConstants.DESCRIPTION_PROPERTY);

  private GeometryFactory geometryFactory;

  private final IoFactoryRegistry ioFactoryRegistry = IoFactoryRegistry.getInstance();

  public DataObjectReaderHttpMessageConverter() {
    super(DataObjectReader.class, IoFactoryRegistry.getInstance()
      .getMediaTypes(DataObjectReaderFactory.class),
      IoFactoryRegistry.getInstance().getMediaTypes(
        DataObjectWriterFactory.class));
  }

  public GeometryFactory getGeometryFactory() {
    return this.geometryFactory;
  }

  public List<String> getRequestAttributeNames() {
    return this.requestAttributeNames;
  }

  @Override
  public DataObjectReader read(final Class<? extends DataObjectReader> clazz,
    final HttpInputMessage inputMessage) throws IOException,
    HttpMessageNotReadableException {
    try {
      final HttpHeaders headers = inputMessage.getHeaders();
      final MediaType mediaType = headers.getContentType();
      Charset charset = mediaType.getCharSet();
      if (charset == null) {
        charset = FileUtil.UTF8;
      }
      final InputStream body = inputMessage.getBody();
      final String mediaTypeString = mediaType.getType() + "/"
          + mediaType.getSubtype();
      final DataObjectReaderFactory readerFactory = this.ioFactoryRegistry.getFactoryByMediaType(
        DataObjectReaderFactory.class, mediaTypeString);
      if (readerFactory == null) {
        throw new HttpMessageNotReadableException("Cannot read data in format"
            + mediaType);
      } else {
        final Reader<DataObject> reader = readerFactory.createDataObjectReader(new InputStreamResource(
          "dataObjectInput", body));

        GeometryFactory factory = this.geometryFactory;
        final ServletWebRequest requestAttributes = (ServletWebRequest)RequestContextHolder.getRequestAttributes();
        final String srid = requestAttributes.getParameter("srid");
        if (srid != null && srid.trim().length() > 0) {
          factory = GeometryFactory.getFactory(Integer.parseInt(srid));
        }
        reader.setProperty(IoConstants.GEOMETRY_FACTORY, factory);
        return (DataObjectReader)reader;
      }
    } catch (final IOException e) {
      throw new HttpMessageNotReadableException("Error reading data", e);
    }
  }

  public void setGeometryFactory(final GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
  }

  public void setRequestAttributeNames(final List<String> requestAttributeNames) {
    this.requestAttributeNames = requestAttributeNames;
  }

  @Override
  public void write(final DataObjectReader reader, final MediaType mediaType,
    final HttpOutputMessage outputMessage) throws IOException,
    HttpMessageNotWritableException {
    if (!HttpServletUtils.getResponse().isCommitted()) {
      MediaType actualMediaType;
      if (mediaType == null) {
        actualMediaType = getDefaultMediaType();
      } else {
        actualMediaType = mediaType;
      }
      if (actualMediaType != null) {
        final Charset charset = HttpServletUtils.setContentTypeWithCharset(
          outputMessage, actualMediaType);
        final String mediaTypeString = actualMediaType.getType() + "/"
            + actualMediaType.getSubtype();
        final DataObjectWriterFactory writerFactory = this.ioFactoryRegistry.getFactoryByMediaType(
          DataObjectWriterFactory.class, mediaTypeString);
        if (writerFactory == null) {
          throw new IllegalArgumentException("Media type " + actualMediaType
            + " not supported");
        } else {
          final DataObjectMetaData metaData = reader.getMetaData();
          final HttpHeaders headers = outputMessage.getHeaders();
          final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
          String baseName = (String)requestAttributes.getAttribute(
            "contentDispositionFileName", RequestAttributes.SCOPE_REQUEST);
          if (baseName == null) {
            baseName = HttpServletUtils.getRequestBaseFileName();
          }
          String contentDisposition = (String)requestAttributes.getAttribute(
            "contentDisposition", RequestAttributes.SCOPE_REQUEST);
          if (contentDisposition == null) {
            contentDisposition = "attachment";
          }
          final String fileName = baseName + "."
              + writerFactory.getFileExtension(mediaTypeString);
          headers.set("Content-Disposition", contentDisposition + "; filename="
              + fileName);

          final OutputStream body = outputMessage.getBody();
          final Writer<DataObject> writer = writerFactory.createDataObjectWriter(
            baseName, metaData, body, charset);
          if (Boolean.FALSE.equals(requestAttributes.getAttribute("wrapHtml",
            RequestAttributes.SCOPE_REQUEST))) {
            writer.setProperty(IoConstants.WRAP_PROPERTY, false);
          }
          final HttpServletRequest request = HttpServletUtils.getRequest();
          String callback = request.getParameter("jsonp");
          if (callback == null) {
            callback = request.getParameter("callback");
          }
          if (callback != null) {
            writer.setProperty(IoConstants.JSONP_PROPERTY, callback);
          }
          for (final String attributeName : requestAttributes.getAttributeNames(RequestAttributes.SCOPE_REQUEST)) {
            final Object value = requestAttributes.getAttribute(attributeName,
              RequestAttributes.SCOPE_REQUEST);
            if (value != null && attributeName.startsWith("java:")
                || this.requestAttributeNames.contains(attributeName)) {
              writer.setProperty(attributeName, value);
            }
          }

          final Iterator<DataObject> iterator = reader.iterator();
          if (iterator.hasNext()) {
            DataObject dataObject = iterator.next();
            final Geometry geometry = dataObject.getGeometryValue();
            if (geometry != null) {
              final GeometryFactory geometryFactory = GeometryFactory.getFactory(geometry);
              writer.setProperty(IoConstants.GEOMETRY_FACTORY, geometryFactory);
            }

            writer.write(dataObject);
            while (iterator.hasNext()) {
              dataObject = iterator.next();
              writer.write(dataObject);

            }
          }
          writer.close();
        }
      }
    }
  }
}
