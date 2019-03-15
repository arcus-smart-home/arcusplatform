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
package com.iris.client.message;

public interface ClientErrorCodes {
	public static interface HubRegistration {
		public static final String MISSING_ARGUMENT = "error.register.missingargument";
		public static final String ALREADY_REGISTERED = "error.register.alreadyregistered";
		public static final String NOT_FOUND = "error.register.hubnotfound";
		public static final String INCORRECT_ACCOUNT = "error.hub.incorrectaccount";
	}
}

