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

import java.time.Instant;

import com.google.inject.Singleton;
import com.iris.platform.notification.Notification;

@Singleton
public class BackoffRetryManager implements RetryManager {

	public static final int MAX_BACKOFF_DELAY = 120;
	
	public boolean hasExpired (Notification notification) {
						
		// Has time to live elapsed?
		if (Instant.now().isAfter(notification.getExpirationTime())) {
			return true;
		}
		
		// Has number of retries exceeded limit?
		if (notification.getDeliveryAttempts() > maxAllowableRetriesForNotification(notification)) {
			return true;
		}
		
		return false;		
	}
	
	public int maxAllowableRetriesForNotification (Notification notification) {
		// TODO: Logic to determine max number of retries?
		return 10;
	}
	
	public int secondsBeforeRetry (Notification notification) {	
		int delay = (int) Math.ceil((Math.pow(2, notification.getDeliveryAttempts()) - 1) / 2);	
		return delay > MAX_BACKOFF_DELAY ? MAX_BACKOFF_DELAY : delay;
	}
}

