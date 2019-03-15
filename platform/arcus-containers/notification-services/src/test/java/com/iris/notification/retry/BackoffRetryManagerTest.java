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
package com.iris.notification.retry;

import static org.junit.Assert.*;

import java.time.Instant;

import org.junit.Test;
import org.mockito.Mockito;

import com.iris.notification.message.NotificationBuilder;
import com.iris.notification.retry.BackoffRetryManager;
import com.iris.platform.notification.Notification;


public class BackoffRetryManagerTest {

	private BackoffRetryManager uut = new BackoffRetryManager();

	@Test
	public void shouldExpireTimedOutNotification () {
		Notification n = Mockito.mock(Notification.class);
		Mockito.when(n.getExpirationTime()).thenReturn(Instant.MIN);
		
		assertTrue(uut.hasExpired(n));
	}

	@Test
	public void shouldExpireTooManyRetires () {
		Notification n = Mockito.mock(Notification.class);
		Mockito.when(n.getExpirationTime()).thenReturn(Instant.MAX);
		Mockito.when(n.getDeliveryAttempts()).thenReturn(Integer.MAX_VALUE);
		
		assertTrue(uut.hasExpired(n));
	}

	@Test
	public void shouldntExpireValidNotification () {
		Notification n = Mockito.mock(Notification.class);
		Mockito.when(n.getExpirationTime()).thenReturn(Instant.MAX);
		Mockito.when(n.getDeliveryAttempts()).thenReturn(0);
		
		assertFalse(uut.hasExpired(n));		
	}
	
	@Test
	public void shouldReturnExponentialBackoffTimes () {
		Notification notification = new NotificationBuilder().build();
		
		for (int i = 0; i < 20; i++) {
			int actualDelay = uut.secondsBeforeRetry(notification);
			int calculatedDelay = (int) Math.ceil((Math.pow(2, notification.getDeliveryAttempts()) - 1) / 2);
			int expectedDelay = calculatedDelay > BackoffRetryManager.MAX_BACKOFF_DELAY ? BackoffRetryManager.MAX_BACKOFF_DELAY : calculatedDelay;
			
			assertEquals(expectedDelay, actualDelay);
		}	
	}
}

