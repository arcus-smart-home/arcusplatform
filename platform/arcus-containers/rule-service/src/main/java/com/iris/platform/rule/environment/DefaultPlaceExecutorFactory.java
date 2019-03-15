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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.common.rule.Rule;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.action.stateful.StatefulAction;
import com.iris.common.rule.condition.Condition;
import com.iris.common.scheduler.ExecutorScheduler;
import com.iris.core.platform.AnalyticsMessageBus;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.model.Model;
import com.iris.platform.model.ModelDao;
import com.iris.platform.rule.RuleDao;
import com.iris.platform.rule.RuleDefinition;
import com.iris.platform.rule.RuleEnvironment;
import com.iris.platform.rule.RuleEnvironmentDao;
import com.iris.platform.rule.analytics.SimpleRuleAnalyticsWrapper;
import com.iris.platform.rule.catalog.RuleCatalog;
import com.iris.platform.rule.catalog.RuleTemplate;
import com.iris.platform.rule.service.RuleCatalogLoader;
import com.iris.platform.scene.SceneDao;
import com.iris.util.ThreadPoolBuilder;


/**
 *
 */
@Singleton
public class DefaultPlaceExecutorFactory implements PlaceExecutorFactory {
   private static final Logger logger = LoggerFactory.getLogger(DefaultPlaceExecutorFactory.class);
   
   @Inject(optional=true)
   @Named("rule.scheduler.threads")
   private int schedulerThreads = 10;
   @Inject(optional=true)
   @Named("rule.executor.threads")
   private int poolSize = 100;
   @Inject(optional=true)
   @Named("rule.instance.maxQueueDepth")
   private int maxQueueDepth = 100;
   @Inject(optional=true)
   @Named("rule.cascade.mode")
   private String ruleCascadeMode = "DISABLE";

   private final PlatformMessageBus platformBus;
   private final AnalyticsMessageBus analyticsBus;
   private final RuleDao ruleDefDao;
   private final RuleEnvironmentDao ruleEnvDao;
   private final RuleCatalogLoader ruleCatalogLoader;
   private final SceneHandlerFactory sceneHandlerFactory;
   private final ModelDao modelDao;

   private final ThreadPoolExecutor executor;
   private final ExecutorScheduler scheduler;
   private final PlaceExecutorEventLoop eventLoop;

   @Inject
   public DefaultPlaceExecutorFactory(
         PlatformMessageBus platformBus,
         AnalyticsMessageBus analyticsBus,
         RuleDao ruleDefDao,
         RuleEnvironmentDao ruleEnvDao,
         RuleCatalogLoader ruleCatalogLoader,
         SceneDao sceneDao,
         SceneHandlerFactory sceneHandlerFactory,
         ModelDao modelDao,
         PlaceExecutorRegistry registry
   ) {
      this.platformBus = platformBus;
      this.analyticsBus = analyticsBus;
      this.ruleDefDao = ruleDefDao;
      this.ruleEnvDao = ruleEnvDao;
      this.ruleCatalogLoader = ruleCatalogLoader;
      this.sceneHandlerFactory = sceneHandlerFactory;
      this.modelDao = modelDao;

      this.executor =
            new ThreadPoolBuilder()
               .withBlockingBacklog()
               .withMaxPoolSize(poolSize)
               .withNameFormat("rule-thread-%d")
               .withMetrics("service.rule")
               .build()
               ;
      // TODO why isn't this injected?
      this.scheduler =
            new ExecutorScheduler(
               Executors.newScheduledThreadPool(
                     schedulerThreads,
                     ThreadPoolBuilder
                        .defaultFactoryBuilder()
                        .setNameFormat("rule-scheduler-%d")
                        .build()
                )
            );
      this.eventLoop = new PlaceExecutorEventLoop(registry, scheduler);
   }
   
   @PreDestroy
   public void stop() {
      this.executor.shutdownNow();
      this.scheduler.stop();
   }

   @Nullable
   @Override
   public PlaceEnvironmentExecutor load(UUID placeId) {
      RuleEnvironment environment = ruleEnvDao.findByPlace(placeId);
      if(environment == null) {
         return null;
      }
      if(environment.getRules() == null || environment.getRules().isEmpty()) {
         return new InactivePlaceExecutor(() -> this.doLoad(placeId), placeId);
      }
      else {
         return this.doLoad(placeId);
      }
   }

   @Override
   public PlaceEnvironmentExecutor reload(UUID placeId, PlaceEnvironmentExecutor executor) {
      RuleEnvironment environment = ruleEnvDao.findByPlace(placeId);
      if(environment == null) {
         logger.warn("Place [{}] can't be loaded -- has it been deleted?", placeId);
         return null;
      }
      return reload(environment, executor);
   }

   private PlaceEnvironmentExecutor reload(RuleEnvironment environment, PlaceEnvironmentExecutor executor) {
      boolean wasActive;
      DefaultPlaceExecutor theExecutor;
      if(executor instanceof InactivePlaceExecutor) {
         theExecutor = (DefaultPlaceExecutor) ((InactivePlaceExecutor) executor).delegate();
         wasActive = false;
      }
      else if(executor instanceof DefaultPlaceExecutor) {
         theExecutor = (DefaultPlaceExecutor) executor;
         wasActive = true;
      }
      else {
         throw new IllegalArgumentException("Can only reload executors created by this factory");
      }

      initialize(environment, theExecutor);
      boolean isActive = environment.getRules() != null && !environment.getRules().isEmpty();
      if(!wasActive && isActive) {
         logger.debug("Place [{}] has added a rule, switching to live executor", environment.getPlaceId());
         return theExecutor;
      }
      else if(wasActive && !isActive) {
         logger.debug("Place [{}] has removed all rules, switching to empty executor", environment.getPlaceId());
         final UUID placeId = environment.getPlaceId();
         return new InactivePlaceExecutor(() -> doLoad(placeId), placeId, theExecutor);
      }
      else {
         logger.debug("Place [{}] rule executor has been successfully rebuilt", environment.getPlaceId());
         return theExecutor;
      }
   }
   
   @Nullable
   private DefaultPlaceExecutor doLoad(UUID placeId) {
      RuleEnvironment environment = ruleEnvDao.findByPlace(placeId);
      if(environment == null) {
         return null;
      }
      RuleModelStore models = loadModels(environment);
      DefaultPlaceExecutor placeExecutor = new DefaultPlaceExecutor(platformBus, environment, models, executor, maxQueueDepth, ruleCascadeMode);
      initialize(environment, placeExecutor);
      return placeExecutor;
   }
   
   private void initialize(RuleEnvironment environment, DefaultPlaceExecutor executor) {
      TimeZone tz = getTimeZone(environment.getPlaceId(), executor.getModelStore());
      // TODO make this pluggable
      List<PlaceEventHandler> handlers = new ArrayList<>();
      {
         RuleCatalog catalog = ruleCatalogLoader.getCatalogForPlace(environment.getPlaceId());
         PlatformRuleContext.Builder builder =
               PlatformRuleContext
                  .builder()
                  .withEventLoop(eventLoop)
                  .withPlatformBus(platformBus)
                  .withPlaceId(environment.getPlaceId())
                  .withModels(executor.getModelStore())
                  .withTimeZone(tz)
                  ;
         
         for(RuleDefinition definition: environment.getRules()) {
            Address address = Address.platformService(definition.getId().getRepresentation(), RuleCapability.NAMESPACE);
            builder.withSource(address);
            builder.withVariables(definition.getVariables());
            builder.withLogger(LoggerFactory.getLogger("rules." + definition.getRuleTemplate()));
            Rule rule = instantiate(catalog, definition, builder.build(), environment, address);
            if(rule == null) {
               definition.setDisabled(true);
            }
            else {
               RuleTemplate template = catalog.getById(definition.getRuleTemplate());
               boolean premium = template != null && template.isPremium();
               handlers.add(new RuleHandler(rule, definition, ruleDefDao, premium));
            }
         }
      }
      
      List<SceneHandler> sceneHandlers = sceneHandlerFactory.create(environment, executor.getModelStore()); 
      handlers.addAll(sceneHandlers);
      
      executor.setHandlers(handlers);
   }

   private TimeZone getTimeZone(UUID placeId, RuleModelStore models) {
      String tzId = (String) models.getAttributeValue(Address.platformService(placeId, PlaceCapability.NAMESPACE), PlaceCapability.ATTR_TZID);
      TimeZone tz = TimeZone.getDefault();
      if (!StringUtils.isEmpty(tzId)) {
         try {
            tz = TimeZone.getTimeZone(tzId);
         }
         catch(Exception e) {
            logger.warn("Unable to load timezone [{}]", tzId, e);
         }
      }
      return tz;
   }

   private RuleModelStore loadModels(RuleEnvironment environment) {
      // TODO update RuleEnvironment to come populated with all the attributes
      Collection<Model> models = modelDao.loadModelsByPlace(environment.getPlaceId(), RuleModelStore.TRACKED_TYPES);
      RuleModelStore modelStore = new RuleModelStore();
      modelStore.addModel(models.stream().filter((m) -> m != null).map(Model::toMap).collect(Collectors.toList()));
      return modelStore;
   }

   private Rule instantiate(RuleCatalog catalog, RuleDefinition definition, RuleContext context, RuleEnvironment environment, Address ruleAddress) {
      try {
         return generateRule(definition, context, environment, ruleAddress);
      }
      catch(Exception e) {
         context.logger().warn("Unable to deserialize rule [{}], attempting to regenerate", ruleAddress, e);
      }

      if(catalog == null) {
         context.logger().error("Unable to regenerate rule [{}]: rule catalog unavailable", ruleAddress);
         return null;
      }
      
      // Try to regenerate the rule using the template and definition
      RuleTemplate template = catalog.getById(definition.getRuleTemplate());
      if(template == null) {
         context.logger().error("Unable to regenerate rule [{}]: rule template [{}] unavailable", ruleAddress, definition.getRuleTemplate());
         return null;
      }
      
      try {
         RuleDefinition rd = template.regenerate(definition);
         Rule rule = generateRule(rd, context, environment, ruleAddress);
         Preconditions.checkNotNull(rule, "Unable to regenerate rule");
         // The regenerated definition needs to be saved.
         ruleDefDao.save(rd);
         context.logger().info("Rule [{}] has been healed", ruleAddress);
         return rule;
      } catch (Exception e) {
         context.logger().error("Cannot regenerate rule [{}:{}]", definition.getId(), definition.getName(), e);
         return null;
      }
   }
   
   private Rule generateRule(RuleDefinition definition, RuleContext context, RuleEnvironment environment, Address ruleAddress) {
      Condition condition = definition.createCondition(environment);
      StatefulAction action = definition.createAction(environment);
      
      return new SimpleRuleAnalyticsWrapper(analyticsBus, definition, context, condition, action, ruleAddress);
   }

}

