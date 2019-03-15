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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;

import com.google.inject.Inject;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.HistoryLogEntryType;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({ HistoryAppenderDAO.class, ObjectNameCache.class})
public class EventAppenderTestCase extends IrisMockTestCase {

   @Inject protected HistoryAppenderDAO mockAppenderDao;
   @Inject protected ObjectNameCache mockNameCache;

   protected final static String INSTANCE_FENRIS = "FENRIS";

   protected final static HistoryLogEntryType CRITICAL = HistoryLogEntryType.CRITICAL_PLACE_LOG;
   protected final static HistoryLogEntryType PLACE    = HistoryLogEntryType.DETAILED_PLACE_LOG;
   protected final static HistoryLogEntryType DEVICE   = HistoryLogEntryType.DETAILED_DEVICE_LOG;
   protected final static HistoryLogEntryType PERSON   = HistoryLogEntryType.DETAILED_PERSON_LOG;
   protected final static HistoryLogEntryType RULE     = HistoryLogEntryType.DETAILED_RULE_LOG;
   protected final static HistoryLogEntryType SUBSYS   = HistoryLogEntryType.DETAILED_SUBSYSTEM_LOG;
   protected final static HistoryLogEntryType ALARM    = HistoryLogEntryType.DETAILED_ALARM_LOG;

   protected final static UUID PLACE_ID = UUID.fromString("25169b48-8db2-4155-91b2-1f8ff05c3818");
   protected final static UUID DEVICE_ID = UUID.fromString("eb32f0c6-c05c-4e0d-8084-021dbf0ec585");
   protected final static UUID PERSON_ID = UUID.fromString("d75079bc-8883-4989-9730-a92ca2b93c2f");
   protected final static UUID RULE_ID = UUID.fromString("c65079bc-8883-4989-9730-a92ca2b93c2f");

   protected final static String PLACE_NAME = "MyPlace";
   protected final static String DEVICE_NAME = "MyDevice";
   protected final static String PERSON_NAME = "Emily";
   protected final static String RULE_NAME = "The One Rule";

   protected final static Address placeAddress = Address.platformService(PLACE_ID, PlaceCapability.NAMESPACE);
   protected final static Address deviceAddress = Address.platformDriverAddress(DEVICE_ID);
   protected final static Address personAddress = Address.platformService(PERSON_ID, PersonCapability.NAMESPACE);
   protected final static Address ruleAddress = Address.platformService(RULE_ID, RuleCapability.NAMESPACE);

   protected final static Address ACTOR_PERSON = personAddress;
   protected final static Address ACTOR_RULE = ruleAddress;

   public EventAppenderTestCase() {
      super();
   }

   protected Capture<HistoryLogEntry> expectAndCaptureAppend() {
      Capture<HistoryLogEntry> eventRef = Capture.newInstance();
      mockAppenderDao.appendHistoryEvent(EasyMock.capture(eventRef));
      EasyMock.expectLastCall();
      return eventRef;
   }
   
   protected void expectNameLookup(Address address, String name) {
   	EasyMock
   		.expect(mockNameCache.getName(address))
   		.andReturn(name);
   }

   protected void expectFindRuleName() {
   	EasyMock
   		.expect(mockNameCache.getRuleName(ruleAddress))
   		.andReturn(RULE_NAME)
   		;
   }

   protected void expectFindPersonName() {
   	EasyMock
   		.expect(mockNameCache.getPersonName(personAddress))
   		.andReturn(PERSON_NAME)
   		;
   }

   protected void expectFindPlaceName() {
      EasyMock
         .expect(mockNameCache.getPlaceName(placeAddress))
         .andReturn(PLACE_NAME)
         ;
   }

   protected void expectFindDeviceName() {
      EasyMock
         .expect(mockNameCache.getDeviceName(deviceAddress))
         .andReturn(DEVICE_NAME)
         ;
   }

   protected TestContextBuilder context() {
   	return new TestContextBuilder();
   }

   protected class TestContext {
   	private final String templateName;
   	private final List<HistoryLogEntryType> entries;
   	private final Map<Integer, String> values;
   	private final UUID placeId;

		public TestContext(String templateName, List<HistoryLogEntryType> entries, Map<Integer, String> values) {
			this.placeId = PLACE_ID;
	      this.templateName = templateName;
	      this.entries = entries;
	      this.values = values;
      }

		public UUID placeId() {
			return placeId;
		}

		public String template() {
   		return templateName;
   	}

   	public List<HistoryLogEntryType> types() {
   		return entries;
   	}

   	public HistoryLogEntryType type(int index) {
   		return entries.get(index);
   	}

   	public Set<Integer> keys() {
   		return values.keySet();
   	}

   	public Object value(int index) {
   		return values.get(index);
   	}
   }

   protected class TestContextBuilder {
   	private String template;
   	private List<HistoryLogEntryType> entries = new ArrayList<>();
   	private Map<Integer, String> values = new HashMap<>();

   	public TestContextBuilder withLogType(HistoryLogEntryType entry) {
   		entries.add(entry);
   		return this;
   	}

   	public TestContextBuilder template(String template) {
   		this.template = template;
   		return this;
   	}

   	public TestContextBuilder withValue(int index, String value) {
   		this.values.put(index, value);
   		return this;
   	}

   	public TestContext build() {
   		return new TestContext(template, entries, values);
   	}
   }

   protected EventBuilder eventBuilder() {
      return new EventBuilder();
   }

   protected class EventBuilder {
      private Map<String, Object> attributes = new HashMap<>();
      private String event;
      private Address actor;
      private Address source;

      public EventBuilder event(String event) {
         this.event = event;
         return this;
      }

      public EventBuilder from(Address source) {
         this.source = source;
         return this;
      }

      public EventBuilder withActor(Address actor) {
         this.actor = actor;
         return this;
      }

      public EventBuilder withAttriburtes(Map<String, Object> attrs) {
      	attributes.putAll(attrs);
      	return this;
      }

      public EventBuilder withAttribute(String attr, Object value) {
         attributes.put(attr, value);
         return this;
      }

      public PlatformMessage build() {
      	
      	MessageBody body = MessageBody.buildMessage(event, attributes);
   		PlatformMessage.Builder builder = PlatformMessage
													         .builder()
													         .withPlaceId(PLACE_ID)
													         .from(source)
													         .broadcast()
													         .withPayload(body);
			if (actor != null) {
				builder.withActor(actor);
			}
			return builder.create();
   	}
   }

   protected ValueChangeBuilder valueChangeBuilder() {
   	return new ValueChangeBuilder();
   }

   protected class ValueChangeBuilder {
   	private Map<String, Object> attributes = new HashMap<>();
   	private Address actor;
   	private Address source;

   	public ValueChangeBuilder fromDevice() {
   		source = deviceAddress;
   		return this;
   	}

   	public ValueChangeBuilder withActor(Address actor) {
   		this.actor = actor;
   		return this;
   	}

   	public ValueChangeBuilder withAttribute(String attr, Object value) {
   		attributes.put(attr, value);
   		return this;
   	}

   	public PlatformMessage build() {

   		PlatformMessage.Builder builder = PlatformMessage
													         .builder()
													         .withPlaceId(PLACE_ID)
													         .from(source)
													         .broadcast()
													         .withPayload(Capability.EVENT_VALUE_CHANGE, attributes);
			if (actor != null) {
				builder.withActor(actor);
			}
			return builder.create();
   	}
   }

   @SuppressWarnings("unchecked")
   protected void verifyValues(TestContext context, Capture<HistoryLogEntry>... captures) {
		int entryIndex = 0;
		for (HistoryLogEntryType type : context.types()) {
			HistoryLogEntry entry = captures[entryIndex].getValue();
			assertEquals(type, entry.getType());
			assertEquals(context.template(), entry.getMessageKey());
			
			List<String> values = entry.getValues();
			System.out.println(entry.getType() + " : " + entry.getMessageKey() + " : " + values);
			int numValues = values.size();
         for (int index = 0; index < numValues; index++) {
         	assertEquals(context.value(index), values.get(index));
         }
			entryIndex++;
		}
	}
	
	protected class Tester {
		private TestContext ctx;
		private Address actor;
		private String event;
		private boolean reject = false;
		private Map<Address, String> lookups = new HashMap<Address, String>();
		private Map<String, Object> attributes = new HashMap<String, Object>();
		private final Address sourceAddress;
		private final HistoryAppender appender;
		
		protected Tester(Address source, HistoryAppender appender) {
			this.sourceAddress = source;
			this.appender = appender;
		}
		
		public Tester withActor(Address actor) {
			this.actor = actor;
			return this;
		}
		
		public Tester withLookup(Address address, String name) {
			this.lookups.put(address, name);
			return this;
		}
		
		public Tester event(String event) {
			this.event = event;
			return this;
		}
		
		public Tester context(TestContext ctx) {
			this.ctx = ctx;
			return this;
		}
		
		public Tester withAttr(String attribute, Object value) {
			this.attributes.put(attribute, value);
			return this;
		}
		
		public Tester reject() {
			this.reject = true;
			return this;
		}
		
		@SuppressWarnings("unchecked")
      public void go() {
			EventBuilder builder = eventBuilder()
					.event(event)
					.from(sourceAddress);
		
			if (actor != null) {
				builder.withActor(actor);
			}
			
			if (!attributes.isEmpty()) {
				builder.withAttriburtes(attributes);
			}
			
			PlatformMessage msg = builder.build();
			expectFindPlaceName();
			
			if (actor != null && actor == ACTOR_PERSON) {
				expectFindPersonName();
			}
			else if (actor != null && actor == ACTOR_RULE) {
				expectFindRuleName();
			}
			
			if (!lookups.isEmpty()) {
				for (Map.Entry<Address, String> lookup : lookups.entrySet()) {
					expectNameLookup(lookup.getKey(), lookup.getValue());
				}
			}
			
			Capture<HistoryLogEntry> event1 = expectAndCaptureAppend();
	   	Capture<HistoryLogEntry> event2 = expectAndCaptureAppend();
	   	Capture<HistoryLogEntry> event3 = expectAndCaptureAppend();
	   	Capture<HistoryLogEntry> event4 = null;
	   	if (actor != null && actor == ACTOR_PERSON) {
				event4 = expectAndCaptureAppend();
			}
	   	else if (actor != null && actor == ACTOR_RULE) {
	   		event4 = expectAndCaptureAppend();
	   	}
         else if (ctx.types().size() > 3) {
            event4 = expectAndCaptureAppend();
         }
	   	EasyMock.replay(mockAppenderDao, mockNameCache);

	   	if (reject) {
	   		Assert.assertFalse(appender.append(msg));
	   		return;
	   	}
	   	Assert.assertTrue(appender.append(msg));

			if (event4 != null) {
				verifyValues(ctx, event1, event2, event3, event4);
			}
			else {
				verifyValues(ctx, event1, event2, event3);
			}
		}
	}
}

