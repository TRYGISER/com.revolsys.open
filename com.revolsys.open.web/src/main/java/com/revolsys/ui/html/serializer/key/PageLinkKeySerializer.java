package com.revolsys.ui.html.serializer.key;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.revolsys.io.xml.XmlWriter;
import com.revolsys.ui.html.HtmlUtil;
import com.revolsys.ui.html.builder.HtmlUiBuilder;
import com.revolsys.ui.html.builder.HtmlUiBuilderAware;
import com.revolsys.util.JavaBeanUtil;

public class PageLinkKeySerializer extends AbstractKeySerializer implements
  HtmlUiBuilderAware<HtmlUiBuilder<?>> {
  private String pageName;

  private Map<String, String> parameterKeys = new LinkedHashMap<String, String>();

  private HtmlUiBuilder<?> uiBuilder;

  public String getPageName() {
    return pageName;
  }

  public Map<String, String> getParameterKeys() {
    return parameterKeys;
  }

  public HtmlUiBuilder<?> getUiBuilder() {
    return uiBuilder;
  }

  public void serialize(final XmlWriter out, final Object object) {
    try {
      HtmlUiBuilder<? extends Object> uiBuilder = this.uiBuilder;
      final String[] parts = getKey().split("\\.");
      Object currentObject = object;
      String key = parts[0];
      for (int i = 0; i < parts.length - 1; i++) {
        currentObject = JavaBeanUtil.getValue(currentObject, key);
        if (currentObject == null) {
          uiBuilder.serializeNullLabel(out, key);
          return;
        }

        uiBuilder = uiBuilder.getBuilder(currentObject);
        if (uiBuilder == null) {
          final String message = currentObject.getClass().getName()
            + " does not have a property " + key;
          out.element(HtmlUtil.B, message);
          return;
        }
        key = parts[i + 1];

      }
      uiBuilder.serializeLink(out, currentObject, key, pageName, parameterKeys);
    } catch (final Throwable e) {
      Logger.getLogger(getClass()).error("Unable to serialize " + pageName, e);
    }
  }

  public void serializeValue(final XmlWriter out) {
    out.text(getLabel());
  }

  public void setHtmlUiBuilder(final HtmlUiBuilder<?> uiBuilder) {
    this.uiBuilder = uiBuilder;
  }

  public void setPageName(final String pageName) {
    this.pageName = pageName;
  }

  public void setParameterKeys(final Map<String, String> parameterKeys) {
    this.parameterKeys = parameterKeys;
  }

  public void setUiBuilder(final HtmlUiBuilder<?> uiBuilder) {
    this.uiBuilder = uiBuilder;
  }
}
