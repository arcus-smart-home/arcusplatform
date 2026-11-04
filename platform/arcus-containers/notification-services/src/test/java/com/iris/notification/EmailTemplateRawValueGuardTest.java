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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Source-level backstop for {@link EmailTemplateRenderGuardTest}, which is the
 * main check. Rendering only sees the branches its values select, so this test
 * also reads the template source and fails on any raw {{{x}}}, {{&x}} or raw
 * block in an html-body section, or anywhere in a partial an html-body
 * includes, directly or through other partials. Subject and plaintext-body
 * sections are plain text and are meant to stay raw, so they are not checked.
 */
public class EmailTemplateRawValueGuardTest {

   private static final File TEMPLATE_DIR = new File("src/dist/templates");
   private static final String EMAIL_SUFFIX = "-email.hbs";

   /**
    * Raw values allowed in HTML, as "template-file: expression". A value may
    * only go here when its producer deliberately puts markup in it and escapes
    * every user-entered part itself; say which producer next to the entry.
    * No template needs one today.
    */
   private static final Set<String> ALLOWED_RAW = ImmutableSet.of();

   private static final Pattern HTML_BODY = Pattern.compile(
         "\\{\\{~?#section\\s+([\"'])html-body\\1\\s*~?\\}\\}(.*?)\\{\\{~?/section\\s*~?\\}\\}",
         Pattern.DOTALL
   );
   // {{{x}}}, {{~{x}~}}, {{{{raw}}}} blocks and {{&x}} all skip escaping.
   private static final Pattern RAW = Pattern.compile(
         "\\{\\{~?\\{\\s*(.*?)\\s*\\}~?\\}\\}|\\{\\{~?&\\s*(.*?)\\s*~?\\}\\}",
         Pattern.DOTALL
   );
   // {{> name}} and {{#> name}}, with or without quotes around the name.
   private static final Pattern PARTIAL = Pattern.compile("\\{\\{~?#?>\\s*[\"']?([\\w.-]+)");

   @Test
   public void htmlBodiesHaveNoRawValues() throws IOException {
      List<String> violations = new ArrayList<>();
      Set<String> partials = new TreeSet<>();
      Deque<String> toScan = new ArrayDeque<>();
      int sections = 0;

      for(File template: emailTemplates()) {
         Matcher body = HTML_BODY.matcher(read(template));
         while(body.find()) {
            sections++;
            collectRaw(template.getName(), body.group(2), violations);
            collectPartials(body.group(2), partials, toScan);
         }
      }

      // Partials such as layout-html-email are rendered inside the html-body,
      // so all of their content is HTML, and so is any partial they include.
      while(!toScan.isEmpty()) {
         File file = new File(TEMPLATE_DIR, toScan.pop() + ".hbs");
         assertTrue("html-body includes missing partial " + file, file.isFile());
         String content = read(file);
         collectRaw(file.getName(), content, violations);
         collectPartials(content, partials, toScan);
      }

      // Guard against the patterns silently matching nothing.
      assertTrue("found only " + sections + " html-body sections in " + TEMPLATE_DIR.getAbsolutePath(), sections > 100);
      assertTrue("found no partials included from html-body sections", partials.contains("layout-html-email"));

      if(!violations.isEmpty()) {
         fail(
               violations.size() + " raw values in HTML email content; use {{x}} so they are escaped:\n  " +
               String.join("\n  ", violations)
         );
      }
   }

   private static void collectRaw(String name, String html, List<String> violations) {
      Matcher raw = RAW.matcher(html);
      while(raw.find()) {
         String expression = raw.group(1) != null ? raw.group(1) : raw.group(2);
         String entry = name + ": " + expression;
         if(!ALLOWED_RAW.contains(entry)) {
            violations.add(entry);
         }
      }
   }

   private static void collectPartials(String content, Set<String> partials, Deque<String> toScan) {
      Matcher partial = PARTIAL.matcher(content);
      while(partial.find()) {
         if(partials.add(partial.group(1))) {
            toScan.push(partial.group(1));
         }
      }
   }

   private static List<File> emailTemplates() {
      File[] files = TEMPLATE_DIR.listFiles((dir, name) -> name.endsWith(EMAIL_SUFFIX));
      assertTrue("no email templates in " + TEMPLATE_DIR.getAbsolutePath(), files != null && files.length > 0);
      Arrays.sort(files);
      return Arrays.asList(files);
   }

   private static String read(File file) throws IOException {
      return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
   }
}
