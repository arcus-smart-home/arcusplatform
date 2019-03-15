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

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.iris.platform.notification.NotificationMethod;

@RunWith(MockitoJUnitRunner.class)
public class MapNotificationProviderRegistryTest {

    @Mock
    private Map<String, NotificationProvider> providers;

    @InjectMocks
    private MapNotificationProviderRegistry uut;

    @Test
    public void shouldReturnExistantProvider() throws Exception {

        NotificationProvider expectedProvider = Mockito.mock(NotificationProvider.class);
        Mockito.when(providers.get("APNS")).thenReturn(expectedProvider);

        assertEquals(expectedProvider, uut.getInstanceForProvider(NotificationMethod.APNS));
    }

    @Test(expected = NoSuchProviderException.class)
    public void shouldThrowExceptionForNonExistantProvider() throws Exception {
        uut.getInstanceForProvider(NotificationMethod.APNS);
    }

}

