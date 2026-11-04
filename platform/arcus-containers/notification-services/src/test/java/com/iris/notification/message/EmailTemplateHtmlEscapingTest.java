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
package com.iris.notification.message;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.template.HandlebarsTemplateService;
import com.iris.messages.model.BaseEntity;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.notification.provider.EmailSections;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;
import com.iris.platform.notification.provider.NotificationProviderUtil;
import com.iris.platform.rule.RuleDao;
import com.iris.test.IrisTestCase;

/**
 * Renders real email templates from src/dist/templates through the real
 * {@link HandlebarsTemplateService} and {@link TemplateMessageRenderer}, the
 * path the email providers use, with names that hold markup. The html-body
 * goes out as text/html, so a name there must come out escaped; the subject
 * and plaintext-body are plain text and must keep every occurrence of the name
 * as typed. EmailTemplateRenderGuardTest runs the same checks on every
 * template with made-up values; this test uses real entities and parameters.
 */
public class EmailTemplateHtmlEscapingTest extends IrisTestCase {

   private static final String LINK = "<a href=\"https://evil.example.com\">x</a>";
   private static final String LINK_ESCAPED = "&lt;a href=&quot;https://evil.example.com&quot;&gt;x&lt;/a&gt;";
   private static final String IMAGE = "<img src=\"https://evil.example.com/i.png\">";
   private static final String IMAGE_ESCAPED = "&lt;img src=&quot;https://evil.example.com/i.png&quot;&gt;";
   private static final String APOSTROPHE = "O'Brien & Daughters";
   private static final String APOSTROPHE_ESCAPED = "O&#x27;Brien &amp; Daughters";

   // Everything the HBS3 escaping strategy produces.
   private static final Pattern ENTITY = Pattern.compile("&(?:lt|gt|quot|amp|#x27|#x60|#x3D);");

   private PlaceDAO placeDao;
   private DeviceDAO deviceDao;
   private PersonDAO personDao;
   private RuleDao ruleDao;
   private TemplateMessageRenderer renderer;

   // IrisTestCase.setUp registers the src/dist resource factory the template
   // loader reads from, and JUnit runs it before this method.
   @Before
   public void createRenderer() {
      // None of these values are {address}.attribute lookups, so the
      // parameter parser must not touch the DAOs.
      placeDao = EasyMock.createMock(PlaceDAO.class);
      deviceDao = EasyMock.createMock(DeviceDAO.class);
      personDao = EasyMock.createMock(PersonDAO.class);
      ruleDao = EasyMock.createMock(RuleDao.class);
      EasyMock.replay(placeDao, deviceDao, personDao, ruleDao);

      renderer = new TemplateMessageRenderer(
            new HandlebarsTemplateService("templates", 0),
            new ParameterParser(new ValueLookup(placeDao, deviceDao, personDao, ruleDao))
      );
      renderer.staticResourceServerUrl = "https://static.example.com";
      renderer.atTwitter = "example";
      renderer.redirectBaseUrl = "https://redirect.example.com";
      renderer.webHomeBaseUrl = "https://home.example.com";
      renderer.init();
   }

   @After
   public void verifyMocks() {
      EasyMock.verify(placeDao, deviceDao, personDao, ruleDao);
   }

   @Test
   public void deviceNameIsEscapedInHtmlBody() {
      Map<String, String> parts = render(
            "water.lowsalt",
            entities(place("Example Cabin"), person("Alex", "Sample"), null),
            "deviceName", LINK
      );

      assertEquals("Low Salt", parts.get(EmailSections.SUBJECT));
      assertEscaped(parts.get(EmailSections.HTML_BODY), LINK, LINK_ESCAPED);
      assertPlainText(parts, LINK + " at Example Cabin is low on salt.");
   }

   @Test
   public void placeAndOwnerNamesAreEscapedInHtmlBody() {
      Place place = place(LINK);
      place.setStreetAddress1("1 Example Road & Annex");
      place.setCity("O'Exampleton");
      Map<String, String> parts = render(
            "alarm.smoke.triggered",
            entities(place, person("Alex", "Sample"), person(IMAGE, APOSTROPHE)),
            "deviceName", "Hallway Smoke",
            "deviceType", "Smoke Detector"
      );

      assertEquals("Smoke Alarm Triggered", parts.get(EmailSections.SUBJECT));
      String html = parts.get(EmailSections.HTML_BODY);
      assertEscaped(html, LINK, LINK_ESCAPED);
      assertEscaped(html, IMAGE, IMAGE_ESCAPED);
      assertEscaped(html, APOSTROPHE, APOSTROPHE_ESCAPED);
      assertEscaped(html, "1 Example Road & Annex", "1 Example Road &amp; Annex");
      assertPlainText(
            parts,
            "The Smoke Alarm at " + IMAGE + " " + APOSTROPHE + "'s place (" + LINK + ") was triggered",
            "\n" + LINK + "\n1 Example Road & Annex\nO'Exampleton, "
      );
   }

   @Test
   public void ruleNameIsEscapedInHtmlBody() {
      Map<String, String> parts = render(
            "alarm.panic.triggered.rule",
            entities(place(APOSTROPHE), person("Alex", "Sample"), person("Sam", "Sample")),
            "_ruleName", LINK
      );

      assertEscaped(parts.get(EmailSections.HTML_BODY), LINK, LINK_ESCAPED);
      assertPlainText(parts, "Sam Sample's place (" + APOSTROPHE + ") was triggered");
   }

   @Test
   public void personNameIsEscapedInHtmlBodyAndRuleNameKeptInSubject() {
      Map<String, String> parts = render(
            "rule.4d073c.absence",
            entities(place("Example Cabin"), person("Alex", "Sample"), null),
            "person", IMAGE,
            "_ruleName", LINK + " " + APOSTROPHE
      );

      assertEquals(LINK + " " + APOSTROPHE + " Triggered", parts.get(EmailSections.SUBJECT));
      assertEscaped(parts.get(EmailSections.HTML_BODY), IMAGE, IMAGE_ESCAPED);
      assertPlainText(parts, IMAGE + " has left Example Cabin.");
   }

   @Test
   public void invitationSubjectKeepsInviterNameAsTyped() {
      Map<String, String> parts = render(
            "account.person.invited",
            entities(place("Example Cabin"), person("Alex", "Sample"), null),
            "invitorFirstName", APOSTROPHE,
            "invitorLastName", LINK
      );

      assertEquals("Accept Your Invitation To " + APOSTROPHE + " " + LINK + "'s Place", parts.get(EmailSections.SUBJECT));
      assertPlainText(parts);
   }

   private static void assertEscaped(String html, String raw, String escaped) {
      assertNotNull(html);
      assertFalse("raw value in html-body:\n" + html, html.contains(raw));
      assertTrue("escaped value missing from html-body:\n" + html, html.contains(escaped));
   }

   /**
    * No value anywhere in the subject or plaintext-body may be HTML-escaped,
    * and each fragment must appear in the plaintext-body as typed.
    */
   private static void assertPlainText(Map<String, String> parts, String... fragments) {
      String subject = parts.get(EmailSections.SUBJECT);
      String text = parts.get(EmailSections.PLAINTEXT_BODY);
      assertNotNull(subject);
      assertNotNull(text);
      assertFalse("HTML-escaped value in subject: " + subject, ENTITY.matcher(subject).find());
      assertFalse("HTML-escaped value in plaintext-body:\n" + text, ENTITY.matcher(text).find());
      for(String fragment: fragments) {
         assertTrue("plaintext-body is missing [" + fragment + "]:\n" + text, text.contains(fragment));
      }
   }

   private Map<String, String> render(String messageKey, Map<String, BaseEntity<?, ?>> entities, String... params) {
      NotificationBuilder builder = new NotificationBuilder().withMessageKey(messageKey);
      for(int i = 0; i < params.length; i += 2) {
         builder.withMessageParam(params[i], params[i + 1]);
      }
      Notification notification = builder.build();
      Person recipient = (Person) entities.get(NotificationProviderUtil.RECIPIENT_KEY);
      return renderer.renderMultipartMessage(notification, NotificationMethod.EMAIL, recipient, entities);
   }

   private static Map<String, BaseEntity<?, ?>> entities(Place place, Person recipient, Person owner) {
      Map<String, BaseEntity<?, ?>> entities = new HashMap<>();
      entities.put(NotificationProviderUtil.PLACE_KEY, place);
      entities.put(NotificationProviderUtil.RECIPIENT_KEY, recipient);
      if(owner != null) {
         entities.put(NotificationProviderUtil.OWNER_KEY, owner);
      }
      return entities;
   }

   private static Place place(String name) {
      Place place = new Place();
      place.setName(name);
      return place;
   }

   private static Person person(String firstName, String lastName) {
      Person person = new Person();
      person.setFirstName(firstName);
      person.setLastName(lastName);
      return person;
   }
}
