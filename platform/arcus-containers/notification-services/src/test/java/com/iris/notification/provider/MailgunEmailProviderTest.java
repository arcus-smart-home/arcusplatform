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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
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
import com.mailgun.model.message.MessageResponse;

import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ MailgunClient.class })
public class MailgunEmailProviderTest {
    
   protected Notification notification = new NotificationBuilder().build();

   @Mock
   protected MailgunMessagesApi mailgunMessagesApi;

   @Mock
   protected PersonDAO personDao;

   @Mock
   protected Person person;

   @Mock
   protected NotificationMessageRenderer messageRenderer;

   @Mock
   protected UpstreamNotificationResponder responder;

   @Mock
   private PlaceDAO placeDao;
   @Mock
   private Place place;
   private UUID placeId = UUID.randomUUID();
   private UUID personId = UUID.randomUUID();

   @Mock
   protected Logger logger;

   @InjectMocks
   protected MailgunEmailProvider uut;

   private String expectedEmailBody = "test-message";
   private String expectedEmailFromEmail = "bill@birditzman.com";
   private String expectedFirstName = "Bill";
   private String expectedLastName = "Birditzman";
   private String expectedFullName = expectedFirstName + " " + expectedLastName;

   
   @Before
   public void initializeMailgunMock() throws Exception {
      new FieldSetter(uut, uut.getClass().getDeclaredField("mailgunMessagesApiUS")).set(mailgunMessagesApi);
      new FieldSetter(uut, uut.getClass().getDeclaredField("logger")).set(logger);
      Map<String, String> renderedParts = new HashMap<String, String>();
      renderedParts.put("", expectedEmailBody);

      notification = new NotificationBuilder().withPersonId(personId).withPlaceId(placeId).build();
      Map<String, BaseEntity<?, ?>> entityMap = new HashMap<>(2);
      entityMap.put(NotificationProviderUtil.RECIPIENT_KEY, person);
      entityMap.put(NotificationProviderUtil.PLACE_KEY, place);

      mockStatic(MailgunClient.class);
      when(MailgunClient.config(Mockito.any(String.class)).createApi(MailgunMessagesApi.class)).thenReturn(mailgunMessagesApi);

      Mockito.when(personDao.findById(Mockito.any())).thenReturn(person);
      Mockito.when(placeDao.findById(placeId)).thenReturn(place);
      Mockito.when(person.getEmail()).thenReturn(expectedEmailFromEmail);
      Mockito.when(person.getFirstName()).thenReturn(expectedFirstName);
      Mockito.when(person.getLastName()).thenReturn(expectedLastName);
      Mockito.when(messageRenderer.renderMessage(notification, NotificationMethod.EMAIL, person, entityMap)).thenReturn(expectedEmailBody);
      Mockito.when(messageRenderer.renderMultipartMessage(notification, NotificationMethod.EMAIL, person, entityMap)).thenReturn(renderedParts);
      Mockito.when(mailgunMessagesApi.sendMessage(Mockito.any(String.class), Mockito.any(Message.class)));
   }

   @Test
   public void testEmailValidation() {
      Boolean result = uut.isEmailValid( null);
      assertFalse(result);

      result = uut.isEmailValid("");
      assertFalse(result);

      result = uut.isEmailValid((String) null);
      assertFalse(result);

      result = uut.isEmailValid("not valid email");
      assertFalse(result);

      result = uut.isEmailValid("wes.stueve@wds-it.com");
      assertTrue(result);
   }

   @Test(expected = DispatchUnsupportedByUserException.class)
   public void shouldFailWithUnknownCustomer() throws Exception {
      Mockito.when(personDao.findById(Mockito.any())).thenReturn(null);
      uut.notifyCustomer(new NotificationBuilder().build());
   }

   @Test
   public void shouldFailWithNoEmail() throws Exception {
      Mockito.when(personDao.findById(Mockito.any())).thenReturn(person);
      Mockito.when(person.getEmail()).thenReturn(null);

      uut.notifyCustomer(notification);

      Mockito.verify(logger).warn(Mockito.anyString(), Mockito.anyObject(), Mockito.anyObject(), Mockito.anyObject(), Mockito.anyObject());
   }
   
   @Test
   public void shouldSendEmailWithFullName() throws DispatchException, DispatchUnsupportedByUserException, IOException {
      ArgumentCaptor<Message> mailRequestCaptor = ArgumentCaptor.forClass(Message.class);
      uut.notifyCustomer(notification);
      Mockito.verify(mailgunMessagesApi).sendMessage(expectedEmailFromEmail, mailRequestCaptor.capture());

      validateEmail(mailRequestCaptor.getValue(), expectedFullName, expectedEmailFromEmail, expectedEmailBody);
   }

   @Test
   public void shouldSendEmailWithPartialName() throws DispatchException, DispatchUnsupportedByUserException, JsonParseException, JsonMappingException, IOException {
      Mockito.when(person.getFirstName()).thenReturn("");
      ArgumentCaptor<Message> mailRequestCaptor = ArgumentCaptor.forClass(Message.class);
      uut.notifyCustomer(notification);
      Mockito.verify(mailgunMessagesApi).sendMessage(expectedEmailFromEmail, mailRequestCaptor.capture());

      validateEmail(mailRequestCaptor.getValue(), expectedEmailFromEmail, expectedEmailFromEmail, expectedEmailBody);
   }

   @Test
   public void shouldSendEmailWithNoName() throws DispatchException, DispatchUnsupportedByUserException, JsonParseException, JsonMappingException, IOException {
      Mockito.when(person.getFirstName()).thenReturn("");
      Mockito.when(person.getLastName()).thenReturn(null);
      ArgumentCaptor<Message> emailCaptor = ArgumentCaptor.forClass(Message.class);
      uut.notifyCustomer(notification);
      Mockito.verify(mailgunMessagesApi).sendMessage(expectedEmailFromEmail, emailCaptor.capture());

      validateEmail(emailCaptor.getValue(), expectedEmailFromEmail, expectedEmailFromEmail, expectedEmailBody);
   }
   
   private void validateEmail(Message message, String toName, String toEmail, String expectedEmailBody) throws JsonParseException, JsonMappingException, IOException {
      String html = message.getHtml();
      String text = message.getText();
      boolean foundHtmlContent = false;
      boolean foundTextContent = false;
        
      if (html != null && !html.isEmpty()) {
         assertEquals(expectedEmailBody, message.getHtml());
         foundHtmlContent = true;
      } else if (text != null && !text.isEmpty()) {
         assertEquals(expectedEmailBody, message.getText());
         foundTextContent = true;
      }
      assertEquals(true, foundHtmlContent);
      assertEquals(true, foundTextContent);
      
      assertEquals(toEmail, message.getReplyTo());
   }
}
