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
package com.iris.platform.notification.provider.ivr;

public class TwilioHelper {
	
	public final static String ANSWERED_BY_MACHINE = "machine";
   public final static String CALL_STATUS_COMPLETED = "completed";
   public final static String CALL_STATUS_FAILED = "failed";
   public final static String CALL_STATUS_BUSY = "busy";
   public final static String CALL_STATUS_NOANSWER = "no-answer";

   public final static String CALL_STATUS_PARAM_KEY = "CallStatus";
   public final static String ANSWEREDBY_PARAM_KEY = "AnsweredBy";
   public final static String SIGNATURE_HEADER_KEY = "X-Twilio-Signature";
   public final static String HOST_HEADER_KEY = "Host";

   public final static String PERSON_ID_PARAM_NAME = "personId";
   public final static String PLACE_ID_PARAM_NAME = "placeId";
   public final static String NOTIFICATION_ID_PARAM_NAME = "notificationId";
   public final static String NOTIFICATION_EVENT_TIME_PARAM_NAME = "notificationTimestamp";

   public final static String PROTOCOL_HTTPS = "https://";
   
}

