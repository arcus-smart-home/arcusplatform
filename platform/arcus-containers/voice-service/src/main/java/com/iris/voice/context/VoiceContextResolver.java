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
package com.iris.voice.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.errors.NotFoundException;
import com.iris.reflection.MethodInvokerFactory;
import com.iris.util.IrisUUID;

@Singleton
public class VoiceContextResolver implements MethodInvokerFactory.ArgumentResolverFactory<PlatformMessage, MessageBody> {

   private static final Type optionalContext = (new TypeToken<Optional<VoiceContext>>() {}).getType();

   private final Provider<VoiceContextExecutorRegistry> registry;

   @Inject
   public VoiceContextResolver(Provider<VoiceContextExecutorRegistry> registry) {
      this.registry = registry;
   }

   @Nullable
   @Override
   public Function<? super PlatformMessage, ?> getResolverForParameter(Method method, Type parameter, Annotation[] annotations) {
      if(VoiceContext.class.equals(parameter)) {
         return this::getContext;
      }
      if(optionalContext.equals(parameter)) {
         return this::getOptionalContext;
      }
      return null;
   }

   @Nullable
   @Override
   public Function<Object, MessageBody> getResolverForReturnType(Method method) {
      return null;
   }

   private VoiceContext getContext(PlatformMessage message) {
      if(StringUtils.isBlank(message.getPlaceId())) {
         throw new NotFoundException(message.getDestination());
      }
      return registry.get().get(IrisUUID.fromString(message.getPlaceId())).context();
   }

   private Optional<VoiceContext> getOptionalContext(PlatformMessage message) {
      if(StringUtils.isBlank(message.getPlaceId())) {
         return Optional.empty();
      }
      return registry.get().getOptional(IrisUUID.fromString(message.getPlaceId())).map(VoiceContextExecutor::context);
   }
}

