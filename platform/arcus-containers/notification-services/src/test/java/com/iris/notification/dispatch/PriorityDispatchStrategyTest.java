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
package com.iris.notification.dispatch;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.codahale.metrics.Counter;
import com.iris.metrics.IrisMetrics;
import com.iris.notification.NotificationService;
import com.iris.notification.message.NotificationBuilder;
import com.iris.notification.provider.AbstractPushNotificationProvider;
import com.iris.notification.provider.ApnsProvider;
import com.iris.notification.provider.NoSuchProviderException;
import com.iris.notification.provider.NotificationProvider;
import com.iris.notification.provider.NotificationProviderRegistry;
import com.iris.notification.retry.RetryManager;
import com.iris.notification.retry.RetryProcessor;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;
import com.iris.platform.notification.NotificationPriority;
import com.iris.platform.notification.audit.NotificationAuditor;

@RunWith(MockitoJUnitRunner.class)
public class PriorityDispatchStrategyTest {

    @Mock
    private NotificationAuditor audit;

    @Mock
    private NotificationProviderRegistry providerRegistry;

    @Mock
    private RetryManager retryManager;

    @Mock
    private RetryProcessor retryProcessor;

    @InjectMocks
    private PriorityDispatchStrategy uut;

    @Test
    public void shouldSetInitialDispatchMethodForLowPriority() throws Exception {
        Notification notification = new NotificationBuilder().withMethod(null).withPriority(NotificationPriority.LOW).build();
        uut.dispatch(notification);

        assertEquals(NotificationMethod.EMAIL, notification.getMethod());
    }

    @Test
    public void shouldSetInitialDispatchMethodForMediumPriority() throws Exception {
        Notification notification = new NotificationBuilder().withMethod(null).withPriority(NotificationPriority.MEDIUM).build();
        uut.dispatch(notification);

        assertEquals(NotificationMethod.PUSH, notification.getMethod());
    }

    @Test
    public void shouldSetInitialDispatchMethodForHighPriority() throws Exception {
        Notification notification = new NotificationBuilder().withMethod(null).withPriority(NotificationPriority.HIGH).build();
        uut.dispatch(notification);

        assertEquals(NotificationMethod.IVR, notification.getMethod());
    }

    @Test
    public void shouldDispatchToGcmAndApnsForPush() throws Exception {
        AbstractPushNotificationProvider apnsProvider = Mockito.mock(ApnsProvider.class);
        NotificationProvider gcmProvider = Mockito.mock(NotificationProvider.class);
        Mockito.when(apnsProvider.supportedByUser(Mockito.any(Notification.class))).thenReturn(true);

        Mockito.when(providerRegistry.getInstanceForProvider(NotificationMethod.APNS)).thenReturn((NotificationProvider)apnsProvider);
        Mockito.when(providerRegistry.getInstanceForProvider(NotificationMethod.GCM)).thenReturn(gcmProvider);

        Notification notification = new NotificationBuilder().withMethod(null).withPriority(NotificationPriority.MEDIUM).build();
        uut.dispatch(notification);

        Mockito.verify(retryProcessor).split(notification, NotificationMethod.GCM, null);
        Mockito.verify(retryProcessor).split(notification, NotificationMethod.APNS, null);
    }
 
    @Test
    public void shouldIncrementCounterForNoSuchProvider() throws Exception {
        long count = getCounter("notifications.dispatch.ivr.nosuchproviderexception");

        Mockito.when(providerRegistry.getInstanceForProvider(NotificationMethod.IVR)).thenThrow(new NoSuchProviderException("expected-message"));
        Notification notification = new NotificationBuilder().withMethod(null).withPriority(NotificationPriority.HIGH).build();
        uut.dispatch(notification);

        assertEquals(count + 1, getCounter("notifications.dispatch.ivr.nosuchproviderexception"));
    }

    @Test
    public void shouldIncrementCounterForDispatchException() throws Exception {
        long count = getCounter("notifications.dispatch.ivr.dispatchexception");

        Notification notification = new NotificationBuilder().withMethod(null).withPriority(NotificationPriority.HIGH).build();
        NotificationProvider mockProvider = Mockito.mock(NotificationProvider.class);
        Mockito.when(providerRegistry.getInstanceForProvider(NotificationMethod.IVR)).thenReturn(mockProvider);
        Mockito.doThrow(new DispatchException("message")).when(mockProvider).notifyCustomer(notification);
        uut.dispatch(notification);

        assertEquals(count + 1, getCounter("notifications.dispatch.ivr.dispatchexception"));
    }

    @Test
    public void shouldIncrementCounterForDispatchUnsupportedByUserException() throws Exception {
        long count = getCounter("notifications.dispatch.ivr.dispatchunsupportedbyuserexception");

        Notification notification = new NotificationBuilder().withMethod(null).withPriority(NotificationPriority.HIGH).build();
        NotificationProvider mockProvider = Mockito.mock(NotificationProvider.class);
        Mockito.when(providerRegistry.getInstanceForProvider(NotificationMethod.IVR)).thenReturn(mockProvider);
        Mockito.doThrow(new DispatchUnsupportedByUserException("message")).when(mockProvider).notifyCustomer(notification);
        uut.dispatch(notification);

        assertEquals(count + 1, getCounter("notifications.dispatch.ivr.dispatchunsupportedbyuserexception"));
    }

    private long getCounter(String counterName) {
        if (IrisMetrics.metrics(NotificationService.SERVICE_NAME).getMetrics().get(counterName) == null) {
            return 0;
        }

        return ((Counter) IrisMetrics.metrics(NotificationService.SERVICE_NAME).getMetrics().get(counterName)).getCount();
    }
}

