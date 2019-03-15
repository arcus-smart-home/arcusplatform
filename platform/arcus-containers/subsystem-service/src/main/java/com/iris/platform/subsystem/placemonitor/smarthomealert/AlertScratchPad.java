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
package com.iris.platform.subsystem.placemonitor.smarthomealert;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceDriverAddress;
import com.iris.messages.address.HubAddress;
import com.iris.messages.address.HubServiceAddress;
import com.iris.messages.model.Copyable;
import com.iris.messages.type.SmartHomeAlert;

// the context/scratch pad for generated alerts, these are then post processed to present the appropriate view to the
// user interfaces
public class AlertScratchPad implements Copyable<AlertScratchPad> {

   private Map<String, SmartHomeAlert> alerts = new HashMap<>();

   public void putAlert(SmartHomeAlert alert) {
      alerts.putIfAbsent(alert.getAlertkey(), alert);
   }

   public void removeAlert(String key) {
      alerts.remove(key);
   }

   public void deleteAlertsFor(UUID placeId, Address address) {
      if(address instanceof HubServiceAddress || address instanceof HubAddress) {
         removeAlert(AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_HUB_OFFLINE, placeId));
         removeAlert(AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_4G_MODEM_NEEDED, placeId));
         removeAlert(AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_ERROR, placeId));
         removeAlert(AlertKeys.key(SmartHomeAlert.ALERTTYPE_PLACE_4G_SERVICE_SUSPENDED, placeId));
      } else if(address instanceof DeviceDriverAddress) {
         alerts = alerts.entrySet().stream()
            .filter(e -> !Objects.equals(address.getRepresentation(), e.getValue().getSubjectaddr()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      }
   }

   public Collection<SmartHomeAlert> alerts() {
      return alerts(alert -> true);
   }

   public Collection<SmartHomeAlert> alerts(Predicate<SmartHomeAlert> filter) {
      return alerts.values().stream().filter(filter).collect(Collectors.toList());
   }

   public boolean hasAlert(String alertKey) {
      return alerts.containsKey(alertKey);
   }

   public SmartHomeAlert getAlert(String alertKey) {
      return alerts.get(alertKey);
   }

   @Override
   public AlertScratchPad copy() {
      try {
         Map<String, SmartHomeAlert> alertsCopy = new HashMap<>(alerts.size());
         alerts.values().forEach(alert -> alertsCopy.put(alert.getAlertkey(), new SmartHomeAlert(alert.toMap())));
         AlertScratchPad copy = (AlertScratchPad) clone();
         copy.alerts = alertsCopy;
         return copy;
      } catch(CloneNotSupportedException cnse) {
         throw new RuntimeException(cnse);
      }
   }
}

