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

/**
 * Names of the {{#section}} blocks in the *-email templates, as returned by
 * {@code TemplateService.renderMultipart}. The providers send {@link #HTML_BODY}
 * as text/html and the other sections as plain text, so templates escape values
 * with {{x}} in the html-body and leave them raw with {{{x}}} everywhere else.
 * The plaintext-body is never sent as HTML: a template without an html-body
 * gets a text-only email. {@link EmailBodies} makes that choice for the providers.
 */
public final class EmailSections {

   public static final String SUBJECT = "subject";
   public static final String PLAINTEXT_BODY = "plaintext-body";
   public static final String HTML_BODY = "html-body";
   public static final String SENDER_NAME = "sender-name";
   public static final String SENDER_EMAIL = "sender-email";
   public static final String REPLYTO_EMAIL = "replyto-email";

   private EmailSections() {
   }
}
