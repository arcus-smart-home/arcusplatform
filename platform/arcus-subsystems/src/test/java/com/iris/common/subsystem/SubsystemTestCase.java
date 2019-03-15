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
package com.iris.common.subsystem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.scheduler.ScheduledTask;
import com.iris.common.subsystem.SubsystemContext.ResponseAction;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.common.subsystem.safety.SafetySubsystem;
import com.iris.common.subsystem.security.SecurityFixtures;
import com.iris.io.json.JSON;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.ClasspathDefinitionRegistry;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.TransactionalModelStore;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.type.LooselyTypedReference;
import com.iris.util.TypeMarker;

/**
 *
 */
public class SubsystemTestCase<M extends SubsystemModel> extends Assert {
   private static final Logger logger = LoggerFactory.getLogger(SubsystemTestCase.class);

   protected static final Object NULL_VALUE = new Object();
   protected UUID accountId = UUID.randomUUID();
   protected UUID placeId = UUID.randomUUID();
   protected Model placeModel = null;
   protected Model accountModel = null;
   protected Address clientAddress = Address.clientAddress("test", "session");

   protected M model;
   protected TransactionalModelStore store = new TransactionalModelStore();
   protected SubsystemContext<M> context = EasyMock.createNiceMock(SubsystemContext.class);
   protected Capture<MessageBody> responses = EasyMock.newCapture(CaptureType.ALL);
   protected Capture<MessageBody> requests = EasyMock.newCapture(CaptureType.ALL);
   protected Capture<MessageBody> broadcasts = EasyMock.newCapture(CaptureType.ALL);
   protected Capture<Address> requestAddresses = EasyMock.newCapture(CaptureType.ALL);
   protected Capture<Address> sendAddresses = EasyMock.newCapture(CaptureType.ALL);
   protected Capture<MessageBody> sends = EasyMock.newCapture(CaptureType.ALL);
   protected List<String> requestIds = new ArrayList<>();
   
   protected List<SendAndExpect> sendAndExpectOperations = new ArrayList<>();
   
   public final class SendAndExpect {
      private Address requestAddress;
      private MessageBody message;
      private long timeout;
      private TimeUnit timeUnit;
      private SubsystemContext.ResponseAction action;
      
      public SendAndExpect(
            Address requestAddress, 
            MessageBody message, 
            long timeout, 
            TimeUnit timeUnit,
            ResponseAction action
      ) {
         super();
         this.requestAddress = requestAddress;
         this.message = message;
         this.timeout = timeout;
         this.timeUnit = timeUnit;
         this.action = action;
      }

      public Address getRequestAddress() {
         return requestAddress;
      }

      public void setRequestAddress(Address requestAddress) {
         this.requestAddress = requestAddress;
      }

      public MessageBody getMessage() {
         return message;
      }

      public void setMessage(MessageBody message) {
         this.message = message;
      }

      public long getTimeout() {
         return timeout;
      }

      public void setTimeout(long timeout) {
         this.timeout = timeout;
      }

      public TimeUnit getTimeUnit() {
         return timeUnit;
      }

      public void setTimeUnit(TimeUnit timeUnit) {
         this.timeUnit = timeUnit;
      }

      public SubsystemContext.ResponseAction getAction() {
         return action;
      }

      public void setAction(SubsystemContext.ResponseAction action) {
         this.action = action;
      }

      
   }

   private Set<Date> timeouts = new HashSet<>();
   
   protected Map<String, Object> getAdditionalAttributesForSubsystemModel() {
   	return ImmutableMap.<String, Object>of();
   }

   @Before
   public void setUp() {
      placeModel = new SimpleModel(ModelFixtures.createPlaceAttributes());
      placeId = (UUID)placeModel.getAddress().getId();
      accountModel = new SimpleModel(SecurityFixtures.createAccountAttributes());
      accountId = (UUID)accountModel.getAddress().getId();
      model = createSubsystemModel();
      
      EasyMock.expect(context.getPlaceId()).andReturn(placeId).anyTimes();
      EasyMock.expect(context.getAccountId()).andReturn(accountId).anyTimes();
      EasyMock.expect(context.model()).andReturn(model).anyTimes();
      EasyMock.expect(context.models()).andReturn(store).anyTimes();
      EasyMock.expect(context.getLocalTime()).andReturn(Calendar.getInstance()).anyTimes();

      EasyMock.expect(context.logger()).andReturn(LoggerFactory.getLogger(getClass())).anyTimes();
      context.sendResponse(EasyMock.isA(PlatformMessage.class), EasyMock.capture(responses));
      EasyMock
         .expectLastCall()
         .andAnswer(new IAnswer<Void>() {
            @Override
            public Void answer() {
               logger.debug("Respond: {}", responses.getValues().get(responses.getValues().size() - 1));
               return null;
            }
         })
         .anyTimes();
      
      EasyMock
         .expect(context.request(EasyMock.capture(requestAddresses), EasyMock.capture(requests)))
         .andAnswer(new IAnswer<String>() {
            @Override
            public String answer() throws Throwable {
               String correlationId = UUID.randomUUID().toString();
               requestIds.add(correlationId);
               logger.info("Request: {} {}", requestAddresses.getValues().get(requestAddresses.getValues().size() - 1), requests.getValues().get(requests.getValues().size() - 1));
               return correlationId;
            }
         })
         .anyTimes();
      
      {
         final Capture<Address> addressRef = Capture.newInstance(CaptureType.LAST);
         final Capture<MessageBody> messageRef = Capture.newInstance(CaptureType.LAST);
         final Capture<Long> timeoutRef = Capture.newInstance(CaptureType.LAST);
         final Capture<TimeUnit> timeunitRef = Capture.newInstance(CaptureType.LAST);
         final Capture<SubsystemContext.ResponseAction> actionRef = Capture.newInstance(CaptureType.LAST);
         context.sendAndExpectResponse(
               EasyMock.capture(addressRef), 
               EasyMock.capture(messageRef), 
               EasyMock.captureLong(timeoutRef), 
               EasyMock.capture(timeunitRef), 
               EasyMock.capture(actionRef)
         );
         EasyMock
            .expectLastCall()
            .andAnswer(new IAnswer<Void>() {
               @Override
               public Void answer() throws Throwable {
                  sendAndExpectOperations.add(new SendAndExpect(
                     addressRef.getValue(),
                     messageRef.getValue(),
                     timeoutRef.getValue(),
                     timeunitRef.getValue(),
                     actionRef.getValue()
                  ));
                  return null;
               }
            })
            .anyTimes();
      }
      
      final Capture<Date> wakeUpAt = Capture.newInstance(CaptureType.LAST);
      EasyMock
      	.expect(context.wakeUpAt(EasyMock.capture(wakeUpAt)))
      	.andAnswer(new IAnswer<ScheduledTask>() {
				@Override
				public ScheduledTask answer() throws Throwable {
					final Date time = wakeUpAt.getValue();
					return schedule(time);
				}
			})
      	.anyTimes();
      
      final Capture<Long> wakeUpIn = Capture.newInstance(CaptureType.LAST);
      final Capture<TimeUnit> wakeUpInUnits = Capture.newInstance(CaptureType.LAST);
      EasyMock
	   	.expect(context.wakeUpIn(EasyMock.captureLong(wakeUpIn), EasyMock.capture(wakeUpInUnits)))
	   	.andAnswer(new IAnswer<ScheduledTask>() {
				@Override
				public ScheduledTask answer() throws Throwable {
					final Date time = new Date(System.currentTimeMillis() + wakeUpInUnits.getValue().toMillis(wakeUpIn.getValue()));
					return schedule(time);
				}
			})
	   	.anyTimes();
      
      context.broadcast(EasyMock.capture(broadcasts));
      EasyMock
         .expectLastCall()
         .andAnswer(new IAnswer<Void>() {
            @Override
            public Void answer() {
               logger.debug("Broadcast: {}", broadcasts.getValues().get(broadcasts.getValues().size() - 1));
               return null;
            }
         })
         .anyTimes();
      
      context.send(EasyMock.capture(sendAddresses), EasyMock.capture(sends));
      EasyMock
         .expectLastCall()
         .andAnswer(new IAnswer<Void>() {
            @Override
            public Void answer() {
               logger.debug("Send: {} {}", sendAddresses.getValues().get(sendAddresses.getValues().size() - 1), sends.getValues().get(sends.getValues().size() - 1));
               return null;
            }
         })
         .anyTimes();

      // basically this is implementing variable support
      // kind of ugly, but functional
      final Capture<String> nameRef = EasyMock.newCapture(CaptureType.LAST);
      final Capture<Object> valueRef = EasyMock.newCapture(CaptureType.LAST);
      EasyMock
         .expect(context.getVariable(EasyMock.capture(nameRef)))
         .andAnswer(new IAnswer<LooselyTypedReference>() {
            @Override
            public LooselyTypedReference answer() throws Throwable {
               String json = context.model().getAttribute(TypeMarker.string(), "_subvars:" + nameRef.getValue(), "null");
               return JSON.fromJson(json, LooselyTypedReference.class);
            }
         })
         .anyTimes();
      context.setVariable(EasyMock.capture(nameRef), EasyMock.capture(valueRef));
      EasyMock
         .expectLastCall()
         .andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
               context.model().setAttribute("_subvars:" + nameRef.getValue(), JSON.toJson(valueRef.getValue()));
               return null;
            }
         })
         .anyTimes();

      context.commit();
      EasyMock
      	.expectLastCall()
         .andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
               store.commit();
               return null;
            }
         })
      	.anyTimes();
      
      EasyMock.replay(context);
   }
   
   /**
    * Wires in the given subsystem to store change events
    * and sends the startup events.
    * If subsystem version is null this will send Added then Started
    * If subsystem version is not null this will just be Started
    * @param subsystem
    * @throws Exception
    */
   protected void init(final Subsystem<M> subsystem) throws Exception {
   	store.addListener(new Listener<ModelEvent>() {
			@Override
			public void onEvent(ModelEvent event) {
				subsystem.onEvent(event, context);
			}
		});
		if(subsystem instanceof BaseSubsystem) {
			((BaseSubsystem<?>) subsystem).setDefinitionRegistry(ClasspathDefinitionRegistry.instance());
		}
		else if(subsystem instanceof SafetySubsystem) {
			((SafetySubsystem) subsystem).setDefinitionRegistry(ClasspathDefinitionRegistry.instance());
		}
		if(context.model().getVersion() == null) {
			subsystem.onEvent(SubsystemLifecycleEvent.added(context.model().getAddress()), context);
		}
		subsystem.onEvent(SubsystemLifecycleEvent.started(context.model().getAddress()), context);
		commit();
	}

	private ScheduledTask schedule(final Date time) {
		timeouts.add(time);
		return new ScheduledTask() {
			@Override
			public boolean isPending() {
				return true;
			}
			
			@Override
			public boolean cancel() {
				return timeouts.remove(time);
			}
		};
   }
   
   protected M createSubsystemModel() {
   	Map<String, Object> attributes =
         ModelFixtures
            .buildServiceAttributes(placeId, SubsystemCapability.NAMESPACE)
            .create();  
   	return (M) new SubsystemModel(new SimpleModel(attributes));
   }   

   protected MessageReceivedEvent request(String name) {
      return request(MessageBody.buildMessage(name, ImmutableMap.<String,Object>of()));
   }

   protected MessageReceivedEvent request(String name, Map<String, Object> attributes) {
      return request(MessageBody.buildMessage(name, attributes));
   }

   protected MessageReceivedEvent request(MessageBody body) {
      return request(body, clientAddress, null);
   }

   protected MessageReceivedEvent request(MessageBody body, String correlationId) {
      return request(body, clientAddress, null);
   }

   protected MessageReceivedEvent request(MessageBody body, Address source, String correlationId) {
      PlatformMessage.Builder builder = PlatformMessage.buildRequest(body, source, model.getAddress());
      if(correlationId != null) {
         builder.withCorrelationId(correlationId);
      }

      return new MessageReceivedEvent(builder.create());
   }

   protected MessageReceivedEvent event(MessageBody body, Address source) {
      return event(body, source, null);
   }
   
   protected MessageReceivedEvent event(MessageBody body, Address source, String correlationId) {
      PlatformMessage.Builder builder = PlatformMessage.buildBroadcast(body, source);
      if(correlationId != null) {
         builder.withCorrelationId(correlationId);
      }

      return new MessageReceivedEvent(builder.create());
   }
   
   protected ScheduledEvent timeout() {
      return timeout(SubsystemUtils.VAR_TIMEOUT);
   }

   protected ScheduledEvent timeout(String name) {
      Date timeout = SubsystemUtils.getTimeout(context, name).get();
      return new ScheduledEvent(model.getAddress(), timeout.getTime());
   }

   protected <T> void assertSetEquals(Set<T> actual, T... expected) {
      assertEquals(ImmutableSet.of(expected), actual);
   }
   
   protected Model addModel(Map<String,Object> attributes) {
	  return store.addModel(attributes);
   }
   
   protected void online(Model model) {
   	connect(model.getAddress());
   }
   
   protected void offline(Model model) {
   	disconnect(model.getAddress());
   }
   
   public void connect(Address address) {
   	if(address.isHubAddress()) {
   		updateModel(
   			address, 
   			ImmutableMap.<String, Object>of(
   				HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_ONLINE,
   				HubConnectionCapability.ATTR_LASTCHANGE, new Date()
				)
		);
   	}else{
      	updateModel(
      			address, 
      			ImmutableMap.<String, Object>of(
      					DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE,
      					DeviceConnectionCapability.ATTR_LASTCHANGE, new Date()
   				)
   		);
   	}
   }

   public void disconnect(Address address) {
   	if(address.isHubAddress()) {
   		updateModel(
   			address, 
   			ImmutableMap.<String, Object>of(
   				HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_OFFLINE,
   				HubConnectionCapability.ATTR_LASTCHANGE, new Date()
				)
		);
   	}else{
      	updateModel(
      			address, 
      			ImmutableMap.<String, Object>of(
      					DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE,
      					DeviceConnectionCapability.ATTR_LASTCHANGE, new Date()
   				)
   		);
   	}
   }

   protected void updateModel(String address, Map<String,Object> attributes) {
	  updateModel(Address.fromString(address), attributes);
   }

   protected void updateModel(Address address, Map<String,Object> attributes) {
	  store.updateModel(address, attributes);
   }

	protected void removeModel(String address) {
		removeModel(Address.fromString(address));
	}

   protected void removeModel(Model model) {
      removeModel(model.getAddress());
   }

   protected void removeModel(Address address) {
		store.removeModel(address);
	}
   
   protected void commit() {
   	context.commit();
   }
   
   protected Set<String> addressesOf(Model... models) {
   	Set<String> addresses = new HashSet<>(models.length * 2 + 1);
   	for(Model m: models) {
   		addresses.add(m.getAddress().getRepresentation());
   	}
   	return addresses;
   }
   
   protected void assertTimeoutSet() {
   	assertTimeoutSet(SubsystemUtils.VAR_TIMEOUT);
   }
   
   protected void assertTimeoutSet(String name) {
   	Date timeout = context.getVariable(name).as(Date.class);
   	assertNotNull(String.format("No [%s] timeout set", name), timeout);
   	assertTrue("Timeout improperly scheduled", timeouts.contains(timeout));
   }
   
   protected void assertTimeoutCleared() {
   	assertTimeoutCleared(SubsystemUtils.VAR_TIMEOUT);
   }
   
   protected void assertTimeoutCleared(String name) {
   	Date timeout = context.getVariable(name).as(Date.class);
   	assertNull("Timeout variable still set", timeout);
   }
   
   protected void assertNoRequests() {
   	assertTrue("Expected no requests but there were " + requests.getValues(), !requests.hasCaptured());
   }
   
	protected void assertContainsBroadcastEventWithAttrs(final String event, final Map<String,Object>attrs){
		assertContainsBroadcastEventWithAttrs(event, attrs, null);
	}
	
	protected void assertContainsRequestMessageWithAttrs(final String msgType, final Map<String,Object>attrs){
	      boolean isMatched = containsRequestMessageWithAttrs(msgType, attrs);
	      assertTrue(String.format("could not locate event %s with attributes %s",msgType,attrs),isMatched);		         	 
	}
	
	protected void assertNotContainsRequestMessageWithAttrs(final String msgType, final Map<String,Object>attrs){
	      boolean isMatched = containsRequestMessageWithAttrs(msgType, attrs);
	      assertFalse(String.format("should not locate event %s with attributes %s",msgType,attrs),isMatched);		         	 
	}
	
	protected boolean containsRequestMessageWithAttrs(final String msgType, final Map<String,Object>attrs) {		
	      boolean isMatched = containsMessageWithAttrs(requests, msgType, attrs);
	      return isMatched;
	}
	
	protected boolean containsSendMessageWithAttrs(final String msgType, final Map<String,Object>attrs) {		
	      boolean isMatched = containsMessageWithAttrs(sends, msgType, attrs);
	      return isMatched;
	}
	
	protected void assertErrorResponse(String expectedErr) {
		List<MessageBody> bodies = responses.getValues();
		assertEquals(1, bodies.size());
		boolean found = false;
		for(MessageBody curResponse : bodies) {
			if(curResponse instanceof ErrorEvent) {
				if(((ErrorEvent)curResponse).getCode().equalsIgnoreCase(expectedErr)) {
					found = true;
					break;  //found it
				}
			}
		}
		if(!found) fail("Did not find ErrorResponse with code "+expectedErr);
	}
	
	protected void clearRequests() {
		requestIds.clear();
		requestAddresses.reset();
		requests.reset();
		sendAddresses.reset();
		sends.reset();
	}
	
	protected void assertSendAndExpect(Address sendToAddress, String msgType) {
		boolean found = false;
		for(SendAndExpect cur : sendAndExpectOperations) {
			if(cur.getRequestAddress().equals(sendToAddress) && cur.getMessage().getMessageType().equals(msgType)) {
				found = true;
				break;
			}
		}
		if(!found) {
			fail("assertSendAndExpect did not find address["+sendToAddress+"], msgType["+msgType+"]");
		}
	}
	
	protected void assertNoHubRequestsSent() {
		assertTrue("Expected hub requests to be empty, but was " + sendAndExpectOperations, sendAndExpectOperations.isEmpty());
	}
	
	private boolean containsMessageWithAttrs(Capture<MessageBody> capturedMsg, final String msgType, final Map<String,Object>attrs) {
		FluentIterable<MessageBody> matched = FluentIterable.from(capturedMsg.getValues()).filter(new Predicate<MessageBody>() {
	         public boolean apply(MessageBody message) {
	            if(!message.getMessageType().equals(msgType)){
	               return false;
	            }
	            for(Map.Entry<String,Object>attr:attrs.entrySet()){
	            	if(attr.getValue() == NULL_VALUE) {
	            		if( message.getAttributes().get(attr.getKey()) != null ) {
	            			System.out.println(String.format("[%s] does not match [expected, actual]=[ %s, %s]",attr.getKey(), null, message.getAttributes().get(attr.getKey())));
	            			return false;
	            		}else {
	            			continue;
	            		}
	            	}else if(!attr.getValue().equals(message.getAttributes().get(attr.getKey()))){
	            	   System.out.println(String.format("[%s] does not match [expected, actual]=[ %s, %s]",attr.getKey(), attr.getValue(), message.getAttributes().get(attr.getKey())));
	                  return false;
	               }
	            }
	            return true;
	          }
	        });
	      boolean isMatched = (matched != null && matched.size() > 0);
	      return isMatched;
	}
	
	protected void assertContainsBroadcastEventWithAttrs(final String event, final Map<String,Object>attrs, Integer fireCount){
	      FluentIterable<MessageBody> matched = FluentIterable.from(broadcasts.getValues()).filter(new Predicate<MessageBody>() {
	         public boolean apply(MessageBody message) {
	            if(!message.getMessageType().equals(event)){
	               return false;
	            }
	            for(Map.Entry<String,Object>attr:attrs.entrySet()){
	            	if(attr.getValue() == NULL_VALUE) {
	            		if( message.getAttributes().get(attr.getKey()) != null ) {
	            			System.out.println(String.format("[%s] does not match [expected, actual]=[ %s, %s]",attr.getKey(), null, message.getAttributes().get(attr.getKey())));
	            			return false;
	            		}else {
	            			continue;
	            		}
	            	}else if(!attr.getValue().equals(message.getAttributes().get(attr.getKey()))){
	            	   System.out.println(String.format("[%s] does not match [expected, actual]=[ %s, %s]",attr.getKey(), attr.getValue(), message.getAttributes().get(attr.getKey())));
	                  return false;
	               }
	            }
	            return true;
	          }
	        });
	      if(matched == null || matched.size() > 1){
	         for(MessageBody mb:broadcasts.getValues()){
	            System.out.println(String.format("the following messages were broadcast message %s",mb));
	         }
	      }
	      boolean isMatched = (matched != null && matched.size() > 0);
	      assertTrue(String.format("could not locate event %s with attributes %s",event,attrs),isMatched);	      
    	  if(fireCount != null && fireCount.intValue() > 0 && matched != null) {
    		  assertTrue(String.format("fire count does not match [expected, actual] =[%s, %s]",fireCount.intValue(),matched.size()), fireCount.intValue() == matched.size());
	      }
 	}
	

}

