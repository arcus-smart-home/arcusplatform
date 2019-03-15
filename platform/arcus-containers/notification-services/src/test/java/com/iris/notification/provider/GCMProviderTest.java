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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.iris.core.dao.MobileDeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.io.json.JSON;
import com.iris.messages.model.MobileDevice;
import com.iris.messages.model.Person;
import com.iris.notification.message.NotificationMessageRenderer;
import com.iris.notification.provider.gcm.GcmSender;
import com.iris.notification.retry.RetryProcessor;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;

@RunWith(MockitoJUnitRunner.class)
public class GCMProviderTest {
    @Mock
    private NotificationMessageRenderer messageRenderer;

    @Mock
    private MobileDeviceDAO mobileDeviceDao;

    @Mock
    private PersonDAO personDao;

    @Mock
    private GcmSender sender;

    @Mock
    private Notification notification;

    @Mock
    private Person person;

    @Mock
    private RetryProcessor retryProcessor;

    @InjectMocks
    private GCMProvider gcmProvider;

    private UUID personId = UUID.randomUUID();
    private String iosToken1 = "<00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000>";
    private String iosToken2 = "<99999999 99999999 99999999 99999999 99999999 99999999 99999999 99999999>";

    private String renderedMessage = "{'title' : 'Is the Garage Door Open?', 'message' : 'button pressed', 'vibrate' : 1, 'sound' : 1}";

    @Before
    public void setup() {
        ArrayList<MobileDevice> mobileDevices = new ArrayList<MobileDevice>();

        MobileDevice ios1 = new MobileDevice();
        ios1.setOsType("ios");
        ios1.setNotificationToken(iosToken1);

        MobileDevice ios2 = new MobileDevice();
        ios2.setOsType("ios");
        ios2.setNotificationToken(iosToken2);

        MobileDevice android1 = new MobileDevice();
        android1.setOsType("android");
        android1.setNotificationToken("android1-token");

        MobileDevice android2 = new MobileDevice();
        android2.setOsType("android");
        android2.setNotificationToken("android2-token");

        mobileDevices.add(ios1);
        mobileDevices.add(ios2);
        mobileDevices.add(android1);
        mobileDevices.add(android2);

        Mockito.when(notification.getPersonId()).thenReturn(personId);
        Mockito.when(personDao.findById(personId)).thenReturn(person);
        Mockito.when(mobileDeviceDao.listForPerson(person)).thenReturn(mobileDevices);
        Mockito.when(messageRenderer.renderMessage(notification, NotificationMethod.GCM,null, null)).thenReturn(renderedMessage);
    }

    @Test
    public void testNotifyCustomer() throws Exception {
        Map<String, Object> expectedPayload = new HashMap<String, Object>();

        Object notificationData = JSON.fromJson(renderedMessage, Map.class);
        expectedPayload.put("data", notificationData);

        gcmProvider.notifyCustomer(notification);
        Mockito.verify(retryProcessor).split(notification, NotificationMethod.GCM, "android1-token");
        Mockito.verify(retryProcessor).split(notification, NotificationMethod.GCM, "android2-token");
    }

}

