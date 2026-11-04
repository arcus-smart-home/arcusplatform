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
package com.iris.notification.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.iris.core.template.TemplateService;

public class EmailBodiesTest {

   private static final String TEXT = "\nHi Alex,\n<a href=\"https://evil.example.com\">O'Brien & Daughters</a>\n";
   private static final String HTML = "\n<p>Hi Alex,</p>\n";

   @Test
   public void bothSectionsGiveBothPartsUnchanged() {
      EmailBodies bodies = EmailBodies.from(sections(TEXT, HTML));
      assertEquals(TEXT, bodies.getText());
      assertEquals(HTML, bodies.getHtml());
   }

   @Test
   public void missingHtmlBodyGivesTextOnly() {
      EmailBodies bodies = EmailBodies.from(sections(TEXT, null));
      assertEquals(TEXT, bodies.getText());
      // The plaintext-body leaves values raw, so it never stands in for HTML.
      assertNull(bodies.getHtml());
   }

   @Test
   public void missingPlaintextBodyGivesHtmlOnly() {
      EmailBodies bodies = EmailBodies.from(sections(null, HTML));
      assertNull(bodies.getText());
      assertEquals(HTML, bodies.getHtml());
   }

   @Test
   public void blankSectionsGiveNoPart() {
      for(String blank: new String[] { "", " ", "\n\t \n" }) {
         EmailBodies bodies = EmailBodies.from(sections(blank, blank));
         assertNull("text for [" + blank + "]", bodies.getText());
         assertNull("html for [" + blank + "]", bodies.getHtml());
      }
   }

   @Test
   public void textOutsideTheSectionsIsNotUsed() {
      Map<String, String> sections = new HashMap<>();
      sections.put("", TEXT);
      sections.put(TemplateService.UNSECTIONED_CONTENT, TEXT);
      EmailBodies bodies = EmailBodies.from(sections);
      assertNull(bodies.getText());
      assertNull(bodies.getHtml());
   }

   private static Map<String, String> sections(String text, String html) {
      Map<String, String> sections = new HashMap<>();
      sections.put(EmailSections.SUBJECT, "Subject");
      if(text != null) {
         sections.put(EmailSections.PLAINTEXT_BODY, text);
      }
      if(html != null) {
         sections.put(EmailSections.HTML_BODY, html);
      }
      return sections;
   }
}
