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
package com.iris.core.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.capability.attribute.Attributes;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.device.attributes.AttributeMap;
import com.iris.io.json.JSON;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Fixtures;

public class TestPlatformMessageJsonSerialization {

   private UUID         devId;
   private Set<String>  capabilities;
   private AttributeMap attributes;

   @SuppressWarnings("unchecked")
   @Before
   public void setUp() throws Exception {
      Bootstrap bootstrap = Bootstrap.builder()
            .withModuleClasses(MessagesModule.class, AttributeMapTransformModule.class)
            .build();
      ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));

      devId = UUID.randomUUID();

      capabilities = new HashSet<String>();
      capabilities.add(DeviceCapability.NAMESPACE);
      capabilities.add(DeviceAdvancedCapability.NAMESPACE);
      capabilities.add(DeviceConnectionCapability.NAMESPACE);
      capabilities.add(DevicePowerCapability.NAMESPACE);
      capabilities.add(ContactCapability.NAMESPACE);
      capabilities.add(TemperatureCapability.NAMESPACE);

      attributes = AttributeMap.mapOf(
            DeviceCapability.KEY_ACCOUNT.valueOf(UUID.randomUUID().toString()),
            Capability.KEY_CAPS.valueOf(capabilities),
            DeviceCapability.KEY_DEVTYPEHINT.valueOf(ContactCapability.NAMESPACE),
            Capability.KEY_ID.valueOf(devId.toString()),
            Capability.KEY_IMAGES.valueOf(Collections.<String,String>singletonMap("icon", UUID.randomUUID().toString())),
            DeviceCapability.KEY_MODEL.valueOf("Model"),
            DeviceCapability.KEY_NAME.valueOf("Contact Sensor"),
            DeviceCapability.KEY_PLACE.valueOf(UUID.randomUUID().toString()),
            DeviceCapability.KEY_VENDOR.valueOf("Vendor"),
            DeviceAdvancedCapability.KEY_ADDED.valueOf(new Date()),
            DeviceAdvancedCapability.KEY_DRIVERNAME.valueOf("DriverClass"),
            DeviceAdvancedCapability.KEY_DRIVERVERSION.valueOf("0.0.0"),
            DeviceAdvancedCapability.KEY_PROTOCOL.valueOf("ipcd"),
            DeviceAdvancedCapability.KEY_SUBPROTOCOL.valueOf("foobar"),
            DeviceConnectionCapability.KEY_LASTCHANGE.valueOf(new Date()),
            DeviceConnectionCapability.KEY_SIGNAL.valueOf(100),
            DeviceConnectionCapability.KEY_STATE.valueOf(DeviceConnectionCapability.STATE_ONLINE),
            DevicePowerCapability.KEY_BATTERY.valueOf(100),
            DevicePowerCapability.KEY_LINECAPABLE.coerceToValue(false),
            DevicePowerCapability.KEY_SOURCE.coerceToValue(DevicePowerCapability.SOURCE_BATTERY),
            ContactCapability.KEY_CONTACT.coerceToValue(ContactCapability.CONTACT_CLOSED),
            TemperatureCapability.KEY_TEMPERATURE.valueOf(70.0));
   }

   @After
   public void tearDown() throws Exception {
      ServiceLocator.destroy();
   }

   @Test
   public void testDeviceCommandSerialization() throws Exception {
      Map<String,Object> attributes = new HashMap<>();
      attributes.put(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      MessageBody cmd = MessageBody.buildMessage("swit:switch", attributes);
      PlatformMessage msg =
            PlatformMessage
               .buildMessage(
                     cmd,
                     Fixtures.createClientAddress(),
                     Address.protocolAddress("IPCD", ProtocolDeviceId.hashDeviceId("Blackbox_Switch1_1"))
               )
               .create();

      String json = JSON.toJson(msg);
      System.out.println(json);
      PlatformMessage msg2 = JSON.fromJson(json, PlatformMessage.class);

      assertEquals(msg.getDestination(), msg2.getDestination());
      assertEquals(msg.getMessageType(), msg2.getMessageType());
      assertEquals(msg.getSource(), msg2.getSource());
      assertEquals(msg.getTimestamp(), msg2.getTimestamp());
      assertEquals(msg.getCorrelationId(), msg2.getCorrelationId());

      MessageBody cmd2 = msg2.getValue();
      assertEquals(cmd.getMessageType(), cmd2.getMessageType());
      assertNotNull(cmd2.getAttributes());
      assertEquals(1, cmd2.getAttributes().size());
      assertEquals(attributes, cmd2.getAttributes());
   }

   @Test
   public void testDeviceEvent() throws Exception {

      MessageBody change = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, Attributes.transformFromAttributeMap(attributes));

      PlatformMessage msg =
            PlatformMessage
            .buildMessage(
                  change,
                  Fixtures.createDeviceAddress(),
                  Fixtures.createClientAddress()
            )
            .create();
      String json = JSON.toJson(msg);

      System.out.println(json);

      PlatformMessage msg2 = JSON.fromJson(json, PlatformMessage.class);

      assertEquals(msg.getDestination(), msg2.getDestination());
      assertEquals(msg.getMessageType(), msg2.getMessageType());
      assertEquals(msg.getSource(), msg2.getSource());
      assertEquals(msg.getTimestamp(), msg2.getTimestamp());
      assertEquals(msg.getCorrelationId(), msg2.getCorrelationId());
      assertFalse(msg2.isError());
      assertEquals(Capability.EVENT_VALUE_CHANGE, msg2.getMessageType());
      assertEquals(msg.getValue().getClass(), MessageBody.class);

      MessageBody change2 = msg2.getValue();

      Fixtures.assertMapEquals(
            attributes.toMap(), 
            Attributes.transformToAttributeMap(change2.getAttributes()).toMap()
      );
   }

   @Test
   public void testErrorEvent() throws Exception {

      MessageBody change = Errors.fromCode("test", "Message");

      PlatformMessage msg =
            PlatformMessage
            .buildMessage(
                  change,
                  Fixtures.createDeviceAddress(),
                  Fixtures.createClientAddress()
            )
            .create();
      String json = JSON.toJson(msg);

      System.out.println(json);

      PlatformMessage msg2 = JSON.fromJson(json, PlatformMessage.class);

      assertEquals(msg.getDestination(), msg2.getDestination());
      assertEquals(msg.getMessageType(), msg2.getMessageType());
      assertEquals(msg.getSource(), msg2.getSource());
      assertEquals(msg.getTimestamp(), msg2.getTimestamp());
      assertEquals(msg.getCorrelationId(), msg2.getCorrelationId());
      assertTrue(msg2.isError());
      assertEquals(msg.getMessageType(), msg2.getMessageType());
      assertEquals(msg.getValue().getClass(), ErrorEvent.class);
      assertEquals(msg.getValue().getAttributes(), msg2.getValue().getAttributes());
   }

}

