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
package com.iris.agent.zwave;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.hal.Model;
import com.iris.agent.zwave.events.ZWEvent;
import com.iris.agent.zwave.events.ZWEventDispatcher;
import com.iris.agent.zwave.events.ZWEventListener;
import com.iris.agent.zwave.events.ZWHomeIdChangedEvent;
import com.iris.agent.zwave.events.ZWProtocolVersionEvent;
import com.iris.messages.capability.HubZwaveCapability;
import com.iris.protoc.runtime.ProtocUtil;

public class ZWAttributes implements ZWEventListener {
   private static long lastResetTimestamp = Long.MIN_VALUE;
   
   private static final HubAttributesService.Attribute<String> state = HubAttributesService.ephemeral(String.class, HubZwaveCapability.ATTR_STATE, HubZwaveCapability.STATE_INIT);
   private static final HubAttributesService.Attribute<Integer> numdevs = HubAttributesService.persisted(Integer.class, HubZwaveCapability.ATTR_NUMDEVICES, 0);
   private static final HubAttributesService.Attribute<String> hardware = HubAttributesService.persisted(String.class, HubZwaveCapability.ATTR_HARDWARE, null);
   private static final HubAttributesService.Attribute<String> firmware = HubAttributesService.persisted(String.class, HubZwaveCapability.ATTR_FIRMWARE, null);
   private static final HubAttributesService.Attribute<String> protocol = HubAttributesService.persisted(String.class, HubZwaveCapability.ATTR_PROTOCOL, null);
   private static final HubAttributesService.Attribute<String> zwhomeid = HubAttributesService.persisted(String.class, HubZwaveCapability.ATTR_HOMEID, null);

   private static final HubAttributesService.Attribute<Boolean> issuc = HubAttributesService.persisted(Boolean.class, HubZwaveCapability.ATTR_ISSUC, false);
   private static final HubAttributesService.Attribute<Boolean> issecondary = HubAttributesService.persisted(Boolean.class, HubZwaveCapability.ATTR_ISSECONDARY, false);
   private static final HubAttributesService.Attribute<Boolean> isonother = HubAttributesService.persisted(Boolean.class, HubZwaveCapability.ATTR_ISONOTHERNETWORK, false);

   // Uptime is calculated by a supplier whenever it is accessed.
   private static final HubAttributesService.Attribute<Long> uptime = HubAttributesService.computed(Long.class, HubZwaveCapability.ATTR_UPTIME, new Supplier<Long>() {
      @Override
      public Long get() {
         if (lastResetTimestamp == Long.MIN_VALUE) {
            return 0L;
         }

         return TimeUnit.MILLISECONDS.convert(System.nanoTime() - lastResetTimestamp, TimeUnit.NANOSECONDS);
      }
   });
   
   /**
    * Constructs an instance of ZWAttributes.
    * 
    * This will register the instance as a listener to controller internal events, poke the uptime
    * attribute so it gets an initial setting, and set any attributes that are not
    * calculated from events.
    */
   public ZWAttributes() {
      ZWEventDispatcher.INSTANCE.register(this);
      uptime.poke();
      setChipVersion();
      
      //TODO: Not sure how to get these yet so will set typical case.
      issuc.set(true);
      issecondary.set(false);
      isonother.set(false);
   }
   
   /**
    * Processes events and sets attributes accordingly.
    */
   @Override
   public void onZWEvent(ZWEvent event) {
      switch(event.getType()) {
      case NODE_ADDED:
         updateNumDevices();
         break;
      case NODE_REMOVED:
         updateNumDevices();
         break;
      case BOOTSTRAPPED:
         state.set(HubZwaveCapability.STATE_NORMAL);
         updateNumDevices();
         break;
      case START_PAIRING:
         state.set(HubZwaveCapability.STATE_PAIRING);
         break;
      case START_UNPAIRING:
         state.set(HubZwaveCapability.STATE_UNPAIRING);
         break;
      case STOP_PAIRING:
         state.set(HubZwaveCapability.STATE_NORMAL);
         break;
      case STOP_UNPAIRING:
         state.set(HubZwaveCapability.STATE_NORMAL);
         break;
      case HOME_ID_CHANGED:
         zwhomeid.set(ProtocUtil.toHexString(((ZWHomeIdChangedEvent)event).getHomeId()));
         break;
      case PROTOCOL_VERSION:
         ZWProtocolVersionEvent proEvent = (ZWProtocolVersionEvent)event;
         // These appear to be set to the same thing in our current implementation
         firmware.set(String.format("%d.%d", proEvent.getVersion(), proEvent.getSubversion()));
         protocol.set(String.format("%d.%d", proEvent.getVersion(), proEvent.getSubversion()));
         break;
      default:
         break;
         
      }
   }
   
   /**
    * Since there isn't an obvious way to get the chip version, the
    * hub model is used to determine which chip is being used.
    */
   private static void setChipVersion() {
      //TODO: Get chip version directly if possible.
      String model = IrisHal.getModel();
      if (Model.isV2(model)) {
         hardware.set("ZM5304AU-CME3R");
      }
      else if (Model.isV3(model)) {
         hardware.set("ZM5101");
      }
      else {
         hardware.set("unknown");
      }
   }
   
   /**
    * Updates the number of devices by asking the ZWNetwork instance how many
    * devices it has mapped. ZWNetwork takes care of subtracting the hub from
    * the count.
    */
   private void updateNumDevices() {
      int devices = ZWServices.INSTANCE.getNetwork().getNumDevices();
      numdevs.set(devices);
   }
}
