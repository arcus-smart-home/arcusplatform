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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import com.iris.notification.provider.EmailSections;

/**
 * Shared by the email template tests: where the templates are, which sections
 * are plain text, and what HTML escaping looks like.
 */
public final class EmailTemplates {

   /** Relative to the project directory, which is where Gradle runs tests. */
   public static final File TEMPLATE_DIR = new File("src/dist/templates");

   /** Every section that is sent as plain text, which is all of them except the html-body. */
   public static final Set<String> PLAIN_TEXT_SECTIONS = ImmutableSet.of(
         EmailSections.SUBJECT,
         EmailSections.PLAINTEXT_BODY,
         EmailSections.SENDER_NAME,
         EmailSections.SENDER_EMAIL,
         EmailSections.REPLYTO_EMAIL
   );

   /** Everything the HBS3 escaping strategy produces. */
   public static final Pattern HBS3_ENTITY = Pattern.compile("&(?:lt|gt|quot|amp|#x27|#x60|#x3D);");

   private static final String EMAIL_SUFFIX = "-email.hbs";
   private static final String LAYOUT_PREFIX = "layout-";

   private EmailTemplates() {
   }

   /** Every *-email.hbs file except the layout partials, sorted by name. */
   public static List<File> emailTemplates() {
      File[] files = TEMPLATE_DIR.listFiles((dir, name) -> name.endsWith(EMAIL_SUFFIX) && !name.startsWith(LAYOUT_PREFIX));
      assertTrue("no email templates in " + TEMPLATE_DIR.getAbsolutePath(), files != null && files.length > 0);
      Arrays.sort(files);
      return Arrays.asList(files);
   }

   /** The id the template service loads a template file by. */
   public static String templateId(File template) {
      return template.getName().substring(0, template.getName().length() - ".hbs".length());
   }

   public static String read(File file) throws IOException {
      return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
   }
}
