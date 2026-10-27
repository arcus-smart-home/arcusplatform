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
package com.iris.platform.alarm.notification.strategy;

import java.util.List;
import java.util.UUID;

import com.iris.common.alarm.AlertType;
import com.iris.messages.address.Address;
import com.iris.platform.alarm.incident.Trigger;

public interface NotificationStrategy {
   void execute(Address incidentAddress, UUID placeId, List<Trigger> triggers);
   boolean cancel(Address incidentAddress, UUID placeId, Address cancelledBy, List<String> alarms);
   void acknowledge(Address incidentAddress, AlertType type);
}

