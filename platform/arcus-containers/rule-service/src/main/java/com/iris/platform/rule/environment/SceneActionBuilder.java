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
package com.iris.platform.rule.environment;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeTypes;
import com.iris.common.rule.action.Action;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.action.ActionList;
import com.iris.common.rule.action.SendAction;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.NotificationCapability.NotifyRequest;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AccountModel;
import com.iris.platform.rule.service.SceneCatalogLoader;
import com.iris.platform.scene.catalog.SceneCatalog;
import com.iris.platform.scene.catalog.SceneTemplate;
import com.iris.platform.scene.resolver.ActionResolver;

/**
 * 
 */
public class SceneActionBuilder {
   private static final AttributeType TYPE_ACTIONS =
         AttributeTypes.listOf(com.iris.messages.type.Action.TYPE);
   
   private static String getAccountOwner(ActionContext context) {
      for(Model model: context.getModels()) {
         if(model.supports(AccountCapability.NAMESPACE)) {
            return AccountModel.getOwner(model);
         }
      }
      return null;
   }
   
   public static SceneActionBuilder builder(UUID placeId) {
      Preconditions.checkNotNull(placeId, "placeId may not be null");
      SceneActionBuilder builder = ServiceLocator.getInstance(SceneActionBuilder.class);
      builder.placeId = placeId;
      return builder;
   }
   
   private SceneCatalogLoader loader;
   
   private List<com.iris.messages.type.Action> actions;
   private SceneTemplate template;
   private UUID placeId;
   private ActionContext context;
   private String accountOwner;
   private String name;
   private boolean notification = false;
   
   @Inject
   SceneActionBuilder(SceneCatalogLoader loader) {
      this.loader = loader;
   }
   
   public SceneActionBuilder withNotification(boolean notification) {
      this.notification = notification;
      return this;
   }
   
   public SceneActionBuilder withContext(ActionContext context) {
      this.context = context;
      this.accountOwner = getAccountOwner(context);
      return this;
   }
   
   public SceneActionBuilder withActions(List<Map<String, Object>> actions) {
      return withActionTypes((List<com.iris.messages.type.Action>) TYPE_ACTIONS.coerce(actions));
   }

   public SceneActionBuilder withActionTypes(List<com.iris.messages.type.Action> actions) {
      this.actions = actions == null ? ImmutableList.of() : ImmutableList.copyOf(actions);
      return this;
   }
   
   public SceneActionBuilder withName(String name) {
      this.name = name;
      return this;
   }

   public SceneActionBuilder withTemplateId(String templateId) {
      Preconditions.checkNotNull(templateId, "templateId may not be null");
      SceneCatalog catalog = loader.getCatalogForPlace(placeId);
      template = catalog.getById(templateId);
      if(template == null) {
         throw new IllegalArgumentException("Unable to load scene template for id " + templateId);
      }
      return this;
   }
   
   /**
    * If any of the action specifications are invalid this will return an
    * error, whereas {@code buildResilient()} which will create any actions
    * that are possible, dropping ones with errors.
    * @return
    */
   @Nullable
   public Action buildStrict() throws IllegalStateException {
      Preconditions.checkNotNull(placeId, "Must specify placeId");
      Preconditions.checkNotNull(template, "Must specify a sceneTemplate");
      Preconditions.checkNotNull(context, "Must specify the context");
      
      ActionList.Builder builder = new ActionList.Builder();
      if(actions != null) {
         for(com.iris.messages.type.Action action: actions) {
            addAction(action, template, builder);
         }
      }
      if(notification) {
         addNotification(builder);
      }
      if(builder.isEmpty()) {
         return null;
      }
      return builder.build();
   }
   
   @Nullable
   public Action buildResilient() {
      Preconditions.checkNotNull(placeId, "Must specify placeId");
      Preconditions.checkNotNull(template, "Must specify a sceneTemplate");
      Preconditions.checkNotNull(name, "Must specify a name for the scene");
      Preconditions.checkNotNull(context, "Must specify the context");
      
      boolean isPremiumPlace = context.isPremium();
      ActionList.Builder builder = new ActionList.Builder();
      if(actions != null) {
         for(com.iris.messages.type.Action action: actions) {
            try {
               if(!Boolean.TRUE.equals(action.getPremium()) || isPremiumPlace) {
            	   addAction(action, template, builder);
               }else{
            	   context.logger().debug("Scene action [{}] for scene [{}] is skipped because place [{}] requires PREMIUM service level", action.getName(), template.getName(), context.getPlaceId());
               }
            }
            catch(Exception e) {
               context.logger().warn("Unable to create action for template [{}]", template, e);
            }
         }
      }
      if(notification) {
         addNotification(builder);
      }
      if(builder.isEmpty()) {
         return null;
      }
      return builder.build();
   }

   private void addAction(com.iris.messages.type.Action action, SceneTemplate template, ActionList.Builder builder) {
      ActionResolver resolver = template.getAction(action.getTemplate());
      if(resolver == null) {
         throw new IllegalArgumentException("Unable to create action " + action.getTemplate());
      }
      for(Map.Entry<String, Map<String, Object>> entry: action.getContext().entrySet()) {
         Address target = Address.fromString(entry.getKey());
         Action a = resolver.generate(context, target, entry.getValue());
         builder.addAction(a);
      }
   }

   private void addNotification(ActionList.Builder builder) {
      SendAction action = new SendAction(
            NotificationCapability.NotifyRequest.NAME,
            Functions.constant(Address.platformService(NotificationCapability.NAMESPACE)),
            ImmutableMap.<String, Object>of(
                  NotifyRequest.ATTR_PRIORITY, NotifyRequest.PRIORITY_MEDIUM,
                  NotifyRequest.ATTR_PLACEID, placeId.toString(),
                  NotifyRequest.ATTR_PERSONID, accountOwner,
                  NotifyRequest.ATTR_MSGKEY, "scene." + template.getId() + ".run",
                  NotifyRequest.ATTR_MSGPARAMS, ImmutableMap.of("_scene", name)
            )
      );
      builder.addAction(action);
   }

}

