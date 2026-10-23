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
package com.iris.platform.alarm.service;

import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.type.IncidentTrigger;
import com.iris.platform.alarm.incident.Trigger;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class AlertUtil {

   private AlertUtil() {
   }

   static void validateTriggers(String param, List<Map<String, Object>> triggers) {
      if(triggers == null) {
         throw new ErrorEventException(Errors.missingParam(param));
      }
      if(triggers.isEmpty()) {
         throw new ErrorEventException(Errors.invalidParam(param));
      }
   }

   static void validateAlertState(String param, String alertState) {
      if(StringUtils.isBlank(alertState)) {
         throw new ErrorEventException(Errors.missingParam(param));
      }
   }

   static List<Trigger> convertTriggers(List<Map<String, Object>> triggers) {
      return triggers.stream().map((t) -> Trigger.builder(new IncidentTrigger(t)).build()).collect(Collectors.toList());
   }
}

