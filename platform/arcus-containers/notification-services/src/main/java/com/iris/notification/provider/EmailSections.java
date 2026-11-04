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

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Names of the {{#section}} blocks in the *-email templates, as returned by
 * {@code TemplateService.renderMultipart}. The providers send {@link #HTML_BODY}
 * as text/html and everything in {@link #PLAIN_TEXT} as plain text, so templates
 * escape values with {{x}} in the html-body and leave them raw with {{{x}}} in
 * the other sections. That is only safe because every email template has an
 * html-body: without one the providers send the plaintext-body as the HTML part.
 */
public final class EmailSections {

   public static final String SUBJECT = "subject";
   public static final String PLAINTEXT_BODY = "plaintext-body";
   public static final String HTML_BODY = "html-body";
   public static final String SENDER_NAME = "sender-name";
   public static final String SENDER_EMAIL = "sender-email";
   public static final String REPLYTO_EMAIL = "replyto-email";

   /** Every section that is sent as plain text, which is all of them except the html-body. */
   public static final Set<String> PLAIN_TEXT =
         ImmutableSet.of(SUBJECT, PLAINTEXT_BODY, SENDER_NAME, SENDER_EMAIL, REPLYTO_EMAIL);

   private EmailSections() {
   }
}
