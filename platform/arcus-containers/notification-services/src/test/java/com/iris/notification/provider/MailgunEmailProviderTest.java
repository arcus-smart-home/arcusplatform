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

import com.iris.core.dao.AccountDAO;
import com.mailgun.model.message.MessageResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.model.BaseEntity;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.notification.dispatch.DispatchException;
import com.iris.notification.dispatch.DispatchUnsupportedByUserException;
import com.iris.notification.message.NotificationBuilder;
import com.iris.notification.message.NotificationMessageRenderer;
import com.iris.notification.upstream.UpstreamNotificationResponder;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;
import com.iris.platform.notification.provider.NotificationProviderUtil;
import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.model.message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MailgunEmailProviderTest {

   protected Notification notification = new NotificationBuilder().build();

   protected MailgunMessagesApi mailgunMessagesApi;

   protected AccountDAO accountDao;

   protected PersonDAO personDao;

   protected Person person;

   protected String defaultSenderName;

   protected String defaultSenderEmail;

   protected NotificationMessageRenderer messageRenderer;

   protected UpstreamNotificationResponder responder;

   protected MessageResponse messageResponse;

   private final UUID placeId = UUID.randomUUID();
   private final UUID personId = UUID.randomUUID();

   protected Logger logger;

   protected MailgunEmailProvider mockMailgunEmailProvider;

   private final String expectedEmailBody = "test-message";
   private final String expectedEmailToEmail = "bill@birditzman.com";
   private final String expectedFirstName = "Bill";
   private final String expectedLastName = "Birditzman";

   private MockedStatic<LoggerFactory> mockedLoggerFactory;

   @Before
   public void initializeMailgunMock() throws Exception {
      logger = Mockito.mock(Logger.class);
      Place place = Mockito.mock(Place.class);
      PlaceDAO placeDao = Mockito.mock(PlaceDAO.class);
      responder = Mockito.mock(UpstreamNotificationResponder.class);
      messageRenderer = Mockito.mock(NotificationMessageRenderer.class);
      person = Mockito.mock(Person.class);
      personDao = Mockito.mock(PersonDAO.class);
      accountDao = Mockito.mock(AccountDAO.class);
      mailgunMessagesApi = Mockito.mock(MailgunMessagesApi.class);
      messageResponse = Mockito.mock(MessageResponse.class);

      Map<String, String> renderedParts = new HashMap<>();
      renderedParts.put(EmailSections.PLAINTEXT_BODY, expectedEmailBody);

      notification = new NotificationBuilder().withPersonId(personId).withPlaceId(placeId).build();
      Map<String, BaseEntity<?, ?>> entityMap = new HashMap<>(2);
      entityMap.put(NotificationProviderUtil.RECIPIENT_KEY, person);
      entityMap.put(NotificationProviderUtil.PLACE_KEY, place);

      mockedLoggerFactory = Mockito.mockStatic(LoggerFactory.class);
      mockedLoggerFactory.when(() -> LoggerFactory.getLogger(MailgunEmailProvider.class)).thenReturn(logger);

      Mockito.when(personDao.findById(Mockito.any())).thenReturn(person);
      Mockito.when(placeDao.findById(placeId)).thenReturn(place);
      Mockito.when(person.getEmail()).thenReturn(expectedEmailToEmail);
      Mockito.when(person.getFirstName()).thenReturn(expectedFirstName);
      Mockito.when(person.getLastName()).thenReturn(expectedLastName);
      Mockito.when(messageRenderer.renderMessage(notification, NotificationMethod.EMAIL, person, entityMap)).thenReturn(expectedEmailBody);
      Mockito.when(messageRenderer.renderMultipartMessage(notification, NotificationMethod.EMAIL, person, entityMap)).thenReturn(renderedParts);
      Mockito.when(mailgunMessagesApi.sendMessage(Mockito.any(String.class), Mockito.any(Message.class))).thenReturn(messageResponse);
      mockMailgunEmailProvider = new MailgunEmailProvider(mailgunMessagesApi, "testDomain", personDao, placeDao, accountDao, messageRenderer, responder);

      setField(mockMailgunEmailProvider, "logger", logger);
   }

   @After
   public void tearDown() {
      mockedLoggerFactory.close();
   }

   @Test
   public void testEmailValidation() {
      Boolean result = mockMailgunEmailProvider.isEmailValid( null);
      assertFalse(result);

      result = mockMailgunEmailProvider.isEmailValid("");
      assertFalse(result);

      result = mockMailgunEmailProvider.isEmailValid(null);
      assertFalse(result);

      result = mockMailgunEmailProvider.isEmailValid("not valid email");
      assertFalse(result);

      result = mockMailgunEmailProvider.isEmailValid("wes.stueve@wds-it.com");
      assertTrue(result);
   }

   @Test(expected = DispatchUnsupportedByUserException.class)
   public void shouldFailWithUnknownCustomer() throws Exception {
      Mockito.when(personDao.findById(Mockito.any())).thenReturn(null);
      mockMailgunEmailProvider.notifyCustomer(new NotificationBuilder().build());
   }

   @Test
   public void shouldFailWithNoEmail() throws Exception {
      Mockito.when(personDao.findById(Mockito.any())).thenReturn(person);
      Mockito.when(person.getEmail()).thenReturn(null);

      mockMailgunEmailProvider.notifyCustomer(notification);

      Mockito.verify(logger).warn(
              Mockito.eq("Notification [{}] for placeId [{}] for person [{}] contained invalid recipientEmail [{}]."),
              Mockito.eq("message-key"),
              Mockito.eq(placeId),
              Mockito.eq(personId),
              Mockito.isNull());
   }

   @Test
   public void shouldSendEmail() throws DispatchException, DispatchUnsupportedByUserException {
      ArgumentCaptor<Message> mailRequestCaptor = ArgumentCaptor.forClass(Message.class);
      Map<String, String> renderedParts = new HashMap<>();
      renderedParts.put(EmailSections.SENDER_NAME, "Wes Stueve");
      renderedParts.put(EmailSections.SENDER_EMAIL, "wes.stueve@wds-it.com");
      renderedParts.put(EmailSections.REPLYTO_EMAIL, "test@example.com");
      renderedParts.put(EmailSections.SUBJECT, "subject");
      renderedParts.put(EmailSections.HTML_BODY, expectedEmailBody);
      Mockito.when(messageRenderer.renderMultipartMessage(Mockito.any(Notification.class), Mockito.any(NotificationMethod.class), Mockito.any(Person.class), Mockito.any())).thenReturn(renderedParts);
      Mockito.when(messageResponse.getId()).thenReturn("message-successfully-sent");
      mockMailgunEmailProvider.notifyCustomer(notification);
      Mockito.verify(mailgunMessagesApi).sendMessage(Mockito.eq("testDomain"), mailRequestCaptor.capture());

      String expectedFromEmail = "Wes Stueve <wes.stueve@wds-it.com>";
      validateEmail(expectedFromEmail, mailRequestCaptor.getValue());
   }

   /**
    * The plaintext-body leaves values unescaped, so a template without an
    * html-body must get a text-only email, never its plain text as HTML.
    */
   @Test
   public void shouldSendTextOnlyEmailWithoutHtmlBody() throws Exception {
      String plaintext = "<a href=\"https://evil.example.com\">O'Brien & Daughters</a>";
      Message message = send(EmailSections.PLAINTEXT_BODY, plaintext);

      assertEquals(plaintext, message.getText());
      assertNull(message.getHtml());
   }

   /** A blank html-body would show an empty email, so it is left out too. */
   @Test
   public void shouldSendTextOnlyEmailWithBlankHtmlBody() throws Exception {
      Message message = send(EmailSections.PLAINTEXT_BODY, "plain text", EmailSections.HTML_BODY, "\n\t \n");

      assertEquals("plain text", message.getText());
      assertNull(message.getHtml());
   }

   /** Without a plaintext-body the message goes out as HTML only. */
   @Test
   public void shouldSendHtmlOnlyEmailWithoutPlaintextBody() throws Exception {
      Message message = send(EmailSections.HTML_BODY, "<p>html</p>");
      assertNull(message.getText());
      assertEquals("<p>html</p>", message.getHtml());
   }

   /** And so it does with a blank one. */
   @Test
   public void shouldSendHtmlOnlyEmailWithBlankPlaintextBody() throws Exception {
      Message message = send(EmailSections.PLAINTEXT_BODY, " \n", EmailSections.HTML_BODY, "<p>html</p>");
      assertNull(message.getText());
      assertEquals("<p>html</p>", message.getHtml());
   }

   /** Renders the given sections plus sender, reply-to and subject, and returns the message sent to Mailgun. */
   private Message send(String... sections) throws Exception {
      ArgumentCaptor<Message> mailRequestCaptor = ArgumentCaptor.forClass(Message.class);
      Map<String, String> renderedParts = new HashMap<>();
      renderedParts.put(EmailSections.SENDER_NAME, "Example Sender");
      renderedParts.put(EmailSections.SENDER_EMAIL, "sender@example.com");
      renderedParts.put(EmailSections.REPLYTO_EMAIL, "test@example.com");
      renderedParts.put(EmailSections.SUBJECT, "subject");
      for(int i = 0; i < sections.length; i += 2) {
         renderedParts.put(sections[i], sections[i + 1]);
      }
      Mockito.when(messageRenderer.renderMultipartMessage(Mockito.any(Notification.class), Mockito.any(NotificationMethod.class), Mockito.any(Person.class), Mockito.any())).thenReturn(renderedParts);
      Mockito.when(messageResponse.getId()).thenReturn("message-successfully-sent");

      mockMailgunEmailProvider.notifyCustomer(notification);
      Mockito.verify(mailgunMessagesApi).sendMessage(Mockito.eq("testDomain"), mailRequestCaptor.capture());
      return mailRequestCaptor.getValue();
   }

   private void validateEmail(String expectedFromEmail, Message message) {
      String html = message.getHtml();
      String text = message.getText();

      if (html != null && !html.isEmpty()) {
         assertEquals(expectedEmailBody, message.getHtml());
      } else if (text != null && !text.isEmpty()) {
         assertEquals(expectedEmailBody, message.getText());
      }

      assertEquals(expectedFromEmail, message.getFrom());
      assertEquals("subject", message.getSubject());
      assertEquals("test@example.com", message.getReplyTo());
      assertTrue(message.getTo().contains(expectedEmailToEmail));
   }

   @Test
   public void shouldSendEmailWithDefaultSender() throws Exception {
      defaultSenderName = "Arcus Platform";
      defaultSenderEmail = "mail@arcus.test.net";
      setField(mockMailgunEmailProvider, "defaultSenderName", defaultSenderName);
      setField(mockMailgunEmailProvider, "defaultSenderEmail", defaultSenderEmail);
      ArgumentCaptor<Message> mailRequestCaptor = ArgumentCaptor.forClass(Message.class);
      Map<String, String> renderedParts = new HashMap<>();
      renderedParts.put(EmailSections.REPLYTO_EMAIL, "test@example.com");
      renderedParts.put(EmailSections.SUBJECT, "subject");
      renderedParts.put(EmailSections.HTML_BODY, expectedEmailBody);
      Mockito.when(messageRenderer.renderMultipartMessage(Mockito.any(Notification.class), Mockito.any(NotificationMethod.class), Mockito.any(Person.class), Mockito.any())).thenReturn(renderedParts);
      Mockito.when(messageResponse.getId()).thenReturn("message-successfully-sent");
      mockMailgunEmailProvider.notifyCustomer(notification);
      Mockito.verify(mailgunMessagesApi).sendMessage(Mockito.eq("testDomain"), mailRequestCaptor.capture());

      validateEmail(defaultSenderName + " <" + defaultSenderEmail + ">", mailRequestCaptor.getValue());
   }

   private static void setField(Object target, String fieldName, Object value) throws Exception {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
   }
}
