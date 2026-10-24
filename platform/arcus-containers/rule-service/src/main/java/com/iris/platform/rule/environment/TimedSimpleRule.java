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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.StringUtils.defaultString;

import com.codahale.metrics.Timer;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.action.stateful.StatefulAction;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.simple.SimpleRule;
import com.iris.messages.address.Address;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.tag.TaggingMetric;
import com.iris.platform.rule.RuleDefinition;

/**
 * Wraps a SimpleRule to time how long it fires, tagged by rule template.
 *
 */
public class TimedSimpleRule extends SimpleRule {

	private static final IrisMetricSet metrics = IrisMetrics.metrics("rule.service");
	private static final TaggingMetric<Timer> ruleFiredDurationTimer = metrics.taggingTimer("rule.fired.duration");

	private static final String RULE_TEMPLATE_GENERIC = "generic";

	private final RuleDefinition definition;
	
	private long fireEventStartTime; 
	
   public TimedSimpleRule(RuleDefinition definition, RuleContext context, Condition condition, StatefulAction action, Address address) {
		super(context, condition, action, address);
		
		this.definition = definition;
	}

   @Override
   public void onStartedFiring(RuleEvent event) {
	   super.onStartedFiring(event);

	   this.fireEventStartTime = System.currentTimeMillis();
   }

   @Override
   public void onStoppedFiring(RuleEvent event) {
	   super.onStoppedFiring(event);
	   
      String ruleTemplate = defaultString(definition.getRuleTemplate(), RULE_TEMPLATE_GENERIC);
	   long fireEventDurationMs = System.currentTimeMillis() - this.fireEventStartTime;
	   ruleFiredDurationTimer.tag("ruleTemplate", ruleTemplate).update(fireEventDurationMs, MILLISECONDS);
   }   
}
