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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.codahale.metrics.Counter;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.notification.NotificationService;
import com.iris.notification.provider.MapNotificationProviderRegistry;
import com.iris.notification.provider.NoSuchProviderException;
import com.iris.notification.provider.NotificationProvider;
import com.iris.notification.retry.RetryManager;
import com.iris.notification.retry.RetryProcessor;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;
import com.iris.platform.notification.audit.AuditEventState;
import com.iris.platform.notification.audit.NotificationAuditor;

@RunWith(MockitoJUnitRunner.class)
public class MethodDispatchStrategyTest {

    private final IrisMetricSet METRICS = IrisMetrics.metrics(NotificationService.SERVICE_NAME);

    @Mock
    private NotificationAuditor audit;

    @Mock
    private MapNotificationProviderRegistry providerRegistry;

    @Mock
    private RetryManager retryManager;

    @Mock
    private RetryProcessor retryProcessor;

    @Mock
    private NotificationProvider provider;

    @Mock
    private Notification notification;

    @InjectMocks
    private MethodDispatchStrategy uut;

    @Before
    public void setup() {
        Mockito.when(notification.getMethod()).thenReturn(NotificationMethod.EMAIL);
    }

    @Test
    public void notificationShouldBeSent() throws Exception {

        Mockito.when(retryManager.hasExpired(notification)).thenReturn(false);
        Mockito.when(providerRegistry.getInstanceForProvider(Mockito.any())).thenReturn(provider);

        uut.dispatch(notification);
        Mockito.verify(provider).notifyCustomer(notification);
        Mockito.verify(audit).log(notification, AuditEventState.SENT);
    }

    @Test
    public void dispatchExceptionShouldRetry() throws Exception {
        Exception exception = new DispatchException("message");

        Mockito.when(retryManager.hasExpired(notification)).thenReturn(false);
        Mockito.when(providerRegistry.getInstanceForProvider(Mockito.any())).thenReturn(provider);
        Mockito.doThrow(exception).when(provider).notifyCustomer(notification);

        uut.dispatch(notification);
        Mockito.verify(retryProcessor).retry(notification, exception);
        assertEquals(1L, ((Counter) METRICS.getMetrics().get("notifications.dispatch.email.dispatchexception")).getCount());
    }

    @Test
    public void terminalDispatchExceptionShouldTransitionToError() throws Exception {
        Exception exception = new DispatchUnsupportedByUserException("message");

        Mockito.when(retryManager.hasExpired(notification)).thenReturn(false);
        Mockito.when(providerRegistry.getInstanceForProvider(Mockito.any())).thenReturn(provider);
        Mockito.doThrow(exception).when(provider).notifyCustomer(notification);

        uut.dispatch(notification);
        Mockito.verify(audit).log(notification, AuditEventState.ERROR, exception);
        assertEquals(1L, ((Counter) METRICS.getMetrics().get("notifications.dispatch.email.dispatchunsupportedbyuserexception")).getCount());
    }

    @Test
    public void runtimeExceptionShouldTransitionToFailed() throws Exception {
        RuntimeException exception = new RuntimeException("message");

        Mockito.when(retryManager.hasExpired(notification)).thenReturn(false);
        Mockito.when(providerRegistry.getInstanceForProvider(Mockito.any())).thenReturn(provider);
        Mockito.doThrow(exception).when(provider).notifyCustomer(notification);

        uut.dispatch(notification);
        Mockito.verify(audit).log(notification, AuditEventState.FAILED, exception);
        assertEquals(1L, ((Counter) METRICS.getMetrics().get("notifications.dispatch.email.runtimeexception")).getCount());
    }

    @Test
    public void noSuchProviderExceptionShouldTransitionToError() throws Exception {
        NoSuchProviderException exception = new NoSuchProviderException("message");

        Mockito.when(retryManager.hasExpired(notification)).thenReturn(false);
        Mockito.when(providerRegistry.getInstanceForProvider(Mockito.any())).thenThrow(exception);

        uut.dispatch(notification);
        Mockito.verify(audit).log(notification, AuditEventState.ERROR, exception);
        assertEquals(1L, ((Counter) METRICS.getMetrics().get("notifications.dispatch.email.nosuchproviderexception")).getCount());
    }
}

