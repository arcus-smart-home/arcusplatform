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
package com.iris.core.protocol.ipcd;

import java.util.UUID;
import java.util.stream.Stream;

import com.iris.core.protocol.ipcd.exceptions.IpcdDaoException;
import com.iris.messages.address.Address;
import com.iris.platform.partition.PlatformPartition;
import com.iris.protocol.ipcd.IpcdDevice;
import com.iris.protocol.ipcd.message.model.Device;

public interface IpcdDeviceDao {
   IpcdDevice findByProtocolAddress(String address);

   IpcdDevice findByProtocolAddress(Address address);

   IpcdDevice save(IpcdDevice ipcdDevice);

   void delete(IpcdDevice ipcdDevice);

   /**
    * Returns a stream of IpcdDevice objects, the entries in this stream may be loaded
    * lazily as the stream is processed.
    * @param partitionId
    * @return
    */
   Stream<IpcdDevice> streamByPartitionId(int partitionId);

   default Stream<IpcdDevice> streamByPartition(PlatformPartition partition) { return streamByPartitionId(partition.getId()); }

   String claimAndGetProtocolAddress(Device d, UUID accountId, UUID placeId) throws IpcdDaoException;
   void completeRegistration(String protocolAddress, UUID placeId, String driverAddress) throws IpcdDaoException;
   void clearRegistration(String protocolAddress, UUID placeId) throws IpcdDaoException;
   void forceRegistration(String protocolAddress, UUID accountId, UUID placeId, String driverAddress);
   void delete(String protocolAddress, UUID placeId);
   void offline(String protocolAddress);
}

