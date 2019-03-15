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
package com.iris.platform.services.hub.handlers;

import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.client.model.device.ClientDeviceModel.Base;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.HubRegistrationDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ClientAddress;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.capability.PairingSubsystemCapability;
import com.iris.messages.capability.PlaceCapability.RegisterHubV2Request;
import com.iris.messages.capability.PlaceCapability.RegisterHubV2Response;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Hub;
import com.iris.messages.model.HubRegistration;
import com.iris.messages.model.HubRegistration.RegistrationState;
import com.iris.messages.model.Place;
import com.iris.messages.type.Population;
import com.iris.platform.hub.registration.HubRegistrationConfig;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.services.place.handlers.RegisterHubV2Handler;
import com.iris.platform.manufacture.kitting.dao.ManufactureKittingDao;
import com.iris.platform.manufacture.kitting.kit.Kit;
import com.iris.platform.manufacture.kitting.kit.KitDevice;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.HubRegistrationFixtures;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({
   HubDAO.class,
   HubRegistrationDAO.class, 
   PlacePopulationCacheManager.class,
   PairingDeviceDao.class,
   ManufactureKittingDao.class  
})
@Modules({InMemoryMessageModule.class })
public class TestRegisterHubV2Handler extends IrisMockTestCase {

   @Inject private HubDAO hubDao;
   @Inject private PairingDeviceDao pairDao;
   @Inject private ManufactureKittingDao kitDao;
   @Inject private HubRegistrationDAO hubRegDao;
   @Inject private RegisterHubV2Handler handler;
   @Inject private HubRegistrationConfig config;
   @Inject protected PlacePopulationCacheManager mockPopulationCacheMgr;
   
   private Place place;
   private ClientAddress msgSource;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      
      place = Fixtures.createPlace();
      place.setId(UUID.randomUUID());
      msgSource = Fixtures.createClientAddress();
      
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   }

   @Override
   public void tearDown() throws Exception {
     
      super.tearDown();
   }

   @Test
   public void testInvalidHubId() throws Exception {
      String hubId = "LWW-1000";
      EasyMock.expect(hubDao.findById(hubId)).andReturn(null);
      EasyMock.expect(hubRegDao.findById(hubId)).andReturn(null);
      replay();
      MessageBody msgBody = RegisterHubV2Request.builder().withHubId(hubId).build();
      try {
         MessageBody response = handler.handleRequest(place, PlatformMessage.builder().withPayload(msgBody).from(msgSource).create());
         assertEquals(RegisterHubV2Response.NAME, response.getMessageType());
         assertNull(RegisterHubV2Response.getHub(response));
         assertEquals(RegisterHubV2Response.STATE_OFFLINE, RegisterHubV2Response.getState(response));
      } catch(Exception e) {
         fail(e.getMessage());
      }
      verify();
   }
   
   
   @Test
   public void testHubDuringRegistrationDownloading() throws Exception {
      HubRegistration hubReg = HubRegistrationFixtures.createHubRegistration();
      hubReg.setDownloadProgress(50);
      EasyMock.expect(hubDao.findById(hubReg.getId())).andReturn(null);     
      EasyMock.expect(hubRegDao.findById(hubReg.getId())).andReturn(hubReg);
      replay();
      MessageBody msgBody = RegisterHubV2Request.builder().withHubId(hubReg.getId()).build();
      try {
         MessageBody response = handler.handleRequest(place, PlatformMessage.builder().withPayload(msgBody).from(msgSource).create());
         assertEquals(RegisterHubV2Response.NAME, response.getMessageType());
         assertNull(RegisterHubV2Response.getHub(response));
         assertEquals(RegisterHubV2Response.STATE_DOWNLOADING, RegisterHubV2Response.getState(response));
         assertEquals(new Integer(50), RegisterHubV2Response.getProgress(response));
      } catch(Exception e) {
         fail(e.getMessage());
      }
      verify();
   }
   
   private Hub createHubFromHubRegistration(HubRegistration hubReg) {
      Hub hub = Fixtures.createHub();
      hub.setId(hubReg.getId());
      hub.setPlace(null);  //disassociate hub with any account/place
      hub.setAccount(null);
      return hub;
   }
   
   @Test
   public void testHubRegisterSuccess() throws Exception {
      HubRegistration hubReg = HubRegistrationFixtures.createHubRegistration();
      hubReg.setState(RegistrationState.APPLYING);
      hubReg.setDownloadProgress(100);
      Hub hub = createHubFromHubRegistration(hubReg);
      EasyMock.expect(kitDao.getKit(hubReg.getId())).andReturn(null);
      EasyMock.expect(hubDao.findById(hubReg.getId())).andReturn(hub);     
      EasyMock.expect(hubRegDao.findById(hubReg.getId())).andReturn(hubReg);
      EasyMock.expect(hubDao.findHubForPlace(place.getId())).andReturn(null);  //place has no existing hub
      Capture<Hub> captureSavedHub = EasyMock.newCapture(CaptureType.LAST);
      Capture<HubRegistration> captureSavedHubReg = EasyMock.newCapture(CaptureType.LAST);
      EasyMock.expect(hubDao.save(EasyMock.capture(captureSavedHub))).andReturn(hub);
      EasyMock.expect(hubRegDao.save(EasyMock.capture(captureSavedHubReg))).andReturn(hubReg);
      
      EasyMock.expect(hubDao.findHubModel(hubReg.getId())).andAnswer(new IAnswer<ModelEntity>()
      {
         @Override
         public ModelEntity answer() throws Throwable
         {
            ModelEntity model = new ModelEntity();
            model.setAttribute(HubCapability.ATTR_ID, hub.getId());
            return model;
         }
      });
      
      
      replay();
      MessageBody msgBody = RegisterHubV2Request.builder().withHubId(hubReg.getId()).build();
      try {
         MessageBody response = handler.handleRequest(place, PlatformMessage.builder().withPayload(msgBody).from(msgSource).create());
         assertEquals(RegisterHubV2Response.NAME, response.getMessageType());
         assertNotNull(RegisterHubV2Response.getHub(response));
         assertEquals(RegisterHubV2Response.STATE_REGISTERED, RegisterHubV2Response.getState(response));
         assertEquals(new Integer(100), RegisterHubV2Response.getProgress(response));
         
         Hub savedHub = captureSavedHub.getValue();
         assertEquals(HubCapability.REGISTRATIONSTATE_REGISTERED, savedHub.getRegistrationState());
         assertEquals(place.getAccount(), savedHub.getAccount());
         assertEquals(place.getId(), savedHub.getPlace());
         
         HubRegistration savedHubReg = captureSavedHubReg.getValue();
         assertEquals(RegistrationState.REGISTERED, savedHubReg.getState());
         
      } catch(Exception e) {
         fail(e.getMessage());
      }
      verify();
   }   
   
   @Test
   public void testHubRegisterOphaned() {
      HubRegistration hubReg = HubRegistrationFixtures.createHubRegistration();
      hubReg.setState(RegistrationState.APPLYING);
      hubReg.setDownloadProgress(100);
      Hub hub = createHubFromHubRegistration(hubReg);
      hub.setRegistrationState(HubCapability.REGISTRATIONSTATE_ORPHANED);
      EasyMock.expect(hubDao.findById(hubReg.getId())).andReturn(hub);    
      
      replay();
      assertErrorResponse( RegisterHubV2Request.builder().withHubId(hubReg.getId()).build(), RegisterHubV2Response.CODE_ERROR_REGISTER_ORPHANEDHUB);
      
      verify();
   }

   
   @Test
   public void testHubInvalidIdFormat() {
      assertErrorResponse( RegisterHubV2Request.builder().withHubId("LWW-a123").build(), Errors.CODE_INVALID_PARAM);
      
   }
   
   private void assertErrorResponse(MessageBody msgBody, String code) {
      try {
         handler.handleRequest(place, PlatformMessage.builder().withPayload(msgBody).from(msgSource).create());
         fail("should fail");
         
      } catch(ErrorEventException e) {
         assertEquals(code, e.getCode());
      } catch(Exception e) {
         fail(e.getMessage());
      }
   }
   
   @Test
   public void testMissingHubId() {
      assertErrorResponse( RegisterHubV2Request.builder().withHubId("").build(), Errors.CODE_MISSING_PARAM);
   }
   
   @Test
   public void testPlaceAlreadyHasHub() {
      HubRegistration hubReg = HubRegistrationFixtures.createHubRegistration();
      hubReg.setState(RegistrationState.APPLYING);
      hubReg.setDownloadProgress(100);
      Hub hub = createHubFromHubRegistration(hubReg);
      EasyMock.expect(hubDao.findById(hubReg.getId())).andReturn(hub);    
     
      
      Hub existingHub = Fixtures.createHub();
      existingHub.setPlace(place.getId());
      existingHub.setAccount(place.getAccount());
      existingHub.setRegistrationState(HubCapability.REGISTRATIONSTATE_REGISTERED);
      EasyMock.expect(hubDao.findHubForPlace(place.getId())).andReturn(existingHub);  //place has an existing hub
      
      replay();
      assertErrorResponse( RegisterHubV2Request.builder().withHubId(hubReg.getId()).build(), RegisterHubV2Response.CODE_ERROR_REGISTER_ACTIVEHUB);  
      
      verify();
   }
   
   @Test
   public void testHubAlreadyRegistered() {
      HubRegistration hubReg = HubRegistrationFixtures.createHubRegistration();
      hubReg.setState(RegistrationState.APPLYING);
      hubReg.setDownloadProgress(100);
      Hub hub = createHubFromHubRegistration(hubReg);
      hub.setAccount(UUID.randomUUID()); //set hub account to a different account id
      EasyMock.expect(hubDao.findById(hubReg.getId())).andReturn(hub);
      
      replay();
      assertErrorResponse( RegisterHubV2Request.builder().withHubId(hubReg.getId()).build(), RegisterHubV2Response.CODE_ERROR_REGISTER_ALREADYREGISTERED);
      
      verify();
   }
   
   @Test
   public void testHubUpgradeError() {
      HubRegistration hubReg = HubRegistrationFixtures.createHubRegistration();
      hubReg.setUpgradeErrorCode("someError");
      hubReg.setUpgradeErrorMessage("someErrorMessage");
      hubReg.setUpgradeErrorTime(new Date());
      EasyMock.expect(hubDao.findById(hubReg.getId())).andReturn(null);     
      EasyMock.expect(hubRegDao.findById(hubReg.getId())).andReturn(hubReg);
      
      replay();
      
      assertErrorResponse( RegisterHubV2Request.builder().withHubId(hubReg.getId()).build(), RegisterHubV2Response.CODE_ERROR_FWUPGRADE_FAILED);
      
      verify();
   }
   
   @Test
   public void testHubUpgradeErrorTooOld() {
      HubRegistration hubReg = HubRegistrationFixtures.createHubRegistration();
      hubReg.setUpgradeErrorCode("someError");
      hubReg.setUpgradeErrorMessage("someErrorMessage");
      //already pass the max age threshold
      hubReg.setUpgradeErrorTime(new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(config.getUpgradeErrorMaxAgeInMin()+2)));
      EasyMock.expect(hubDao.findById(hubReg.getId())).andReturn(null);     
      EasyMock.expect(hubRegDao.findById(hubReg.getId())).andReturn(hubReg);
      
      replay();
      
      MessageBody msgBody = RegisterHubV2Request.builder().withHubId(hubReg.getId()).build();
      try {
         MessageBody response = handler.handleRequest(place, PlatformMessage.builder().withPayload(msgBody).from(msgSource).create());
         assertEquals(RegisterHubV2Response.NAME, response.getMessageType());
         assertNull(RegisterHubV2Response.getHub(response));
         assertEquals(RegisterHubV2Response.STATE_DOWNLOADING, RegisterHubV2Response.getState(response));
         assertEquals(new Integer(50), RegisterHubV2Response.getProgress(response));
      } catch(Exception e) {
         fail(e.getMessage());
      }
      
      verify();
   }
   
   @Test
   public void testHubIsPartOfKit() throws Exception {
	      HubRegistration hubReg = HubRegistrationFixtures.createHubRegistration();
	      hubReg.setState(RegistrationState.APPLYING);
	      hubReg.setDownloadProgress(100);
	      Hub hub = createHubFromHubRegistration(hubReg);
	      Kit kit = Kit.builder()
	    		  		.withType("test")
	    		  		.withHubId(hub.getId())
	    		  		.withDevice(
	    		  				KitDevice.builder()
	    		  					.withEuid("8011223344556677")
	    		  					.withInstallCode("0011223344556677889900AABBCCDDEEFFBC49")
	    		  					.build())
	    		  		.build();
	      
	      EasyMock.expect(kitDao.getKit(hubReg.getId())).andReturn(kit);
	      EasyMock.expect(hubDao.findById(hubReg.getId())).andReturn(hub);     
	      EasyMock.expect(hubRegDao.findById(hubReg.getId())).andReturn(hubReg);
	      EasyMock.expect(hubDao.findHubForPlace(place.getId())).andReturn(null);  //place has no existing hub
	      EasyMock.expect(pairDao.save(EasyMock.anyObject())).andAnswer(() -> {
	      		PairingDevice dev = (PairingDevice) EasyMock.getCurrentArguments()[0];
	      		dev.setId(hub.getPlace(), 1);
	      		Set<String> tags = (Set<String>) dev.getAttribute(Base.ATTR_TAGS);
	      		assert(tags.contains("KIT"));
	      		return dev;
   			});
	      Capture<Hub> captureSavedHub = EasyMock.newCapture(CaptureType.LAST);
	      Capture<HubRegistration> captureSavedHubReg = EasyMock.newCapture(CaptureType.LAST);
	      EasyMock.expect(hubDao.save(EasyMock.capture(captureSavedHub))).andReturn(hub);
	      EasyMock.expect(hubRegDao.save(EasyMock.capture(captureSavedHubReg))).andReturn(hubReg);
	      
	      EasyMock.expect(hubDao.findHubModel(hubReg.getId())).andAnswer(new IAnswer<ModelEntity>()
	      {
	         @Override
	         public ModelEntity answer() throws Throwable
	         {
	            ModelEntity model = new ModelEntity();
	            model.setAttribute(HubCapability.ATTR_ID, hub.getId());
	            return model;
	         }
	      });
	      
	      
	      replay();
	      MessageBody msgBody = RegisterHubV2Request.builder().withHubId(hubReg.getId()).build();

         MessageBody response = handler.handleRequest(place, PlatformMessage.builder().withPayload(msgBody).from(msgSource).create());
         assertEquals(RegisterHubV2Response.NAME, response.getMessageType());
         assertNotNull(RegisterHubV2Response.getHub(response));
         assertEquals(RegisterHubV2Response.STATE_REGISTERED, RegisterHubV2Response.getState(response));
         assertEquals(new Integer(100), RegisterHubV2Response.getProgress(response));
         
         Hub savedHub = captureSavedHub.getValue();
         assertEquals(HubCapability.REGISTRATIONSTATE_REGISTERED, savedHub.getRegistrationState());
         assertEquals(place.getAccount(), savedHub.getAccount());
         assertEquals(place.getId(), savedHub.getPlace());
         
         HubRegistration savedHubReg = captureSavedHubReg.getValue();
         assertEquals(RegistrationState.REGISTERED, savedHubReg.getState());
	         
	      verify();

   }
   
   
}

