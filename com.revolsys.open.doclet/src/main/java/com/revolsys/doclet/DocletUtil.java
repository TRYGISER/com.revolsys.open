package com.revolsys.doclet;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.springframework.util.StringUtils;

import com.revolsys.io.FileUtil;
import com.revolsys.io.xml.XmlWriter;
import com.revolsys.util.HtmlUtil;
import com.revolsys.util.Property;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationTypeDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;
import com.sun.javadoc.WildcardType;

public class DocletUtil {

  private static final Map<String, String> PACKAGE_URLS = new LinkedHashMap<String, String>();

  static {
    addPackageUrl("java.", "http://docs.oracle.com/javase/6/docs/api/");
    addPackageUrl("com.revolsys.jts.", "http://tsusiatsoftware.net/jts/javadoc/");
  }

  public static void addPackageUrl(final String packagePrefix, final String url) {
    PACKAGE_URLS.put(packagePrefix, url);
  }

  public static void anchor(final XmlWriter writer, final String name, final String title) {
    writer.startTag(HtmlUtil.A);
    writer.attribute(HtmlUtil.ATTR_NAME, name);
    writer.text(title);
    writer.endTag(HtmlUtil.A);
  }

  public static void contentContainer(final XmlWriter writer, final String firstColClass) {
    writer.startTag(HtmlUtil.DIV);
    writer.attribute(HtmlUtil.ATTR_CLASS, "container-fluid");

    writer.startTag(HtmlUtil.DIV);
    writer.attribute(HtmlUtil.ATTR_CLASS, "row");

    writer.startTag(HtmlUtil.DIV);
    writer.attribute(HtmlUtil.ATTR_CLASS, firstColClass);
  }

  public static void copyFiles(final String destDir) {
    for (final String name : Arrays.asList("bootstrap-custom.css", "javadoc.css", "javadoc.js",
      "javadoc.js", "prettify.js", "prettify.css")) {
      FileUtil.copy(DocletUtil.class.getResourceAsStream("/com/revolsys/doclet/" + name), new File(
        destDir, name));
    }
  }

  public static String description(final ClassDoc containingClass, final Tag doc) {
    final Tag[] tags = doc.inlineTags();
    return description(containingClass, tags);
  }

  public static String description(final ClassDoc containingClass, final Tag[] tags) {
    final StringBuilder text = new StringBuilder();
    if (tags != null && tags.length > 0) {
      for (final Tag tag : tags) {
        final String kind = tag.kind();
        if (tag instanceof SeeTag) {
          final SeeTag seeTag = (SeeTag)tag;
          seeTag(text, containingClass, seeTag);
        } else if ("Text".equals(kind)) {
          text.append(tag.text());
        }
      }
    }
    return text.toString();
  }

  public static void description(final XmlWriter writer, final ClassDoc containingClass,
    final Doc doc) {
    final Tag[] tags = doc.inlineTags();
    description(writer, containingClass, tags);
  }

  public static void description(final XmlWriter writer, final ClassDoc containingClass,
    final Tag[] tags) {
    if (tags != null && tags.length > 0) {
      for (final Tag tag : tags) {
        final String kind = tag.kind();
        if (tag instanceof SeeTag) {
          final SeeTag seeTag = (SeeTag)tag;
          seeTag(writer, containingClass, seeTag);
        } else if ("Text".equals(kind)) {
          writer.write(tag.text());
        }
      }
    }
  }

  public static void descriptionTd(final XmlWriter writer, final ClassDoc containingClass,
    final Map<String, Tag[]> descriptions, final String name) {
    writer.startTag(HtmlUtil.TD);
    writer.attribute(HtmlUtil.ATTR_CLASS, "description");
    final Tag[] description = descriptions.get(name);
    description(writer, containingClass, description);
    writer.endTagLn(HtmlUtil.TD);
  }

  public static void documentationReturn(final XmlWriter writer, final MethodDoc method) {
    final Type type = method.returnType();
    if (type != null && !"void".equals(type.qualifiedTypeName())) {
      Tag[] descriptionTags = null;
      for (final Tag tag : method.tags()) {
        if (tag.name().equals("@return")) {
          descriptionTags = tag.inlineTags();
        }
      }
      writer.startTag(HtmlUtil.DIV);
      writer.startTag(HtmlUtil.STRONG);
      writer.text("Return");
      writer.endTag(HtmlUtil.STRONG);
      writer.endTagLn(HtmlUtil.DIV);

      typeNameLink(writer, type);
      writer.text(" ");
      description(writer, method.containingClass(), descriptionTags);
    }
  }

  public static void endContentContainer(final XmlWriter writer) {
    writer.endTagLn(HtmlUtil.DIV);
    writer.endTagLn(HtmlUtil.DIV);
    writer.endTagLn(HtmlUtil.DIV);
  }

  public static AnnotationDesc getAnnotation(final AnnotationDesc[] annotations, final String name) {
    for (final AnnotationDesc annotation : annotations) {
      final AnnotationTypeDoc annotationType = annotation.annotationType();
      final String annotationName = qualifiedName(annotationType);
      if (name.equals(annotationName)) {
        return annotation;
      }
    }
    return null;
  }

  public static AnnotationDesc getAnnotation(final ProgramElementDoc doc, final String name) {
    final AnnotationDesc[] annotations = doc.annotations();
    return getAnnotation(annotations, name);
  }

  public static String getExternalUrl(final String qualifiedTypeName) {
    for (final Entry<String, String> entry : PACKAGE_URLS.entrySet()) {
      final String packagePrefix = entry.getKey();
      if (qualifiedTypeName.startsWith(packagePrefix)) {
        final String baseUrl = entry.getValue();
        final String url = baseUrl + qualifiedTypeName.replaceAll("\\.", "/")
          + ".html?is-external=true";
        return url;
      }
    }
    return null;
  }

  public static Map<String, Tag[]> getParameterDescriptions(final ExecutableMemberDoc method) {
    final Map<String, Tag[]> descriptions = new HashMap<String, Tag[]>();
    for (final ParamTag tag : method.paramTags()) {
      final String parameterName = tag.parameterName();
      final Tag[] commentTags = tag.inlineTags();
      descriptions.put(parameterName, commentTags);
    }
    return descriptions;
  }

  public static boolean hasAnnotation(final AnnotationDesc[] annotations, final String name) {
    final AnnotationDesc annotation = getAnnotation(annotations, name);
    return annotation != null;
  }

  public static boolean hasAnnotation(final ProgramElementDoc doc, final String name) {
    final AnnotationDesc annotation = getAnnotation(doc, name);
    return annotation != null;
  }

  public static void headOld(final XmlWriter writer, final String docTitle) {
    writer.startTag(HtmlUtil.HEAD);
    writer.element(HtmlUtil.TITLE, docTitle);
    for (final String url : Arrays.asList(
      "https://code.jquery.com/ui/1.11.2/themes/cupertino/jquery-ui.css",
      "https://cdn.datatables.net/1.10.6/css/jquery.dataTables.min.css", "prettify.css",
      "javadoc.css")) {
      HtmlUtil.serializeCss(writer, url);

    }
    for (final String url : Arrays.asList("https://code.jquery.com/jquery-1.11.1.min.js",
      "https://code.jquery.com/ui/1.11.2/jquery-ui.min.js",
      "https://cdn.datatables.net/1.10.6/js/jquery.dataTables.min.js", "prettify.js", "javadoc.js")) {
      HtmlUtil.serializeScriptLink(writer, url);
    }
    writer.endTagLn(HtmlUtil.HEAD);
  }

  public static void htmlFoot(final XmlWriter writer) {
    HtmlUtil.serializeScriptLink(writer,
      "https://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js",
      "https://cdnjs.cloudflare.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js",
      "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/js/bootstrap.min.js",
      "https://cdnjs.cloudflare.com/ajax/libs/prettify/r298/run_prettify.js",
      "https://cdnjs.cloudflare.com/ajax/libs/jquery.tocify/1.9.0/javascripts/jquery.tocify.min.js");
    writer.startTag(HtmlUtil.SCRIPT);
    writer.textLn("$(function() {");
    writer.textLn("  $('#toc').tocify({theme:'bootstrap3',context:'.col-md-9',selectors:'h1,h2,h3,h4'});");
    writer.textLn("});");
    writer.endTag(HtmlUtil.SCRIPT);

    writer.endTagLn(HtmlUtil.BODY);
    writer.endTagLn(HtmlUtil.HTML);
    writer.endDocument();
  }

  public static void htmlHead(final XmlWriter writer, final String docTitle) {
    writer.docType("<!DOCTYPE html>");
    writer.startTag(HtmlUtil.HTML);
    writer.attribute(HtmlUtil.ATTR_LANG, "en");
    writer.newLine();

    writer.startTagLn(HtmlUtil.HEAD);

    writer.startTag(HtmlUtil.META);
    writer.attribute(HtmlUtil.ATTR_CHARSET, "utf-8");
    writer.endTagLn(HtmlUtil.META);

    writer.startTag(HtmlUtil.META);
    writer.attribute(HtmlUtil.ATTR_HTTP_EQUIV, "X-UA-Compatible");
    writer.attribute(HtmlUtil.ATTR_CONTENT, "IE=edge");
    writer.endTagLn(HtmlUtil.META);

    writer.startTag(HtmlUtil.META);
    writer.attribute(HtmlUtil.ATTR_NAME, "viewport");
    writer.attribute(HtmlUtil.ATTR_CONTENT, "width=device-width, initial-scale=1");
    writer.endTagLn(HtmlUtil.META);

    writer.elementLn(HtmlUtil.TITLE, docTitle);

    HtmlUtil.serializeCss(
      writer,
      "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap.min.css",
      "https://maxcdn.bootstrapcdn.com/bootswatch/3.3.4/flatly/bootstrap.min.css",
      "https://cdnjs.cloudflare.com/ajax/libs/prettify/r298/prettify.min.css",
      "https://cdnjs.cloudflare.com/ajax/libs/jquery.tocify/1.9.0/stylesheets/jquery.tocify.min.css",
      "bootstrap-custom.css");

    HtmlUtil.serializeStyle(writer, "body{padding-top:60px}\n"
      + "*[id]:before {display:block;content:' ';margin-top:-75px;height:75px;visibility:hidden;}");
    writer.endTagLn(HtmlUtil.HEAD);

    writer.startTag(HtmlUtil.BODY);
    writer.attribute("data-spy", "scroll");
    writer.attribute("data-target", "#navMain");
    writer.attribute("data-offset", "60");
    writer.newLine();
  }

  public static boolean isTypeIncluded(final Type type) {
    final ClassDoc classDoc = type.asClassDoc();
    final ClassDoc annotationDoc = type.asAnnotationTypeDoc();
    final boolean included = annotationDoc != null && annotationDoc.isIncluded()
      || classDoc != null && classDoc.isIncluded();
    return included;
  }

  public static void label(final StringBuilder text, final String label, final boolean code) {
    if (code) {
      text.append("<code>");
    }
    text(text, label);
    if (code) {
      text.append("</code>");
    }
  }

  public static void label(final XmlWriter writer, final String label, final boolean code) {
    if (code) {
      writer.startTag(HtmlUtil.CODE);
    }
    writer.text(label);
    if (code) {
      writer.endTagLn(HtmlUtil.CODE);
    }
  }

  public static void link(final StringBuilder text, final String url, final String label,
    final boolean code) {
    final boolean hasUrl = StringUtils.hasText(url);
    if (hasUrl) {
      text.append("<a href=\"");
      text.append(url);
      text.append("\">");
    }
    label(text, label, code);
    if (hasUrl) {
      text.append("</a>");
    }
  }

  public static void link(final XmlWriter writer, final String url, final String label,
    final boolean code) {
    final boolean hasUrl = StringUtils.hasText(url);
    if (hasUrl) {
      writer.startTag(HtmlUtil.A);
      writer.attribute(HtmlUtil.ATTR_HREF, url);
    }
    label(writer, label, code);
    if (hasUrl) {
      writer.endTag(HtmlUtil.A);
    }
  }

  public static void navbarEnd(final XmlWriter writer) {
    writer.endTagLn(HtmlUtil.UL);
    writer.endTagLn(HtmlUtil.DIV);
    writer.endTagLn(HtmlUtil.DIV);
    writer.endTagLn(HtmlUtil.NAV);

  }

  public static void navbarStart(final XmlWriter writer, final String title) {
    writer.startTag(HtmlUtil.NAV);
    writer.attribute(HtmlUtil.ATTR_ID, "navMain");
    writer.attribute(HtmlUtil.ATTR_CLASS, "navbar navbar-default navbar-fixed-top");
    writer.newLine();

    writer.startTag(HtmlUtil.DIV);
    writer.attribute(HtmlUtil.ATTR_CLASS, "container");
    writer.newLine();

    {
      writer.startTag(HtmlUtil.DIV);
      writer.attribute(HtmlUtil.ATTR_CLASS, "navbar-header");
      writer.newLine();
      {
        writer.startTag(HtmlUtil.BUTTON);
        writer.attribute(HtmlUtil.ATTR_TYPE, "button");
        writer.attribute(HtmlUtil.ATTR_CLASS, "navbar-toggle collapsed");
        writer.attribute("data-toggle", "collapse");
        writer.attribute("data-target", "#navbar");
        writer.attribute("aria-expanded", "false");
        writer.attribute("aria-controls", "navbar");
        writer.newLine();

        HtmlUtil.serializeSpan(writer, "sr-only", "Toggle navigation");

        for (int i = 0; i < 3; i++) {
          writer.startTag(HtmlUtil.SPAN);
          writer.attribute(HtmlUtil.ATTR_CLASS, "icon-bar");
          writer.text("");
          writer.endTag(HtmlUtil.SPAN);
        }
        writer.endTagLn(HtmlUtil.BUTTON);
      }
      writer.startTag(HtmlUtil.A);
      writer.attribute(HtmlUtil.ATTR_CLASS, "navbar-brand");
      writer.attribute(HtmlUtil.ATTR_HREF, "#");
      writer.text(title);
      writer.endTag(HtmlUtil.A);
      writer.endTagLn(HtmlUtil.DIV);
    }
    {
      writer.startTag(HtmlUtil.DIV);
      writer.attribute(HtmlUtil.ATTR_ID, "navbar");
      writer.attribute(HtmlUtil.ATTR_CLASS, "navbar-collapse collapse");
      writer.attribute("aria-expanded", "false");
      writer.newLine();

      writer.startTag(HtmlUtil.UL);
      writer.attribute(HtmlUtil.ATTR_CLASS, "nav navbar-nav");

    }
  }

  public static void navDropdownEnd(final XmlWriter writer) {
    writer.endTagLn(HtmlUtil.UL);
    writer.endTagLn(HtmlUtil.LI);
  }

  public static void navDropdownStart(final XmlWriter writer, final String title, String url,
    final boolean subMenu) {
    writer.startTag(HtmlUtil.LI);
    if (subMenu) {
      writer.attribute(HtmlUtil.ATTR_CLASS, "dropdown-submenu");
    } else {
      writer.attribute(HtmlUtil.ATTR_CLASS, "dropdown");
    }

    writer.startTag(HtmlUtil.A);
    if (url.startsWith("#")) {
      url = "#" + url.substring(1).replaceAll("[^a-zA-Z0-9_]", "_");
    }
    if (subMenu) {
      writer.attribute(HtmlUtil.ATTR_HREF, url);
    } else {
      writer.attribute(HtmlUtil.ATTR_HREF, "#");
      writer.attribute(HtmlUtil.ATTR_CLASS, "dropdown-toggle");
      writer.attribute("data-toggle", "dropdown");
      writer.attribute(HtmlUtil.ATTR_ROLE, "button");
      writer.attribute("aria-expanded", "false");
    }
    writer.text(title);
    if (!subMenu) {
      writer.startTag(HtmlUtil.SPAN);
      writer.attribute(HtmlUtil.ATTR_CLASS, "caret");
      writer.text("");
      writer.endTag(HtmlUtil.SPAN);
    }
    writer.endTag(HtmlUtil.A);

    writer.startTag(HtmlUtil.UL);
    writer.attribute(HtmlUtil.ATTR_CLASS, "dropdown-menu");
    writer.attribute(HtmlUtil.ATTR_ROLE, "menu");
    writer.newLine();
    if (!subMenu) {
      navMenuItem(writer, title, url);
      writer.startTag(HtmlUtil.LI);
      writer.attribute(HtmlUtil.ATTR_CLASS, "divider");
      writer.endTagLn(HtmlUtil.LI);
    }
  }

  public static void navMenuItem(final XmlWriter writer, final String title, String url) {
    writer.startTag(HtmlUtil.LI);

    writer.startTag(HtmlUtil.A);
    if (url.startsWith("#")) {
      url = "#" + url.substring(1).replaceAll("[^a-zA-Z0-9_]", "_");
    }
    writer.attribute(HtmlUtil.ATTR_HREF, url);
    writer.text(title);
    writer.endTag(HtmlUtil.A);

    writer.endTagLn(HtmlUtil.LI);
  }

  public static void panelEnd(final XmlWriter writer) {
    writer.endTagLn(HtmlUtil.DIV);
    writer.endTagLn(HtmlUtil.DIV);
  }

  public static void panelStart(final XmlWriter writer, final String panelClass,
    final QName headerElement, final String id, final String titlePrefix, final String title,
    final String titleSuffix) {
    writer.startTag(HtmlUtil.DIV);
    writer.attribute(HtmlUtil.ATTR_CLASS, "panel " + panelClass);
    writer.newLine();

    writer.startTag(HtmlUtil.DIV);
    writer.attribute(HtmlUtil.ATTR_CLASS, "panel-heading");
    writer.newLine();

    String simpleId = null;
    if (Property.hasValue(id)) {
      simpleId = id.replaceAll("[^a-zA-Z0-9_]", "_");
      if (!id.equals(simpleId)) {
        writer.startTag(HtmlUtil.A);
        writer.attribute(HtmlUtil.ATTR_ID, id);
        writer.text("");
        writer.endTag(HtmlUtil.A);
      }
    }
    writer.startTag(headerElement);
    writer.attribute(HtmlUtil.ATTR_CLASS, "panel-title");

    if (Property.hasValue(id)) {
      writer.attribute(HtmlUtil.ATTR_ID, simpleId);
    }
    if (Property.hasValue(titlePrefix)) {
      writer.element(HtmlUtil.SMALL, titlePrefix);
      writer.text(" ");
    }
    writer.text(title);
    if (Property.hasValue(titleSuffix)) {
      writer.text(" ");
      writer.element(HtmlUtil.SMALL, titleSuffix);
    }
    writer.endTagLn(headerElement);

    writer.endTagLn(HtmlUtil.DIV);

    writer.startTag(HtmlUtil.DIV);
    writer.attribute(HtmlUtil.ATTR_CLASS, "panel-body");
    writer.newLine();
  }

  public static String qualifiedName(final ProgramElementDoc element) {
    final String packageName = element.containingPackage().name();
    return packageName + "." + element.name();
  }

  public static String replaceDocRootDir(final String text) {
    int i = text.indexOf("{@");
    if (i < 0) {
      return text;
    } else {
      final String lowerText = text.toLowerCase();
      i = lowerText.indexOf("{@docroot}", i);
      if (i < 0) {
        return text;
      } else {
        final StringBuffer stringbuffer = new StringBuffer();
        int k = 0;
        do {
          final int j = lowerText.indexOf("{@docroot}", k);
          if (j < 0) {
            stringbuffer.append(text.substring(k));
            break;
          }
          stringbuffer.append(text.substring(k, j));
          k = j + 10;
          stringbuffer.append("./");
          if ("./".length() > 0 && k < text.length() && text.charAt(k) != '/') {
            stringbuffer.append("/");
          }
        } while (true);
        return stringbuffer.toString();
      }
    }
  }

  public static void seeTag(final StringBuilder text, final ClassDoc containingClass,
    final SeeTag seeTag) {
    final String name = seeTag.name();
    if (name.startsWith("@link") || name.equals("@see")) {
      final boolean code = !name.equalsIgnoreCase("@linkplain");
      String label = seeTag.label();

      final StringBuffer stringbuffer = new StringBuffer();

      final String seeTagText = replaceDocRootDir(seeTag.text());
      if (seeTagText.startsWith("<") || seeTagText.startsWith("\"")) {
        stringbuffer.append(seeTagText);
        text.append(seeTagText);
      } else {
        final ClassDoc referencedClass = seeTag.referencedClass();
        final MemberDoc referencedMember = seeTag.referencedMember();
        String referencedMemberName = seeTag.referencedMemberName();
        if (referencedClass == null) {
          final PackageDoc packagedoc = seeTag.referencedPackage();
          if (packagedoc != null && packagedoc.isIncluded()) {
            final String packageName = packagedoc.name();
            if (!StringUtils.hasText(label)) {
              label = packageName;
            }
            link(text, "#" + packageName, label, code);
          } else {
            // TODO link to external package or class
            // String s9 = getCrossPackageLink(referencedClassName);
            // String s8;
            // if (s9 != null)
            // stringbuffer.append(getHyperLink(s9, "", s1.length() != 0 ? s1
            // : s3, false));
            // else if ((s8 = getCrossClassLink(referencedClassName,
            // referencedMemberName, s1, false, "", !plainLink)) != null) {
            // stringbuffer.append(s8);
            // } else {
            // configuration.getDocletSpecificMsg().warning(seeTag.position(),
            // "doclet.see.class_or_package_not_found", name, s2);
            // stringbuffer.append(s1.length() != 0 ? s1 : s3);
            // }
          }
        } else {
          String url = null;
          final String className = referencedClass.qualifiedName();
          if (referencedClass.isIncluded()) {
            url = "#" + className;
          } else {
            url = getExternalUrl(className);
            if (!StringUtils.hasText(url)) {
              label = className;
            }
          }
          if (referencedMember != null) {
            if (referencedMember instanceof ExecutableMemberDoc) {
              if (referencedMemberName.indexOf('(') < 0) {
                final ExecutableMemberDoc executableDoc = (ExecutableMemberDoc)referencedMember;
                referencedMemberName = referencedMemberName + executableDoc.signature();
              }
              if (StringUtils.hasText(referencedMemberName)) {
                label = referencedMemberName;
              } else {
                label = seeTagText;
              }
            }
            if (referencedClass.isIncluded()) {
              url += "." + referencedMemberName;
            } else if (StringUtils.hasText(url)) {
              url += "#" + referencedMemberName;
            } else {
              label = referencedMember.toString();
            }
          }
          if (!StringUtils.hasText(label)) {
            label = referencedClass.name();
          }
          link(text, url, label, code);
        }
      }
    }
  }

  public static void seeTag(final XmlWriter writer, final ClassDoc containingClass,
    final SeeTag seeTag) {
    final String name = seeTag.name();
    if (name.startsWith("@link") || name.equals("@see")) {
      final boolean code = !name.equalsIgnoreCase("@linkplain");
      String label = seeTag.label();

      final StringBuffer stringbuffer = new StringBuffer();

      final String seeTagText = replaceDocRootDir(seeTag.text());
      if (seeTagText.startsWith("<") || seeTagText.startsWith("\"")) {
        stringbuffer.append(seeTagText);
        writer.write(seeTagText);
      } else {
        final ClassDoc referencedClass = seeTag.referencedClass();
        final MemberDoc referencedMember = seeTag.referencedMember();
        String referencedMemberName = seeTag.referencedMemberName();
        if (referencedClass == null) {
          final PackageDoc packagedoc = seeTag.referencedPackage();
          if (packagedoc != null && packagedoc.isIncluded()) {
            final String packageName = packagedoc.name();
            if (!StringUtils.hasText(label)) {
              label = packageName;
            }
            link(writer, "#" + packageName, label, code);
          } else {
            // TODO link to external package or class
            // String s9 = getCrossPackageLink(referencedClassName);
            // String s8;
            // if (s9 != null)
            // stringbuffer.append(getHyperLink(s9, "", s1.length() != 0 ? s1
            // : s3, false));
            // else if ((s8 = getCrossClassLink(referencedClassName,
            // referencedMemberName, s1, false, "", !plainLink)) != null) {
            // stringbuffer.append(s8);
            // } else {
            // configuration.getDocletSpecificMsg().warning(seeTag.position(),
            // "doclet.see.class_or_package_not_found", name, s2);
            // stringbuffer.append(s1.length() != 0 ? s1 : s3);
            // }
          }
        } else {
          String url = null;
          final String className = referencedClass.qualifiedName();
          if (referencedClass.isIncluded()) {
            url = "#" + className;
          } else {
            url = getExternalUrl(className);
            if (!StringUtils.hasText(url)) {
              label = className;
            }
          }
          if (referencedMember != null) {
            if (referencedMember instanceof ExecutableMemberDoc) {
              if (referencedMemberName.indexOf('(') < 0) {
                final ExecutableMemberDoc executableDoc = (ExecutableMemberDoc)referencedMember;
                referencedMemberName = referencedMemberName + executableDoc.signature();
              }
              if (StringUtils.hasText(referencedMemberName)) {
                label = referencedMemberName;
              } else {
                label = seeTagText;
              }
            }
            if (referencedClass.isIncluded()) {
              url += "." + referencedMemberName;
            } else if (StringUtils.hasText(url)) {
              url += "#" + referencedMemberName;
            } else {
              label = referencedMember.toString();
            }
          }
          if (!StringUtils.hasText(label)) {
            label = referencedClass.name();
          }
          link(writer, url, label, code);
        }
      }
    }
  }

  public static void tagWithAnchor(final XmlWriter writer, final QName tag, final String name,
    final String title) {
    writer.startTag(tag);
    writer.attribute(HtmlUtil.ATTR_CLASS, "title");
    writer.startTag(HtmlUtil.A);
    writer.attribute(HtmlUtil.ATTR_NAME, name);
    writer.text(title);
    writer.endTag(HtmlUtil.A);
    writer.endTagLn(tag);
  }

  public static void text(final StringBuilder text, final String string) {
    int index = 0;
    final int lastIndex = string.length();
    String escapeString = null;
    for (int i = index; i < lastIndex; i++) {
      final char ch = string.charAt(i);
      switch (ch) {
        case '&':
          escapeString = "&amp;";
        break;
        case '<':
          escapeString = "&lt;";
        break;
        case '>':
          escapeString = "&gt;";
        break;
        case 9:
        case 10:
        case 13:
        // Accept these control characters
        break;
        default:
          // Reject all other control characters
          if (ch < 32) {
            throw new IllegalStateException("character " + Integer.toString(ch)
              + " is not allowed in output");
          }
        break;
      }
      if (escapeString != null) {
        if (i > index) {
          text.append(string, index, i - index);
        }
        text.append(escapeString);
        escapeString = null;
        index = i + 1;
      }
    }
    if (lastIndex > index) {
      text.append(string, index, lastIndex - index);
    }
  }

  public static void title(final XmlWriter writer, final QName element, final String title) {
    writer.startTag(element);
    writer.startTag(HtmlUtil.SPAN);
    writer.attribute(HtmlUtil.ATTR_CLASS, "label label-primary");
    writer.text(title);
    writer.endTag(HtmlUtil.SPAN);
    writer.endTagLn(element);
  }

  public static void title(final XmlWriter writer, final String name, final String title) {
    writer.startTag(HtmlUtil.DIV);
    writer.attribute(HtmlUtil.ATTR_CLASS, "title");
    anchor(writer, name, title);
    writer.endTagLn(HtmlUtil.DIV);
  }

  public static void typeName(final XmlWriter writer, final Type type) {
    String typeName;
    final String qualifiedTypeName = type.qualifiedTypeName();
    if (isTypeIncluded(type) || getExternalUrl(qualifiedTypeName) != null) {
      typeName = type.typeName();
    } else {
      typeName = qualifiedTypeName;
    }
    writer.text(typeName);
    writer.text(type.dimension());
  }

  public static void typeNameLink(final XmlWriter writer, final Type type) {
    if (type instanceof WildcardType) {
      final WildcardType wildCard = (WildcardType)type;
      writer.text("?");
      final Type[] extendsBounds = wildCard.extendsBounds();
      if (extendsBounds.length > 0) {
        writer.text(" extends ");
        for (int i = 0; i < extendsBounds.length; i++) {
          if (i > 0) {
            writer.text(", ");
          }
          final Type extendsType = extendsBounds[i];
          typeNameLink(writer, extendsType);
        }
      }
    } else {
      final String qualifiedTypeName = type.qualifiedTypeName();
      final String externalLink = getExternalUrl(qualifiedTypeName);

      final boolean included = isTypeIncluded(type);

      if (externalLink != null) {
        HtmlUtil.serializeA(writer, "", externalLink, type.typeName());
      } else if (included) {
        final String url = "#" + qualifiedTypeName;
        HtmlUtil.serializeA(writer, "", url, type.typeName());
      } else {
        writer.text(qualifiedTypeName);
      }
      if (type instanceof ParameterizedType) {
        final ParameterizedType parameterizedType = (ParameterizedType)type;
        final Type[] typeArguments = parameterizedType.typeArguments();
        if (typeArguments.length > 0) {
          writer.text("<");
          for (int i = 0; i < typeArguments.length; i++) {
            if (i > 0) {
              writer.text(", ");
            }
            final Type typeParameter = typeArguments[i];
            typeNameLink(writer, typeParameter);
          }
          writer.text(">");
        }
      }
    }
    writer.text(type.dimension());
  }
}
