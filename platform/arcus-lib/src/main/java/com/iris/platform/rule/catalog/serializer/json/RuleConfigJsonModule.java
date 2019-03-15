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
package com.iris.platform.rule.catalog.serializer.json;

import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.inject.TypeLiteral;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.messages.MessagesModule;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.action.config.ActionListConfig;
import com.iris.platform.rule.catalog.action.config.ForEachModelActionConfig;
import com.iris.platform.rule.catalog.action.config.LogActionConfig;
import com.iris.platform.rule.catalog.action.config.SendActionConfig;
import com.iris.platform.rule.catalog.action.config.SendNotificationActionConfig;
import com.iris.platform.rule.catalog.action.config.SetAttributeActionConfig;
import com.iris.platform.rule.catalog.condition.config.ConditionConfig;
import com.iris.platform.rule.catalog.condition.config.ContextQueryConfig;
import com.iris.platform.rule.catalog.condition.config.DayOfWeekConfig;
import com.iris.platform.rule.catalog.condition.config.DurationConfig;
import com.iris.platform.rule.catalog.condition.config.IfConditionConfig;
import com.iris.platform.rule.catalog.condition.config.OrConfig;
import com.iris.platform.rule.catalog.condition.config.QueryChangeConfig;
import com.iris.platform.rule.catalog.condition.config.ReceivedMessageConfig;
import com.iris.platform.rule.catalog.condition.config.ReferenceFilterConfig;
import com.iris.platform.rule.catalog.condition.config.ThresholdConfig;
import com.iris.platform.rule.catalog.condition.config.TimeOfDayConfig;
import com.iris.platform.rule.catalog.condition.config.ValueChangeConfig;
import com.netflix.governator.annotations.Modules;

/**
 * Contains the necessary binding for serializing / deserializing
 * ActionConfig and ConditionConfig.
 */
@Modules(include = MessagesModule.class)
public class RuleConfigJsonModule extends AbstractIrisModule {

   private static final String ATT_TYPE = "type";
   
   @Override
   protected void configure() {
      bindSetOf(TypeAdapterFactory.class)
         .addBinding()
         .toInstance(createConditionConfigSerializer());
      bindSetOf(TypeAdapterFactory.class)
         .addBinding()
         .toInstance(createActionConfigSerializer());
      bindSetOf(new TypeLiteral<TypeAdapter<?>>() {})
         .addBinding()
         .to(AttributeTypeAdapter.class)
         .asEagerSingleton();
      bindSetOf(new TypeLiteral<TypeAdapter<?>>() {})
         .addBinding()
         .to(TemplatedExpressionTypeAdapter.class)
         .asEagerSingleton();
   }
   
   private TypeAdapterFactory createConditionConfigSerializer() {
      return
            RuntimeTypeAdapterFactory
               .of(ConditionConfig.class, ATT_TYPE)
               .registerSubtype(ContextQueryConfig.class, ContextQueryConfig.TYPE)
               .registerSubtype(DayOfWeekConfig.class, DayOfWeekConfig.TYPE)
               .registerSubtype(DurationConfig.class, DurationConfig.TYPE)
               .registerSubtype(OrConfig.class, OrConfig.TYPE)
               .registerSubtype(QueryChangeConfig.class, QueryChangeConfig.TYPE)
               .registerSubtype(IfConditionConfig.class, IfConditionConfig.TYPE)
               .registerSubtype(ReceivedMessageConfig.class, ReceivedMessageConfig.TYPE)
               .registerSubtype(ReferenceFilterConfig.class, ReferenceFilterConfig.TYPE)
               .registerSubtype(ThresholdConfig.class, ThresholdConfig.TYPE)
               .registerSubtype(TimeOfDayConfig.class, TimeOfDayConfig.TYPE)
               .registerSubtype(ValueChangeConfig.class, ValueChangeConfig.TYPE)
               ;
   }
   
   private TypeAdapterFactory createActionConfigSerializer() {
      return
         RuntimeTypeAdapterFactory
            .of(ActionConfig.class, ATT_TYPE)
            .registerSubtype(SendActionConfig.class, SendActionConfig.TYPE)
            .registerSubtype(SendNotificationActionConfig.class, SendNotificationActionConfig.TYPE)
            .registerSubtype(SetAttributeActionConfig.class, SetAttributeActionConfig.TYPE)
            .registerSubtype(ForEachModelActionConfig.class, ForEachModelActionConfig.TYPE)
            .registerSubtype(LogActionConfig.class, LogActionConfig.TYPE)
            .registerSubtype(ActionListConfig.class, ActionListConfig.TYPE);
   }

}

