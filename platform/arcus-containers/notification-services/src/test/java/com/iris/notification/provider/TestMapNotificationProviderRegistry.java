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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;

import com.iris.platform.notification.NotificationMethod;

public class TestMapNotificationProviderRegistry {

    private MapNotificationProviderRegistry uut = new MapNotificationProviderRegistry();
    private NotificationProvider logProvider = Mockito.mock(NotificationProvider.class);
    private NotificationProvider webhookProvider = Mockito.mock(NotificationProvider.class);

    @Before
    public void setup () throws NoSuchFieldException, SecurityException {
        Map<String, NotificationProvider> registryMap = new HashMap<String,NotificationProvider>();
        registryMap.put("LOG", logProvider);
        registryMap.put("WEBHOOK", webhookProvider);

        new FieldSetter(uut, uut.getClass().getDeclaredField("providerRegistry")).set(registryMap);
    }

    @Test
    public void shouldReturnInstanceForValidProvider() throws Exception {
        assertEquals(logProvider, uut.getInstanceForProvider(NotificationMethod.LOG));
        assertEquals(webhookProvider, uut.getInstanceForProvider(NotificationMethod.WEBHOOK));
    }

    @Test (expected=IllegalArgumentException.class)
    public void shouldThrowExceptionForNullProvider() throws NoSuchProviderException {
        uut.getInstanceForProvider(null);
    }

    @Test (expected=IllegalArgumentException.class)
    public void shouldThrowExceptionForEmptyProvider() throws NoSuchProviderException {
        uut.getInstanceForProvider(null);
    }

    @Test (expected=NoSuchProviderException.class)
    public void shouldThrowExceptionForUnknownProvider() throws NoSuchProviderException {
        uut.getInstanceForProvider(NotificationMethod.EMAIL);
    }
}

