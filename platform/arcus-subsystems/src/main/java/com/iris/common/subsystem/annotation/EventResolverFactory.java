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
package com.iris.common.subsystem.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.common.base.Function;
import com.iris.common.subsystem.event.SubsystemEventAndContext;
import com.iris.messages.address.Address;
import com.iris.messages.event.AddressableEvent;
import com.iris.messages.model.Model;

/**
 * 
 */
public class EventResolverFactory extends SubsystemResolverFactory<Void> {
   private static final Function<SubsystemEventAndContext, Address> GetAddress = new Function<SubsystemEventAndContext, Address>() {
      @Override
      public Address apply(SubsystemEventAndContext input) {
         return input.getEvent().getAddress();
      }
   };
   private static final Function<SubsystemEventAndContext, AddressableEvent> GetEvent = new Function<SubsystemEventAndContext, AddressableEvent>() {
      @Override
      public AddressableEvent apply(SubsystemEventAndContext input) {
         return input.getEvent();
      }
   };
   private static final Function<SubsystemEventAndContext, Model> GetModel = new Function<SubsystemEventAndContext, Model>() {
      @Override
      public Model apply(SubsystemEventAndContext input) {
         Address address = input.getEvent().getAddress();
         return input.getContext().models().getModelByAddress(address);
      }
   };
   
   
   private final Class<?> eventType;
   
   public EventResolverFactory(Class<? extends AddressableEvent> eventType) {
      this.eventType = eventType;
   }
   
   @Override
   public Function<? super SubsystemEventAndContext, ?> getResolverForParameter(Method method, Type parameter, Annotation[] annotations) {
      if(parameter instanceof Class) {
         Class<?> type = (Class<?>) parameter;
         if(type.isAssignableFrom(eventType)) {
            return GetEvent;
         }
         if(type.isAssignableFrom(Address.class)) {
            return GetAddress;
         }
         if(type.equals(Model.class)) {
            return GetModel;
         }
      }
      else if(parameter instanceof ParameterizedType) {
         return getResolverForParameter(method, ((ParameterizedType) parameter).getRawType(), annotations);
      }
      return super.getResolverForParameter(method, parameter, annotations);
   }

}

