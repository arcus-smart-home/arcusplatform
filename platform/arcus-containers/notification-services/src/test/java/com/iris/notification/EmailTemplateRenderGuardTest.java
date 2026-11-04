/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.notification;

import static com.iris.notification.EmailTemplates.HBS3_ENTITY;
import static com.iris.notification.EmailTemplates.PLAIN_TEXT_SECTIONS;
import static com.iris.notification.EmailTemplates.emailTemplates;
import static com.iris.notification.EmailTemplates.read;
import static com.iris.notification.EmailTemplates.templateId;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.core.template.HandlebarsTemplateService;
import com.iris.core.template.TemplateService;
import com.iris.notification.provider.EmailSections;
import com.iris.test.IrisTestCase;

/**
 * Renders every *-email template through the real {@link HandlebarsTemplateService}
 * with every value it looks up set to markup, then checks the output rather than
 * the template source, so layouts, nested partials, helpers, and partials that
 * carry over from one section into the next are all covered.
 * <p>
 * The html-body is sent as text/html, so no value may reach it unescaped. The
 * other sections are sent as plain text, so no value may be HTML-escaped there.
 * Every template must render both an html-body and a plaintext-body; text
 * outside the sections is never sent, so it is not checked.
 * <p>
 * Values come from {@link Probe}, a map that answers any name, and any property
 * of any name (_place.name, _person.firstName, ...), with markup naming it, so
 * the test needs no list of message parameters or entity fields. A template with
 * {{#if}}, {{#unless}} or {{#if_equal}} is rendered again with each condition
 * flipped on its own, and with all of them flipped when it has more than one.
 * Other mixes of branches are not rendered; today no template has more than
 * one condition, and EmailTemplateRawValueGuardTest reads every branch in the
 * source.
 */
public class EmailTemplateRenderGuardTest extends IrisTestCase {
   private static final Logger LOGGER = LoggerFactory.getLogger(EmailTemplateRenderGuardTest.class);

   /**
    * Helpers that cast their argument to String or parse it as a number cannot
    * take a {@link Probe}, so these names get a plain String instead. Where the
    * helper prints its argument the String is still markup, so that output is
    * checked too. A new helper argument that needs one fails the render with a
    * ClassCastException or NumberFormatException naming the template.
    */
   private static final Map<String, String> HELPER_ARGUMENTS = ImmutableMap.<String, String>builder()
         .put("current", "21.5")                         // celsiusToFahrenheit parses a number
         .put("doorState", markup("doorState"))          // enum
         .put("lockState", markup("lockState"))          // enum
         .put("mode", markup("mode"))                    // enum
         .put("triggerWhen", markup("triggerWhen"))      // if_equal
         .put("_person.email", markup("_person.email"))  // urlEncode
         .build();

   private static final Pattern RAW = Pattern.compile("<probe-x name=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
   private static final Pattern ESCAPED = Pattern.compile("&lt;probe-x name=&quot;(.*?)&quot;", Pattern.CASE_INSENSITIVE);
   private static final Pattern IF = Pattern.compile("\\{\\{~?#(?:if|unless)\\s+([^\\s}~]+)\\s*~?\\}\\}");
   private static final Pattern IF_EQUAL = Pattern.compile("\\{\\{~?#if_equal\\s+([^\\s}~]+)\\s+[\"']([^\"']*)[\"']");

   private TemplateService templates;

   // IrisTestCase.setUp, which JUnit runs first, registers src/dist as the
   // location the template loader reads "templates" from.
   @Before
   public void createTemplateService() {
      templates = new HandlebarsTemplateService("templates", 0);
   }

   @Test
   public void renderedEmailsEscapeHtmlAndNothingElse() throws IOException {
      List<String> failures = new ArrayList<>();
      int rendered = 0;
      int renders = 0;
      int htmlWithValues = 0;
      int plainTextWithValues = 0;

      for(File template: emailTemplates()) {
         String templateId = templateId(template);
         rendered++;

         boolean sawHtmlValue = false;
         boolean sawPlainValue = false;
         for(Map.Entry<String, Probe> pass: passes(read(template)).entrySet()) {
            String label = templateId + pass.getKey();
            renders++;
            Map<String, String> parts;
            try {
               parts = templates.renderMultipart(templateId, pass.getValue());
            }
            catch(RuntimeException e) {
               failures.add(label + ": render failed: " + e.getMessage());
               continue;
            }

            if(parts.get(EmailSections.PLAINTEXT_BODY) == null) {
               failures.add(label + ": no plaintext-body section");
            }
            String html = parts.get(EmailSections.HTML_BODY);
            if(html == null) {
               failures.add(label + ": no html-body section");
            }
            else {
               Set<String> raw = names(RAW, html);
               if(!raw.isEmpty()) {
                  failures.add(label + ": raw in html-body: " + raw);
               }
               sawHtmlValue |= ESCAPED.matcher(html).find();
            }

            for(String section: PLAIN_TEXT_SECTIONS) {
               String text = parts.get(section);
               if(text == null) {
                  continue;
               }
               if(HBS3_ENTITY.matcher(text).find()) {
                  failures.add(label + ": HTML-escaped in " + section + ": " + names(ESCAPED, text));
               }
               sawPlainValue |= RAW.matcher(text).find();
            }
         }
         htmlWithValues += sawHtmlValue ? 1 : 0;
         plainTextWithValues += sawPlainValue ? 1 : 0;
      }

      // Guard against probes that silently stop reaching the output.
      String counts = rendered + " templates, " + renders + " renders, values in the html-body of " +
            htmlWithValues + " and the plain text of " + plainTextWithValues;
      assertTrue("too few templates rendered: " + counts, rendered > 100);
      assertTrue("probes did not reach the html-body: " + counts, htmlWithValues > 100);
      assertTrue("probes did not reach the plain text: " + counts, plainTextWithValues > 100);
      LOGGER.info("Checked {}", counts);

      if(!failures.isEmpty()) {
         fail(failures.size() + " problems (" + counts + "):\n  " + String.join("\n  ", failures));
      }
   }

   /**
    * The contexts to render a template with, by label: every condition taken
    * the way the probes make it go, then each condition flipped on its own,
    * then all of them flipped when there are several. Flipping a #if or
    * #unless leaves its name out; flipping an #if_equal gives its name the
    * value it is compared with.
    */
   private static Map<String, Probe> passes(String source) {
      // name -> value that flips its condition, null for "leave it out". Keyed
      // by name, so a name tested twice (say #if_equal against two values) is
      // only flipped one way; no template does that today.
      Map<String, String> flips = new LinkedHashMap<>();
      Matcher m = IF.matcher(source);
      while(m.find()) {
         flips.put(m.group(1), null);
      }
      m = IF_EQUAL.matcher(source);
      while(m.find()) {
         flips.put(m.group(1), m.group(2));
      }

      Map<String, Probe> passes = new LinkedHashMap<>();
      passes.put("", flipped(Collections.emptyMap()));
      for(Map.Entry<String, String> flip: flips.entrySet()) {
         passes.put(" (" + flip.getKey() + " flipped)", flipped(Collections.singletonMap(flip.getKey(), flip.getValue())));
      }
      if(flips.size() > 1) {
         passes.put(" (all conditions flipped)", flipped(flips));
      }
      return passes;
   }

   private static Probe flipped(Map<String, String> flips) {
      Map<String, String> values = new HashMap<>(HELPER_ARGUMENTS);
      Set<String> absent = new HashSet<>();
      for(Map.Entry<String, String> flip: flips.entrySet()) {
         if(flip.getValue() == null) {
            absent.add(flip.getKey());
         }
         else {
            values.put(flip.getKey(), flip.getValue());
         }
      }
      return new Probe("", values, absent);
   }

   private static String markup(String name) {
      return "<probe-x name=\"" + name + "\">'&</probe-x>";
   }

   private static Set<String> names(Pattern pattern, String text) {
      Set<String> names = new TreeSet<>();
      Matcher m = pattern.matcher(text);
      while(m.find()) {
         names.add(m.group(1));
      }
      return names;
   }

   /**
    * Template context that resolves any name. A name in values gets that
    * String, a name in absent gets nothing, and any other name gets a Probe,
    * which prints as markup naming its path and resolves its own properties
    * the same way, so {{_place.name}} prints markup naming "_place.name".
    * Handlebars looks names up in a Map with get().
    */
   private static final class Probe extends AbstractMap<String, Object> {
      private final String path;
      private final Map<String, String> values;
      private final Set<String> absent;

      Probe(String path, Map<String, String> values, Set<String> absent) {
         this.path = path;
         this.values = values;
         this.absent = absent;
      }

      @Override
      public Object get(Object key) {
         String name = path.isEmpty() ? String.valueOf(key) : path + "." + key;
         if(absent.contains(name)) {
            return null;
         }
         String value = values.get(name);
         return value != null ? value : new Probe(name, values, absent);
      }

      @Override
      public boolean containsKey(Object key) {
         return get(key) != null;
      }

      // Never empty, so {{#if name}} is true unless the name is absent.
      @Override
      public Set<Entry<String, Object>> entrySet() {
         return Collections.<String, Object>singletonMap(path, markup(path)).entrySet();
      }

      @Override
      public String toString() {
         return markup(path);
      }

      @Override
      public boolean equals(Object o) {
         return this == o;
      }

      @Override
      public int hashCode() {
         return System.identityHashCode(this);
      }
   }
}
