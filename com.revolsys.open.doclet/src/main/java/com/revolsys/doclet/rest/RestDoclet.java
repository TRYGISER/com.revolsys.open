package com.revolsys.doclet.rest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.revolsys.doclet.DocletUtil;
import com.revolsys.io.xml.XmlWriter;
import com.revolsys.util.CaseConverter;
import com.revolsys.util.HtmlUtil;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

public class RestDoclet {
  public static LanguageVersion languageVersion() {
    return LanguageVersion.JAVA_1_5;
  }

  public static int optionLength(String optionName) {
    optionName = optionName.toLowerCase();
    if (optionName.equals("-d") || optionName.equals("-doctitle")) {
      return 2;
    }
    return -1;
  }

  public static boolean start(final RootDoc root) {
    new RestDoclet(root).start();
    return true;
  }

  public static boolean validOptions(final String options[][],
    final DocErrorReporter docerrorreporter) {
    final boolean flag = true;
    for (final String[] option : options) {
      final String argName = option[0].toLowerCase();
      if (argName.equals("-d")) {
        final String destDir = option[1];
        final File file = new File(destDir);
        if (!file.exists()) {
          docerrorreporter.printNotice("Create directory" + destDir);
          file.mkdirs();
        }
        if (!file.isDirectory()) {
          docerrorreporter.printError("Destination not a directory" + file.getPath());
          return false;
        } else if (!file.canWrite()) {
          docerrorreporter.printError("Destination directory not writable " + file.getPath());
          return false;
        }
      } else if (argName.equals("-htmlheader")) {
        if (!new File(option[1]).exists()) {
          docerrorreporter.printError("Header file does not exist" + option[1]);
          return false;
        }
      } else if (argName.equals("-htmlfooter")) {
        if (!new File(option[1]).exists()) {
          docerrorreporter.printError("Footer file does not exist" + option[1]);
          return false;
        }
      }
    }

    return flag;
  }

  private String destDir = ".";

  private String docTitle;

  private final RootDoc root;

  private XmlWriter writer;

  public RestDoclet(final RootDoc root) {
    this.root = root;
  }

  public void addResponseStatusDescription(final Map<String, List<String>> responseCodes,
    final String code, final String description) {
    List<String> descriptions = responseCodes.get(code);
    if (descriptions == null) {
      descriptions = new ArrayList<String>();
      responseCodes.put(code, descriptions);
    }
    descriptions.add(description);
  }

  public void documentation() {
    DocletUtil.contentContainer(this.writer, "col-md-12");

    this.writer.element(HtmlUtil.H1, this.docTitle);
    DocletUtil.description(this.writer, null, this.root);
    for (final PackageDoc packageDoc : this.root.specifiedPackages()) {
      final Map<String, ClassDoc> classes = new TreeMap<String, ClassDoc>();
      for (final ClassDoc classDoc : packageDoc.ordinaryClasses()) {
        classes.put(classDoc.name(), classDoc);
      }
      for (final ClassDoc classDoc : classes.values()) {
        documentationClass(classDoc);
      }
    }
    DocletUtil.endContentContainer(this.writer);
  }

  public void documentationClass(final ClassDoc classDoc) {
    if (DocletUtil.hasAnnotation(classDoc, "org.springframework.stereotype.Controller")) {
      final String id = DocletUtil.qualifiedName(classDoc);
      final String name = classDoc.name();
      final String title = CaseConverter.toCapitalizedWords(name);
      DocletUtil.panelStart(this.writer, "panel-default", HtmlUtil.H2, id, null, title, null);
      DocletUtil.description(this.writer, classDoc, classDoc);
      for (final MethodDoc methodDoc : classDoc.methods()) {
        documentationMethod(classDoc, methodDoc);
      }
      DocletUtil.panelEnd(this.writer);
    }
  }

  public void documentationMethod(final ClassDoc classDoc, final MethodDoc methodDoc) {
    final AnnotationDesc requestMapping = DocletUtil.getAnnotation(methodDoc,
      "org.springframework.web.bind.annotation.RequestMapping");
    if (requestMapping != null) {
      final String name = methodDoc.name();
      final String id = DocletUtil.qualifiedName(classDoc) + "." + name;
      final String title = CaseConverter.toCapitalizedWords(name);
      DocletUtil.panelStart(this.writer, "panel-primary", HtmlUtil.H3, id, null, title, null);

      DocletUtil.description(this.writer, methodDoc.containingClass(), methodDoc);
      requestMethods(requestMapping);
      uriTemplates(requestMapping);
      uriTemplateParameters(methodDoc);
      parameters(methodDoc);
      responseStatus(methodDoc);

      DocletUtil.panelEnd(this.writer);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T getElementValue(final AnnotationDesc annotation, final String name) {
    for (final ElementValuePair pair : annotation.elementValues()) {
      if (pair.element().name().equals(name)) {
        return (T)pair.value().value();
      }
    }
    return null;
  }

  public void navbar() {
    DocletUtil.navbarStart(this.writer, this.docTitle);
    for (final PackageDoc packageDoc : this.root.specifiedPackages()) {
      final Map<String, ClassDoc> classes = new TreeMap<String, ClassDoc>();
      for (final ClassDoc classDoc : packageDoc.ordinaryClasses()) {
        classes.put(classDoc.name(), classDoc);
      }
      for (final ClassDoc classDoc : classes.values()) {
        navMenu(classDoc);
      }
    }
    DocletUtil.navbarEnd(this.writer);
  }

  public void navMenu(final ClassDoc classDoc) {
    final String id = DocletUtil.qualifiedName(classDoc);
    final String name = classDoc.name();
    final String title = CaseConverter.toCapitalizedWords(name);
    DocletUtil.navDropdownStart(this.writer, title, "#" + id, false);
    for (final MethodDoc methodDoc : classDoc.methods()) {
      final AnnotationDesc requestMapping = DocletUtil.getAnnotation(methodDoc,
        "org.springframework.web.bind.annotation.RequestMapping");
      if (requestMapping != null) {
        navMenu(classDoc, methodDoc);
      }
    }
    DocletUtil.navDropdownEnd(this.writer);
  }

  public void navMenu(final ClassDoc classDoc, final MethodDoc methodDoc) {
    final String name = methodDoc.name();
    final String id = DocletUtil.qualifiedName(classDoc) + "." + name;
    final String title = CaseConverter.toCapitalizedWords(name);
    DocletUtil.navMenuItem(this.writer, title, "#" + id);
  }

  private void parameters(final MethodDoc method) {
    final List<Parameter> parameters = new ArrayList<Parameter>();
    for (final Parameter parameter : method.parameters()) {
      final AnnotationDesc[] annotations = parameter.annotations();
      if (DocletUtil.hasAnnotation(annotations,
        "org.springframework.web.bind.annotation.RequestParam")
        || DocletUtil.hasAnnotation(annotations,
          "org.springframework.web.bind.annotation.RequestBody")) {
        parameters.add(parameter);
      }
    }
    if (!parameters.isEmpty()) {
      final Map<String, Tag[]> descriptions = DocletUtil.getParameterDescriptions(method);

      DocletUtil.panelStart(this.writer, "panel-info", HtmlUtil.H4, null, null, "Parameters", null);
      this.writer.element(
        HtmlUtil.P,
        "The resource supports the following parameters. "
          + "For HTTP get requests these must be specified using query string parameters. "
          + "For HTTP POST requests these can be specified using query string, application/x-www-form-urlencoded parameters or multipart/form-data unless otherwise specified. "
          + "Array values [] can be specified by including the parameter multiple times in the request.");

      this.writer.startTag(HtmlUtil.DIV);
      this.writer.attribute(HtmlUtil.ATTR_CLASS, "table-responsive");
      this.writer.startTag(HtmlUtil.TABLE);
      this.writer.attribute(HtmlUtil.ATTR_CLASS, "table table-striped table-bordered");

      this.writer.startTag(HtmlUtil.THEAD);
      this.writer.startTag(HtmlUtil.TR);
      this.writer.element(HtmlUtil.TH, "Parameter");
      this.writer.element(HtmlUtil.TH, "Type");
      this.writer.element(HtmlUtil.TH, "Default");
      this.writer.element(HtmlUtil.TH, "Required");
      this.writer.element(HtmlUtil.TH, "Description");
      this.writer.endTag(HtmlUtil.TR);
      this.writer.endTag(HtmlUtil.THEAD);

      this.writer.startTag(HtmlUtil.TBODY);
      for (final Parameter parameter : parameters) {
        this.writer.startTag(HtmlUtil.TR);
        final String name = parameter.name();
        final AnnotationDesc requestParam = DocletUtil.getAnnotation(parameter.annotations(),
          "org.springframework.web.bind.annotation.RequestParam");
        final AnnotationDesc requestBody = DocletUtil.getAnnotation(parameter.annotations(),
          "org.springframework.web.bind.annotation.RequestBody");
        String paramName = name;
        String defaultValue = "-";
        String typeName = parameter.typeName();
        typeName = typeName.replaceAll("java.util.List<([^>]+)>", "$1\\[\\]");
        typeName = typeName.replaceFirst("^java.lang.", "");
        typeName = typeName.replaceAll("org.springframework.web.multipart.MultipartFile", "File");

        boolean required = true;
        if (requestParam != null) {
          final String value = getElementValue(requestParam, "value");
          if (value != null && !value.trim().equals("")) {
            paramName = value;
          }
          defaultValue = getElementValue(requestParam, "defaultValue");
          if (defaultValue == null) {
            defaultValue = "-";
          }
          required = Boolean.FALSE != (Boolean)getElementValue(requestParam, "required");
        }
        if (requestBody != null) {
          required = true;
          paramName = "HTTP Request body or 'body' parameter";
          typeName = "binary/character data";
        }

        this.writer.startTag(HtmlUtil.TD);
        this.writer.startTag(HtmlUtil.CODE);
        this.writer.text(paramName);
        this.writer.endTag(HtmlUtil.CODE);
        this.writer.endTag(HtmlUtil.TD);

        this.writer.startTag(HtmlUtil.TD);
        this.writer.startTag(HtmlUtil.CODE);
        this.writer.text(typeName);
        this.writer.endTag(HtmlUtil.CODE);
        this.writer.endTag(HtmlUtil.TD);

        this.writer.element(HtmlUtil.TD, defaultValue);
        if (required) {
          this.writer.element(HtmlUtil.TD, "Yes");
        } else {
          this.writer.element(HtmlUtil.TD, "No");
        }
        DocletUtil.descriptionTd(this.writer, method.containingClass(), descriptions, name);
        this.writer.endTag(HtmlUtil.TR);
      }
      this.writer.endTag(HtmlUtil.TBODY);

      this.writer.endTag(HtmlUtil.TABLE);
      this.writer.endTag(HtmlUtil.DIV);
      DocletUtil.panelEnd(this.writer);
    }
  }

  private void requestMethods(final AnnotationDesc requestMapping) {
    final AnnotationValue[] methods = getElementValue(requestMapping, "method");
    if (methods != null && methods.length > 0) {
      DocletUtil.panelStart(this.writer, "panel-info", HtmlUtil.H4, null, null, "HTTP Request Methods", null);
      this.writer.element(HtmlUtil.P,
        "The resource can be accessed using the following HTTP request methods.");
      this.writer.startTag(HtmlUtil.UL);
      for (final AnnotationValue value : methods) {
        final FieldDoc method = (FieldDoc)value.value();
        this.writer.element(HtmlUtil.LI, method.name());
      }
      this.writer.endTag(HtmlUtil.UL);
      DocletUtil.panelEnd(this.writer);
    }
  }

  private void responseStatus(final MethodDoc method) {
    final Map<String, List<String>> responseStatusDescriptions = new TreeMap<String, List<String>>();

    for (final Tag tag : method.tags()) {
      if (tag.name().equals("@web.response.status")) {
        final String text = DocletUtil.description(method.containingClass(), tag);

        final int index = text.indexOf(" ");
        if (index != -1) {
          final String status = text.substring(0, index);
          final String description = text.substring(index + 1).trim();
          addResponseStatusDescription(responseStatusDescriptions, status, description);
        }
      }
    }
    addResponseStatusDescription(
      responseStatusDescriptions,
      "500",
      "<p><b>Internal Server Error</b></p>"
        + "<p>This error indicates that there was an unexpected error on the server. "
        + "This is sometimes temporary so try again after a few minutes. "
        + "The problem could also be caused by bad input data so verify all input parameters and files. "
        + "If the problem persists contact the support desk with exact details of the parameters you were using.</p>");
    if (!responseStatusDescriptions.isEmpty()) {
      DocletUtil.panelStart(this.writer, "panel-info", HtmlUtil.H4, null, null, "HTTP Status Codes", null);
      this.writer.element(
        HtmlUtil.P,
        "The resource will return one of the following status codes. The HTML error page may include an error message. The descriptions of the messages and the cause are described below.");
      this.writer.startTag(HtmlUtil.DIV);
      this.writer.attribute(HtmlUtil.ATTR_CLASS, "table-responsive");

      this.writer.startTag(HtmlUtil.TABLE);
      this.writer.attribute(HtmlUtil.ATTR_CLASS, "table table-striped table-bordered");

      this.writer.startTag(HtmlUtil.THEAD);
      this.writer.startTag(HtmlUtil.TR);
      this.writer.element(HtmlUtil.TH, "HTTP Status Code");
      this.writer.element(HtmlUtil.TH, "Description");
      this.writer.endTag(HtmlUtil.TR);
      this.writer.endTag(HtmlUtil.THEAD);

      this.writer.startTag(HtmlUtil.TBODY);
      for (final Entry<String, List<String>> entry : responseStatusDescriptions.entrySet()) {
        final String code = entry.getKey();
        for (final String message : entry.getValue()) {
          this.writer.startTag(HtmlUtil.TR);
          this.writer.element(HtmlUtil.TD, code);
          this.writer.startTag(HtmlUtil.TD);
          this.writer.write(message);
          this.writer.endTag(HtmlUtil.TD);

          this.writer.endTag(HtmlUtil.TR);
        }
      }
      this.writer.endTag(HtmlUtil.TBODY);

      this.writer.endTag(HtmlUtil.TABLE);
      this.writer.endTag(HtmlUtil.DIV);
      DocletUtil.panelEnd(this.writer);
    }
  }

  private void setOptions(final String[][] options) {
    for (final String[] option : options) {
      final String optionName = option[0];
      if (optionName.equals("-d")) {
        this.destDir = option[1];

      } else if (optionName.equals("-doctitle")) {
        this.docTitle = option[1];
      }
    }
    try {
      final File dir = new File(this.destDir);
      final File indexFile = new File(dir, "index.html");
      final FileWriter out = new FileWriter(indexFile);
      this.writer = new XmlWriter(out, false);
      DocletUtil.copyFiles(this.destDir);
    } catch (final IOException e) {
      throw new IllegalArgumentException(e.fillInStackTrace().getMessage(), e);
    }
  }

  private void start() {
    try {
      setOptions(this.root.options());

      DocletUtil.htmlHead(this.writer, this.docTitle);

      navbar();

      documentation();

      DocletUtil.htmlFoot(this.writer);
    } finally {
      if (this.writer != null) {
        this.writer.close();
      }
    }
  }

  private void uriTemplateParameters(final MethodDoc method) {
    final List<Parameter> parameters = new ArrayList<Parameter>();
    for (final Parameter parameter : method.parameters()) {
      if (DocletUtil.hasAnnotation(parameter.annotations(),
        "org.springframework.web.bind.annotation.PathVariable")) {
        parameters.add(parameter);
      }
    }
    if (!parameters.isEmpty()) {
      final Map<String, Tag[]> descriptions = DocletUtil.getParameterDescriptions(method);
      DocletUtil.panelStart(this.writer, "panel-info", HtmlUtil.H4, null, null, "URI Template Parameters", null);
      this.writer.element(
        HtmlUtil.P,
        "The URI templates support the following parameters which must be replaced with values as described below.");
      this.writer.startTag(HtmlUtil.DIV);
      this.writer.attribute(HtmlUtil.ATTR_CLASS, "table-responsive");

      this.writer.startTag(HtmlUtil.TABLE);
      this.writer.attribute(HtmlUtil.ATTR_CLASS, "table table-striped table-bordered");

      this.writer.startTag(HtmlUtil.THEAD);
      this.writer.startTag(HtmlUtil.TR);
      this.writer.element(HtmlUtil.TH, "Parameter");
      this.writer.element(HtmlUtil.TH, "Type");
      this.writer.element(HtmlUtil.TH, "Description");
      this.writer.endTag(HtmlUtil.TR);
      this.writer.endTag(HtmlUtil.THEAD);

      this.writer.startTag(HtmlUtil.TBODY);
      for (final Parameter parameter : parameters) {
        this.writer.startTag(HtmlUtil.TR);
        final String name = parameter.name();
        this.writer.element(HtmlUtil.TD, "{" + name + "}");
        this.writer.element(HtmlUtil.TD, parameter.typeName());
        DocletUtil.descriptionTd(this.writer, method.containingClass(), descriptions, name);

        this.writer.endTag(HtmlUtil.TR);
      }
      this.writer.endTag(HtmlUtil.TBODY);

      this.writer.endTag(HtmlUtil.TABLE);
      this.writer.endTag(HtmlUtil.DIV);
      DocletUtil.panelEnd(this.writer);
    }
  }

  private void uriTemplates(final AnnotationDesc requestMapping) {
    final AnnotationValue[] uriTemplates = getElementValue(requestMapping, "value");
    if (uriTemplates.length > 0) {
      DocletUtil.panelStart(this.writer, "panel-info", HtmlUtil.H4, null, null, "URI Templates", null);
      this.writer.element(
        HtmlUtil.P,
        "The URI templates define the paths that can be appended to the base URL of the service to access this resource.");

      for (final AnnotationValue uriTemplate : uriTemplates) {
        this.writer.element(HtmlUtil.PRE, uriTemplate.value());
      }
      DocletUtil.panelEnd(this.writer);
    }
  }

}
