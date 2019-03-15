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
package com.iris.platform.scheduler.cassandra;

public class ScheduledEventTable {

   public static final String NAME = "scheduled_event";

   public static final class Columns {
      public static final String PARTITION_ID = "partitionId";
      public static final String TIME_BUCKET = "timeBucket";
      public static final String SCHEDULED_TIME = "scheduledTime";
      public static final String PLACE_ID = "placeId";
      public static final String SCHEDULER = "scheduler";
      public static final String EXPIRES_AT = "expiresAt";
   }
}

