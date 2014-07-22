package com.revolsys.ui.web.rest.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.revolsys.io.IoConstants;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.io.MapWriter;
import com.revolsys.io.MapWriterFactory;
import com.revolsys.io.json.JsonParser;
import com.revolsys.ui.web.utils.HttpServletUtils;

public class MapHttpMessageConverter extends AbstractHttpMessageConverter<Map> {

  private final IoFactoryRegistry ioFactoryRegistry = IoFactoryRegistry.getInstance();

  public MapHttpMessageConverter() {
    super(Map.class, Collections.singleton(MediaType.APPLICATION_JSON),
      IoFactoryRegistry.getInstance().getMediaTypes(MapWriterFactory.class));
  }

  @Override
  public Map read(final Class<? extends Map> clazz,
    final HttpInputMessage inputMessage) throws IOException,
    HttpMessageNotReadableException {
    try {
      final Map<String, Object> map = new HashMap<String, Object>();
      final InputStream in = inputMessage.getBody();
      final Map<String, Object> readMap = JsonParser.read(in);
      if (readMap != null) {
        map.putAll(readMap);
      }
      return map;
    } catch (final Throwable e) {
      throw new HttpMessageNotReadableException(e.getMessage(), e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void write(final Map map, final MediaType mediaType,
    final HttpOutputMessage outputMessage) throws IOException,
    HttpMessageNotWritableException {
    if (!HttpServletUtils.getResponse().isCommitted()) {
      final Charset charset = HttpServletUtils.setContentTypeWithCharset(
        outputMessage, mediaType);
      final OutputStream body = outputMessage.getBody();
      final String mediaTypeString = mediaType.getType() + "/"
        + mediaType.getSubtype();
      final MapWriterFactory writerFactory = this.ioFactoryRegistry.getFactoryByMediaType(
        MapWriterFactory.class, mediaTypeString);
      final MapWriter writer = writerFactory.getMapWriter(body, charset);
      writer.setProperty(IoConstants.INDENT_PROPERTY, true);
      writer.setProperty(IoConstants.SINGLE_OBJECT_PROPERTY, true);
      final HttpServletRequest request = HttpServletUtils.getRequest();
      String callback = request.getParameter("jsonp");
      if (callback == null) {
        callback = request.getParameter("callback");
      }
      if (callback != null) {
        writer.setProperty(IoConstants.JSONP_PROPERTY, callback);
      }
      final Object title = request.getAttribute(IoConstants.TITLE_PROPERTY);
      if (title != null) {
        writer.setProperty(IoConstants.TITLE_PROPERTY, title);
      }
      writer.write(map);
      writer.close();
    }
  }
}
