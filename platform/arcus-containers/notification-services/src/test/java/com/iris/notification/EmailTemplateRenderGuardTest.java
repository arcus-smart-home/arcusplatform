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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
 * Keeping plain text raw is safe only because every template has an html-body:
 * without one the providers would send the plaintext-body as the HTML part, so
 * a missing html-body fails here too.
 * <p>
 * Values come from {@link Probe}, a map that answers any name, and any property
 * of any name (_place.name, _person.firstName, ...), with markup naming it, so
 * the test needs no list of message parameters or entity fields. A template with
 * {{#if}}, {{#unless}} or {{#if_equal}} is rendered a second time with those
 * conditions flipped, so both branches are checked.
 */
public class EmailTemplateRenderGuardTest extends IrisTestCase {
   private static final Logger LOGGER = LoggerFactory.getLogger(EmailTemplateRenderGuardTest.class);

   private static final File TEMPLATE_DIR = new File("src/dist/templates");
   private static final String EMAIL_SUFFIX = "-email.hbs";
   private static final String LAYOUT_PREFIX = "layout-";

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
   // Everything the HBS3 escaping strategy produces.
   private static final Pattern ENTITY = Pattern.compile("&(?:lt|gt|quot|amp|#x27|#x60|#x3D);");
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
         String templateId = template.getName().substring(0, template.getName().length() - ".hbs".length());
         rendered++;

         List<Probe> passes = new ArrayList<>();
         passes.add(new Probe("", HELPER_ARGUMENTS, Collections.emptySet()));
         Probe flipped = flipConditions(read(template));
         if(flipped != null) {
            passes.add(flipped);
         }

         boolean sawHtmlValue = false;
         boolean sawPlainValue = false;
         for(Probe context: passes) {
            String label = templateId + (context == flipped ? " (conditions flipped)" : "");
            renders++;
            Map<String, String> parts;
            try {
               parts = templates.renderMultipart(templateId, context);
            }
            catch(RuntimeException e) {
               failures.add(label + ": render failed: " + e.getMessage());
               continue;
            }

            String html = parts.get(EmailSections.HTML_BODY);
            if(html == null) {
               failures.add(label + ": no html-body section");
               continue;
            }
            Set<String> raw = names(RAW, html);
            if(!raw.isEmpty()) {
               failures.add(label + ": raw in html-body: " + raw);
            }
            sawHtmlValue |= ESCAPED.matcher(html).find();

            for(String section: EmailSections.PLAIN_TEXT) {
               String text = parts.get(section);
               if(text == null) {
                  continue;
               }
               if(ENTITY.matcher(text).find()) {
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
    * A context for a second render that takes the other branch of every
    * condition in the template: names tested by #if or #unless are missing,
    * and names tested by #if_equal get the value they are compared with.
    * Returns null when the template has no conditions.
    */
   private static Probe flipConditions(String source) {
      Map<String, String> values = new HashMap<>(HELPER_ARGUMENTS);
      Set<String> absent = new HashSet<>();
      Matcher m = IF.matcher(source);
      while(m.find()) {
         absent.add(m.group(1));
      }
      m = IF_EQUAL.matcher(source);
      while(m.find()) {
         values.put(m.group(1), m.group(2));
      }
      return absent.isEmpty() && values.equals(HELPER_ARGUMENTS) ? null : new Probe("", values, absent);
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

   private static List<File> emailTemplates() {
      File[] files = TEMPLATE_DIR.listFiles((dir, name) -> name.endsWith(EMAIL_SUFFIX) && !name.startsWith(LAYOUT_PREFIX));
      assertTrue("no email templates in " + TEMPLATE_DIR.getAbsolutePath(), files != null && files.length > 0);
      Arrays.sort(files);
      return Arrays.asList(files);
   }

   private static String read(File file) throws IOException {
      return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
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
