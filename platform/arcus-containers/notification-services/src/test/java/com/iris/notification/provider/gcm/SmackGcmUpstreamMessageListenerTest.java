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
package com.iris.notification.provider.gcm;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.iris.notification.upstream.UpstreamNotificationResponder;
import com.iris.platform.notification.NotificationMethod;


@RunWith(MockitoJUnitRunner.class)
public class SmackGcmUpstreamMessageListenerTest
{
   @Mock
   private UpstreamNotificationResponder errorResponder;
   
   @Mock
   private Map<String, Object> jsonObject;
   
   @InjectMocks
   private SmackGcmUpstreamMessageListener smackGcmUpstreamMessageListener;

   private String notificationToken;
   
   @Before
   public void init() {
      notificationToken = Byte.valueOf(Byte.MAX_VALUE).toString();
   }

   @Test
   public void shouldHandleInvalidTokenError() throws Exception {
      
      Mockito.when(jsonObject.get("message_id")).thenReturn((String)"{\"customMessage\":\"This is a Test\"}");
      Mockito.when(jsonObject.get("error")).thenReturn((String)"DEVICE_UNREGISTERED");
      Mockito.when(jsonObject.get("from")).thenReturn(notificationToken);
      
      smackGcmUpstreamMessageListener.handleNackReceipt(jsonObject);
      Mockito.verify(errorResponder).handleDeviceUnregistered(NotificationMethod.GCM, notificationToken);
   }
}

