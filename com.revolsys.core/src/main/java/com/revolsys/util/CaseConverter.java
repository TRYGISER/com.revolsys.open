package com.revolsys.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CaseConverter {
  public static final String LOWER_CAMEL_CASE_RE = "";

  public static String captialize(
    final String text) {
    return Character.toUpperCase(text.charAt(0))
      + text.substring(1).toLowerCase();
  }

  public static List splitWords(
    final String text) {
    if (text == null) {
      return Collections.EMPTY_LIST;
    } else {
      final Pattern p = Pattern.compile("([A-Z]+)$" + "|" + "([A-Z]+)[ _]"
        + "|" + "([a-zA-Z][^A-Z _]*)");
      final Matcher m = p.matcher(text);
      final List words = new ArrayList();
      while (m.find()) {
        for (int i = 1; i <= m.groupCount(); i++) {
          final String group = m.group(i);
          if (group != null) {
            words.add(m.group(i));
          }
        }
      }
      return words;
    }
  }

  public static String toCapitalizedWords(
    final String text) {
    final List words = splitWords(text);
    final StringBuffer result = new StringBuffer();
    for (final Iterator iter = words.iterator(); iter.hasNext();) {
      final String word = (String)iter.next();
      result.append(captialize(word));
      if (iter.hasNext()) {
        result.append(" ");
      }
    }
    return result.toString();
  }

  public static String toLowerCamelCase(
    final String text) {
    final List words = splitWords(text);
    if (words.size() == 0) {
      return "";
    } else if (words.size() == 1) {
      return ((String)words.get(0)).toLowerCase();
    } else {
      final StringBuffer result = new StringBuffer();
      final Iterator iter = words.iterator();
      result.append(((String)iter.next()).toLowerCase());
      while (iter.hasNext()) {
        final String word = (String)iter.next();
        result.append(captialize(word));
      }
      return result.toString();
    }
  }

  public static String toLowerUnderscore(
    final String text) {
    final List words = splitWords(text);
    final StringBuffer result = new StringBuffer();
    for (final Iterator iter = words.iterator(); iter.hasNext();) {
      final String word = (String)iter.next();
      result.append(word.toLowerCase());
      if (iter.hasNext()) {
        result.append("_");
      }
    }
    return result.toString();
  }

  public static String toSentence(
    final String text) {
    final List words = splitWords(text);
    if (words.size() == 0) {
      return "";
    } else if (words.size() == 1) {
      return (String)words.get(0);
    } else {
      final StringBuffer result = new StringBuffer();
      final Iterator iter = words.iterator();
      result.append(captialize((String)iter.next()));
      while (iter.hasNext()) {
        final String word = (String)iter.next();
        result.append(word.toLowerCase());
        if (iter.hasNext()) {
          result.append(" ");
        }
      }
      return result.toString();
    }
  }

  public static String toUpperCamelCase(
    final String text) {
    final List words = splitWords(text);
    final StringBuffer result = new StringBuffer();
    for (final Iterator iter = words.iterator(); iter.hasNext();) {
      final String word = (String)iter.next();
      result.append(captialize(word));
    }
    return result.toString();
  }

  public static String toUpperUnderscore(
    final String text) {
    final List words = splitWords(text);
    final StringBuffer result = new StringBuffer();
    for (final Iterator iter = words.iterator(); iter.hasNext();) {
      final String word = (String)iter.next();
      result.append(word.toUpperCase());
      if (iter.hasNext()) {
        result.append("_");
      }
    }
    return result.toString();
  }

  private CaseConverter() {
  }
}
