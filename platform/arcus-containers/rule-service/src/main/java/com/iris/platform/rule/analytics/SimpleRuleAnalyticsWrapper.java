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
package com.iris.platform.rule.analytics;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.StringUtils.defaultString;

import com.codahale.metrics.Timer;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.action.Action;
import com.iris.common.rule.action.stateful.StatefulAction;
import com.iris.common.rule.action.stateful.StatefulActionWrapper;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.simple.SimpleRule;
import com.iris.core.platform.AnalyticsMessageBus;
import com.iris.core.platform.TaggedEventBuilder;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.tag.TaggingMetric;
import com.iris.platform.rule.RuleDefinition;

/**
 * Wraps a SimpleRule to capture context and generate analytics messages.
 *
 */
public class SimpleRuleAnalyticsWrapper extends SimpleRule {

	private static final IrisMetricSet metrics = IrisMetrics.metrics("rule.service");
	private static final TaggingMetric<Timer> ruleFiredDurationTimer = metrics.taggingTimer("rule.fired.duration");

	private static final String RULE_TEMPLATE_GENERIC = "generic";

	private final AnalyticsMessageBus analyticsBus;
	private final RuleDefinition definition;
	
	private long fireEventStartTime; 
	
	public SimpleRuleAnalyticsWrapper(AnalyticsMessageBus analyticsBus, RuleDefinition definition, RuleContext context, Condition condition, Action action, Address address) {
		this(analyticsBus, definition, context, condition, new StatefulActionWrapper(action), address);
	}

   public SimpleRuleAnalyticsWrapper(AnalyticsMessageBus analyticsBus, RuleDefinition definition, RuleContext context, Condition condition, StatefulAction action, Address address) {
		super(context, condition, action, address);
		
		this.analyticsBus = analyticsBus;
		this.definition = definition;
	}

   @Override
   public void onStartedFiring(RuleEvent event) {
	   super.onStartedFiring(event);

	   this.fireEventStartTime = System.currentTimeMillis();

	   PlatformMessage message = TaggedEventBuilder.ruleStartedFiringMessageBuilder()
			   .withSource(getAddress())
			   .withRuleDefinition(definition)
			   .withContext(getContext())
			   .withRuleEvent(event)
			   .build();

	   analyticsBus.send(message);			   
   }

   @Override
   public void onStoppedFiring(RuleEvent event) {
	   super.onStoppedFiring(event);
	   
      String ruleTemplate = defaultString(definition.getRuleTemplate(), RULE_TEMPLATE_GENERIC);
	   long fireEventDurationMs = System.currentTimeMillis() - this.fireEventStartTime;
	   ruleFiredDurationTimer.tag("ruleTemplate", ruleTemplate).update(fireEventDurationMs, MILLISECONDS);
	   
	   PlatformMessage message = TaggedEventBuilder.ruleStoppedFiringMessageBuilder()
			   .withSource(getAddress())
			   .withRuleDefinition(definition)
			   .withContext(getContext())
			   .withRuleEvent(event)
			   .build();
	   
	   analyticsBus.send(message);
   }   
}

