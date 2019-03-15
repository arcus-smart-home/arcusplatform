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
package com.iris.platform.pairing;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.model.ChildId;
import com.iris.messages.model.CompositeId;
import com.iris.platform.model.AbstractModelEntity;
import com.iris.protocol.zwave.ZWaveProtocol;

public class PairingDevice extends AbstractModelEntity<PairingDevice> {
   private static final Set<String> capabilities = ImmutableSet.of(Capability.NAMESPACE, PairingDeviceCapability.NAMESPACE);

   private UUID placeId;
   private int contextId = -1;
   private String population;
   private DeviceProtocolAddress protocolAddress;
   
   public PairingDevice() {
      super();
   }

   /**
    * Creates a new PairingDevice with an initial set of
    * attributes.  This is useful for restoring a PairingDevice
    * without marking fields as dirty.
    * @param attributes
    */
   public PairingDevice(Map<String, Object> attributes) {
      super(attributes);
   }
   
   public PairingDevice(PairingDevice copy) {
      super(copy);
      this.placeId = copy.placeId;
      this.population = copy.population;
      this.contextId = copy.contextId;
      this.protocolAddress = copy.protocolAddress;
   }

   @Override
   public PairingDevice copy() {
      return new PairingDevice(this);
   }

   @Override
   public String getType() {
      return PairingDeviceCapability.NAMESPACE;
   }

   @Override
   public Set<String> getCapabilities() {
      return capabilities;
   }

   @Override
   public void update(Map<String, Object> attributes) {
      for(Map.Entry<String, Object> attribute: attributes.entrySet()) {
         setAttribute(attribute.getKey(), attribute.getValue());
      }
   }

   @Override
   public Address getAddress() {
      return placeId != null && contextId > -1 ? Address.platformService(getPlaceId(), getType(), getContextId()) : null;
   }

   @Override
   public String getId() {
      return placeId != null && contextId > -1 ? new ChildId(placeId, contextId).getRepresentation() : null;
   }

   @Override
   public void setId(String id) {
      if(id == null) {
         this.placeId = null;
         this.contextId = -1;
         this.population = null;
      }
      else {
         CompositeId<UUID, Integer> compositeId = ChildId.fromString(id);
         setId(compositeId.getPrimaryId(), compositeId.getSecondaryId());
      }
      
   }
   
   public void setId(UUID placeId, Integer sequenceId) {
      this.placeId = placeId;
      this.contextId = sequenceId != null ? sequenceId : -1;
   }

   public UUID getPlaceId() {
      return placeId;
   }

   public void setPlaceId(UUID placeId) {
      this.placeId = placeId;
      this.contextId = -1;
   }
   
   public int getContextId() {
      return contextId;
   }

   public DeviceProtocolAddress getProtocolAddress() {
      return protocolAddress;
   }

   public void setProtocolAddress(DeviceProtocolAddress deviceAddress) {
      this.protocolAddress = deviceAddress;
   }
   
   public boolean isHubDevice() {
      return this.protocolAddress != null && this.protocolAddress.isHubAddress();
   }
   
   @Nullable
   public String getHubId() {
      return this.protocolAddress != null ? this.protocolAddress.getHubId() : null;
   }
   
   public String getRemoveMode() {
   	if( !isHubDevice() ) {
			return PairingDeviceCapability.REMOVEMODE_CLOUD;
		}else if(getProtocolAddress().getProtocolName().equals(ZWaveProtocol.NAMESPACE)) {
			return PairingDeviceCapability.REMOVEMODE_HUB_MANUAL;
		}else{
			return PairingDeviceCapability.REMOVEMODE_HUB_AUTOMATIC;
		}
   }
      

   @Override
	public Map<String, Object> toMap() {
		Map<String, Object> returnMap = super.toMap();
		//Add read only attributes
		returnMap.put(PairingDeviceCapability.ATTR_REMOVEMODE, getRemoveMode());
		returnMap.put(PairingDeviceCapability.ATTR_PROTOCOLADDRESS, getProtocolAddress());		
		return returnMap;
	}

	@Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
      result = prime * result + ((protocolAddress == null) ? 0 : protocolAddress.hashCode());
      result = prime * result + contextId;
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (!super.equals(obj))
         return false;
      if (getClass() != obj.getClass())
         return false;
      PairingDevice other = (PairingDevice) obj;
      if (placeId == null) {
         if (other.placeId != null)
            return false;
      } else if (!placeId.equals(other.placeId))
         return false;
      if (protocolAddress == null) {
         if (other.protocolAddress != null)
            return false;
      } else if (!protocolAddress.equals(other.protocolAddress))
         return false;
      if (contextId != other.contextId)
         return false;
      return true;
   }

	public String getPopulation() {
		return population;
	}

	public void setPopulation(String population) {
		this.population = population;
	}

}

