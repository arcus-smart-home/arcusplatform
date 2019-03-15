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
package com.iris.protocol.control;

import java.util.Date;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.MessageBody;

public class DeviceOfflineEvent extends MessageBody {
   public static final String MESSAGE_TYPE = "DeviceOfflineEvent";
   public static final String ATTR_LAST_CONTACT = "lastContact";
   public static final String ATTR_MESSAGE = "message";
   public static final String MESSAGE_UNPAIRED = "left network";

   public static DeviceOfflineEvent create() {
      return new DeviceOfflineEvent("", null);
   }

   public static DeviceOfflineEvent create(Long lastContact) {
      return new DeviceOfflineEvent("", lastContact);
   }

   public static DeviceOfflineEvent create(Date lastContact) {
      return new DeviceOfflineEvent("", lastContact.getTime());
   }

   public static DeviceOfflineEvent create(String msg) {
      return new DeviceOfflineEvent(msg, null);
   }

   public static DeviceOfflineEvent create(String msg, Long lastContact) {
      return new DeviceOfflineEvent(msg, lastContact);
   }

   public static DeviceOfflineEvent create(String msg, Date lastContact) {
      return new DeviceOfflineEvent(msg, lastContact.getTime());
   }

   DeviceOfflineEvent(String msg, Long timestamp) {
      super(MESSAGE_TYPE, timestamp == null ? ImmutableMap.<String,Object>of(ATTR_MESSAGE, msg) : ImmutableMap.<String,Object>of(ATTR_MESSAGE, msg, ATTR_LAST_CONTACT, timestamp));
   }

   public String getMessage() {
      Object msg = getAttributes().get(ATTR_MESSAGE);
      return msg == null ? "" : String.valueOf(msg);
   }

   public Date getLastContact() {
      Object lastContact = getAttributes().get(ATTR_LAST_CONTACT);
      return lastContact == null ? null : new Date(((Number) lastContact).longValue());
   }
}

