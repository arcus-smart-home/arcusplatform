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
/**
 *
 */
package com.iris.core.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.iris.core.driver.DeviceDriverStateHolder;
import com.iris.device.attributes.AttributeKey;
import com.iris.messages.model.Device;
import com.iris.platform.model.ModelEntity;

/**
 *
 */
public interface DeviceDAO extends CRUDDao<UUID, Device> {

   /**
    * Loads the given device by its hubid
    *
    * @param hubId
    * @return
    */
   public default List<Device> findByHubId(String hubId) { return findByHubId(hubId, false); }
   
   public List<Device> findByHubId(String hubId, boolean includeTombstoned);

	public Device findByProtocolAddress(String protocolAddress);
	
	/**
    * Loads the devices transformed into a capability set.  Unlike loadAttributes,
    * this will merge the static device keys and the attributes.
	 * @param accountId
	 * @return
	 */
	public default List<Map<String, Object>> listDeviceAttributesByAccountId(UUID accountId) { return listDeviceAttributesByAccountId(accountId, false); }
	
	public List<Map<String, Object>> listDeviceAttributesByAccountId(UUID accountId, boolean includeTombstoned);

	/**
	 * Loads the devices transformed into a capability set.  Unlike loadAttributes,
	 * this will merge the static device keys and the attributes.
	 * @param placeId
	 * @return
	 */
	public default List<Map<String, Object>> listDeviceAttributesByPlaceId(UUID placeId) { return listDeviceAttributesByPlaceId(placeId, false); }
	
	public List<Map<String, Object>> listDeviceAttributesByPlaceId(UUID placeId, boolean includeTombstoned);
	
	public default List<Device> listDevicesByPlaceId(UUID placeId) { return listDevicesByPlaceId(placeId, false); };

   public List<Device> listDevicesByPlaceId(UUID placeId, boolean includeTombstoned);

   public default Stream<ModelEntity> streamDeviceModelByPlaceId(UUID placeId) { return streamDeviceModelByPlaceId(placeId, false); }
   
   public Stream<ModelEntity> streamDeviceModelByPlaceId(UUID placeId, boolean includeTombstoned);
   public ModelEntity modelById(UUID id);
   
	public DeviceDriverStateHolder loadDriverState(Device device);
	public void replaceDriverState(Device device, DeviceDriverStateHolder state);
	public void updateDriverState(Device device, DeviceDriverStateHolder state);

	@SuppressWarnings("rawtypes")
   public void removeAttributes(Device device, Collection<AttributeKey> attributeKeys);
}

