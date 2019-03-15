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

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.notification.dispatch.DispatchException;
import com.iris.notification.dispatch.DispatchUnsupportedByUserException;
import com.iris.notification.message.NotificationBuilder;
import com.iris.notification.message.NotificationMessageRenderer;
import com.iris.notification.provider.twilio.TwilioSender;
import com.iris.notification.upstream.UpstreamNotificationResponder;
import com.iris.platform.notification.Notification;

@RunWith(MockitoJUnitRunner.class)
public class IVRProviderTest {

    protected Notification notification;

    @Mock
    protected PersonDAO personDao;

    @Mock
    protected Person person;

    @Mock
    protected NotificationMessageRenderer messageRenderer;

    @Mock
    protected TwilioSender twilioSender;

    @Mock
    protected UpstreamNotificationResponder responder;
    
    @Mock private PlaceDAO placeDao;   
    @Mock private Place place;

    @InjectMocks
    protected IVRProvider ivr;

    @Before
    public void initializeIVRMock() throws Exception {

       notification = new NotificationBuilder()
          .withPersonId(UUID.randomUUID())
          .withPlaceId(UUID.randomUUID())
          .build();

       Mockito.when(personDao.findById(notification.getPersonId())).thenReturn(person);
       Mockito.when(placeDao.findById(notification.getPlaceId())).thenReturn(place);
       Mockito.when(person.getMobileNumber()).thenReturn("5555555555");
    }

    @Test
    public void shouldTriggerCall() throws DispatchException, DispatchUnsupportedByUserException {
       ivr.notifyCustomer(notification);
       Mockito.verify(twilioSender).sendIVR(notification, person);
    }


}

