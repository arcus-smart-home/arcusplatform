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

import static com.iris.info.IrisApplicationInfo.getApplicationVersion;
import static com.iris.messages.service.SessionService.TaggedEvent.SOURCE_PLATFORM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iris.common.rule.Context;
import com.iris.common.rule.action.Action;
import com.iris.common.rule.event.RuleEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;
import com.iris.messages.service.SessionService.TaggedEvent;
import com.iris.platform.rule.RuleDefinition;

public abstract class TaggedEventBuilder<T extends TaggedEventBuilder<T>>
{
   public static final String TAG_RULE_FIRED_START = "iris.platform.rule.fired.start";
   public static final String TAG_RULE_FIRED_END   = "iris.platform.rule.fired.end";
   public static final String TAG_SCENE_FIRED      = "iris.platform.scene.fired";

   public static final String KEY_RULE_TEMPLATE_ID = "ruleTemplateId";
   public static final String KEY_RULE_NAME        = "ruleName";
   public static final String KEY_PARTICIPATING    = "participating";
   public static final String KEY_CAUSE            = "cause";

   public static final String KEY_ACTION           = "action";

   public static RuleStartedFiringMessageBuilder ruleStartedFiringMessageBuilder()
   {
      return new RuleStartedFiringMessageBuilder();
   }

   public static RuleStoppedFiringMessageBuilder ruleStoppedFiringMessageBuilder()
   {
      return new RuleStoppedFiringMessageBuilder();
   }

   public static SceneFiredMessageBuilder sceneFiredMessageBuilder()
   {
      return new SceneFiredMessageBuilder();
   }

   private final String tagName;
   private Address source;
   protected Context context;

   public TaggedEventBuilder(String tagName)
   {
      this.tagName = tagName;
   }

   @SuppressWarnings("unchecked")
   public T withSource(Address source)
   {
      this.source = source;
      return (T) this;
   }

   @SuppressWarnings("unchecked")
   public T withContext(Context context)
   {
      this.context = context;
      return (T) this;
   }

   public PlatformMessage build()
   {
      MessageBody messageBody = TaggedEvent.builder()
         .withName(tagName)
         .withPlaceId(context == null ? null : context.getPlaceId().toString())
         .withSource(SOURCE_PLATFORM)
         .withVersion(getApplicationVersion())
         .withServiceLevel(context == null ? null : context.getServiceLevel())
         .withContext(buildTagContext())
         .build();

      return PlatformMessage.builder()
         .from(source)
         .withPlaceId(context == null ? null : context.getPlaceId())
         .withPopulation(context.getPopulation())
         .withPayload(messageBody)
         .create();
   }

   protected abstract Map<String, Object> buildTagContext();

   public static class RuleStartedFiringMessageBuilder extends RuleFiredMessageBuilder
   {
      public RuleStartedFiringMessageBuilder()
      {
         super(TAG_RULE_FIRED_START);
      }
   }

   public static class RuleStoppedFiringMessageBuilder extends RuleFiredMessageBuilder
   {
      public RuleStoppedFiringMessageBuilder()
      {
         super(TAG_RULE_FIRED_END);
      }
   }

   public abstract static class RuleFiredMessageBuilder extends TaggedEventBuilder<RuleFiredMessageBuilder>
   {
      protected RuleDefinition ruleDefinition;
      protected RuleEvent ruleEvent;

      protected RuleFiredMessageBuilder(String tagName)
      {
         super(tagName);
      }

      public RuleFiredMessageBuilder withRuleDefinition(RuleDefinition ruleDefinition)
      {
         this.ruleDefinition = ruleDefinition;
         return this;
      }

      public RuleFiredMessageBuilder withRuleEvent(RuleEvent ruleEvent)
      {
         this.ruleEvent = ruleEvent;
         return this;
      }

      @Override
      protected Map<String, Object> buildTagContext()
      {
         Map<String, Object> tagContext = new HashMap<>();

         if (ruleDefinition != null)
         {
            tagContext.put(KEY_RULE_TEMPLATE_ID, ruleDefinition.getRuleTemplate());
            tagContext.put(KEY_RULE_NAME, ruleDefinition.getName());
         }

         if (ruleEvent != null)
         {
            tagContext.put(KEY_CAUSE, ruleEvent.getType().toString());
         }

         if (context != null)
         {
            Map<String, List<String>> participants = new HashMap<>();

            for (Model model : context.getModels())
            {
               List<String> addresses = participants.get(model.getType());
               if (addresses == null)
               {
                  addresses = new ArrayList<>();
                  participants.put(model.getType(), addresses);
               }

               addresses.add(model.getAddress().getRepresentation());
            }

            tagContext.put(KEY_PARTICIPATING, participants);
         }

         return tagContext;
      }
   }

   public static class SceneFiredMessageBuilder extends TaggedEventBuilder<SceneFiredMessageBuilder>
   {
      private Action sceneAction;

      public SceneFiredMessageBuilder()
      {
         super(TAG_SCENE_FIRED);
      }

      public SceneFiredMessageBuilder withAction(Action sceneAction)
      {
         this.sceneAction = sceneAction;
         return this;
      }

      @Override
      protected Map<String, Object> buildTagContext()
      {
         Map<String, Object> tagContext = new HashMap<>();

         tagContext.put(KEY_ACTION, sceneAction);

         return tagContext;
      }
   }
}

