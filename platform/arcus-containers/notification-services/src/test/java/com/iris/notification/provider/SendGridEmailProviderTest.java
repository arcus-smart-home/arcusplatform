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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.slf4j.Logger;


@RunWith(MockitoJUnitRunner.class)
public class SendGridEmailProviderTest {

   protected Notification notification = new NotificationBuilder().build();

   @Mock
   protected SendGrid sendGrid;

   @Mock
   protected PersonDAO personDao;

   @Mock
   protected Person person;

   @Mock
   protected Response response;

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
   protected SendGridEmailProvider uut;

   private String expectedEmailBody = "test-message";
   private String expectedEmailFromEmail = "bill@birditzman.com";
   private String expectedFirstName = "Bill";
   private String expectedLastName = "Birditzman";
   private String expectedFullName = expectedFirstName + " " + expectedLastName;


   @Before
   public void initializeSendGridMock() throws Exception {
      new FieldSetter(uut, uut.getClass().getDeclaredField("sendGrid")).set(sendGrid);
      new FieldSetter(uut, uut.getClass().getDeclaredField("logger")).set(logger);
      Map<String, String> renderedParts = new HashMap<String, String>();
      renderedParts.put("", expectedEmailBody);

      notification = new NotificationBuilder().withPersonId(personId).withPlaceId(placeId).build();
      Map<String, BaseEntity<?, ?>> entityMap = new HashMap<>(2);
      entityMap.put(NotificationProviderUtil.RECIPIENT_KEY, person);
      entityMap.put(NotificationProviderUtil.PLACE_KEY, place);

      Mockito.when(personDao.findById(Mockito.any())).thenReturn(person);
      Mockito.when(placeDao.findById(placeId)).thenReturn(place);
      Mockito.when(person.getEmail()).thenReturn(expectedEmailFromEmail);
      Mockito.when(person.getFirstName()).thenReturn(expectedFirstName);
      Mockito.when(person.getLastName()).thenReturn(expectedLastName);
      Mockito.when(messageRenderer.renderMessage(notification, NotificationMethod.EMAIL, person, entityMap)).thenReturn(expectedEmailBody);
      Mockito.when(messageRenderer.renderMultipartMessage(notification, NotificationMethod.EMAIL, person, entityMap)).thenReturn(renderedParts);
      Mockito.when(sendGrid.api(Mockito.any())).thenReturn(response);

   }

   @Test
   public void testEmailValidation() {
      Boolean result = uut.isEmailValid((Email) null);
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
      ArgumentCaptor<Request> mailRequestCaptor = ArgumentCaptor.forClass(Request.class);
      uut.notifyCustomer(notification);
      Mockito.verify(sendGrid).api(mailRequestCaptor.capture());

      validateEmail(mailRequestCaptor.getValue(), expectedFullName, expectedEmailFromEmail, expectedEmailBody);
   }

   @Test
   public void shouldSendEmailWithPartialName() throws DispatchException, DispatchUnsupportedByUserException, JsonParseException, JsonMappingException, IOException {
      Mockito.when(person.getFirstName()).thenReturn("");
      ArgumentCaptor<Request> mailRequestCaptor = ArgumentCaptor.forClass(Request.class);
      uut.notifyCustomer(notification);
      Mockito.verify(sendGrid).api(mailRequestCaptor.capture());

      validateEmail(mailRequestCaptor.getValue(), expectedEmailFromEmail, expectedEmailFromEmail, expectedEmailBody);
   }

   @Test
   public void shouldSendEmailWithNoName() throws DispatchException, DispatchUnsupportedByUserException, JsonParseException, JsonMappingException, IOException {
      Mockito.when(person.getFirstName()).thenReturn("");
      Mockito.when(person.getLastName()).thenReturn(null);
      ArgumentCaptor<Request> emailCaptor = ArgumentCaptor.forClass(Request.class);
      uut.notifyCustomer(notification);
      Mockito.verify(sendGrid).api(emailCaptor.capture());

      validateEmail(emailCaptor.getValue(), expectedEmailFromEmail, expectedEmailFromEmail, expectedEmailBody);
   }

   @Test(expected = DispatchException.class)
   public void shouldThrowExceptionIfSendGridThrowsException() throws DispatchException, DispatchUnsupportedByUserException, IOException {
      Mockito.when(sendGrid.api(Mockito.any())).thenThrow(new IOException(new Exception()));
      uut.notifyCustomer(notification);
   }

    /*
     * TODO - Can't test because response.statusCode is not a method. 
     
    @Test (expected=DispatchException.class)
    public void shouldThrowExceptionIfSendGridFails() throws DispatchException, DispatchUnsupportedByUserException {
        Mockito.when(response.statusCode).thenReturn(300);
        
        uut.notifyCustomer(notification);
    }
    
    
*/


   private void validateEmail(Request request, String toName, String toEmail, String message) throws JsonParseException, JsonMappingException, IOException {
      String body = request.getBody();
      ObjectMapper objectMapper = new ObjectMapper();
      Mail curMail = objectMapper.readValue(body, Mail.class);

      boolean foundHtmlContent = false;
      boolean foundTextContent = false;
      for (Content curContent : curMail.getContent()) {
         if (curContent.getType().equals("text/html")) {
            assertEquals(expectedEmailBody, curContent.getValue());
            foundHtmlContent = true;
         } else if (curContent.getType().equals("text/plain")) {
            assertEquals(expectedEmailBody, curContent.getValue());
            foundTextContent = true;
         }
      }
      assertEquals(true, foundHtmlContent);
      assertEquals(true, foundTextContent);
      List<Personalization> otherData = curMail.getPersonalization();
      assertEquals(1, otherData.size());
      Email to = otherData.get(0).getTos().get(0);
      assertEquals(toEmail, to.getEmail());
      assertEquals(toName, to.getName());
   }


}

