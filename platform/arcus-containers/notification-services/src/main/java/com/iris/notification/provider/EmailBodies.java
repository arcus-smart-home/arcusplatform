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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * The text and HTML parts of a rendered email, chosen in one place for every
 * provider. The text part is the plaintext-body and the HTML part is the
 * html-body. The plaintext-body leaves values unescaped, so it is never used as
 * the HTML part. A section that is missing or renders blank gives no part, and
 * providers send only the parts that are present.
 */
public final class EmailBodies {

   private final String text;
   private final String html;

   private EmailBodies(String text, String html) {
      this.text = text;
      this.html = html;
   }

   /** Picks the parts out of the sections returned by {@code TemplateService.renderMultipart}. */
   public static EmailBodies from(Map<String, String> sections) {
      return new EmailBodies(
            StringUtils.defaultIfBlank(sections.get(EmailSections.PLAINTEXT_BODY), null),
            StringUtils.defaultIfBlank(sections.get(EmailSections.HTML_BODY), null)
      );
   }

   /** The text/plain part, or null when there is none. */
   public String getText() {
      return text;
   }

   /** The text/html part, or null when there is none. */
   public String getHtml() {
      return html;
   }
}
