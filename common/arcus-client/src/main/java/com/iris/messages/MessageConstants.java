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
package com.iris.messages;

import com.iris.messages.services.PlatformConstants;

public class MessageConstants {
   @Deprecated
   public final static String HUB_SERVICE = "HUBS";

   public final static String DRIVER = "DRIV";
   public final static String CLIENT = "CLNT";
   public final static String BROADCAST = "BRDC";
   public static final String HUB = "HUB";
   public static final String PROTOCOL = "PROT";
   public static final String SERVICE = "SERV";
   public static final String BRIDGE = "BRDG";

   /* @deprecated Use generated capability instead */
   @Deprecated
   public static final String MSG_BASE_GETATTRIBUTES = "base:GetAttributes";
   /* @deprecated Use generated capability instead */
   @Deprecated
   public static final String MSG_BASE_GETATTRIBUTES_RESPONSE = "base:GetAttributesResponse";
   /* @deprecated Use generated capability instead */
   @Deprecated
   public static final String MSG_BASE_SETATTRIBUTES = "base:SetAttributes";
   /* @deprecated Use generated capability instead */
   @Deprecated
   public static final String MSG_BASE_VALUECHANGE = "base:ValueChange";
   /* @deprecated Use generated capability instead */
   @Deprecated
   public static final String MSG_BASE_ADDED = "base:Added";
   public static final String MSG_ERROR = "Error";
   public static final String MSG_DEVICE_COMMAND = "DeviceCommand";
   public static final String MSG_SESSION_CREATED = "SessionCreated";
   public static final String MSG_EMPTY_MESSAGE = "EmptyMessage";
   public static final String MSG_PING_REQUEST = PlatformConstants.NAMESPACE + ":PingRequest";
   public static final String MSG_PONG_RESPONSE = PlatformConstants.NAMESPACE + ":PongResponse";
   public static final String MSG_CREATE_ACCOUNT = PlatformConstants.SERVICE_ACCOUNTS + ":CreateAccount";
   public static final String MSG_ACCOUNT_SIGNUP_STATE_TRANSITION = PlatformConstants.SERVICE_ACCOUNTS + ":SignupTransition";
   public static final String MSG_CREATE_ACCOUNT_RESPONSE = PlatformConstants.SERVICE_ACCOUNTS + ":CreateAccountResponse";
   public static final String MSG_ACCOUNT_LIST_DEVICES = PlatformConstants.SERVICE_ACCOUNTS + ":ListDevices";
   public static final String MSG_ACCOUNT_LIST_DEVICES_RESPONSE = PlatformConstants.SERVICE_ACCOUNTS + ":ListDevicesResponse";
   public static final String MSG_LIST_HUBS = PlatformConstants.SERVICE_ACCOUNTS + ":ListHubs";
   public static final String MSG_LIST_HUBS_RESPONSE = PlatformConstants.SERVICE_ACCOUNTS + ":ListHubsResponse";
   public static final String MSG_GET_HUB = PlatformConstants.SERVICE_PLACES + ":GetHub";
   public static final String MSG_GET_HUB_RESPONSE = PlatformConstants.SERVICE_PLACES + ":GetHubResponse";
   public static final String MSG_PLACE_LIST_DEVICES = PlatformConstants.SERVICE_PLACES + ":ListDevices";
   public static final String MSG_PLACE_LIST_DEVICES_RESPONSE = PlatformConstants.SERVICE_PLACES + ":ListDevicesResponse";
   public static final String MSG_REGISTER_HUB = PlatformConstants.SERVICE_PLACES + ":RegisterHub";
   public static final String MSG_HUB_CONNECTED_EVENT = "HubConnectedEvent";
   public static final String MSG_HUB_AUTHORIZED_EVENT = "HubAuthorizedEvent";
   public static final String MSG_HUB_REGISTERED_REQUEST = "HubRegisteredRequest";
   public static final String MSG_HUB_REGISTERED_RESPONSE = "HubRegisteredResponse";
   public static final String MSG_ADD_DEVICE_REQUEST = PlatformConstants.NAMESPACE + ":AddDeviceRequest";
   public static final String MSG_ADD_DEVICE_RESPONSE = PlatformConstants.NAMESPACE + ":AddDeviceResponse";
   public static final String MSG_REMOVE_DEVICE_REQUEST = PlatformConstants.NAMESPACE + ":RemoveDeviceRequest";
   public static final String MSG_REMOVE_DEVICE_RESPONSE = PlatformConstants.NAMESPACE + ":RemoveDeviceResponse";
   public static final String MSG_HISTORY_ENTRY_MESSAGE = "HistoryEntryMessage";
   public static final String MSG_NOTIFICATION_MESSAGE = "NotificationMessage";

   // Matches any message type.
   public static final String MSG_ANY_MESSAGE_TYPE = "*";

   // Attribute Names
   public static final String ATTR_PROTOCOLNAME = "protocolName";
   public static final String ATTR_DEVICEID = "deviceId";
   public static final String ATTR_HUBID = "hubId";
   public static final String ATTR_ACCOUNTID = "accountId";
   public static final String ATTR_PLACEID = "placeId";
   public static final String ATTR_PROTOCOLATTRIBUTES = "protocolAttributes";
   public static final String ATTR_DEVICEADDRESS = "deviceAddress";
   public static final String ATTR_MIGRATED = "migrated";
   public static final String ATTR_REFLEXVERSION = "reflexVersion";

   // Common tag names
   public static final String TAG_MIGRATED = "MIGRATED";

   private MessageConstants() {}
}

