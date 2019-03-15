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
package com.iris.platform.history.appender.matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.util.SetMap;

public class FilteredMatcherImpl implements FilteredMatcher {
	
	private final Predicate<PlatformMessage> filter;
	private final Matcher eventMatcher;
	private final Matcher valueChangeMatcher;
	
	private FilteredMatcherImpl(Predicate<PlatformMessage> filter, Matcher eventMatcher, Matcher... valueMatchers) {
		this.filter = filter;
		this.eventMatcher = eventMatcher;
		if (eventMatcher == null && (valueMatchers == null || valueMatchers.length == 0)) {
			throw new IllegalArgumentException("A FilteredMatcher must include at least one matcher");
		}
		if (valueMatchers.length == 0) {
			valueChangeMatcher = null;
		}
		else if (valueMatchers.length == 1) {
			valueChangeMatcher = valueMatchers[0];
		}
		else {
			valueChangeMatcher = new CompositeMatcher(valueMatchers);
		}
	}

	@Override
	public MatchResults matches(PlatformMessage message) {
		if (filter == null || filter.apply(message)) {
			if (valueChangeMatcher != null && Capability.EVENT_VALUE_CHANGE.equals(message.getMessageType())) {
				return valueChangeMatcher.matches(message.getValue());
			}
			else if (eventMatcher != null) {
				return eventMatcher.matches(message.getValue());
			}
		}
		return MatchResults.FALSE;
	}
	
	public static Builder builder() { return new Builder(); }
	
	public static class Builder {
		private Predicate<PlatformMessage> filter = null;
		private final List<Matcher> valueChangeMatchers = new ArrayList<>();
		private final List<String> eventNames = new ArrayList<>();
		private final SetMap<String, String> enumValues = new SetMap<>();
		
		public Builder withGroup(String group) {
			filter = (msg) -> group.equals(msg.getSource().getGroup());
			return this;
		}
		
		public Builder addValue(String attr, String[] values) {
			for (String value : values) {
				enumValues.putItem(attr, value);
			}
			return this;
		}
		
		public Builder addAnyValue(String attr) {
			valueChangeMatchers.add(new AnyValueChangeMatcher(attr));
			return this;
		}
		
		public Builder addEvent(String eventName) {
			eventNames.add(eventName);
			return this;
		}
		
		public FilteredMatcher build() {
			Matcher eventMatcher = eventNames.isEmpty()
					? null
					: new AnyEventMatcher(eventNames.toArray(new String[eventNames.size()]));
			
			for (Map.Entry<String, Set<String>> entry : enumValues.entrySet()) {
				valueChangeMatchers.add(new EnumValueChangeMatcher(entry.getKey(), 
						entry.getValue().toArray(new String[entry.getValue().size()])));
			}
			
			return new FilteredMatcherImpl(filter,
						eventMatcher,
						valueChangeMatchers.toArray(new Matcher[valueChangeMatchers.size()]));
		}
	}

}

