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
package com.iris.core.platform;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.definition.EventDefinition;
import com.iris.capability.definition.MethodDefinition;
import com.iris.capability.definition.ObjectDefinition;
import com.iris.capability.definition.ParameterDefinition;
import com.iris.capability.key.NamespacedKey;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.errors.Errors;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Place;
import com.iris.reflection.MethodDiscoverer;
import com.iris.reflection.MethodInvokerFactory.ArgumentResolverFactory;
import com.iris.reflection.Methods;
import com.iris.reflection.Resolvers;

@Singleton
public class PlatformDispatcherFactory {
   private static final Logger logger = LoggerFactory.getLogger(PlatformDispatcherFactory.class);
   
   private final DefinitionRegistry registry;
   private final PlatformMessageBus messageBus;
   // TODO executor here?
   
   @Inject
   public PlatformDispatcherFactory(
         DefinitionRegistry registry,
         PlatformMessageBus messageBus
   ) {
      this.registry = registry;
      this.messageBus = messageBus;
   }
   
   public DispatcherBuilder buildDispatcher() {
      return new DispatcherBuilder();
   }

   private static final MethodDiscoverer FindRequests =
         Methods
            .findMethods()
            .annotatedWith(Request.class)
            .build();
   private static final MethodDiscoverer FindOnMessages = Methods.findMethods().annotatedWith(OnMessage.class).build();

   public class DispatcherBuilder {

      private Set<AddressMatcher> matchers = new HashSet<>();
      private ArgumentResolverFactory<PlatformMessage, MessageBody> resolver = null;
      private ImmutableMap.Builder<String, Function<PlatformMessage, ? extends MessageBody>> requestBuilder =
            ImmutableMap.builder();
      private Map<String, List<Consumer<? super PlatformMessage>>> eventListeners = new HashMap<>();

      public DispatcherBuilder addAddressMatcher(AddressMatcher matcher) {
         Preconditions.checkArgument(matcher != null, "Matcher may not be null");
         matchers.add(matcher);
         return this;
      }

      public DispatcherBuilder addBroadcastMatcher() {
         matchers.add(AddressMatchers.BROADCAST_MESSAGE_MATCHER);
         return this;
      }

      public DispatcherBuilder addRequestHandler(String messageType, Function<PlatformMessage, ? extends MessageBody> handler) {
         requestBuilder.put(messageType, handler);
         return this;
      }
      
      public DispatcherBuilder addFallbackRequestHandler(Function<PlatformMessage, ? extends MessageBody> handler) {
         requestBuilder.put(MessageConstants.MSG_ANY_MESSAGE_TYPE, handler);
         return this;
      }
      
      public DispatcherBuilder addUnsupportedRequestHandler(String messageType) {
         requestBuilder.put(messageType, UnsupportedRequestHandler.Instance);
         return this;
      }

      public DispatcherBuilder addUnsupportedFallbackRequestHandler() {
         addFallbackRequestHandler(UnsupportedRequestHandler.Instance);
         return this;
      }
      
      public DispatcherBuilder addEventConsumer(String messageType, Consumer<? super PlatformMessage> handler) {
         eventListeners.computeIfAbsent(messageType, (k) -> new ArrayList<>()).add(handler);
         return this;
      }

      public DispatcherBuilder addAnnotatedHandler(Object handler) {
         addAnnotatedRequests(handler);
         addAnnotatedEvents(handler);
         return this;
      }
      
      public DispatcherBuilder addArgumentResolverFactory(ArgumentResolverFactory<PlatformMessage, MessageBody> resolver) {
         this.resolver = this.resolver == null ? 
               resolver :
               Resolvers.chain(this.resolver, resolver);
         return this;
      }

      private DispatcherBuilder addAnnotatedRequests(Object handler) {
         List<Method> methods = FindRequests.discover(handler.getClass());
         for(Method m: methods) {
            Request request = m.getAnnotation(Request.class);
            NamespacedKey name = NamespacedKey.parse(request.value());
            boolean service = request.service();
            if(MessageConstants.MSG_ANY_MESSAGE_TYPE.equals(name.getRepresentation())) {
               // wildcard
               logger.info("Binding [{}] to [{}]", request.value(), m);
               Function<PlatformMessage, MessageBody> h = wrapHandler(handler, m, PlatformContextArgumentResolver.WildCardResolver);
               addFallbackRequestHandler(h);
            }
            else {
               MethodDefinition definition = lookupMethod(name, service);
      
               logger.info("Binding [{}] to [{}]", request.value(), m);
               Function<PlatformMessage, MessageBody> h = wrapHandler(handler, m, new PlatformContextArgumentResolver(definition));
               addRequestHandler(request.value(), h);
            }
         }
         return this;
      }

      private DispatcherBuilder addAnnotatedEvents(Object handler) {
         List<Method> methods = FindOnMessages.discover(handler.getClass());
         for(Method m : methods) {
            OnMessage onMsg = m.getAnnotation(OnMessage.class);
            Predicate<Address> matcher = getMatcher(onMsg.from());
            String[] types = onMsg.types();
            for(String s : types) {
               Consumer<PlatformMessage> c;
               if(MessageConstants.MSG_ANY_MESSAGE_TYPE.equals(s)) {
                  logger.info("Method [{}] listening for all events", m);
                  c = wrapConsumer(handler, m, matcher, PlatformContextEventArgumentResolver.WildCardEventResolver);
               }
               else {
                  NamespacedKey name = NamespacedKey.parse(s);
                  EventDefinition evtDef = lookupEvent(name);
                  logger.info("Method [{}] listening for [{}] from [{}]", m, s, matcher);
                  c = wrapConsumer(handler, m, matcher, new PlatformContextEventArgumentResolver(evtDef));
               }
               addEventConsumer(s, c);
            }
         }
         return this;
      }
      
      private Predicate<Address> getMatcher(String[] from) {
         if(from.length == 0) {
            return Predicates.alwaysTrue();
         }
         else if(from.length == 1) {
            return AddressMatchers.fromString(from[0]);
         }
         else {
            return Predicates.or(Arrays.asList(from).stream().map(AddressMatchers::fromString).collect(Collectors.toList()));
         }
      }

      private <R> Consumer<PlatformMessage> wrapConsumer(Object ths, Method m, Predicate<Address> p, ArgumentResolverFactory<PlatformMessage, ?> resolver) {
         ArgumentResolverFactory<PlatformMessage, ?> factory =
               this.resolver == null ?
                     resolver :
                     Resolvers.chain(resolver, (ArgumentResolverFactory) this.resolver);
         Function<PlatformMessage, ?> h = 
               Methods
                  .buildInvokerFactory(factory)
                  .build()
                  .wrapWithThis(m, ths);
         return (message) -> { if(p.apply(message.getSource())) { h.apply(message); } };
      }
      
      private Function<PlatformMessage, MessageBody> wrapHandler(Object ths, Method m, ArgumentResolverFactory<PlatformMessage, MessageBody> resolver) {
         ArgumentResolverFactory<PlatformMessage, MessageBody> factory =
               this.resolver == null ?
                     resolver :
                     Resolvers.chain(resolver, this.resolver);
         Function<PlatformMessage, MessageBody> h = 
               Methods
                  .buildInvokerFactory(factory)
                  .build()
                  .wrapWithThis(m, ths);
         return h;
      }

      private EventDefinition lookupEvent(NamespacedKey name) {
         EventDefinition eventDef = lookupEvent(name.getName(), registry.getService(name.getNamespace()));
         if(eventDef == null) {
            eventDef = lookupEvent(name.getName(), registry.getCapability(name.getNamespace()));
         }
         if(eventDef == null) {
            throw new IllegalArgumentException("No service or capability namespaced with " + name.getNamespace() + " defines an event " + name.getName());
         }
         return eventDef;
      }

      private EventDefinition lookupEvent(String name, ObjectDefinition def) {
         if(def == null) {
            return null;
         }
         return def.getEvents().stream().filter((e) -> Objects.equals(name, e.getName())).findFirst().orElse(null);
      }

      private MethodDefinition lookupMethod(NamespacedKey name, boolean service) {
         ObjectDefinition definition = service ? registry.getService(name.getNamespace()) : registry.getCapability(name.getNamespace());
         if(definition == null) {
            throw new IllegalArgumentException("No " + (service ? "service" : "capability") + " namespaced with " + name.getNamespace() + " exists" + (service ? "" : " (try service=true?)"));
         }
         return
               definition
                  .getMethods()
                  .stream()
                  .filter((method) -> Objects.equals(method.getName(), name.getName()))
                  .findFirst()
                  .orElseThrow(() -> new IllegalArgumentException("Unable to find a " + (service ? "service" : "capability") + " method named " + name.getName() + " on " + name.getNamespace()));
      }

      private Map<String, Consumer<? super PlatformMessage>> marshal(Map<String, List<Consumer<? super PlatformMessage>>> listeners) {
         ImmutableMap.Builder<String, Consumer<? super PlatformMessage>> consumers = ImmutableMap.builder();
         for(Map.Entry<String, List<Consumer<? super PlatformMessage>>> entry: listeners.entrySet()) {
            if(entry.getValue().isEmpty()) {
               // skip
            }
            else if(entry.getValue().size() == 1) {
               consumers.put(entry.getKey(), entry.getValue().get(0));
            }
            else {
               consumers.put(entry.getKey(), new ConsumerList<>(entry.getValue()));
            }
         }
         return consumers.build();
      }

      public PlatformDispatcher build() {
         return new PlatformDispatcher(matchers, requestBuilder.build(), marshal(eventListeners), messageBus);
      }

   }

   private static class ConsumerList<V> implements Consumer<V> {
      private final List<Consumer<? super V>> consumers;
      
      public ConsumerList(List<Consumer<? super V>> consumers) {
         this.consumers = ImmutableList.copyOf(consumers);
      }

      @Override
      public void accept(V value) {
         for(Consumer<? super V> c: consumers) {
            try {
               c.accept(value);
            }
            catch(Exception e) {
               logger.warn("Error notifying consumer [{}] of [{}]", c, value, e);
            }
         }
      }

   }
   
   private static class PlatformContextArgumentResolver implements ArgumentResolverFactory<PlatformMessage, MessageBody> {
      // TODO move these to a utility class
      private static Function<Object, MessageBody> EmptyMessageFn = Functions.constant(MessageBody.emptyMessage());
      private static Function<Object, MessageBody> NoResponseFn = Functions.constant(MessageBody.noResponse());
      private static Function<Object, MessageBody> CastToMessageBodyFn = (any) -> (MessageBody) any;
      private static PlatformContextArgumentResolver WildCardResolver = new PlatformContextArgumentResolver(null);
      
      public static Function<PlatformMessage, Object> coerceFn(ParameterDefinition parameter) {
         final AttributeType type = parameter.getType();
         final String key = parameter.getName();
         return (message) -> type.coerce(message.getValue().getAttributes().get(key));
      }
      
      private final MethodDefinition definition;
      
      private PlatformContextArgumentResolver(MethodDefinition definition) {
         this.definition = definition;
      }
      
      @Override
      public Function<? super PlatformMessage, ?> getResolverForParameter(Method method, Type parameter, Annotation[] annotations) {
         if(PlatformMessage.class.equals(parameter)) {
            return (message) -> message;
         }
         else if(Place.class.equals(parameter)) {
            return 
                  (message) -> 
                     !StringUtils.isEmpty(message.getPlaceId()) ? 
                        ServiceLocator.getInstance(PlaceDAO.class).findById(UUID.fromString(message.getPlaceId())) :
                        null ;
         }
         else if(MessageBody.class.equals(parameter)) {
            return (message) -> message.getValue();
         }
         if(definition != null) {
            for(Annotation annotation: annotations) {
               String name = null;
               if(annotation instanceof javax.inject.Named) {
                  name = ((javax.inject.Named) annotation).value();
               }
               else if(annotation instanceof com.google.inject.name.Named) {
                  name = ((com.google.inject.name.Named) annotation).value();
               }
               if(name != null) {
                  for(ParameterDefinition arg: definition.getParameters()) {
                     if(arg.getName().equals(name)) {
                        if(TypeUtils.isAssignable(arg.getType().getJavaType(), parameter)) {
                           return coerceFn(arg);
                        }
                        else {
                           throw new IllegalArgumentException("Argument " + name + " on method " + definition.getName() + " of type " + arg.getType() + " is not assignable to " + parameter + " (expected " + arg.getType().getJavaType() + ")");
                        }
                     }
                  }
                  throw new IllegalArgumentException("Method " + definition.getName() + " does not support a parameter named " + name);
               }
            }
         }
         
         return null;
      }

      @Override
      public Function<Object, MessageBody> getResolverForReturnType(Method method) {
         Request request = method.getAnnotation(Request.class);
         if(request != null && request.response() == false) {
            if(Void.TYPE.equals(method.getReturnType())) {
               return NoResponseFn;
            }
            throw new IllegalArgumentException("Method is marked as response = false, so it must have a void return type");
         }
         else {
            if(Void.TYPE.equals(method.getReturnType())) {
               if(!definition.getReturnValues().isEmpty()) {
                  throw new IllegalArgumentException("Must return a MessageBody for " + method.getName());
               }
               return EmptyMessageFn;
            }
            if(MessageBody.class.equals(method.getReturnType())) {
               if(definition.getReturnValues().isEmpty()) {
                  logger.warn("Returning a response to " + definition.getName() + " breaks the capability contract");
               }
               return CastToMessageBodyFn;
            }
            throw new IllegalArgumentException("Unsupported return type of " + method.getReturnType() + ". Expected " + (definition.getReturnValues().isEmpty() ? "MessageBody" : "void"));
         }
      }
      
   }

   private static class PlatformContextEventArgumentResolver implements ArgumentResolverFactory<PlatformMessage, Void> {
      private static PlatformContextEventArgumentResolver WildCardEventResolver = new PlatformContextEventArgumentResolver(null);

      public static Function<PlatformMessage, Object> coerceFn(ParameterDefinition parameter) {
         final AttributeType type = parameter.getType();
         final String key = parameter.getName();
         return (message) -> type.coerce(message.getValue().getAttributes().get(key));
      }

      private final EventDefinition definition;

      private PlatformContextEventArgumentResolver(EventDefinition definition) {
         this.definition = definition;
      }

      @Override
      public Function<? super PlatformMessage, ?> getResolverForParameter(Method method, Type parameter, Annotation[] annotations) {
         if(PlatformMessage.class.equals(parameter)) {
            return (message) -> message;
         }
         else if(Place.class.equals(parameter)) {
            return
                  (message) ->
                        !StringUtils.isEmpty(message.getPlaceId()) ?
                              ServiceLocator.getInstance(PlaceDAO.class).findById(UUID.fromString(message.getPlaceId())) :
                              null ;
         }
         else if(MessageBody.class.equals(parameter)) {
            return (message) -> message.getValue();
         }
         if(definition != null) {
            for(Annotation annotation: annotations) {
               String name = null;
               if(annotation instanceof javax.inject.Named) {
                  name = ((javax.inject.Named) annotation).value();
               }
               else if(annotation instanceof com.google.inject.name.Named) {
                  name = ((com.google.inject.name.Named) annotation).value();
               }
               if(name != null) {
                  for(ParameterDefinition arg: definition.getParameters()) {
                     if(arg.getName().equals(name)) {
                        if(TypeUtils.isAssignable(arg.getType().getJavaType(), parameter)) {
                           return coerceFn(arg);
                        }
                        else {
                           throw new IllegalStateException("Argument " + name + " on event " + definition.getName() + " of type " + arg.getType() + " is not assignable to " + parameter + " (expected " + arg.getType().getJavaType() + ")");
                        }
                     }
                  }
                  throw new IllegalStateException("Event " + definition.getName() + " does not support a parameter named " + name);
               }
            }
         }

         return null;
      }

      @Override
      public Function<Object, Void> getResolverForReturnType(Method method) {
         if(Void.TYPE.equals(method.getReturnType())) {
            return Functions.constant(null);
         }
         throw new IllegalArgumentException("Unsupported return type of " + method.getReturnType() + ". Expected void.");
      }

   }
   
   private static class UnsupportedRequestHandler implements Function<PlatformMessage, MessageBody> {
      private static final UnsupportedRequestHandler Instance = new UnsupportedRequestHandler();

      @Override
      public MessageBody apply(PlatformMessage input) {
         logger.warn("Received request for unsupported message type: [{}] to address: [{}]", input.getMessageType(), input.getDestination());
         return Errors.unsupportedMessageType(input.getMessageType());
      }
      
   }
   
}

