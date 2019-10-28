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
package com.iris.agent.zwave.node;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.util.ByteUtils;
import com.iris.agent.zwave.ZWConfig;
import com.iris.agent.zwave.ZWUtils;
import com.iris.agent.zwave.code.cmdclass.CmdClasses;
import com.iris.agent.zwave.events.ZWEventDispatcher;
import com.iris.agent.zwave.events.ZWNodeGoneOfflineEvent;
import com.iris.agent.zwave.events.ZWNodeGoneOnlineEvent;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;

public class ZWNode {
   public final static int INVALID_VALUE = Integer.MIN_VALUE;
   
   // The ZWave id of this node. A value between 1 - 232. 
   private final int nodeId;
   
   // The home id of the network the node is on
   private final long homeId;
   
   // The protocol id of this device on the IRIS platform
   private final ProtocolDeviceId deviceId;
   
   // The ZWave basic device type. 
   // BASIC_TYPE_CONTROLLER
   // BASIC_TYPE_STATIC_CONTROLLER
   // BASIC_TYPE_SLAVE
   // BASIC_TYPE_ROUTING_SLAVE
   private final int basicDeviceType;
   
   // ZWave generic device type. (used to match to generic driver)
   private final int genericDeviceType;
   
   // ZWave generic specific device type. (used to match to generic driver)
   private final int specificDeviceType;
   
   // Manufacturer specific id (identifies the vendor)
   private final int manufacturerId;
   
   // Manufacturer specific product type id (used to match to specific driver)
   private final int productTypeId;
   
   // Manufacturer specific product id (used to match to specific driver)
   private final int productId;
   
   // Supported Command Classes
   private final Set<Integer> cmdClassSet = new HashSet<>();
   
   // Indicates if the a device is current online
   private boolean isOnline = true;
   
   // The timeout in secs before a device is considered offline. 
   // A value of zero indicates that the value has not be set.
   private int offlineTimeout = 0; 
   
   private int strikes = 0;
   
   private long lastCall = System.currentTimeMillis();
   
   // Indicates if the node has been fully paired and set up.
   private final boolean complete;
   
   /**
    * Constructs an incomplete node. This is used during the pairing process while more information about the node is gathered.
    * 
    * @param nodeId - The ZWave node id (a value of 1 - 232)
    * @param homeId - The home id of the ZWave network this node is on.
    */
   ZWNode(int nodeId, long homeId) {
      this(nodeId, homeId, INVALID_VALUE, INVALID_VALUE, INVALID_VALUE, INVALID_VALUE, INVALID_VALUE, INVALID_VALUE, null, true, 0);
   }
   
   /**
    * Constructs a complete node if all the information is present. This is used to construct the node after the pairing
    * process is complete.
    * 
    * @param nodeId - The ZWave node id (a value of 1 - 232)
    * @param homeId - The home id of the ZWave network this node is on.
    * @param basicType - The ZWave basic type of device
    * @param genericType - The ZWave generic type of device
    * @param specificType - The ZWave specific type of device
    * @param manufacturerId - The id of the vendor
    * @param productTypeId - The manufacturer specific product type id
    * @param productId - The manufacturer specific product id
    * @param cmdClassSet - Set of command classes
    * @param online - Indicates if the device is online.
    * @param offlineTimeout - The amount of time before the device is considered offline
    */
   ZWNode(int nodeId, 
         long homeId, 
         int basicType, 
         int genericType, 
         int specificType, 
         int manufacturerId, 
         int productTypeId, 
         int productId, 
         Collection<Integer> cmdClassSet, 
         boolean online, 
         int offlineTimeout) {
      this.nodeId = nodeId;
      this.homeId = homeId;
      this.deviceId = ZWUtils.getDeviceId(homeId, nodeId);
      this.basicDeviceType = basicType;
      this.genericDeviceType = genericType;
      this.specificDeviceType = specificType;
      this.manufacturerId = manufacturerId;
      this.productTypeId = productTypeId;
      this.productId = productId;
      if (cmdClassSet != null) {
         this.cmdClassSet.addAll(cmdClassSet);
      }
      this.complete = ( nodeId != INVALID_VALUE
            && homeId != INVALID_VALUE
            && basicType != INVALID_VALUE
            && genericType != INVALID_VALUE
            && specificType != INVALID_VALUE
            && manufacturerId != INVALID_VALUE
            && productTypeId != INVALID_VALUE
            && productId != INVALID_VALUE);
   }
   
   /**
    * Helper function to create an incomplete node.
    * 
    * @param nodeId - The ZWave node id (a value of 1 - 232)
    * @param homeId - The home id of the ZWave network this node is on.
    * @return incomplete zip node
    */
   public static ZWNode createPartialNode(int nodeId, long homeId) {
      return new ZWNode(nodeId, homeId);
   }
   
   /**
    * Helper function to provide a node builder based on a node id. Used to create an incomplete node during
    * the pairing process or to create a node when loading from the database.
    * 
    * @param nodeId - Zwave node id
    * @return zip node builder
    */
   public static ZWNodeBuilder builder(int nodeId) {
      return new ZWNodeBuilder(nodeId);
   }
   
   /**
    * Returns the protocol address for this node.
    * 
    * @return protocol address
    */
   public Address getProtocolAddresss() {
      return Address.hubProtocolAddress(HubAttributesService.getHubId(), "ZWAV", getDeviceId());
   }
   
   /**
    * Indicates if this device supports the wakeup command class.
    * 
    * @return true if the device supports the wakeup command class
    */
   public boolean isWakeupDevice() {
      return cmdClassSet.contains(CmdClasses.WAKEUP_COMMAND_CLASS_ID);
   }
   
   /**
    * Indicates if this node is the controller node for the network.
    * 
    * @return true if the node in the controller
    */
   public boolean isGateway() {
      return this.nodeId == ZWConfig.GATEWAY_NODE_ID;
   }
   
   /**
    * Indicates if the node is completely defined.
    * 
    * @return true if this node is completely defined.
    */
   public boolean isComplete() {
      return complete;
   }
   
   /**
    * Indicates if the node is not completely defined.
    * 
    * @return true if this node is not completely defined.
    */
   public boolean isPartial() {
      return !complete;
   }
   
   /**
    * The ZWave node id (a value of 1 - 232). This is a one byte value.
    * 
    * @return ZWave node id
    */
   public int getNodeId() {
      return nodeId;
   }
   
   /**
    * The home id of the ZWave network this node is on. This is a four byte value.
    * 
    * @return ZWave network home id
    */
   public long getHomeId() {
      return homeId;
   }
   
   /**
    * The protocol id used to identify this node on the Acrus platform.
    * 
    * @return Acrus protocol id
    */
   public ProtocolDeviceId getDeviceId() {
      return deviceId;
   }

   /**
    * The ZWave basic type of device
    * 
    * @return basic device type
    */
   public int getBasicDeviceType() {
      return basicDeviceType;
   }

   /**
    * The ZWave generic type of device
    * 
    * @return generic device type
    */
   public int getGenericDeviceType() {
      return genericDeviceType;
   }

   /**
    * The ZWave specific type of device
    * 
    * @return specific device type
    */
   public int getSpecificDeviceType() {
      return specificDeviceType;
   }

   /**
    * The id of the vendor
    * 
    * @return vendor id
    */
   public int getManufacturerId() {
      return manufacturerId;
   }

   /**
    * The manufacturer specific product type id
    * 
    * @return product type id
    */
   public int getProductTypeId() {
      return productTypeId;
   }

   /**
    * The manufacturer specific product id
    * 
    * @return product id
    */
   public int getProductId() {
      return productId;
   }
   
   /**
    * Indicates if the device is online
    * 
    * @return true if online
    */
   public boolean isOnline() {
      return isOnline;
   }

   /**
    * Sets the online status of this device
    * 
    * @param isOnline true if online
    */
   public void setOnline(boolean isOnline) {
      if (this.isOnline != isOnline) {
         this.isOnline = isOnline;
         if (this.isOnline) {
            ZWEventDispatcher.INSTANCE.dispatch(new ZWNodeGoneOnlineEvent(getNodeId()));
         }
         else {
            ZWEventDispatcher.INSTANCE.dispatch(new ZWNodeGoneOfflineEvent(getNodeId()));
         }
      }
   }

   /**
    * Returns the time in secs before a device
    * is considered offline
    * 
    * @return time in secs or 0 if not set
    */
   public int getOfflineTimeout() {
      return offlineTimeout;
   }

   /**
    * Sets the time in secs before a device is
    * considered offline
    * 
    * @param offlineTimeout time in secs or 0 if not set
    */
   public void setOfflineTimeout(int offlineTimeout) {
      this.offlineTimeout = offlineTimeout;
   }

   /**
    * Returns the supported command classes represented
    * as integers
    * 
    * @return supported command classes
    */
   public Set<Integer> getCmdClassSet() {
      return Collections.unmodifiableSet(cmdClassSet);
   }
   
   /**
    * Returns the supported command classes as an array
    * of bytes
    * 
    * @return supported command classes
    */
   public byte[] getCmdClassBytes() {
      return ByteUtils.ints2Bytes(cmdClassSet);
   }
   
   /**
    * Gets the number of attempts made to communicate with a device
    * that hasn't been heard from for longer than it's offline 
    * timeout.
    * 
    * @return number of attempts
    */
   public int getStrikes() {
      return strikes;
   }
   
   /**
    * Sets the number of attempts made to communicate with a device
    * that hasn't been heard from for longer than it's offline 
    * timeout.
    * 
    * @param strikes number of attempts
    */
   public void setStrikes(int strikes) {
      this.strikes = strikes;
   }

   /**
    * Gets the time (in system milliseconds) that the hub agent
    * last received communication from the node. 
    * 
    * @param lastCall time in system milliseconds
    */
   public long getLastCall() {
      return lastCall;
   }

   /**
    * Sets the time (in system milliseconds) that the hub agent
    * last received communication from the node. 
    * 
    * @param lastCall time in system milliseconds
    */
   public void setLastCall(long lastCall) {
      this.strikes = 0;
      this.lastCall = lastCall;
   }
}
