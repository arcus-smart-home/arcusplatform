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

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.base.Preconditions;
import com.iris.core.dao.CRUDDao;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.messages.address.Address;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.model.ChildId;
import com.iris.messages.model.CompositeId;

public interface PairingDeviceDao extends CRUDDao<String, PairingDevice> {

   @Nullable
   @Override
   default PairingDevice findById(String id) {
      CompositeId<UUID, Integer> cid = ChildId.fromString(id);
      return findById(cid.getPrimaryId(), cid.getSecondaryId());
   }

   @Nullable
   default PairingDevice findByAddress(Address address) {
      PlatformServiceAddress psa = validate(address);
      return findById((UUID) psa.getId(), psa.getContextQualifier());
   }
   
   @Nullable
   PairingDevice findById(UUID placeId, int sequenceId);
   
   /**
    * Gets the PairingDevice associated with the address.
    * @param address
    * @return
    */
   @Nullable
   PairingDevice findByProtocolAddress(UUID placeId, Address protocolAddress);
   
   /**
    * Gets all the PairingDevices associated with the given
    * place.
    * @param placeId
    * @return
    */
   List<PairingDevice> listByPlace(UUID placeId);
   
   /**
    * Gets all the PairingDevices associated with the given
    * hub at the given place.
    * @param hubId
    * @return
    */
   default List<PairingDevice> listByHubId(UUID placeId, String hubId) {
      try(Context ctx = Metrics.listByHubIdTimer.time()) {
         return
            listByPlace(placeId)
               .stream()
               .filter((device) -> Objects.equals(hubId, device.getHubId()))
               .collect(Collectors.toList());
      }
   }
   
   /**
    * Deletes all the PairingDevices associated with the place.
    * @param placeId
    */
   void deleteByPlace(UUID placeId);
   
   public static class Metrics {
      public static final Timer listByPlaceTimer = DaoMetrics.readTimer(PairingDeviceDao.class, "listByPlace");
      public static final Timer listByHubIdTimer = DaoMetrics.readTimer(PairingDeviceDao.class, "listByHubId");
      public static final Timer findByProtocolAddressTimer = DaoMetrics.readTimer(PairingDeviceDao.class, "findByAddress");
      public static final Timer insertTimer = DaoMetrics.insertTimer(PairingDeviceDao.class, "save");
      public static final Timer updateTimer = DaoMetrics.updateTimer(PairingDeviceDao.class, "save");
      public static final Timer deleteByPlaceTimer = DaoMetrics.deleteTimer(PairingDeviceDao.class, "deleteByPlace");
      public static final Timer deleteTimer = DaoMetrics.deleteTimer(PairingDeviceDao.class, "delete");
   }
   
   public static PlatformServiceAddress validate(Address address) {
      Preconditions.checkArgument(address instanceof PlatformServiceAddress, "Not a platform address");
      Preconditions.checkArgument(PairingDeviceCapability.NAMESPACE.equals(address.getGroup()), "Not a pairing device address");
      return (PlatformServiceAddress) address;
   }
}

