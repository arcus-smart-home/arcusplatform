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

import javax.inject.Named;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeTypes;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.event.SubsystemEventAndContext;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.reflection.MethodInvokerFactory.ArgumentResolverFactory;

/**
 * 
 */
public class RequestHandlerResolverFactory implements ArgumentResolverFactory<SubsystemEventAndContext, MessageBody> {
   private static final Function<Object, MessageBody> ReturnEmpty = Functions.constant(MessageBody.emptyMessage());
   
   @Override
   public Function<? super SubsystemEventAndContext, ?> getResolverForParameter(Method method, Type parameter, Annotation[] annotations) {
      String name = nameOf(annotations);
      if(name != null) {
         AttributeType type = AttributeTypes.fromJavaType(parameter);
         return new GetAttribute(name, type);
      }
      if(parameter instanceof Class) {
         return doGetResolverForParameter(method, (Class<?>)  parameter);
      }
      else if(parameter instanceof ParameterizedType) {
         return getResolverForParameter(method, ((ParameterizedType) parameter).getRawType(), annotations);
      }
      else {
         throw new IllegalArgumentException("Unable to resolve parameter for type " + parameter);
      }
   }
   
   protected Function<? super SubsystemEventAndContext, ?> doGetResolverForParameter(Method method, Class<?> parameter) {
      if(parameter.isAssignableFrom(SubsystemContext.class)) {
         return SubsystemResolverFactory.GetContext;
      }
      if(parameter.equals(PlatformMessage.class)) {
         return MessageReceivedResolverFactory.GetMessage;
      }
      if(parameter.equals(MessageBody.class)) {
         return MessageReceivedResolverFactory.GetMessageBody;
      }
      throw new IllegalArgumentException("Unresolvable parameter type [" + parameter + "] for method [" + method + "] on class [" + method.getDeclaringClass() + "]");
   }

   @Override
   public Function<Object, MessageBody> getResolverForReturnType(Method method) {
      if(Void.TYPE.equals(method.getReturnType())) {
         // TODO should use the definition to return the proper type here..
         return ReturnEmpty;
      }
      if(MessageBody.class.isAssignableFrom(method.getReturnType())) {
         return new CastFunction<MessageBody>(MessageBody.class);
      }
      
      throw new IllegalArgumentException("Can't translate " + method.getReturnType() + " into a MessageBody");
   }
   
   private String nameOf(Annotation [] annotations) {
      for(Annotation annotation: annotations) {
         if(annotation instanceof Named) {
            return ((Named) annotation).value();
         }
         if(annotation instanceof com.google.inject.name.Named) {
            return ((com.google.inject.name.Named) annotation).value();
         }
      }
      return null;
   }

   private static class GetAttribute implements Function<SubsystemEventAndContext, Object> {
      private final AttributeType type;
      private final String name;
      
      GetAttribute(String name, AttributeType type) {
         this.type = type;
         this.name = name;
      }
      
      @Override
      public Object apply(SubsystemEventAndContext input) {
         MessageReceivedEvent event = (MessageReceivedEvent) input.getEvent();
         Object value = event.getMessage().getValue().getAttributes().get(name);
         return type.coerce(value);
      }
      
   }

}

