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
package com.iris.platform.rule.environment;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.common.rule.action.Action;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.event.ModelRemovedEvent;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.scene.Scene;
import com.iris.common.scene.SceneContext;
import com.iris.core.platform.AnalyticsMessageBus;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.handlers.AddTagsModelRequestHandler;
import com.iris.core.platform.handlers.GetAttributesModelRequestHandler;
import com.iris.core.platform.handlers.RemoveTagsModelRequestHandler;
import com.iris.core.platform.handlers.SetAttributesModelRequestHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.SceneModel;
import com.iris.messages.type.ActionTemplate;
import com.iris.platform.rule.analytics.SceneAnalyticsWrapper;
import com.iris.platform.scene.catalog.SceneCatalog;
import com.iris.platform.scene.catalog.SceneTemplate;
import com.iris.platform.scene.resolver.ActionResolver;

/**
 * Responsible for handling events to a single scene.  This
 * helps to bind the scene and the definition together.
 *  
 */
public class SceneHandler implements PlaceEventHandler {
   private volatile Scene scene;
   
   private final SceneContext context;
   private final AnalyticsMessageBus analyticsBus;
   private final Map<String, ContextualRequestMessageHandler<Model>> modelHandlers;
   private final SceneCatalog sceneCatalog;
   
   // last known state of satisfiability
   private boolean available;
   
   public SceneHandler(
      SceneContext context,
      GetAttributesModelRequestHandler getAttributesHandler,
      SetAttributesModelRequestHandler setAttributesHandler,
      AddTagsModelRequestHandler addTagsHandler,
      RemoveTagsModelRequestHandler removeTagsHandler,
      AnalyticsMessageBus analyticsBus, 
      SceneCatalog sceneCatalog
   ) {
      this.context = context;
      this.analyticsBus = analyticsBus;
      this.sceneCatalog = sceneCatalog;
      this.modelHandlers = 
            ImmutableMap
               .<String, ContextualRequestMessageHandler<Model>>builder()
               .put(getAttributesHandler.getMessageType(), getAttributesHandler)
               .put(setAttributesHandler.getMessageType(), setAttributesHandler)
               .put(addTagsHandler.getMessageType(), addTagsHandler)
               .put(removeTagsHandler.getMessageType(), removeTagsHandler)
               .build();
      rebuild();
   }
   
   public boolean isDeleted() {
      return getContext().isDeleted();
   }
   
   public SceneContext getContext() {
      return context;
   }

   public Address getAddress() {
      return context.model().getAddress();
   }
   
   public void start() {
      // no-op
   }
   
   public void stop() {
      // no-op
   }

   public void onEvent(RuleEvent event) {
      try {
         boolean wasAvailable = this.available;
         if(isAvailable()) {
            this.available = true;
            if(!wasAvailable) {
               context
                  .logger()
                  .debug("Scene {} became available because of event {}", scene.getAddress(), event);
               available = true;
               SceneModel.setEnabled(scene.getContext().model(), isAvailable());
            }
         } 
         else if(wasAvailable) {
            this.available = false;
            context
               .logger()
               .debug("Scene {} became unavailable because of event {}", scene.getAddress(), event);
            SceneModel.setEnabled(scene.getContext().model(), false);
         }
         if(event instanceof ModelRemovedEvent) {
            onDeleted(((ModelRemovedEvent) event).getModel().getAddress().getRepresentation());
         }else if(event instanceof AttributeValueChangedEvent) {
        	 AttributeValueChangedEvent valueChangeEvent = (AttributeValueChangedEvent)event;
        	 if(valueChangeEvent.getAddress().equals(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE)) && PlaceCapability.ATTR_SERVICELEVEL.equals(valueChangeEvent.getAttributeName())){
        		 //place service level change, rebuild 
        		 rebuild();
        	 }
         }
      } 
      catch(Exception e) {
         context.logger().warn("Error dispatching [{}]", event, e);
      }
      finally {
         context.commit();
      }
   }
   
   // TODO collapse this all into the dispatcher
   public MessageBody handleRequest(PlatformMessage message) {
      try {
         final String messageType = message.getMessageType();
         if(Capability.CMD_SET_ATTRIBUTES.equals(messageType)) {
            validate(message.getValue());
            return modelHandlers.get(messageType).handleRequest(context.model(), message);
         }
         else if(modelHandlers.containsKey(messageType)) {
            return modelHandlers.get(messageType).handleRequest(context.model(), message);
         }
         else if(messageType.equals(SceneCapability.FireRequest.NAME)) {
        	 context.setActor(message.getActor());
            return fire();
         }
         else if(messageType.equals(SceneCapability.DeleteRequest.NAME)) {
            return delete();
         }
         else {
            return Errors.unsupportedMessageType(messageType);
         }
      }
      finally {
         afterRequest();
      }
   }

   public boolean isAvailable() {
      return 
            scene != null && 
            scene.isSatisfiable();
   }

   // TODO turn this into a ContextualRequestHandler<Scene>?
   protected MessageBody fire() {
      if(!isAvailable()) {
         return Errors.invalidRequest("Scene is disabled");
      }
      context.model().setAttribute(SceneCapability.ATTR_LASTFIRETIME, new Date());
      scene.execute();
      return SceneCapability.FireResponse.instance();
   }

   // TODO turn this into a ContextualRequestHandler<Scene>?
   protected MessageBody delete() {
      context.delete();
      return SceneCapability.DeleteResponse.instance();
   }
   
   /**
    * Called when one of the models in the context is deleted
    * @param address
    */
   protected void onDeleted(String address) {
      boolean rebuildRequired = false;
      List<Map<String, Object>> actions = new ArrayList<>(SceneModel.getActions(context.model(), ImmutableList.of()));
      // scan the actions for context that references the address, removing them as we go
      for(int i = 0; i < actions.size(); i++) {
         com.iris.messages.type.Action action = new com.iris.messages.type.Action(actions.get(i));
         if(action.getContext().containsKey(address)) {
            rebuildRequired = true;
            if(action.getContext().size() == 1) {
               actions.remove(i);
               i--; // kind of weird to decrement the loop counter, but should work
            }
            else {
               Map<String, Map<String, Object>> context = new HashMap<String, Map<String, Object>>(action.getContext());
               context.remove(address);
               action.setContext(context);
               actions.set(i, action.toMap());
            }
         }
      }
      
      if(rebuildRequired) {
         context.logger().debug("Rebuilding scene because [{}] was deleted", address);
         SceneModel.setActions(context.model(), actions);
         rebuild();
      }
   }
   
   protected void validate(MessageBody setAttributes) {
      Map<String, Object> attributes = setAttributes.getAttributes();
      if(requiresRebuild(attributes)) {
         List<Map<String, Object>> actions = 
               attributes.containsKey(SceneCapability.ATTR_ACTIONS) ?
                     (List<Map<String, Object>>) SceneCapability.TYPE_ACTIONS.coerce(attributes.get(SceneCapability.ATTR_ACTIONS)) :
                     SceneModel.getActions(context.model());
         //For each action, determine the premium flag in the corresponding actionTemplate rather than the one sent back from the client
         String sceneTemplateId = SceneModel.getTemplate(context.model());        
         if(actions != null && actions.size() > 0) {     
        	 SceneTemplate curTemplate = sceneCatalog.getById(sceneTemplateId);
	         for(Map<String, Object> curAction : actions) {	        	 
	        	 ActionResolver actionResolver = curTemplate.getAction((String)curAction.get(com.iris.messages.type.Action.ATTR_TEMPLATE));
	        	 ActionTemplate actionTemplate = actionResolver.resolve(this.context);
	        	 curAction.put(com.iris.messages.type.Action.ATTR_PREMIUM, Boolean.TRUE.equals(actionTemplate.getPremium())) ;
	         }	             
         }
         boolean notification = 
               attributes.containsKey(SceneCapability.ATTR_NOTIFICATION) ?
                     (Boolean) SceneCapability.TYPE_NOTIFICATION.coerce(attributes.get(SceneCapability.ATTR_NOTIFICATION)) :
                     SceneModel.getNotification(context.model());
         String name =
               attributes.containsKey(SceneCapability.ATTR_NAME) ?
                     (String) SceneCapability.TYPE_NAME.coerce(attributes.get(SceneCapability.ATTR_NAME)) :
                     SceneModel.getName(context.model());
                     
         SceneActionBuilder
            .builder(context.getPlaceId())
            .withActions(actions)
            .withNotification(notification)
            .withName(name)
            .withContext(context)
            .withTemplateId(sceneTemplateId)
            .buildStrict();
      }
   }
   
   protected void afterRequest() {
      Map<String, Object> dirtyAttributes = context.model().getDirtyAttributes();
      if(requiresRebuild(dirtyAttributes)) {
         rebuild();
      }
      context.commit();
   }

   private void rebuild() {
      try {
         this.scene = new SceneAnalyticsWrapper(analyticsBus, getAddress(), getContext(), create());
         this.context.model().setAttribute(SceneCapability.ATTR_ENABLED, isAvailable());
      }
      catch(Exception e) {
         this.context.logger().warn("Unable to build scene", e);
         this.scene = new SceneAnalyticsWrapper(analyticsBus, getAddress(), getContext(), null);
         this.context.model().setAttribute(SceneCapability.ATTR_ENABLED, false);
      }
   }

   private boolean requiresRebuild(Map<String, Object> attributes) {
	   if(attributes != null) {
		   return
	         attributes.containsKey(SceneCapability.ATTR_ACTIONS) ||
	         attributes.containsKey(SceneCapability.ATTR_NAME) ||
	         attributes.containsKey(SceneCapability.ATTR_NOTIFICATION);
	   }else {
		   return false;
	   }
      
   }

   private Action create() {
      return
            SceneActionBuilder
               .builder(context.getPlaceId())
               .withActions(SceneModel.getActions(context.model()))
               .withNotification(SceneModel.getNotification(context.model()))
               .withName(SceneModel.getName(context.model()))
               .withContext(context)
               .withTemplateId(SceneModel.getTemplate(context.model()))
               .buildResilient()
               ;
   }
   
}

