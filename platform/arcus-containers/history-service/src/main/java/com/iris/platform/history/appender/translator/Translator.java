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
package com.iris.platform.history.appender.translator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.iris.messages.PlatformMessage;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.model.ChildId;
import com.iris.messages.model.CompositeId;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.SubsystemId;
import com.iris.platform.history.appender.BaseHistoryAppender;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.matcher.MatchResults;

/**
 * Automatically selects message key based on presence of instance, actor, and method information
 * For any given base key, the following keys must be defined
 *   key_base
 *   key_base.pers
 *   key_base.pers.meth
 *   key_base.rule
 *   key_base.inst
 *   key_base.inst.pers
 *   key_base.inst.pers.meth
 *   key_base.inst.rule
 *   
 *  Each version needs to have a .name, .short, and .long version
 *  
 *  All value lists have the following pre-defined values packed in
 *   {0} = Device Name
 *   {1} = Instance Name
 *   {2} = Actor Name (could be person or rule)
 *   {3} = Method Name (e.g. 'on the device', 'via the Android app', 'via the iOS app', etc.)
 *
 * @author sxperry
 */
public abstract class Translator {
	
	private static final String[] STRING_ARRAY = new String[0];
	private final List<ValueGetter> getters = new ArrayList<>();
	
	/**
	 * Base implementation of generate entries for all device value change appenders
	 * Defines which logs entries are appended to by default (with the exception of the critical 
	 * place log).  Selects appropriate version of the message key for the entry based on whether
	 * or not actor, method, etc. have been extracted from the message.
	 * @param message
	 * @param context
	 * @param matchResults
	 * @return
	 */
	public List<HistoryLogEntry> generateEntries(PlatformMessage message, MessageContext context, MatchResults matchResults) {
		List<HistoryLogEntry> entries = new ArrayList<HistoryLogEntry>();
	
		EntryTemplate template = selectTemplate(matchResults);
		
		List<String> values = new ArrayList<String>();
		if (context.getDeviceName() != null) {
			values.add(context.getDeviceName());
		} else if (context.getHubName() != null) {
			values.add(context.getHubName());
		} else {
			values.add("");
		}
		
		if (matchResults.getFoundInstance() == null) {
			values.add("");	
		} else {
			values.add(matchResults.getFoundInstance());
		}
		
		values.add(context.getActorName());
		values.add(context.getMethodName());
		values.addAll(generateValues(message, context, matchResults));
	
		String key = template.getKeyBase();
		if (!StringUtils.isEmpty(matchResults.getFoundInstance())) {
			key += ".inst";
		}
		if (!StringUtils.isEmpty(context.getActorName()) && (context.actorIsPerson())) {
			key += ".pers";
			if (!StringUtils.isEmpty(context.getMethodName())) {
				key += ".meth";	
			}
		} else if (!StringUtils.isEmpty(context.getActorName()) && context.actorIsRule()) {
			key += ".rule";
		}
	
		// all value change entries go in the detailed place log and into the detailed device log
		entries.add(BaseHistoryAppender.detailedPlaceEvent(context.getTimestamp(), context.getPlaceId(), key, context.getSubjectAddress(), values.toArray(STRING_ARRAY)));
		if (template.isCritical()) {
			entries.add(BaseHistoryAppender.criticalPlaceEvent(context.getTimestamp(), context.getPlaceId(), key, context.getSubjectAddress(), values.toArray(STRING_ARRAY)));
		}
		// if actor name is defined...
		if (!StringUtils.isEmpty(context.getActorName())) {
			if (context.actorIsPerson()) {
				entries.add(BaseHistoryAppender.detailedPersonEvent(context.getTimestamp(), (UUID)context.getActorAddress().getId(), key, context.getSubjectAddress(), values.toArray(STRING_ARRAY)));
			}
			if (context.actorIsRule()) {
				PlatformServiceAddress psa = (PlatformServiceAddress) context.getActorAddress();
				CompositeId<UUID, Integer> id = new ChildId((UUID) psa.getId(), psa.getContextQualifier());
				entries.add(BaseHistoryAppender.detailedRuleEvent(context.getTimestamp(), id, key, context.getSubjectAddress(), values.toArray(STRING_ARRAY)));
			}
		}
		if (context.subjectIsDevice()) {
			entries.add(BaseHistoryAppender.detailedDeviceEvent(context.getTimestamp(), context.getDeviceId(), key, context.getSubjectAddress(), values.toArray(STRING_ARRAY)));
		} else if (context.subjectIsSubsystem()) {
			CompositeId<UUID, String> id = new SubsystemId((UUID)context.getSubjectAddress().getId(), (String)context.getSubjectAddress().getGroup());
			entries.add(BaseHistoryAppender.detailedSubsystemEvent(context.getTimestamp(), id, key, context.getSubjectAddress(), values.toArray(STRING_ARRAY)));
		} else if (context.subjectIsHub()) {
			entries.add(BaseHistoryAppender.detailedHubEvent(context.getTimestamp(), context.getHubId(), key, context.getSubjectAddress(), values.toArray(STRING_ARRAY)));			
		}
			
		return entries;
	}
	
	/**
	 * Add a value getter to the translator.
	 */
	public void appendGetter(ValueGetter getter) {
		this.getters.add(getter);
	}
	
	/**
	 * Select and return the entry template required for these match results
	 * @param matchResults
	 * @return
	 */
	protected abstract EntryTemplate selectTemplate(MatchResults matchResults);
	
	/**
	 * Generate and return the values that are unique to this message to they can be appended to the 
	 * standard values such as Device Name, Actor Name, Method Name, etc.
	 * @param message
	 * @param context
	 * @param matchResults
	 * @return
	 */
	public List<String> generateValues(PlatformMessage message, MessageContext context, MatchResults matchResults) {
		if (getters == null || getters.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> values = new ArrayList<>(getters.size());
		for (ValueGetter getter : getters) {
			values.add(getter.get(message, context, matchResults));
		}
		return values;
	}
}

