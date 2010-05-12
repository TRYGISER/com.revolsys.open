/*
 * Copyright 2004-2005 Revolution Systems Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.revolsys.ui.html.view;


import com.revolsys.ui.html.HtmlUtil;
import com.revolsys.xml.io.XmlWriter;

/**
 * @author paustin
 * @version 1.0
 */
public class SelectAllElement extends Element {
  private String cssClass;

  private String fieldName;

  private String formName;

  public SelectAllElement(final String formName, final String fieldName,
    final String cssClass) {
    this.cssClass = cssClass;
    this.fieldName = fieldName;
    this.formName = formName;
  }

  public void serializeElement(final XmlWriter out) {
    out.startTag(HtmlUtil.DIV);
    out.attribute(HtmlUtil.ATTR_CLASS, cssClass);
    out.text("select ");
    HtmlUtil.serializeA(out, cssClass, "javascript:setCheckboxState('"
      + formName + "','" + fieldName + "',true)", "all");
    HtmlUtil.serializeA(out, cssClass, "javascript:setCheckboxState('"
      + formName + "','" + fieldName + "',false)", "none");
    out.endTag(HtmlUtil.DIV);
  }
}
