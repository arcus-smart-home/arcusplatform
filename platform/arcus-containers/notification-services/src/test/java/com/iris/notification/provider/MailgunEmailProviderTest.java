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
import com.mailgun.client.MailgunClient;
import com.mailgun.model.message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

   private final static String SENDER_NAME_SECTION = "sender-name";
   private final static String SENDER_EMAIL_SECTION = "sender-email";
   private final static String REPLYTO_EMAIL_SECTION = "replyto-email";
   private final static String SUBJECT_SECTION = "subject";
   private final static String HTML_BODY_SECTION = "html-body";

   private MockedStatic<MailgunClient> mockedMailgunClient;
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
      renderedParts.put("", expectedEmailBody);

      notification = new NotificationBuilder().withPersonId(personId).withPlaceId(placeId).build();
      Map<String, BaseEntity<?, ?>> entityMap = new HashMap<>(2);
      entityMap.put(NotificationProviderUtil.RECIPIENT_KEY, person);
      entityMap.put(NotificationProviderUtil.PLACE_KEY, place);

      mockedMailgunClient = Mockito.mockStatic(MailgunClient.class);
      mockedLoggerFactory = Mockito.mockStatic(LoggerFactory.class);
      mockedLoggerFactory.when(() -> LoggerFactory.getLogger(MailgunEmailProvider.class)).thenReturn(logger);
      MailgunClient.MailgunClientBuilder mockMailgunClientBuilder = Mockito.mock(MailgunClient.MailgunClientBuilder.class);
      mockedMailgunClient.when(() -> MailgunClient.config(Mockito.any(String.class))).thenReturn(mockMailgunClientBuilder);
      Mockito.when(mockMailgunClientBuilder.createApi(MailgunMessagesApi.class)).thenReturn(mailgunMessagesApi);

      Mockito.when(personDao.findById(Mockito.any())).thenReturn(person);
      Mockito.when(placeDao.findById(placeId)).thenReturn(place);
      Mockito.when(person.getEmail()).thenReturn(expectedEmailToEmail);
      Mockito.when(person.getFirstName()).thenReturn(expectedFirstName);
      Mockito.when(person.getLastName()).thenReturn(expectedLastName);
      Mockito.when(messageRenderer.renderMessage(notification, NotificationMethod.EMAIL, person, entityMap)).thenReturn(expectedEmailBody);
      Mockito.when(messageRenderer.renderMultipartMessage(notification, NotificationMethod.EMAIL, person, entityMap)).thenReturn(renderedParts);
      Mockito.when(mailgunMessagesApi.sendMessage(Mockito.any(String.class), Mockito.any(Message.class))).thenReturn(messageResponse);
      mockMailgunEmailProvider = new MailgunEmailProvider("fakeApiKey", "testDomain", personDao, placeDao, accountDao, messageRenderer, responder);

      setField(mockMailgunEmailProvider, "logger", logger);
   }

   @After
   public void tearDown() {
      mockedLoggerFactory.close();
      mockedMailgunClient.close();
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
      renderedParts.put(SENDER_NAME_SECTION, "Wes Stueve");
      renderedParts.put(SENDER_EMAIL_SECTION, "wes.stueve@wds-it.com");
      renderedParts.put(REPLYTO_EMAIL_SECTION, "test@example.com");
      renderedParts.put(SUBJECT_SECTION, "subject");
      renderedParts.put(HTML_BODY_SECTION, expectedEmailBody);
      Mockito.when(messageRenderer.renderMultipartMessage(Mockito.any(Notification.class), Mockito.any(NotificationMethod.class), Mockito.any(Person.class), Mockito.any())).thenReturn(renderedParts);
      Mockito.when(messageResponse.getId()).thenReturn("message-successfully-sent");
      mockMailgunEmailProvider.notifyCustomer(notification);
      Mockito.verify(mailgunMessagesApi).sendMessage(Mockito.eq("testDomain"), mailRequestCaptor.capture());

      String expectedFromEmail = "Wes Stueve <wes.stueve@wds-it.com>";
      validateEmail(expectedFromEmail, mailRequestCaptor.getValue());
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
      renderedParts.put(REPLYTO_EMAIL_SECTION, "test@example.com");
      renderedParts.put(SUBJECT_SECTION, "subject");
      renderedParts.put(HTML_BODY_SECTION, expectedEmailBody);
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
