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

import java.io.InputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.iris.Utils;
import com.iris.bootstrap.guice.Injectors;
import com.iris.common.subsystem.annotation.AnnotatedSubsystemFactory;
import com.iris.common.subsystem.event.SubsystemActivatedEvent;
import com.iris.common.subsystem.event.SubsystemAddedEvent;
import com.iris.common.subsystem.event.SubsystemDeactivatedEvent;
import com.iris.common.subsystem.event.SubsystemEventAndContext;
import com.iris.common.subsystem.event.SubsystemRemovedEvent;
import com.iris.common.subsystem.event.SubsystemResponseEvent;
import com.iris.common.subsystem.event.SubsystemStartedEvent;
import com.iris.common.subsystem.event.SubsystemStoppedEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.errors.Errors;
import com.iris.messages.event.AddressableEvent;
import com.iris.messages.event.ListenerList;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.event.ModelReportEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.model.Version;

/**
 * 
 */
public class AnnotatedSubsystem<M extends SubsystemModel> implements Subsystem<M> {
   private static Logger logger = LoggerFactory.getLogger(AnnotatedSubsystem.class);
   
   private final String name;
   private final String namespace;
   private final Version version;
   private final String hash;
   private final Class<M> type;
   
   private final Map<String, Function<SubsystemEventAndContext, MessageBody>> requestHandlers;
   private final ListenerList<SubsystemEventAndContext> messageReceivedListeners;
   private final ListenerList<SubsystemEventAndContext> modelAddedListeners;
   private final ListenerList<SubsystemEventAndContext> modelChangedListeners;
   private final ListenerList<SubsystemEventAndContext> attrReportListeners;
   private final ListenerList<SubsystemEventAndContext> modelRemovedListeners;
   private final ListenerList<SubsystemEventAndContext> scheduledListeners;
   
   /**
    * 
    */
   public AnnotatedSubsystem() {
      this.type = typeOf(this.getClass());
      this.name = nameOf(type);
      this.namespace = namespaceOf(type);
      this.hash = hashOf(this.getClass());
      this.version = Injectors.getServiceVersion(this, Version.UNVERSIONED);
      this.requestHandlers = AnnotatedSubsystemFactory.createRequestHandlers(this);
      this.messageReceivedListeners = AnnotatedSubsystemFactory.createMessageReceivedListeners(this);
      this.modelAddedListeners = AnnotatedSubsystemFactory.createAddedListeners(this);
      this.modelChangedListeners = AnnotatedSubsystemFactory.createValueChangedListeners(this);
      this.attrReportListeners = AnnotatedSubsystemFactory.createAttrReportListeners(this);
      this.modelRemovedListeners = AnnotatedSubsystemFactory.createRemovedListeners(this);
      this.scheduledListeners = AnnotatedSubsystemFactory.createScheduledEventListeners(this);
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public String getNamespace() {
      return namespace;
   }

   @Override
   public Class<M> getType() {
      return type;
   }

   @Override
   public Version getVersion() {
      return version;
   }
   
   public String getHash() {
      return hash;
   }

   @Override
   public void onEvent(AddressableEvent event, SubsystemContext<M> context) {
      if(event instanceof MessageReceivedEvent) {
         MessageReceivedEvent mre = (MessageReceivedEvent) event;
         if(mre.getMessage().isRequest()) {
            handleRequest(mre, context);
         }
         else {
            fireMessageReceived(mre, context);
         }
      }
      else if(event instanceof SubsystemResponseEvent) {
         handleResponse((SubsystemResponseEvent) event, context);
      }
      else if(event instanceof ModelAddedEvent) {
         fireModelAdded((ModelAddedEvent) event, context);
      }
      else if(event instanceof ModelChangedEvent) {
         fireModelChanged((ModelChangedEvent) event, context);
      }
      else if(event instanceof ModelReportEvent) {
         fireReport((ModelReportEvent) event, context);
      }
      else if(event instanceof ModelRemovedEvent) {
         fireModelRemoved((ModelRemovedEvent) event, context);
      }
      else if(event instanceof ScheduledEvent) {
         fireScheduledEvent((ScheduledEvent) event, context);
      }
      else if(event instanceof SubsystemActivatedEvent) {
         onActivated(context);
      }
      else if(event instanceof SubsystemAddedEvent) {
         onAdded(context);
      }
      else if(event instanceof SubsystemStartedEvent) {
         onStarted(context);
      }
      else if(event instanceof SubsystemStoppedEvent) {
         onStopped(context);
      }
      else if(event instanceof SubsystemRemovedEvent) {
         onRemoved(context);
      }
      else if(event instanceof SubsystemDeactivatedEvent) {
         onDeactivated(context);
      }
      else {
         context.logger().debug("Unrecognized event {}", event);
      }
   }

   protected void onActivated(SubsystemContext<M> context) {
      context.logger().debug("{} activated", getName());
   }

   protected void onAdded(SubsystemContext<M> context) {
      context.logger().debug("{} added", getName());
   }

   protected void onStarted(SubsystemContext<M> context) {
      context.logger().debug("{} started", getName());
   }

   protected void onStopped(SubsystemContext<M> context) {
      context.logger().debug("{} stopped", getName());
   }

   protected void onRemoved(SubsystemContext<M> context) {
      context.logger().debug("{} removed", getName());
   }

   protected void onDeactivated(SubsystemContext<M> context) {
      context.logger().debug("{} deactivated", getName());
   }

   protected final void handleRequest(MessageReceivedEvent event, SubsystemContext<M> context) {
      PlatformMessage request = event.getMessage();
      String type = request.getMessageType();
      Function<SubsystemEventAndContext, MessageBody> handler = requestHandlers.get(type);
      if(handler == null) {
         context.sendResponse(request, Errors.invalidRequest(type));
      }
      else {
         MessageBody response;
         try {
            response = handler.apply(new SubsystemEventAndContext(context, event));
         }
         catch(Exception e) {
            context.logger().warn("Error handling request {}", request, e);
            response = Errors.fromException(e);
         }
         if(response != null) {
            context.sendResponse(request, response);
         }
      }
   }

   @SuppressWarnings("unchecked")
	protected final void handleResponse(SubsystemResponseEvent event, SubsystemContext<M> context) {
      if(event.isTimeout()) {
         ((SubsystemContext.ResponseAction<M>) event.getAction()).onTimeout(context);
      }
      else if(event.getResponse() != null) {
         ((SubsystemContext.ResponseAction<M>) event.getAction()).onResponse(context, event.getResponse());
      }
      else {
      	((SubsystemContext.ResponseAction<M>) event.getAction()).onError(context, event.getError());
      }
   }

   protected final void fireMessageReceived(MessageReceivedEvent event, SubsystemContext<M> context) {
      if(messageReceivedListeners.hasListeners()) {
         messageReceivedListeners.fireEvent(new SubsystemEventAndContext(context, event));
      }
   }

   protected final void fireModelAdded(ModelAddedEvent event, SubsystemContext<M> context) {
      if(modelAddedListeners.hasListeners()) {
         modelAddedListeners.fireEvent(new SubsystemEventAndContext(context, event));
      }
   }

   protected final void fireModelChanged(ModelChangedEvent event, SubsystemContext<M> context) {
      if(modelChangedListeners.hasListeners()) {
         modelChangedListeners.fireEvent(new SubsystemEventAndContext(context, event));
      }
   }

   protected final void fireReport(ModelReportEvent event, SubsystemContext<M> context) {
      if(attrReportListeners.hasListeners()) {
         attrReportListeners.fireEvent(new SubsystemEventAndContext(context, event));
      }
   }

   protected final void fireModelRemoved(ModelRemovedEvent event, SubsystemContext<M> context) {
      if(modelRemovedListeners.hasListeners()) {
         modelRemovedListeners.fireEvent(new SubsystemEventAndContext(context, event));
      }
   }
   
   protected final void fireScheduledEvent(ScheduledEvent event, SubsystemContext<M> context) {
      if(scheduledListeners.hasListeners()) {
         scheduledListeners.fireEvent(new SubsystemEventAndContext(context, event));
      }
   }

   @SuppressWarnings("unchecked")
	private static <M extends SubsystemModel> Class<M> typeOf(Class<?> type) {
      com.iris.common.subsystem.annotation.Subsystem annotation = type.getAnnotation(com.iris.common.subsystem.annotation.Subsystem.class);
      if(annotation == null) {
         throw new IllegalStateException("Must add an @Subsystem annotation");
      }
      return (Class<M>) annotation.value();
   }

   private static <M extends SubsystemModel> String hashOf(Class<?> type) {
      try(InputStream is = type.getResourceAsStream(type.getSimpleName() + ".class")) {
         return Utils.shortHash(is);
      }
      catch(Exception e) {
         logger.warn("Unable to determine hash for class {}", type);
         return "";
      }
   }

   private static String nameOf(Class<? extends SubsystemModel> type) {
      try {
         return (String) type.getField("NAME").get(null);
      }
      catch (IllegalArgumentException | IllegalAccessException
            | NoSuchFieldException | SecurityException e) {
         throw new IllegalArgumentException("Unable to retrieve NAME from the model class " + type, e);
      }
   }

   private static String namespaceOf(Class<? extends SubsystemModel> type) {
      try {
         return (String) type.getField("NAMESPACE").get(null);
      }
      catch (IllegalArgumentException | IllegalAccessException
            | NoSuchFieldException | SecurityException e) {
         throw new IllegalArgumentException("Unable to retrieve NAMESPACE from the model class " + type, e);
      }
   }

   
}

