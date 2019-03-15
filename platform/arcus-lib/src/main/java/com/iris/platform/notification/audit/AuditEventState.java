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
package com.iris.platform.notification.audit;

public enum AuditEventState {

	SENT, 			// Notification message received and sent to dispatcher; transition to FAILED, ACCEPTED or RETRY
	FAILED, 		// Message has timed out or exceeded allowable retries; terminal state
	ACCEPTED, 		// Provider gateway accepted message; terminal state unless provider support delivery receipt
	RETRY, 			// Provider gateway failed to accept or deliver message; transition to RETRY, ACCEPTED or FAILED
	DELIVERED, 		// Provider indicates that message was delivered to receipient
	ERROR;			// System error: notification could not be processed (invalid message, unknown provider, etc.); terminal state
}

