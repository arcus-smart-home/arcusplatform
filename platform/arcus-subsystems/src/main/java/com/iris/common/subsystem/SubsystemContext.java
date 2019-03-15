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
package com.iris.common.subsystem;

import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;

import com.iris.common.scheduler.ScheduledTask;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.context.PlaceContext;
import com.iris.messages.model.Model;
import com.iris.type.LooselyTypedReference;
import com.iris.util.Subscription;

/**
 * 
 */
// TODO should all this fold down into PlaceContext?
public interface SubsystemContext<M extends Model> extends PlaceContext {
   
   M model();
   
   void broadcast(MessageBody message);
   
   void send(Address address, MessageBody message);

   void sendAndExpectResponse(Address address, MessageBody message, long timeout, TimeUnit units, ResponseAction<? super M> action);

   void sendResponse(PlatformMessage request, MessageBody message);

   String request(Address address, MessageBody message);

   String request(Address address, MessageBody shutoff, int timeToLiveMs);
   
   ScheduledTask wakeUpIn(long time, TimeUnit unit);
   
   ScheduledTask wakeUpAt(Date timestamp);
   
   Object getAttribute(String name);
   
   LooselyTypedReference getVariable(String name);
   
   void setVariable(String name, Object value);
   
   void setActor(Address actor);

   void setTimeZone(TimeZone tz);
   
   boolean isPersisted();
   
   boolean isDeleted();
   
   void commit();

   void delete();

   Subscription addBindSubscription(@NonNull Subscription subscription);
   void unbind();

   interface ResponseAction<M extends Model> {
      
      void onResponse(SubsystemContext<M> context, PlatformMessage response);
      
      void onError(SubsystemContext<M> context, Throwable cause);
      
      void onTimeout(SubsystemContext<M> context);
   }
}

