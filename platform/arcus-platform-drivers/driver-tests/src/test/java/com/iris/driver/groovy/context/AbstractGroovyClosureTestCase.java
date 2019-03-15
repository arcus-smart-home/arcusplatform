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
package com.iris.driver.groovy.context;

import java.util.Date;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;

import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.file.FileDAOModule;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.messaging.memory.InMemoryProtocolMessageBus;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.DeviceDriverDefinition;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.ClasspathResourceConnector;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.messages.address.Address;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;

@Mocks({ DeviceDAO.class, PersonDAO.class, PersonPlaceAssocDAO.class, PlaceDAO.class, PlacePopulationCacheManager.class })
@Modules({ InMemoryMessageModule.class, FileDAOModule.class })
public abstract class AbstractGroovyClosureTestCase extends IrisMockTestCase {
   protected Script script;
   protected DeviceDriverContext context;
   @Inject protected DeviceDAO mockDeviceDao;
   @Inject protected InMemoryProtocolMessageBus protocolBus;
   @Inject protected InMemoryPlatformMessageBus platformBus;
   @Inject protected PlacePopulationCacheManager mockPopulationCacheMgr;
   
   protected Address driverAddress = Fixtures.createDeviceAddress();
   protected Address protocolAddress = Fixtures.createProtocolAddress();

   protected void initTest(String scriptName) throws Exception {
      Capture<Device> deviceRef = Capture.newInstance();
      EasyMock
         .expect(mockDeviceDao.save(EasyMock.capture(deviceRef)))
         .andAnswer(() -> {
            Device device = deviceRef.getValue().copy();
            if(device.getId() == null) {
               device.setId(UUID.randomUUID());
            }
            if(device.getCreated() == null) {
               device.setCreated(new Date());
            }
            device.setModified(new Date());
            return device;
         })
         .anyTimes();
      mockDeviceDao.updateDriverState(EasyMock.notNull(), EasyMock.notNull());
      EasyMock.expectLastCall().anyTimes();
      EasyMock.replay(mockDeviceDao);
      
      Device device = Fixtures.createDevice();
      device.setDriverAddress(driverAddress.getRepresentation());
      device.setProtocolAddress(protocolAddress.getRepresentation());
      DeviceDriver driver = EasyMock.createNiceMock(DeviceDriver.class);
      EasyMock.expect(driver.getDefinition()).andReturn(DeviceDriverDefinition.builder().withName("TestDriver").create()).anyTimes();
      EasyMock.expect(driver.getBaseAttributes()).andReturn(AttributeMap.emptyMap()).anyTimes();
      EasyMock.replay(driver);

      GroovyScriptEngine engine = new GroovyScriptEngine(new ClasspathResourceConnector(TestGroovyCapabilityDefinition.class));
      context = new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr);
      script = engine.createScript(scriptName, new Binding());
      script.run();
      GroovyContextObject.setContext(context);
   }

   @After
   @Override
   public void tearDown() throws Exception {
      GroovyContextObject.clearContext();
      super.tearDown();
   }

}

