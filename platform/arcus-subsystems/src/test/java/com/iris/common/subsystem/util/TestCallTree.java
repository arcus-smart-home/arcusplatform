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
package com.iris.common.subsystem.util;
import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.security.SecurityFixtures;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.NotificationCapability.NotifyRequest;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.SimpleModelStore;
import com.iris.messages.model.serv.PersonModel;
import com.iris.messages.model.subs.SecuritySubsystemModel;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.CallTreeEntry;
import com.iris.type.LooselyTypedReference;
import com.iris.util.TypeMarker;


public class TestCallTree {

   private static final Logger LOGGER = LoggerFactory.getLogger(TestCallTree.class);
   
   private CallTree callTree = null;
   private SubsystemContext<SubsystemModel> context =null;
   private final static int SEQ_TIMEOUT=90000;
   public static final String SAFETY_ALERT_KEY = "safety.alert";
   public static final String SECURITY_ALERT_KEY = "security.alert";
   private static final TypeMarker<List<Map<String, Object>>> CALL_TREE_MARKER =
         new TypeMarker<List<Map<String,Object>>>() {};

   
   private SubsystemModel model =null;
   SimpleModelStore store=null;

   private Model owner = null;
   private Model person2 = null;
   private Model person3 = null;
   private Model person4 = null;

   private Model place = null;
   private Model placeModel = null;
   private Model accountModel = null;
   
   Capture<Date> wakeUpTimeout = null;
   Capture<MessageBody> messgaesSent = null;
   
   @SuppressWarnings("unchecked")
   @Before
   public void setUp() throws Exception {
      context = EasyMock.createNiceMock(SubsystemContext.class);
      initEasyMockVariableSupport(context);

      place=new SimpleModel(ModelFixtures.createPlaceAttributes());
      model = new SubsystemModel(new SimpleModel(ModelFixtures.buildServiceAttributes(place.getId(), SubsystemCapability.NAMESPACE).create()));
      owner = new PersonModel(new SimpleModel(ModelFixtures.createPersonAttributes()));

      placeModel = new SimpleModel(ModelFixtures.createPlaceAttributes());
      accountModel = new SimpleModel(SecurityFixtures.createAccountAttributes());
      accountModel.setAttribute(AccountCapability.ATTR_OWNER, owner.getId());

      person2 = new PersonModel(new SimpleModel(ModelFixtures.createPersonAttributes()));
      person3 = new PersonModel(new SimpleModel(ModelFixtures.createPersonAttributes()));
      person4 = new PersonModel(new SimpleModel(ModelFixtures.createPersonAttributes()));

      setupHappyPath(true);
   }   
   
   private void setupHappyPath(boolean callReplay){

      setupCallTree();

      EasyMock.expect(context.model()).andReturn(model).anyTimes();
      EasyMock.expect(context.logger()).andReturn(LOGGER).anyTimes();
      EasyMock.expect(context.getAccountId()).andReturn((UUID) accountModel.getAddress().getId()).anyTimes();
      EasyMock.expect(context.getPlaceId()).andReturn((UUID) placeModel.getAddress().getId()).anyTimes();
      
      store = new SimpleModelStore();
      EasyMock.expect(context.models()).andReturn(store).anyTimes();
      store.addModel(owner.toMap());
      
      
      messgaesSent = EasyMock.newCapture(CaptureType.ALL);
      context.send(EasyMock.anyObject(Address.class), EasyMock.capture(messgaesSent));
      EasyMock.expectLastCall().anyTimes();
      
      wakeUpTimeout = EasyMock.newCapture(CaptureType.ALL);
      EasyMock.expect(context.wakeUpAt(EasyMock.capture(wakeUpTimeout))).andReturn(null).anyTimes();
      
      if(callReplay){
         EasyMock.replay(context);
      }
   }
   private void setupCallTree(){
      callTree = new CallTree(SecuritySubsystemCapability.ATTR_CALLTREE);
      model = new SecuritySubsystemModel(new SimpleModel());

      SecuritySubsystemModel.setCallTree(model, ImmutableList.<Map<String,Object>>of(buildCTE(owner,true).toMap(),
            buildCTE(person2,false).toMap(),
            buildCTE(person3,true).toMap(),
            buildCTE(person4,true).toMap()));
      
   }
   
   @Test
   public void testCallTreeEmpty() {
      SecuritySubsystemModel.setCallTree(model, ImmutableList.<Map<String,Object>>of());
      callTree.notifySequential(context, SECURITY_ALERT_KEY, SEQ_TIMEOUT);
      Assert.assertEquals("should have sent 0 messages",0, messgaesSent.getValues().size());
   }
   
   @Test
   public void testNotifyAll() {
      callTree.notifyParallel(context,SAFETY_ALERT_KEY, ImmutableMap.<String, String> of());
      Assert.assertEquals("should have sent three messages",3, messgaesSent.getValues().size());
   }


   @Test
   public void testCallTreeHappyPath() {
      callTree.notifySequential(context, SECURITY_ALERT_KEY, SEQ_TIMEOUT);
      callTree.onScheduledEvent(mockTimeout(),context);
      callTree.onScheduledEvent(mockTimeout(),context);
      Assert.assertEquals("should have sent three messages",3, messgaesSent.getValues().size());
      Assert.assertEquals("should have sent to owner",owner.getId(), messgaesSent.getValues().get(0).getAttributes().get(NotifyRequest.ATTR_PERSONID));
      Assert.assertEquals("should have sent to person3",person3.getId(), messgaesSent.getValues().get(1).getAttributes().get(NotifyRequest.ATTR_PERSONID));
      Assert.assertEquals("should have sent to person4",person4.getId(), messgaesSent.getValues().get(2).getAttributes().get(NotifyRequest.ATTR_PERSONID));
      Assert.assertEquals("should have registered 2 callbacks",2, wakeUpTimeout.getValues().size());
   }
   
   @Test
   public void testCallTreeSync() {
      callTree.syncCallTree(context);
      List<Map<String,Object>>intialCallTree=getCallTree(context);
      assertEquals(1,intialCallTree.size());
      assertEquals(true,intialCallTree.get(0).get(CallTreeEntry.ATTR_ENABLED));
      store.addModel(person2.toMap());
      callTree.syncCallTree(context);
      List<Map<String,Object>>callTree2=getCallTree(context);
      assertEquals(2,callTree2.size());
      assertEquals(false,callTree2.get(1).get(CallTreeEntry.ATTR_ENABLED));
      context.model().setAttribute(SecuritySubsystemCapability.ATTR_CALLTREE,ImmutableList.<Map<String,Object>>of());
      callTree.syncCallTree(context);
      callTree2=getCallTree(context);
      assertEquals(2,callTree2.size());
   }
   
   private List<Map<String,Object>>getCallTree(SubsystemContext<SubsystemModel> context){
      List<Map<String,Object>>callTree=context.model().getAttribute(CALL_TREE_MARKER, SecuritySubsystemCapability.ATTR_CALLTREE).get();
      return callTree;
   }
   @Test
   public void testCallTreeCancel() {
      ScheduledEvent event = mockTimeout();
      callTree.notifySequential(context, SECURITY_ALERT_KEY, SEQ_TIMEOUT);
      callTree.cancel(context);
      Assert.assertEquals("should have cancelled timeout",false,  SubsystemUtils.isMatchingTimeout(event, context,CallTree.CALL_TREE_TIMEOUT_KEY));
   }
   
   
   
   private ScheduledEvent mockTimeout(){
      ScheduledEvent event = new ScheduledEvent(owner.getAddress(),90000);
      context.setVariable(CallTree.CALL_TREE_TIMEOUT_KEY, new Date().getTime()+200000);
      return event;
   }
   
   private CallTreeEntry buildCTE(Model model,boolean enabled){
      CallTreeEntry cte = new CallTreeEntry();
      cte.setPerson(model.getAddress().getRepresentation());
      cte.setEnabled(enabled);
      return cte;
      
   }
   private static void initEasyMockVariableSupport(final SubsystemContext<? extends SubsystemModel> context){
      // basically this is implementing variable support
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
   }

}

