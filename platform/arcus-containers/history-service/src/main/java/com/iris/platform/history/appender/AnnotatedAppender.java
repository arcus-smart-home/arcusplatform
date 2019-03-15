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
package com.iris.platform.history.appender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.annotation.AnyValue;
import com.iris.platform.history.appender.annotation.AutoTranslate;
import com.iris.platform.history.appender.annotation.Event;
import com.iris.platform.history.appender.annotation.Group;
import com.iris.platform.history.appender.annotation.EnumValue;
import com.iris.platform.history.appender.annotation.Values;
import com.iris.platform.history.appender.matcher.FilteredMatcher;
import com.iris.platform.history.appender.matcher.FilteredMatcherImpl;
import com.iris.platform.history.appender.matcher.MatchResults;
import com.iris.platform.history.appender.translator.SimpleTranslator;
import com.iris.platform.history.appender.translator.TranslateOptions;
import com.iris.platform.history.appender.translator.Translator;
import com.iris.platform.history.appender.translator.ValueGetter;
import com.iris.platform.history.appender.translator.ValueGetterFactory;

public abstract class AnnotatedAppender extends BaseHistoryAppender {	
	public final static String ANY_MESSAGE_TYPE = "*";
	
	private final FilteredMatcher filteredMatcher;
	private final Map<TranslatorKey, Translator> translatorMap = new HashMap<>();
	private final Map<TranslatorKey, TranslateOptions> optionsMap = new HashMap<>();
	private final Map<String, Map<String, ValueGetter>> getterRegistry = new HashMap<>();
	
	public AnnotatedAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
		super(appender, cache);
		init();
		filteredMatcher = buildMatchers();
		buildTranslators();
	}
	
	protected void init() {
		// Nothing to do by default.
	}
	
	@Override
   protected MatchResults matches(PlatformMessage message) {
	   MatchResults results = filteredMatcher.matches(message);
	   return results;
   }

	@Override
   protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults) {
	   TranslatorKey key = new TranslatorKey(matchResults, TranslatorKey.USE_VALUE);
	   Translator translator = translatorMap.get(key);
	   if (translator == null) {
	   	// Widen the search
	   	key = new TranslatorKey(matchResults, TranslatorKey.DONT_USE_VALUE);
	   	translator = translatorMap.get(key);
	   }
	   
	   if (translator != null) {
	   	return translator.generateEntries(message, context, matchResults);
	   }
	   else {
	   	return doTranslate(message, context, matchResults, optionsMap.get(key));
	   }
   }
	
	protected List<HistoryLogEntry> doTranslate(PlatformMessage message, MessageContext context, MatchResults matchResults, TranslateOptions options) {
	   return null;
   }
	
	protected Translator createEventTranslator(String event, String template, boolean critical) {
		return new SimpleTranslator(template, critical);
	}
	
	protected Translator createEnumValueChangeTranslator(String attribute, String value, String template, boolean critical) {
		return new SimpleTranslator(template, critical);
	}
	
	protected Translator createAnyValueChangeTranslator(String attribute, String template, boolean critical) {
		return new SimpleTranslator(template, critical);
	}
	
	protected void registerGetter(String valueName, String event, ValueGetter getter) {
		Map<String, ValueGetter> eventGetters = getterRegistry.get(valueName);
		if (eventGetters == null) {
			eventGetters = new HashMap<>();
			getterRegistry.put(valueName, eventGetters);
		}
		eventGetters.put(event, getter);
	}
	
	private FilteredMatcher buildMatchers() {
		Group group = this.getClass().getAnnotation(Group.class);
		Event[] events = this.getClass().getAnnotationsByType(Event.class);
		EnumValue[] values = this.getClass().getAnnotationsByType(EnumValue.class);
		AnyValue[] anyValues = this.getClass().getAnnotationsByType(AnyValue.class);
		
		FilteredMatcherImpl.Builder builder = FilteredMatcherImpl.builder();
		
		if (group != null) {
			builder.withGroup(group.value());
		}
		
		if (events != null && events.length > 0) {
			for (Event event : events) {
				builder.addEvent(event.event());
			}
		}
		
		if (values != null && values.length > 0) {
			for (EnumValue value : values) {
				builder.addValue(value.attr(), value.val());
			}
		}
		
		if (anyValues != null && anyValues.length > 0) {
			for (AnyValue anyValue : anyValues) {
				builder.addAnyValue(anyValue.attr());
			}
		}
		
		return builder.build();
	}
	
	private void buildTranslators() {
		Event[] events = this.getClass().getAnnotationsByType(Event.class);
		EnumValue[] values = this.getClass().getAnnotationsByType(EnumValue.class);
		AnyValue[] anyValues = this.getClass().getAnnotationsByType(AnyValue.class);
		AutoTranslate autoTranslate = this.getClass().getAnnotation(AutoTranslate.class);
		Values valuesAnnotation = this.getClass().getAnnotation(Values.class);
		
		boolean isAutoTranslate = autoTranslate != null;
		String[] valueGetters = valuesAnnotation != null ? valuesAnnotation.value() : null;
		
		if (events != null && events.length > 0) {
			for (Event event : events) {
				TranslatorKey key = new TranslatorKey(event.event());
				Translator translator = null;
				if (isAutoTranslate) {
					translator = createEventTranslator(event.event(), event.tpl(), event.critical());
					addValueGetters(translator, valueGetters);
				}
				if (translator != null) {
					translatorMap.put(key, translator);
				}
				else {
					optionsMap.put(key, TranslateOptions.builder().withTemplate(event.tpl()).withCritical(event.critical()).build());
				}
			}
		}
		
		if (values != null && values.length > 0) {
			for (EnumValue value : values) {
				String[] attrValues = value.val();
				for (String attrValue : attrValues) {
					String template = value.tpl().contains("?") ? value.tpl().replace("?", attrValue.toLowerCase()) : value.tpl();
					TranslatorKey key = new TranslatorKey(value.attr(), attrValue);
					Translator translator = null;
					if (isAutoTranslate) {
						translator = createEnumValueChangeTranslator(value.attr(), attrValue, template, value.critical());
						addValueGetters(translator, valueGetters);
					}
					if (translator != null) {
						translatorMap.put(key, translator);
					}
					else {
						optionsMap.put(key, TranslateOptions.builder().withTemplate(template).withCritical(value.critical()).build());
					}
				}
			}
		}
		
		if (anyValues != null && anyValues.length > 0) {
			for (AnyValue anyValue : anyValues) {
				TranslatorKey key = new TranslatorKey(anyValue.attr(), null);
				Translator translator = null;
				if (isAutoTranslate) {
					translator = createAnyValueChangeTranslator(anyValue.attr(), anyValue.tpl(), anyValue.critical());
					addValueGetters(translator, valueGetters);
				}
				if (translator != null) {
					translatorMap.put(key, translator);
				}
				else {
					optionsMap.put(key, TranslateOptions.builder().withTemplate(anyValue.tpl()).withCritical(anyValue.critical()).build());
				}
			}
		}
	}
	
	private void addValueGetters(Translator translator, String[] valueGetterTypes) {
		if (valueGetterTypes != null && valueGetterTypes.length > 0) {
			for (String valueGetterName : valueGetterTypes) {
				translator.appendGetter(findValueGetter(valueGetterName));
			}
		}
	}
	
	private ValueGetter findValueGetter(String getterName) {
		Map<String, ValueGetter> getterMap = this.getterRegistry.get(getterName);
		return getterMap != null ? new MapValueGetter(getterMap) : ValueGetterFactory.findGetter(getterName); 
	}
		
	private static class MapValueGetter implements ValueGetter {
		private final Map<String, ValueGetter> getterMap;
		
		MapValueGetter(Map<String, ValueGetter> getterMap) {
			this.getterMap = getterMap;
		}

		@Override
      public String get(PlatformMessage message, MessageContext context, MatchResults matchResults) {
	      ValueGetter getter = this.getterMap.get(message.getMessageType());
	      if (getter == null) {
	      	getter = this.getterMap.get(ANY_MESSAGE_TYPE);
	      }
	      return getter != null ? getter.get(message, context, matchResults) : "";
      }	
	}
		
	private static class TranslatorKey {
		final static boolean USE_VALUE = true;
		final static boolean DONT_USE_VALUE = false;
		private final String event;
		private final String attribute;
		private final String value;
		
		TranslatorKey(MatchResults results, boolean includeValue) {
			event = results.getBody() != null 
					? results.getBody().getMessageType()
					: null;
			attribute = results.getBaseAttrib();
			if (includeValue) {
				value = results.getFoundValue() instanceof String ? (String) results.getFoundValue() : null;
			}
			else {
				value = null;
			}
		}
		
		TranslatorKey(String event) {
			this.event = event;
			this.attribute = null;
			this.value = null;
		}
		
		TranslatorKey(String attribute, String value) {
			this.event = Capability.EVENT_VALUE_CHANGE;
			this.attribute = attribute;
			this.value = value;
		}

		@Override
      public int hashCode() {
	      final int prime = 31;
	      int result = 1;
	      result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
	      result = prime * result + ((event == null) ? 0 : event.hashCode());
	      result = prime * result + ((value == null) ? 0 : value.hashCode());
	      return result;
      }

		@Override
      public boolean equals(Object obj) {
	      if (this == obj)
		      return true;
	      if (obj == null)
		      return false;
	      if (getClass() != obj.getClass())
		      return false;
	      TranslatorKey other = (TranslatorKey) obj;
	      if (attribute == null) {
		      if (other.attribute != null)
			      return false;
	      } else if (!attribute.equals(other.attribute))
		      return false;
	      if (event == null) {
		      if (other.event != null)
			      return false;
	      } else if (!event.equals(other.event))
		      return false;
	      if (value == null) {
		      if (other.value != null)
			      return false;
	      } else if (!value.equals(other.value))
		      return false;
	      return true;
      }
	}
	
}

