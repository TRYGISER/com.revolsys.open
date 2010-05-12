package com.revolsys.ui.html.serializer.key;

import java.util.Locale;

import com.revolsys.ui.html.HtmlUtil;
import com.revolsys.util.JavaBeanUtil;
import com.revolsys.xml.io.XmlWriter;

/**
 * Serialize an email address as a mailto:link.
 * 
 * @author Paul Austin
 */
public class EmailKeySerializer implements KeySerializer {

  /**
   * Serialize the value to the XML writer using the settings from the Locale.
   * 
   * @param out The XML writer to serialize to.
   * @param object The object to get the value from.
   * @param key The key of the property on the object to serialize.
   * @param locale The locale.
   */
  public void serialize(final XmlWriter out, final Object object,
    final String key, final Locale locale) {
    Object email = JavaBeanUtil.getProperty(object, key);
    if (email != null) {
      out.startTag(HtmlUtil.A);
      out.attribute(HtmlUtil.ATTR_HREF, "mailto:" + email);
      out.text(email);
      out.endTag(HtmlUtil.A);
    } else {
      out.text("-");
    }
  }
}
