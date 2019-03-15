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
package com.iris.driver.groovy.ipcd;

import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Provides;
import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryProtocolMessageBus;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.DeviceDriverDefinition;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.ClasspathResourceConnector;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.driver.groovy.GroovyDriverTestCase;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.model.CommandType;
import com.iris.protocol.ipcd.message.model.IpcdCommand;
import com.iris.protocol.ipcd.message.model.MessageType;
import com.iris.protocol.ipcd.message.model.SetDeviceInfoCommand;

import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;

public class TestIpcdSender extends GroovyDriverTestCase {
   private GroovyScriptEngine engine;
   private Script script;
   private DeviceDriverContext context;
   private InMemoryProtocolMessageBus bus;
   private IpcdProtocol protocol;

   private Address driverAddress = Fixtures.createDeviceAddress();
   private Address protocolAddress = Address.protocolAddress(IpcdProtocol.NAMESPACE, ProtocolDeviceId.hashDeviceId("BlackBox:ms2:1234"));

   @SuppressWarnings("unchecked")
   @Before
   public void setUp() throws Exception {
      protocol = IpcdProtocol.INSTANCE;
      Device device = Fixtures.createDevice();
      device.setAddress(driverAddress.getRepresentation());
      device.setProtocolAddress(protocolAddress.getRepresentation());
      device.setProtocolAttributes(IpcdFixtures.createProtocolAttributes());
      DeviceDriver driver = EasyMock.createNiceMock(DeviceDriver.class);
      EasyMock.expect(driver.getDefinition()).andReturn(DeviceDriverDefinition.builder().withName("TestDriver").create()).anyTimes();
      EasyMock.expect(driver.getBaseAttributes()).andReturn(AttributeMap.emptyMap()).anyTimes();
      EasyMock.replay(driver);

      ServiceLocator.init(GuiceServiceLocator.create(
            Bootstrap.builder()
               .withModuleClasses(InMemoryMessageModule.class)
               .withModules(new AbstractIrisModule() {

                  @Override
                  protected void configure() {
                     bind(IpcdContext.class);
                  }
                  @Provides
                  public PersonDAO personDao() {
                     return EasyMock.createNiceMock(PersonDAO.class);
                  }
                  @Provides
                  public PersonPlaceAssocDAO personPlaceAssocDao() {
                     return EasyMock.createNiceMock(PersonPlaceAssocDAO.class);
                  }

               }).build().bootstrap()
            ));
      bus = ServiceLocator.getInstance(InMemoryProtocolMessageBus.class);
      engine = new GroovyScriptEngine(new ClasspathResourceConnector(this.getClass()));
      context = new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr);
      script = engine.createScript("TestIpcdSend.gscript", new Binding());
      script.run();
      script.setProperty("Ipcd", ServiceLocator.getInstance(IpcdContext.class));

      GroovyContextObject.setContext(context);
   }

   @Test
   public void testSendRawJsonFromIpcd() throws Exception {
      script.invokeMethod("sendRawJsonFromIpcd", new Object[0]);
      IpcdCommand cmd = extractMessage(bus.take());
      Assert.assertEquals(CommandType.GetDeviceInfo.name(), cmd.getCommand());
      Assert.assertNull(cmd.getTxnid());
   }

   @Test
   public void testSendGetDeviceInfo() throws Exception {
      script.invokeMethod("sendGetDeviceInfo", new Object[0]);
      IpcdCommand cmd = extractMessage(bus.take());
      Assert.assertEquals(CommandType.GetDeviceInfo.name(), cmd.getCommand());
      Assert.assertEquals("1000", cmd.getTxnid());
   }

   @Test
   public void testSendSetDeviceInfo() throws Exception {
      script.invokeMethod("sendSetDeviceInfo", new Object[0]);
      IpcdCommand cmd = extractMessage(bus.take());
      Assert.assertEquals(CommandType.SetDeviceInfo.name(), cmd.getCommand());
      Assert.assertEquals("1001", cmd.getTxnid());
      SetDeviceInfoCommand sdi = (SetDeviceInfoCommand)cmd;
      Map<String, Object> values = sdi.getValues();
      Assert.assertEquals("https://things.iot.net/ipcd", values.get("connectUrl"));
   }

   private IpcdCommand extractMessage(ProtocolMessage msg) {
      Assert.assertEquals(driverAddress, msg.getSource());
      Assert.assertEquals(protocolAddress, msg.getDestination());

      IpcdMessage ipcdMsg = msg.getValue(protocol);
      Assert.assertEquals(MessageType.command, ipcdMsg.getMessageType());

      return (IpcdCommand)ipcdMsg;
   }
}

