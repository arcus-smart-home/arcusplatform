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
package com.iris.messages.address;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.analysis.function.Add;
import org.junit.Test;

import com.iris.Utils;
import com.iris.io.java.JavaDeserializer;
import com.iris.io.java.JavaSerializer;
import com.iris.messages.MessageConstants;
import com.iris.messages.model.ChildId;
import com.iris.util.IrisUUID;

/**
 * @author tweidlin
 *
 */
public class TestAddress {
   static final UUID deviceId = UUID.fromString("c24b0e18-3394-4f81-b762-274ba3605ccc");
   // TODO use a fixed UUID for easier debugging
   static final String hubId = "ABC-1234";

   private static Address[][] PLATFORM_SERVICE_ADDRESSES = new Address[][] {
      new Address[] {
         new PlatformServiceAddress(null, "service", null),
         new PlatformServiceAddress(Address.ZERO_UUID, "service", null),
         new PlatformServiceAddress("", "service", null),
      },
      new Address[] {
         new PlatformServiceAddress(null, "service2", null),
         new PlatformServiceAddress(Address.ZERO_UUID, "service2", null),
         new PlatformServiceAddress("", "service2", null),
      },
      new Address[] {
         new PlatformServiceAddress(deviceId, "service", null),
         new PlatformServiceAddress(deviceId, "service", null),
      },
      new Address[] {
         new PlatformServiceAddress(deviceId, "service", 1),
         new PlatformServiceAddress(deviceId, "service", 1),
      },
      new Address[] {
         new PlatformServiceAddress(deviceId, "service", 2),
         new PlatformServiceAddress(deviceId, "service", 2),
      },
   };

   @Test
   public void testBroadcastAddress() {
      assertIsBroadcastAddress(Address.fromString(null));
      assertIsBroadcastAddress(Address.fromString(""));
      assertIsBroadcastAddress(Address.fromBytes(new byte[44]));
      // TODO test json representations
   }

   @Test
   public void testClientAddress() {
      String address = MessageConstants.CLIENT + ":ServerId:ClientId";
      ClientAddress actual = (ClientAddress) Address.fromString(address);

      assertFalse(actual.isBroadcast());
      assertEquals(address, actual.getRepresentation());
      assertEquals(MessageConstants.CLIENT, actual.getNamespace());
      assertEquals("ServerId", actual.getGroup());
      assertEquals("ClientId", actual.getId());
      assertEquals(actual, Address.clientAddress("ServerId", "ClientId"));
      assertEquals(actual, Address.fromBytes(actual.getBytes()));
   }

   @Test
   public void testPlatformServiceAddress() {
      String address = MessageConstants.SERVICE + ":Devices:";
      PlatformServiceAddress actual = (PlatformServiceAddress) Address.fromString(address);

      assertFalse(actual.isBroadcast());
      assertEquals(address, actual.getRepresentation());
      assertEquals(MessageConstants.SERVICE, actual.getNamespace());
      assertEquals(Address.ZERO_UUID, actual.getId());
      assertEquals(Address.ZERO_UUID, actual.getContextId());
      assertEquals("Devices", actual.getServiceName());
      assertEquals("Devices", actual.getGroup());
      assertEquals(false, actual.isHubAddress());
      assertEquals(null, actual.getHubId());
      assertEquals(null, actual.getContextQualifier());
      assertEquals(actual, Address.platformService("Devices"));
      assertEquals(actual, Address.fromString(MessageConstants.SERVICE + ":Devices:"));
      assertEquals(actual, Address.fromBytes(actual.getBytes()));
   }

   @Test
   public void testPlatformServiceAddressEquals() {
      for (Address[] group1 : PLATFORM_SERVICE_ADDRESSES) {
         for (Address[] group2 : PLATFORM_SERVICE_ADDRESSES) {
            if (group1 == group2) {
               continue;
            }

            for (Address addr1 : group1) {
               for (Address addr2 : group2) {
                  assertNotEquals(addr1, addr2);
               }
            }
         }
      }

      for (Address[] group : PLATFORM_SERVICE_ADDRESSES) {
         for (Address addr1 : group) {
            for (Address addr2 : group) {
               assertEquals(addr1, addr2);
            }
         }
      }
      
      Address expected = Address.fromString("SERV:rule:" + deviceId + ".13");
      assertEquals(expected, Address.platformService(deviceId, "rule", 13));
      assertEquals(expected, Address.platformService(String.valueOf(deviceId), "rule", 13));
   }

   @Test
   public void testPlatformServiceAddressHashCode() {
      for (Address[] group1 : PLATFORM_SERVICE_ADDRESSES) {
         for (Address[] group2 : PLATFORM_SERVICE_ADDRESSES) {
            if (group1 == group2) {
               continue;
            }

            for (Address addr1 : group1) {
               for (Address addr2 : group2) {
                  // Technically these could collide, but that should be unlikely.
                  assertNotEquals(addr1.hashCode(), addr2.hashCode());
               }
            }
         }
      }

      for (Address[] group : PLATFORM_SERVICE_ADDRESSES) {
         for (Address addr1 : group) {
            for (Address addr2 : group) {
               assertEquals(addr1.hashCode(), addr2.hashCode());
            }
         }
      }
   }

   @Test
   public void testLegacyPlatformServiceAddress() {
      String address = MessageConstants.SERVICE + "::Devices";
      PlatformServiceAddress actual = (PlatformServiceAddress) Address.fromString(address);

      assertFalse(actual.isBroadcast());
      assertEquals(MessageConstants.SERVICE + ":Devices:", actual.getRepresentation());
      assertEquals(MessageConstants.SERVICE, actual.getNamespace());
      assertEquals(Address.ZERO_UUID, actual.getId());
      assertEquals(Address.ZERO_UUID, actual.getContextId());
      assertEquals("Devices", actual.getServiceName());
      assertEquals("Devices", actual.getGroup());
      assertEquals(false, actual.isHubAddress());
      assertEquals(null, actual.getHubId());
      assertEquals(null, actual.getContextQualifier());
      assertEquals(actual, Address.platformService("Devices"));
      assertEquals(actual, Address.fromString(MessageConstants.SERVICE + ":Devices:"));
      assertEquals(actual, Address.fromBytes(actual.getBytes()));
   }

   @Test
   public void testPlatformServiceAddressWithContext() {
      UUID contextId = UUID.randomUUID();
      String address = MessageConstants.SERVICE + ":Devices:" + contextId;
      PlatformServiceAddress actual = (PlatformServiceAddress) Address.fromString(address);

      assertFalse(actual.isBroadcast());
      assertEquals(address, actual.getRepresentation());
      assertEquals(MessageConstants.SERVICE, actual.getNamespace());
      assertEquals(contextId, actual.getContextId());
      assertEquals(contextId, actual.getId());
      assertEquals("Devices", actual.getServiceName());
      assertEquals("Devices", actual.getGroup());
      assertEquals(false, actual.isHubAddress());
      assertEquals(null, actual.getHubId());
      assertEquals(null, actual.getContextQualifier());
      assertEquals(actual, Address.platformService(contextId, "Devices"));
      assertEquals(actual, Address.fromString(MessageConstants.SERVICE + ":Devices:" + contextId));
      assertEquals(actual, Address.fromBytes(actual.getBytes()));
   }

   @Test
   public void testPlatformServiceAddressWithContextAndQualifier() {
      UUID contextId = UUID.randomUUID();
      String address = MessageConstants.SERVICE + ":Devices:" + contextId + "." + 1;
      PlatformServiceAddress actual = (PlatformServiceAddress) Address.fromString(address);

      assertFalse(actual.isBroadcast());
      assertEquals(address, actual.getRepresentation());
      assertEquals(MessageConstants.SERVICE, actual.getNamespace());
      assertEquals(contextId, actual.getContextId());
      assertEquals(contextId, actual.getId());
      assertEquals("Devices", actual.getServiceName());
      assertEquals("Devices", actual.getGroup());
      assertEquals(false, actual.isHubAddress());
      assertEquals(null, actual.getHubId());
      assertEquals(Integer.valueOf(1), actual.getContextQualifier());
      assertEquals(actual, Address.platformService(contextId, "Devices", 1));
      assertEquals(actual, Address.fromString(MessageConstants.SERVICE + ":Devices:" + contextId + "." + 1));
      assertEquals(actual, Address.fromBytes(actual.getBytes()));
   }

   @Test
   public void testHubServiceAddress() {
      String address = MessageConstants.SERVICE + ":" + hubId + ":ZWaveController";
      HubServiceAddress actual = (HubServiceAddress) Address.fromString(address);

      assertFalse(actual.isBroadcast());
      assertEquals(address, actual.getRepresentation());
      assertEquals(MessageConstants.SERVICE, actual.getNamespace());
      assertEquals(hubId, actual.getGroup());
      assertEquals("ZWaveController", actual.getServiceName());
      assertEquals(true, actual.isHubAddress());
      assertEquals(hubId, actual.getHubId());
      assertEquals(actual, Address.hubService(hubId, "ZWaveController"));
      assertEquals(actual, Address.fromBytes(actual.getBytes()));
   }

   @Test
   public void testPlatformDriverAddress() {
      String address = MessageConstants.DRIVER + ":dev:" + deviceId;
      DeviceDriverAddress actual = (DeviceDriverAddress) Address.fromString(address);

      assertFalse(actual.isBroadcast());
      assertEquals(address, actual.getRepresentation());
      assertEquals(MessageConstants.DRIVER, actual.getNamespace());
      assertEquals(Address.PLATFORM_DRIVER_GROUP, actual.getGroup());
      assertEquals(deviceId, actual.getId());
      assertEquals(false, actual.isHubAddress());
      assertEquals(null, actual.getHubId());
      assertEquals(actual, Address.platformDriverAddress(deviceId));
      assertEquals(actual, Address.fromBytes(actual.getBytes()));
   }

   @Test
   public void testHubDriverAddress() {
      String address = MessageConstants.DRIVER + ":" + hubId + ":" + deviceId;
      DeviceDriverAddress actual = (DeviceDriverAddress) Address.fromString(address);

      assertFalse(actual.isBroadcast());
      assertEquals(address, actual.getRepresentation());
      assertEquals(MessageConstants.DRIVER, actual.getNamespace());
      assertEquals(hubId, actual.getGroup());
      assertEquals(deviceId, actual.getId());
      assertEquals(true, actual.isHubAddress());
      assertEquals(hubId, actual.getHubId());
      assertEquals(actual, Address.hubDriverAddress(hubId, deviceId));
      assertEquals(actual, Address.fromBytes(actual.getBytes()));
   }

   @Test
   public void testPlatformProtocolAddress() {
      ProtocolDeviceId protocolDeviceId = ProtocolDeviceId.hashDeviceId("Blackbox_ContactSensor1_123456789");
      String address = MessageConstants.PROTOCOL + ":IPCD:" + protocolDeviceId.getRepresentation();
      DeviceProtocolAddress actual = (DeviceProtocolAddress) Address.fromString(address);

      assertFalse(actual.isBroadcast());
      assertEquals(address, actual.getRepresentation());
      assertEquals(MessageConstants.PROTOCOL, actual.getNamespace());
      assertEquals("IPCD", actual.getGroup());
      assertEquals(protocolDeviceId, actual.getId());
      assertEquals(false, actual.isHubAddress());
      assertEquals("IPCD", actual.getProtocolName());
      assertEquals(null, actual.getHubId());
      assertEquals(protocolDeviceId, actual.getProtocolDeviceId());
      assertEquals(actual, Address.protocolAddress("IPCD", protocolDeviceId));
      assertEquals(actual, Address.fromBytes(actual.getBytes()));
   }

   @Test
   public void testHubProtocolAddress() {
      ProtocolDeviceId protocolDeviceId = ProtocolDeviceId.fromBytes(new byte [] { 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f });
      String address = MessageConstants.PROTOCOL + ":ZIGB" + "-" + hubId + ":" + protocolDeviceId.getRepresentation();
      System.out.println(address);
      DeviceProtocolAddress actual = (DeviceProtocolAddress) Address.fromString(address);

      assertFalse(actual.isBroadcast());
      assertEquals(address, actual.getRepresentation());
      assertEquals(MessageConstants.PROTOCOL, actual.getNamespace());
      assertEquals("ZIGB-" + hubId, actual.getGroup());
      assertEquals(protocolDeviceId, actual.getId());
      assertEquals(true, actual.isHubAddress());
      assertEquals("ZIGB", actual.getProtocolName());
      assertEquals(hubId, actual.getHubId());
      assertEquals(protocolDeviceId, actual.getProtocolDeviceId());
      assertEquals(actual, Address.hubProtocolAddress(hubId, "ZIGB", protocolDeviceId));
      assertEquals(actual, Address.fromBytes(actual.getBytes()));
   }

   @Test
   public void testHubAddress() {
      String address = MessageConstants.HUB + "::" + hubId;
      HubAddress actual = (HubAddress) Address.fromString(address);

      assertFalse(actual.isBroadcast());
      assertEquals(address, actual.getRepresentation());
      assertEquals(MessageConstants.HUB, actual.getNamespace());
      assertEquals(Address.PLATFORM_GROUP, actual.getGroup());
      assertEquals(hubId, actual.getId());
      assertEquals(hubId, actual.getHubId());
      assertEquals(actual, Address.hubAddress(hubId));
      assertEquals(actual, Address.fromBytes(actual.getBytes()));
   }

   @Test
   public void testInvalidAddresses() {
      assertInvalidAddress("INVA::15", "No such namespace");

      assertInvalidAddress(MessageConstants.CLIENT, "No client id specified");
      assertInvalidAddress(MessageConstants.CLIENT + ":123456789012345678901:2", "Group id too long");

      assertInvalidAddress(MessageConstants.SERVICE, "No service name specified");
      assertInvalidAddress(MessageConstants.SERVICE + "invalid:Devices", "Invalid group id");

      assertInvalidAddress(MessageConstants.DRIVER, "No device id specified");
      assertInvalidAddress(MessageConstants.DRIVER + "::foo", "Invalid device id specified");

      assertInvalidAddress(MessageConstants.HUB, "No hub id specified");

      assertInvalidAddress("PROT:IPCD", "No device id specified");
      assertInvalidAddress("PROT:IPCD:Blackbox_ContactSensor1_123456789:2", "Invalid additional address parts");
   }
   
   @Test
   public void testPre25Deserialize() {
      deserializeAndAssertEquals("", "rO0ABXNyACpjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLkJyb2FkY2FzdEFkZHJlc3N0sMcShL5JDwIAAHhyACFjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLkFkZHJlc3MAOe9PxI6DnQIAAHhw");
      deserializeAndAssertEquals("BRDG::ipcd", "rO0ABXNyACdjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLkJyaWRnZUFkZHJlc3NzUpywI7bxNQIAAUwACGJyaWRnZUlkdAASTGphdmEvbGFuZy9TdHJpbmc7eHIAIWNvbS5pcmlzLm1lc3NhZ2VzLmFkZHJlc3MuQWRkcmVzcwA570/EjoOdAgAAeHB0AARpcGNk");
      deserializeAndAssertEquals("CLNT:android:session", "rO0ABXNyACdjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLkNsaWVudEFkZHJlc3NC78wsKX5UJQIAAkwACHNlcnZlcklkdAASTGphdmEvbGFuZy9TdHJpbmc7TAAJc2Vzc2lvbklkcQB+AAF4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHQAB2FuZHJvaWR0AAdzZXNzaW9u");
      deserializeAndAssertEquals("DRIV:dev:43dc3964-9d66-454f-8928-476e3b0536fd", "rO0ABXNyAC1jb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLkRldmljZURyaXZlckFkZHJlc3MuO+SG2x6mqgIAAkwAB2dyb3VwSWR0ABJMamF2YS9sYW5nL1N0cmluZztMAAJpZHQAEExqYXZhL3V0aWwvVVVJRDt4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHQAA2RldnNyAA5qYXZhLnV0aWwuVVVJRLyZA/eYbYUvAgACSgAMbGVhc3RTaWdCaXRzSgALbW9zdFNpZ0JpdHN4cIkoR247BTb9Q9w5ZJ1mRU8=");
      deserializeAndAssertEquals("HUB::ABC-1234", "rO0ABXNyACRjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLkh1YkFkZHJlc3NzU0MFvVdaEQIAAUwABWh1YklkdAASTGphdmEvbGFuZy9TdHJpbmc7eHIAIWNvbS5pcmlzLm1lc3NhZ2VzLmFkZHJlc3MuQWRkcmVzcwA570/EjoOdAgAAeHB0AAhBQkMtMTIzNA==");
      deserializeAndAssertEquals("SERV:ABC-1234:hub", "rO0ABXNyACtjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLkh1YlNlcnZpY2VBZGRyZXNzeF9R9Mdbr3wCAAJMAAVodWJJZHQAEkxqYXZhL2xhbmcvU3RyaW5nO0wAC3NlcnZpY2VOYW1lcQB+AAF4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHQACEFCQy0xMjM0dAADaHVi");
      deserializeAndAssertEquals("SERV:hub:ABC-1234", "rO0ABXNyADBjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLlBsYXRmb3JtU2VydmljZUFkZHJlc3PMhnqWCgw4VQIAA0wACWNvbnRleHRJZHQAEkxqYXZhL2xhbmcvT2JqZWN0O0wAEGNvbnRleHRRdWFsaWZpZXJ0ABNMamF2YS9sYW5nL0ludGVnZXI7TAALc2VydmljZU5hbWV0ABJMamF2YS9sYW5nL1N0cmluZzt4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHQACEFCQy0xMjM0cHQAA2h1Yg==");
      deserializeAndAssertEquals("SERV:place:", "rO0ABXNyADBjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLlBsYXRmb3JtU2VydmljZUFkZHJlc3PMhnqWCgw4VQIAA0wACWNvbnRleHRJZHQAEkxqYXZhL2xhbmcvT2JqZWN0O0wAEGNvbnRleHRRdWFsaWZpZXJ0ABNMamF2YS9sYW5nL0ludGVnZXI7TAALc2VydmljZU5hbWV0ABJMamF2YS9sYW5nL1N0cmluZzt4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHNyAA5qYXZhLnV0aWwuVVVJRLyZA/eYbYUvAgACSgAMbGVhc3RTaWdCaXRzSgALbW9zdFNpZ0JpdHN4cAAAAAAAAAAAAAAAAAAAAABwdAAFcGxhY2U=");
      deserializeAndAssertEquals("SERV:person:cb7e6d30-0c68-49c6-b0b4-8788f1cb7f68", "rO0ABXNyADBjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLlBsYXRmb3JtU2VydmljZUFkZHJlc3PMhnqWCgw4VQIAA0wACWNvbnRleHRJZHQAEkxqYXZhL2xhbmcvT2JqZWN0O0wAEGNvbnRleHRRdWFsaWZpZXJ0ABNMamF2YS9sYW5nL0ludGVnZXI7TAALc2VydmljZU5hbWV0ABJMamF2YS9sYW5nL1N0cmluZzt4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHNyAA5qYXZhLnV0aWwuVVVJRLyZA/eYbYUvAgACSgAMbGVhc3RTaWdCaXRzSgALbW9zdFNpZ0JpdHN4cLC0h4jxy39oy35tMAxoScZwdAAGcGVyc29u");
      deserializeAndAssertEquals("SERV:rule:a0d51116-e045-404e-85fb-845a7b9c17f1.1", "rO0ABXNyADBjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLlBsYXRmb3JtU2VydmljZUFkZHJlc3PMhnqWCgw4VQIAA0wACWNvbnRleHRJZHQAEkxqYXZhL2xhbmcvT2JqZWN0O0wAEGNvbnRleHRRdWFsaWZpZXJ0ABNMamF2YS9sYW5nL0ludGVnZXI7TAALc2VydmljZU5hbWV0ABJMamF2YS9sYW5nL1N0cmluZzt4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHNyAA5qYXZhLnV0aWwuVVVJRLyZA/eYbYUvAgACSgAMbGVhc3RTaWdCaXRzSgALbW9zdFNpZ0JpdHN4cIX7hFp7nBfxoNURFuBFQE5zcgARamF2YS5sYW5nLkludGVnZXIS4qCk94GHOAIAAUkABXZhbHVleHIAEGphdmEubGFuZy5OdW1iZXKGrJUdC5TgiwIAAHhwAAAAAXQABHJ1bGU=");
      deserializeAndAssertEquals("SERV:ruletmpl:template", "rO0ABXNyADBjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLlBsYXRmb3JtU2VydmljZUFkZHJlc3PMhnqWCgw4VQIAA0wACWNvbnRleHRJZHQAEkxqYXZhL2xhbmcvT2JqZWN0O0wAEGNvbnRleHRRdWFsaWZpZXJ0ABNMamF2YS9sYW5nL0ludGVnZXI7TAALc2VydmljZU5hbWV0ABJMamF2YS9sYW5nL1N0cmluZzt4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHQACHRlbXBsYXRlcHQACHJ1bGV0bXBs");
   }
   
   @Test
   public void testPost26Deserialize() {
      deserializeAndAssertEquals("", "rO0ABXNyACpjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLkJyb2FkY2FzdEFkZHJlc3N0sMcShL5JDwIAAHhyACFjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLkFkZHJlc3MAOe9PxI6DnQIAAHhw");
      deserializeAndAssertEquals("BRDG::ipcd", "rO0ABXNyACdjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLkJyaWRnZUFkZHJlc3NzUpywI7bxNQIAAUwACGJyaWRnZUlkdAASTGphdmEvbGFuZy9TdHJpbmc7eHIAIWNvbS5pcmlzLm1lc3NhZ2VzLmFkZHJlc3MuQWRkcmVzcwA570/EjoOdAgAAeHB0AARpcGNk");
      deserializeAndAssertEquals("CLNT:android:session", "rO0ABXNyACdjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLkNsaWVudEFkZHJlc3NC78wsKX5UJQIAAkwACHNlcnZlcklkdAASTGphdmEvbGFuZy9TdHJpbmc7TAAJc2Vzc2lvbklkcQB+AAF4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHQAB2FuZHJvaWR0AAdzZXNzaW9u");
      deserializeAndAssertEquals("DRIV:dev:d52adeb5-d01a-41cf-905f-f6bb0d088d78", "rO0ABXNyAC1jb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLkRldmljZURyaXZlckFkZHJlc3MuO+SG2x6mqgIAAkwAB2dyb3VwSWR0ABJMamF2YS9sYW5nL1N0cmluZztMAAJpZHQAEExqYXZhL3V0aWwvVVVJRDt4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHQAA2RldnNyAA5qYXZhLnV0aWwuVVVJRLyZA/eYbYUvAgACSgAMbGVhc3RTaWdCaXRzSgALbW9zdFNpZ0JpdHN4cJBf9rsNCI141SretdAaQc8=");
      deserializeAndAssertEquals("HUB::ABC-1234", "rO0ABXNyACRjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLkh1YkFkZHJlc3NzU0MFvVdaEQIAAUwABWh1YklkdAASTGphdmEvbGFuZy9TdHJpbmc7eHIAIWNvbS5pcmlzLm1lc3NhZ2VzLmFkZHJlc3MuQWRkcmVzcwA570/EjoOdAgAAeHB0AAhBQkMtMTIzNA==");
      deserializeAndAssertEquals("SERV:ABC-1234:hub", "rO0ABXNyACtjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLkh1YlNlcnZpY2VBZGRyZXNzeF9R9Mdbr3wCAAJMAAVodWJJZHQAEkxqYXZhL2xhbmcvU3RyaW5nO0wAC3NlcnZpY2VOYW1lcQB+AAF4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHQACEFCQy0xMjM0dAADaHVi");
      deserializeAndAssertEquals("SERV:hub:ABC-1234", "rO0ABXNyADBjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLlBsYXRmb3JtU2VydmljZUFkZHJlc3PMhnqWCgw4VQIAA0wACWNvbnRleHRJZHQAEkxqYXZhL2xhbmcvT2JqZWN0O0wAEGNvbnRleHRRdWFsaWZpZXJ0ABNMamF2YS9sYW5nL0ludGVnZXI7TAALc2VydmljZU5hbWV0ABJMamF2YS9sYW5nL1N0cmluZzt4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHQACEFCQy0xMjM0cHQAA2h1Yg==");
      deserializeAndAssertEquals("SERV:place:", "rO0ABXNyADBjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLlBsYXRmb3JtU2VydmljZUFkZHJlc3PMhnqWCgw4VQIAA0wACWNvbnRleHRJZHQAEkxqYXZhL2xhbmcvT2JqZWN0O0wAEGNvbnRleHRRdWFsaWZpZXJ0ABNMamF2YS9sYW5nL0ludGVnZXI7TAALc2VydmljZU5hbWV0ABJMamF2YS9sYW5nL1N0cmluZzt4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHNyAA5qYXZhLnV0aWwuVVVJRLyZA/eYbYUvAgACSgAMbGVhc3RTaWdCaXRzSgALbW9zdFNpZ0JpdHN4cAAAAAAAAAAAAAAAAAAAAABwdAAFcGxhY2U=");
      deserializeAndAssertEquals("SERV:person:2dca6c00-7877-4e76-a0ad-36ec428c97c2", "rO0ABXNyADBjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLlBsYXRmb3JtU2VydmljZUFkZHJlc3PMhnqWCgw4VQIAA0wACWNvbnRleHRJZHQAEkxqYXZhL2xhbmcvT2JqZWN0O0wAEGNvbnRleHRRdWFsaWZpZXJ0ABNMamF2YS9sYW5nL0ludGVnZXI7TAALc2VydmljZU5hbWV0ABJMamF2YS9sYW5nL1N0cmluZzt4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHNyAA5qYXZhLnV0aWwuVVVJRLyZA/eYbYUvAgACSgAMbGVhc3RTaWdCaXRzSgALbW9zdFNpZ0JpdHN4cKCtNuxCjJfCLcpsAHh3TnZwdAAGcGVyc29u");
      deserializeAndAssertEquals("SERV:rule:09858d28-932d-4116-ae3c-baae973ac600.1", "rO0ABXNyADBjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLlBsYXRmb3JtU2VydmljZUFkZHJlc3PMhnqWCgw4VQIAA0wACWNvbnRleHRJZHQAEkxqYXZhL2xhbmcvT2JqZWN0O0wAEGNvbnRleHRRdWFsaWZpZXJ0ABNMamF2YS9sYW5nL0ludGVnZXI7TAALc2VydmljZU5hbWV0ABJMamF2YS9sYW5nL1N0cmluZzt4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHNyAA5qYXZhLnV0aWwuVVVJRLyZA/eYbYUvAgACSgAMbGVhc3RTaWdCaXRzSgALbW9zdFNpZ0JpdHN4cK48uq6XOsYACYWNKJMtQRZzcgARamF2YS5sYW5nLkludGVnZXIS4qCk94GHOAIAAUkABXZhbHVleHIAEGphdmEubGFuZy5OdW1iZXKGrJUdC5TgiwIAAHhwAAAAAXQABHJ1bGU=");
      deserializeAndAssertEquals("SERV:ruletmpl:template", "rO0ABXNyADBjb20uaXJpcy5tZXNzYWdlcy5hZGRyZXNzLlBsYXRmb3JtU2VydmljZUFkZHJlc3PMhnqWCgw4VQIAA0wACWNvbnRleHRJZHQAEkxqYXZhL2xhbmcvT2JqZWN0O0wAEGNvbnRleHRRdWFsaWZpZXJ0ABNMamF2YS9sYW5nL0ludGVnZXI7TAALc2VydmljZU5hbWV0ABJMamF2YS9sYW5nL1N0cmluZzt4cgAhY29tLmlyaXMubWVzc2FnZXMuYWRkcmVzcy5BZGRyZXNzADnvT8SOg50CAAB4cHQACHRlbXBsYXRlcHQACHJ1bGV0bXBs");
   }
   
   private void deserializeAndAssertEquals(String representation, String javaSerialized) {
      Address expected = Address.fromString(representation);
      byte [] bytes = Utils.b64Decode(javaSerialized);
      Address actual = JavaDeserializer.<Address>getInstance().deserialize(bytes);
      assertEquals(expected, actual);
      assertEquals(representation, actual.getRepresentation());
   }
   
   @Test
   public void serialize() {
      printSerialized(Address.broadcastAddress());
      printSerialized(Address.bridgeAddress("ipcd"));
      printSerialized(Address.clientAddress("android", "session"));
      printSerialized(Address.deviceAddress("dev", IrisUUID.randomUUID()));
      printSerialized(Address.hubAddress("ABC-1234"));
      printSerialized(Address.hubService("ABC-1234", "hub"));
      printSerialized(Address.platformService("ABC-1234", "hub"));
      printSerialized(Address.platformService("place"));
      printSerialized(Address.platformService(IrisUUID.randomUUID(), "person"));
      printSerialized(Address.platformService(IrisUUID.randomUUID(), "rule", 1));
      printSerialized(Address.platformService("template", "ruletmpl"));
   }
   
   private void printSerialized(Address address) {
      byte[] bytes = JavaSerializer.getInstance().serialize(address);
      System.out.println(address + ": " + Utils.b64Encode(bytes));
   }

   protected void assertIsBroadcastAddress(Address address) {
      assertSame(Address.broadcastAddress(), address);
      assertEquals(true, address.isBroadcast());
      assertEquals("", address.getRepresentation());
      assertArrayEquals(new byte[44], address.getBytes());
      assertEquals(MessageConstants.BROADCAST, address.getNamespace());
      assertTrue(address instanceof BroadcastAddress);
   }

   protected void assertInvalidAddress(String address) {
      assertInvalidAddress(address, null);
   }

   protected void assertInvalidAddress(String address, String reason) {
      try {
         Address generated = Address.fromString(address);
         StringBuilder sb = new StringBuilder("Incorrectly generated [" + generated + "] from [" + address + "]");
         if(!StringUtils.isEmpty(reason)) {
            sb.append(": ").append(reason);
         }
         fail(sb.toString());
      }
      catch(IllegalArgumentException e) {
         // expected
      }
   }


}

