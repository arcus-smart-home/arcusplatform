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
package com.iris.driver.reflex;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;

import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;

public interface ReflexDriverContext {
   Address getProtocolAddress();
   String getProtocolName();

   Logger getDriverLogger();

   boolean wasMessageHandled();
   void markMessageHandled(boolean handled);

   @Nullable Object setVariable(String name, @Nullable Object value);
   @Nullable <T> T getVariable(String name);
   <T> T getVariable(String name, T defaultValue);

   void setAttribute(String key, Object value);
   void setAttributes(Map<String,Object> attrs);
   void emit(MessageBody msg);

   void setResponse(MessageBody msg);
   MessageBody getAndResetResponse();

   void emitAttribute(String key, Object value);
   void emitAttributes(Map<String,Object> attrs);

   void markSetAttributeConsumed(String key);
   Set<String> getAndResetSetAttributesConsumed();

   Map<String,Object> getAttributesToEmit();
   void commit();

   void zigbeeSend(ZigbeeMessage.Protocol msg);
   void zwaveSend(com.iris.protocol.zwave.Protocol.Message msg);
   void zwaveAddScheduledPoll(long time, TimeUnit unit, Collection<byte[]> polls);

   void reset();
}

