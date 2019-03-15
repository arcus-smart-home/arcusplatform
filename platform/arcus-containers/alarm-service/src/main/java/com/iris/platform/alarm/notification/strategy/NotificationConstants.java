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

import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.service.AlarmService;

public interface NotificationConstants {

   Address ALARMSERVICE_ADDRESS = Address.platformService(AlarmService.NAMESPACE);
   Address NOTIFICATIONSERVICE_ADDRESS = Address.platformService(NotificationCapability.NAMESPACE);

   interface TriggerParams {
      String PARAM_DEVICE_NAME = "deviceName";
      String PARAM_DEVICE_TYPE = "deviceType";
      String PARAM_RULE_NAME = "_ruleName";
      String PARAM_ACTOR_FIRST_NAME = "actorFirstName";
      String PARAM_ACTOR_LAST_NAME = "actorLastName";
   }
   
   interface CancelParams {
	   String PARAM_CANCELL_BY_FIRSTNAME = "cancelByFirstName";
	   String PARAM_CANCELL_BY_LASTNAME = "cancelByLastName";
   }
   String KEY_TEMPLATE_FOR_CANCEL="alarm.%s.cancelled";
   String KEY_TEMPLATE_FOR_TRIGGER="alarm.%s.triggered";
   String KEY_TEMPLATE_FOR_TRIGGER_RULE="alarm.%s.triggered.rule";
   String KEY_TEMPLATE_FOR_CANCEL_PRO="alarm.%s.cancelled.pro";
   String KEY_TEMPLATE_FOR_TRIGGER_PRO="alarm.%s.triggered.pro";
   String KEY_TEMPLATE_FOR_TRIGGER_RULE_PRO="alarm.%s.triggered.rule.pro";
   String KEY_TEMPLATE_FOR_PRO_REFUSED="alarm.%s.dispatch.refused";
   String KEY_TEMPLATE_FOR_PRO_CONFIRMED_BY_APP="alarm.%s.confirmed.app";
   String KEY_TEMPLATE_FOR_PRO_DISPATCHED="alarm.%s.dispatched";
   String KEY_TEMPLATE_FOR_PRO_DISPATCH_CANCELLED = "alarm.%s.dispatch.cancel";
   
   String SECURITY_KEY = "alarm.security.triggered";
   String PANIC_KEY = String.format(KEY_TEMPLATE_FOR_TRIGGER, "panic");
   String CO_KEY = String.format(KEY_TEMPLATE_FOR_TRIGGER, "co");
   String SMOKE_KEY = String.format(KEY_TEMPLATE_FOR_TRIGGER, "smoke");
   String WATER_KEY = String.format(KEY_TEMPLATE_FOR_TRIGGER, "water");
}

