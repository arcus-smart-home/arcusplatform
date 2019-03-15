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
package com.iris.driver.service.executor;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.driver.DeviceDriver;
import com.iris.messages.address.Address;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.model.Device;
import com.iris.util.Initializer;

public interface DriverExecutorRegistry {

   /**
    * 
    * @param address
    * @return
    * @throws NotFoundException
    *   In case no such device exists for the given address.
    * @throws RuntimeException
    *   In case there is an error communicating with the persistence layer.
    */
   DriverExecutor loadConsumer(Address address) throws NotFoundException, RuntimeException;

   /**
    * Creates a new executor and associates it with the device & driver.
    * An optional Initializer may be passed in to update / configure the executor before it
    * is exposed to receiving events.
    * @param device
    * @param driver
    * @param initializer
    * @return
    */
   DriverExecutor associate(Device device, DeviceDriver driver, @Nullable Initializer<DriverExecutor> initializer);
   
   /**
    * This call atomically:
    *  - Deletes the Device record from the database
    *  - Invokes the disassociated event on the driver
    *  - Removes the driver
    * @param address
    * @return
    * @throws Exception
    *    If there is an error deleting the record
    *    No guarantees are made whether the record
    *    exists or not in this case
    */
   boolean delete(Address address) throws Exception;
   
   /**
    * This call atomically:
    *  - Tombstones the Device record from the database
    *  - Invokes the disassociated event on the driver
    *  - Removes the driver
    * @param address
    * @throws Exception
    *    If there is an error applying the tombstone
    *    No guarantees are made about the state of the device
    *    in this case
    */
   boolean tombstone(Address address) throws Exception;

   /**
    * Flushes the driver from memory, it may be reloaded via calls
    * to loadConsumer.
    * @param address
    */
   void remove(Address address);

}

