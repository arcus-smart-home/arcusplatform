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

import java.util.UUID;

public interface PlatformDestination {
	public static final String ACCOUNT_DESTINATION = "SERV:accounts:";
	public static final String PAIRING_DESTINATION = "SERV:_HUBID_:hub";
	public static final String DEVICES_DESTINATION = "SERV:devices:";
   public static final String STATUS_DESTINATION = "SERV:status:";
	public static final String ADD_HUB_DESTINATION = "SERV:hub:";

	public interface StaticUUIDs {
		public static final UUID ACCOUNT_ID = UUID.fromString("c24b0e18-3394-4f81-b762-274ba3605ccc");
		public static final UUID PLACE_ID = UUID.fromString("6f293fd5-b65b-4a63-a5ed-1309eda1122a");
	}
}

