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
package com.iris.platform.rule.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.AbstractPlatformMessageListener;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.capability.RuleTemplateCapability;
import com.iris.messages.capability.RuleTemplateCapability.CreateRuleRequest;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Model;
import com.iris.messages.services.PlatformConstants;
import com.iris.metrics.IrisMetrics;
import com.iris.platform.model.ModelDao;
import com.iris.platform.partition.PartitionChangedEvent;
import com.iris.platform.partition.PartitionListener;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.PlatformPartition;
import com.iris.platform.rule.RuleDao;
import com.iris.platform.rule.RuleDefinition;
import com.iris.platform.rule.RuleEnvironmentDao;
import com.iris.platform.rule.catalog.RuleCatalog;
import com.iris.platform.rule.catalog.RuleTemplate;
import com.iris.platform.rule.catalog.selector.ListSelector;
import com.iris.platform.rule.catalog.selector.Selector;
import com.iris.platform.rule.environment.PlaceEnvironmentExecutor;
import com.iris.platform.rule.environment.PlaceExecutorRegistry;
import com.iris.platform.rule.environment.RuleModelStore;
import com.iris.platform.rule.service.handler.rule.ListHistoryEntriesHandler;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.validators.ValidationException;

/**
 *
 */
@Singleton
public class RuleService extends AbstractPlatformMessageListener implements PartitionListener {
   public static final String PROP_THREADPOOL = "service.rule.threadpool";
   
   private static final Logger logger = LoggerFactory.getLogger(RuleService.class);

   private final Timer partitionLoadTimer;
   
   private final RuleEnvironmentDao ruleDao;
   private final PlatformMessageBus platformBus;
   private final Partitioner partitioner;
   private final RuleDao ruleDefDao;
   private final ModelDao modelDao;
   private final PlaceDAO placeDao;
   private final RuleCatalogLoader catalogs;
   private final PlaceExecutorRegistry registry;   
   private final ListHistoryEntriesHandler listHistoryEntries;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public RuleService(
         @Named(PROP_THREADPOOL) Executor executor,
         RuleEnvironmentDao ruleDao,
         PlatformMessageBus platformBus,
         Partitioner partitioner,
         RuleDao ruleDefDao,
         ModelDao modelDao,
         PlaceDAO placeDao,
         RuleCatalogLoader catalogs,
         PlaceExecutorRegistry environments,
         ListHistoryEntriesHandler listHistoryEntries,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      super(platformBus, executor);
      this.partitionLoadTimer = IrisMetrics.metrics("service.rules").timer("partitionloadtime");
      
      this.ruleDao = ruleDao;
      this.platformBus = platformBus;
      this.partitioner = partitioner;
      this.ruleDefDao = ruleDefDao;
      this.modelDao = modelDao;
      this.placeDao = placeDao;
      this.catalogs = catalogs;
      this.registry = environments;      
      this.listHistoryEntries = listHistoryEntries;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   protected void onStart() {
      partitioner.addPartitionListener(this);

      // then start listening
      listen();
   }

   protected void listen() {
      addListeners(
            AddressMatchers.equals(Address.broadcastAddress()),
            AddressMatchers.platformService(MessageConstants.SERVICE, PlatformConstants.SERVICE_RULE),
            AddressMatchers.platformService(MessageConstants.SERVICE, PlatformConstants.SERVICE_RULE_TMPL)
      );
   }
   
   @Override
   public void onPartitionsChanged(PartitionChangedEvent event) {
      logger.info("Loading rules for [{}] partitions...", event.getPartitions().size());
      registry.clear();
      for(PlatformPartition partition: event.getPartitions()) {
         executor().execute(() -> loadRulesByPartition(partition));
      }
   }

   @Override
   protected void handleEvent(PlatformMessage message) {

      if(Capability.EVENT_DELETED.equals(message.getMessageType())) {
         Address source = message.getSource();
         if(source instanceof PlatformServiceAddress && PlatformConstants.SERVICE_PLACES.equals(source.getGroup())) {
            onPlaceDeleted((UUID) source.getId());
            return;
         }
      }

      UUID placeId = getPlaceId(message);
      if(placeId == null) {
         return;
      }

      PlaceEnvironmentExecutor executor = registry.getExecutor(placeId).orNull();
      if(executor == null) {
         return;
      }

      executor.onMessageReceived(message);
   }

   private void onPlaceDeleted(UUID placeId) {
      // remove the executor and stop its execution
      registry.stop(placeId);

      // remove all rules, scenes and actions for the place
      ruleDao.deleteByPlace(placeId);
   }

   @Override
   protected void handleRequestAndSendResponse(PlatformMessage message) {
      // many rule service responses are handled in the rule service handler, which
      // will send a response itself, so allow null response here
      getMessageBus().invokeAndSendIfNotNull(message, () -> Optional.ofNullable(handleRequest(message)));
   }

   @Override
   protected MessageBody handleRequest(PlatformMessage message) throws Exception {
      UUID placeId = message.getPlaceId() == null ? null : UUID.fromString(message.getPlaceId());
      Address destination = message.getDestination();
      
      // dispatch to the specific rule -- move more in here
      if(
            RuleCapability.EnableRequest.NAME.equals(message.getMessageType()) ||
            RuleCapability.DisableRequest.NAME.equals(message.getMessageType())
      ) {
         PlaceEnvironmentExecutor executor = registry.getExecutor(placeId).orNull();
         if(executor == null) {
            return Errors.notFound(message.getDestination());
         }
         executor.handleRequest(message);
         return null;
      }
      
      // rule service requests
      if(com.iris.messages.service.RuleService.ListRuleTemplatesRequest.NAME.equals(message.getMessageType())) {
         return handleListRuleTemplates(message.getValue());
      }
      if(com.iris.messages.service.RuleService.ListRulesRequest.NAME.equals(message.getMessageType())) {
         return handleListRules(message.getValue());
      }
      if(com.iris.messages.service.RuleService.GetCategoriesRequest.NAME.equals(message.getMessageType())) {
         return handleGetCategories(message.getValue());
      }
      if(com.iris.messages.service.RuleService.GetRuleTemplatesByCategoryRequest.NAME.equals(message.getMessageType())) {
         return handleGetRuleTemplatesByCategory(message.getValue());
      }
      
      // rule template requests
      if(RuleTemplateCapability.ResolveRequest.NAME.equals(message.getMessageType())) {
      	MessageBody body = message.getValue();
      	assertPlaceMatches(placeId, destination, body);
         return handleResolve(message.getDestination().getId(), body);
      }
      if(RuleTemplateCapability.CreateRuleRequest.NAME.equals(message.getMessageType())) {
      	MessageBody body = message.getValue();
      	assertPlaceMatches(placeId, destination, body);
         return handleCreateRule(message.getDestination(), message.getDestination().getId(), body);
      }
      if(com.iris.messages.service.RuleService.ListRulesRequest.NAME.equals(message.getMessageType())) {
      	MessageBody body = message.getValue();
      	assertPlaceMatches(placeId, destination, body);
         return handleListRules(message.getValue());
      }
      
      // rule requests
      if(RuleCapability.DeleteRequest.NAME.equals(message.getMessageType())) {
         return handleDeleteRule(placeId, message.getDestination(), message.getValue());
      }
      if(RuleCapability.UpdateContextRequest.NAME.equals(message.getMessageType())) {
         return handleUpdateRuleContext(placeId, message.getDestination(), message.getValue());
      }
      if(RuleCapability.ListHistoryEntriesRequest.NAME.equals(message.getMessageType())) {
         return handleListHistoryEntries(message.getDestination(), message);
      }
      
      // base requests -- could be rule or template
      if(Capability.CMD_GET_ATTRIBUTES.equals(message.getMessageType())) {
         return handleGetAttributes(message.getDestination(), message.getValue(), message.getPlaceId());
      }
      if(Capability.CMD_SET_ATTRIBUTES.equals(message.getMessageType())) {
         return handleSetAttributes(placeId, message.getDestination(), message.getValue());
      }
      return super.handleRequest(message);
   }

   protected void loadRulesByPartition(PlatformPartition partition) {
      try(Timer.Context context = partitionLoadTimer.time()) {
         placeDao
            .streamByPartitionId(partition.getId())
            .forEach((place) -> registry.start(place.getId()));
      }
   }

   // TODO optimize this
   private UUID getPlaceId(PlatformMessage message) {
      String placeId = message.getPlaceId();
      return placeId == null ? null : UUID.fromString(placeId);
   }

   private UUID getPlaceId(MessageBody body) {
      if(body.getAttributes().get(com.iris.messages.service.RuleService.ListRuleTemplatesRequest.ATTR_PLACEID) != null) {
         return UUID.fromString((String) body.getAttributes().get(com.iris.messages.service.RuleService.ListRuleTemplatesRequest.ATTR_PLACEID));
      }
      return null;
   }
   
   // FIXME this results in a lot of duplicated checks -- should consolidate this logic in future revisions
   private void assertPlaceMatches(UUID placeId, Address destination, MessageBody body) {
   	if(placeId == null) {
   		return;
   	}
   	if(!Objects.equals(placeId, getPlaceId(body))) {
   		throw new UnauthorizedRequestException(destination, "Unauthorized access to " + destination);
   	}
   }

   private MessageBody handleListRuleTemplates(MessageBody body) {
      UUID placeId = getPlaceId(body);
      // TODO:  replace with better error events
      Preconditions.checkNotNull(placeId, "The place ID is required");

      RuleCatalog catalog = getRuleCatalogForPlace(placeId);

      List<RuleTemplate> templates = catalog.getTemplates();
      RuleContext context = createSimpleRuleContext(placeId);

      return com.iris.messages.service.RuleService.ListRuleTemplatesResponse.builder()
            .withRuleTemplates(templates.stream().map((rt) -> templateToMap(rt, context)).collect(Collectors.toList()))
            .build();
   }

   private Map<String,Object> templateToMap(RuleTemplate template, RuleContext ruleContext) {
      Map<String,Object> asMap = new HashMap<>();
      asMap.put(Capability.ATTR_ADDRESS, MessageConstants.SERVICE + ":" + RuleTemplateCapability.NAMESPACE + ":" + template.getId());
      asMap.put(Capability.ATTR_CAPS, ImmutableSet.of(Capability.NAMESPACE, RuleTemplateCapability.NAMESPACE));
      asMap.put(Capability.ATTR_ID, template.getId());
      asMap.put(Capability.ATTR_TAGS, template.getTags());
      asMap.put(Capability.ATTR_TYPE, RuleTemplateCapability.NAMESPACE);
      asMap.put(RuleTemplateCapability.ATTR_ADDED, template.getCreated());
      asMap.put(RuleTemplateCapability.ATTR_KEYWORDS, template.getKeywords());
      asMap.put(RuleTemplateCapability.ATTR_LASTMODIFIED, template.getModified());
      asMap.put(RuleTemplateCapability.ATTR_TEMPLATE, template.getTemplate());
      asMap.put(RuleTemplateCapability.ATTR_SATISFIABLE, ruleContext != null && template.isSatisfiable(ruleContext));
      asMap.put(RuleTemplateCapability.ATTR_NAME, template.getName());
      asMap.put(RuleTemplateCapability.ATTR_DESCRIPTION, template.getDescription());
      asMap.put(RuleTemplateCapability.ATTR_CATEGORIES, template.getCategories());
      asMap.put(RuleTemplateCapability.ATTR_PREMIUM, template.isPremium());
      asMap.put(RuleTemplateCapability.ATTR_EXTRA, template.getExtra());
      return asMap;
   }

   private MessageBody handleListRules(MessageBody body) {
      UUID placeId = getPlaceId(body);

      // TODO:  replace with better error events
      Preconditions.checkNotNull(placeId, "The place ID is required");

      List<RuleDefinition> definitions = ruleDefDao.listByPlace(placeId);
      return com.iris.messages.service.RuleService.ListRulesResponse.builder()
            .withRules(definitions.stream().map((r) -> ruleToMap(r)).collect(Collectors.toList()))
            .build();
   }

   private Map<String,Object> ruleToMap(RuleDefinition rd) {
      Map<String,Object> asMap = new HashMap<>();
      asMap.put(Capability.ATTR_ID, rd.getId().getRepresentation());
      asMap.put(Capability.ATTR_ADDRESS, Address.platformService(rd.getId().getRepresentation(), RuleCapability.NAMESPACE).getRepresentation());
      asMap.put(Capability.ATTR_TYPE, RuleCapability.NAMESPACE);
      asMap.put(Capability.ATTR_CAPS, ImmutableSet.of(Capability.NAMESPACE, RuleCapability.NAMESPACE));
      asMap.put(RuleCapability.ATTR_CREATED, rd.getCreated());
      asMap.put(RuleCapability.ATTR_MODIFIED, rd.getModified());
      asMap.put(RuleCapability.ATTR_NAME, rd.getName());
      asMap.put(RuleCapability.ATTR_STATE, rd.isDisabled() ? RuleCapability.STATE_DISABLED : RuleCapability.STATE_ENABLED);
      asMap.put(RuleCapability.ATTR_DESCRIPTION, rd.getDescription());
      asMap.put(RuleCapability.ATTR_TEMPLATE, rd.getRuleTemplate());
      asMap.put(RuleCapability.ATTR_CONTEXT, rd.getVariables());
      return asMap;
   }

   private MessageBody handleGetCategories(MessageBody body) {
      UUID placeId = getPlaceId(body);

      // TODO:  replace with better error events
      Preconditions.checkNotNull(placeId, "The place ID is required");

      RuleCatalog catalog = getRuleCatalogForPlace(placeId);

      return com.iris.messages.service.RuleService.GetCategoriesResponse.builder()
         .withCategories(catalog.getRuleCountByCategory())
         .build();
   }

   private MessageBody handleGetRuleTemplatesByCategory(MessageBody body) {
      UUID placeId = getPlaceId(body);
      String category = com.iris.messages.service.RuleService.GetRuleTemplatesByCategoryRequest.getCategory(body);

      // TODO:  replace with better error events
      Preconditions.checkNotNull(placeId, "The place ID is required");
      Preconditions.checkNotNull(category, "The category is required");

      RuleCatalog catalog = getRuleCatalogForPlace(placeId);
      RuleContext context = createSimpleRuleContext(placeId);

      List<RuleTemplate> templates = catalog.getTemplatesForCategory(category);

      return com.iris.messages.service.RuleService.GetRuleTemplatesByCategoryResponse.builder()
            .withRuleTemplates(templates.stream().map((t) -> templateToMap(t, context)).collect(Collectors.toList()))
            .build();
   }

   private MessageBody handleResolve(Object templateId, MessageBody body) {
      // TODO:  replace with better error events
      Preconditions.checkNotNull(templateId, "The template ID is required from the destination address");

      UUID placeId = getPlaceId(body);
      // TODO:  replace with better error events
      Preconditions.checkNotNull(placeId, "The place ID is required");

      RuleCatalog catalog = getRuleCatalogForPlace(placeId);
      RuleTemplate template = catalog.getById((String) templateId);

      // TODO:  replace with better error events
      Preconditions.checkNotNull(template, "No template could be found with " + templateId);

      RuleContext context = createSimpleRuleContext(placeId);

      Map<String,Selector> resolution = template.resolve(context);
      Map<String,Map<String,Object>> transformed = new HashMap<>();
      resolution.entrySet().forEach((e) -> {
         transformed.put(e.getKey(), selectorToMap(e.getValue()));
      });

      return RuleTemplateCapability.ResolveResponse.builder()
            .withSelectors(transformed)
            .build();
   }

   private RuleContext createSimpleRuleContext(UUID placeId) {
      PlaceEnvironmentExecutor executor = registry.getExecutor(placeId).orNull();
      Collection<Model> models = executor != null ? 
            executor.getModelStore().getModels() : 
            modelDao.loadModelsByPlace(placeId, RuleModelStore.TRACKED_TYPES);

      // use a simple context just for resolving
      SimpleContext context = new SimpleContext(placeId, Address.platformService("rule"), LoggerFactory.getLogger("rules." + placeId));
      models.stream().filter((m) -> m != null).forEach((m) -> context.putModel(m));
      return context;
   }

   private Map<String,Object> selectorToMap(Selector selector) {
      Map<String,Object> asMap = new HashMap<>();
      asMap.put("type", selector.getType());
      if(selector instanceof ListSelector) {
         ListSelector optionSelector = (ListSelector) selector;
         List<List> options = new ArrayList<>();
         optionSelector.getOptions().forEach((o) -> {
            options.add(Arrays.asList(o.getLabel(), o.getValue()));
         });
         asMap.put("options", options);
      }
      return asMap;
   }

   private MessageBody handleCreateRule(Address destination, Object templateId, MessageBody message) {
      Errors.assertRequiredParam(templateId, "templateId");

      UUID placeId = getPlaceId(message);
      String name = RuleTemplateCapability.CreateRuleRequest.getName(message);
      String description = RuleTemplateCapability.CreateRuleRequest.getDescription(message);
      Errors.assertRequiredParam(placeId, "placeId");
      Errors.assertRequiredParam(name, CreateRuleRequest.ATTR_NAME);

      RuleCatalog catalog = getRuleCatalogForPlace(placeId);
      RuleTemplate template = catalog.getById((String) templateId);

      if(templateId == null) {
         throw new ErrorEventException(Errors.notFound(Address.platformService(templateId, RuleTemplateCapability.NAMESPACE)));
      }
      
      Map<String,Object> variables = RuleTemplateCapability.CreateRuleRequest.getContext(message);
      PlaceEnvironmentExecutor executor = registry.getExecutor(placeId).orNull();
      if(executor == null) {
         return Errors.notFound(destination);
      }

      try {
         Callable<RuleDefinition> save = () -> doCreateRule(placeId, template, name, description, variables);
         com.google.common.base.Optional<PlaceEnvironmentExecutor> executorRef = registry.getExecutor(placeId);
         RuleDefinition rule;
         if(executorRef.isPresent()) {
            rule = executorRef.get().submit(save).get();
         }
         else {
            rule = save.call();
            registry.reload(placeId);
         }

         return 
               RuleTemplateCapability.CreateRuleResponse
                  .builder()
                  .withAddress(rule.getAddress())
                  .build();

      }
      catch(Exception e) {
         logger.warn("Error creating a new rule for place [{}] for template [{}] using variables [{}]", placeId, templateId, variables, e);
         return Errors.fromException(e);
      }
   }
   
   // NOTE this must run in the executor thread
   private RuleDefinition doCreateRule(UUID placeId, RuleTemplate template, String name, String description, Map<String, Object> variables) throws ValidationException {
      RuleDefinition rd = template.create(
            placeId,
            name,
            variables
      );
      if(StringUtils.isEmpty(description)) {
         rd.setDescription(template.getDescription());
      }
      else {
         rd.setDescription(description);
      }
      ruleDefDao.save(rd);
      registry.reload(placeId);
      
      Map<String,Object> ruleDefAsMap = ruleToMap(rd);
      MessageBody added = MessageBody.buildMessage(Capability.EVENT_ADDED, ruleDefAsMap);
      PlatformMessage msg =
            PlatformMessage
               .buildBroadcast(added, Address.platformService(placeId + "." + rd.getSequenceId(), PlatformConstants.SERVICE_RULE))
               .withPlaceId(rd.getPlaceId())
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
               .create();
      platformBus.send(msg);
      
      return rd;
   }

   private MessageBody handleDeleteRule(UUID placeId, Address ruleAddress, MessageBody message) {
      PlaceEnvironmentExecutor executor = registry.getExecutor(placeId).orNull();
      if(executor == null) {
         logger.debug("Received delete for rule at empty place [{}]", ruleAddress);
         return RuleCapability.DeleteRequest.instance();
      }
      
      try {
         executor
            .submit(() -> doDelete(ruleAddress))
            .get();
         return RuleCapability.DeleteResponse.instance();
      }
      catch(Exception e) {
         logger.warn("Error deleting rule [{}]", ruleAddress, e);
         return Errors.fromException(e);
      }
   }

   // NOTE this must run in the executor thread
   private void doDelete(Address ruleAddress) {
      RuleDefinition rd = loadRuleDefintion((PlatformServiceAddress) ruleAddress);
      if(rd != null) {
         ruleDefDao.delete(rd.getPlaceId(), rd.getSequenceId());
         PlatformMessage msg =
               PlatformMessage
                  .builder()
                  .broadcast()
                  .from(Address.platformService(rd.getPlaceId(), PlatformConstants.SERVICE_RULE, rd.getSequenceId()))
                  .withPlaceId(rd.getPlaceId())
                  .withPopulation(populationCacheMgr.getPopulationByPlaceId(rd.getPlaceId()))
                  .withPayload(Capability.EVENT_DELETED)
                  .create();
         platformBus.send(msg);
         registry.reload(rd.getPlaceId());
      }
   }

   private MessageBody handleGetAttributes(Address ruleAddress, MessageBody message, String placeId) {
      Errors.assertRequiredParam(placeId, "placeId");

      PlatformServiceAddress addr = (PlatformServiceAddress) ruleAddress;
      Collection<String> nameColl = (Collection<String>) message.getAttributes().get("names");
      Set<String> names = nameColl == null ? Collections.<String>emptySet() : new HashSet<String>(nameColl);

      Map<String,Object> attrs = new HashMap<>();

      if(addr.getGroup().equals(PlatformConstants.SERVICE_RULE_TMPL)) {
         RuleCatalog catalog = getRuleCatalogForPlace(UUID.fromString(placeId));
         RuleTemplate rt = catalog.getById((String) addr.getId());

         // TODO:  better error event
         Preconditions.checkNotNull(rt, "No rule template can be found for " + addr.getId());

         // TODO:  without the place, we cannot determine satisfiability either
         attrs = templateToMap(rt,  null);

      } else if(addr.getGroup().equals(PlatformConstants.SERVICE_RULE)) {
         RuleDefinition ruleDef = loadRuleDefintion(addr);

         // TODO:  better error event
         Preconditions.checkNotNull(ruleDef, "No rule could be found for " + addr.getRepresentation());

         attrs = ruleToMap(ruleDef);
      }


      return MessageBody.buildMessage(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, filter(attrs, names));
   }

   private Map<String,Object> filter(Map<String,Object> attrs, Set<String> caps) {
      if(caps == null || caps.isEmpty()) {
         return attrs;
      }

      return attrs.entrySet().stream()
            .filter((e) -> {
               return caps.contains(e.getKey()) || caps.contains(e.getKey().split(":")[0]);
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
   }

   private MessageBody handleSetAttributes(UUID placeId, Address ruleAddress, MessageBody message) {
      PlatformServiceAddress addr = (PlatformServiceAddress) ruleAddress;
      if(addr.getGroup().equals(PlatformConstants.SERVICE_RULE_TMPL)) {
         return ErrorEvent.fromCode("error.attribute.not_writable", "Rule templates do not have any writable attributes");
      }
      
      PlaceEnvironmentExecutor executor = registry.getExecutor(placeId).orNull();
      if(executor == null) {
         return Errors.notFound(ruleAddress);
      }
      
      Set<String> readOnlyKeys = message.getAttributes().keySet().stream().filter((k) -> {
         return !RuleCapability.ATTR_NAME.equals(k) && !RuleCapability.ATTR_DESCRIPTION.equals(k);
      })
      .collect(Collectors.toSet());

      if(!readOnlyKeys.isEmpty()) {
         return ErrorEvent.fromCode("error.attribute.not_writable", readOnlyKeys.toString() + " are not writable attributes");
      }

      RuleDefinition rd = loadRuleDefintion(addr);
      Map<String,Object> attributesSet = new HashMap<>();
      String name = RuleCapability.getName(message);
      if(name != null) {
         attributesSet.put(RuleCapability.ATTR_NAME, name);
         rd.setName(name);
      }
      String desc = RuleCapability.getDescription(message);
      if(desc != null) {
         attributesSet.put(RuleCapability.ATTR_DESCRIPTION, desc);
         rd.setDescription(desc);
      }

      if(!attributesSet.isEmpty()) {
         try {
            executor
               .submit(() -> doUpdate(rd, attributesSet))
               .get();
         }
         catch(Exception e) {
            logger.warn("Error updating rule [{}]", ruleAddress, e);
            return Errors.fromException(e);
         }
      }

      return MessageBody.emptyMessage();
   }

   private MessageBody handleUpdateRuleContext(UUID placeId, Address ruleAddress, MessageBody message) {
      PlaceEnvironmentExecutor executor = registry.getExecutor(placeId).orNull();
      if(executor == null) {
         return Errors.notFound(ruleAddress);
      }
      
      RuleDefinition rd = loadRuleDefintion((PlatformServiceAddress) ruleAddress);
      if(rd == null) {
         return Errors.notFound(ruleAddress);
      }
      
      Map<String,Object> variables = new HashMap<>(rd.getVariables());
      Map<String,Object> newVariables = RuleCapability.UpdateContextRequest.getContext(message);
      newVariables.entrySet().forEach((e) -> variables.put(e.getKey(), e.getValue()));
      rd.setVariables(variables);

      ImmutableMap.Builder<String, Object> changesBuilder =
         ImmutableMap.<String, Object>builder().put(RuleCapability.ATTR_CONTEXT, variables);

      String templateName = RuleCapability.UpdateContextRequest.getTemplate(message);
      if(!StringUtils.isEmpty(templateName)) {
         logger.debug("Changing rule [{}] from template [{}] to template [{}]", rd.getAddress(), rd.getRuleTemplate(), templateName);
         rd.setRuleTemplate(templateName);
         changesBuilder.put(RuleCapability.ATTR_TEMPLATE, templateName);
      }

      // Need to regenerate the rule definition or it won't actually change the rule, just the definition.
      RuleCatalog catalog = getRuleCatalogForPlace(rd.getPlaceId());
      RuleTemplate template = catalog.getById(rd.getRuleTemplate());
      // TODO:  replace with better error events
      Preconditions.checkNotNull(template, "No template could be found with " + rd.getRuleTemplate());

      try {
         final RuleDefinition toUpdate = template.regenerate(rd);
         executor
            .submit(() -> doUpdate(toUpdate, changesBuilder.build()))
            .get();
      }
      catch (ValidationException ex) {
         logger.error("Error editing rule for place [{}] for template [{}] using variables [{}]", rd.getPlaceId(), rd.getRuleTemplate(), variables, ex);
         return Errors.fromException(ex);
      }
      catch (Exception ex) {
         logger.error("Error updating rule for [{}]", rd.getAddress(), ex);
         return Errors.fromException(ex);
      }

      return RuleCapability.UpdateContextResponse.instance();
   }
   
   // NOTE this must run in the executor thread
   private void doUpdate(RuleDefinition rd, Map<String, Object> changes) {
      ruleDefDao.save(rd);
      registry.reload(rd.getPlaceId());
      PlatformMessage msg =
            PlatformMessage
               .broadcast()
               .from(Address.platformService(rd.getPlaceId(), PlatformConstants.SERVICE_RULE, rd.getSequenceId()))
               .withPlaceId(rd.getPlaceId())
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(rd.getPlaceId()))
               .withPayload(Capability.EVENT_VALUE_CHANGE, changes)
               .create();
      platformBus.send(msg);
   }

   private MessageBody handleListHistoryEntries(Address ruleAddress, PlatformMessage message) {
      RuleDefinition rd = loadRuleDefintion((PlatformServiceAddress) ruleAddress);
      if(rd == null) {
         throw new NotFoundException(ruleAddress);
      }
      Errors.assertPlaceMatches(message, rd.getPlaceId());
      return listHistoryEntries.handleRequest(rd, message);
   }

   private RuleDefinition loadRuleDefintion(PlatformServiceAddress ruleAddress) {
      PlatformServiceAddress platformAddr = ruleAddress;
      UUID placeId = (UUID) platformAddr.getId();
      Integer ruleSequence = platformAddr.getContextQualifier();
      return ruleDefDao.findById(placeId, ruleSequence);
   }
   
   private RuleCatalog getRuleCatalogForPlace(UUID placeId) {
      return catalogs.getCatalogForPlace(placeId);
   }
}


