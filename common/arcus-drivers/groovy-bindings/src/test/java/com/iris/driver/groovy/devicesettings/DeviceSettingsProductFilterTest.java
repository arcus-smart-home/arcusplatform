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
package com.iris.driver.groovy.devicesettings;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.devicesettings.DeviceSettingsContext;
import com.iris.driver.event.DeviceConnectedEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceSettingsCapability;
import com.iris.protocol.constants.ZwaveConstants;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;

/**
 * Parameters limited with {@code products:} only exist on the matching Z-Wave product.
 * Modeled on the Ring range extender, whose two generations reuse numbers 1-2.
 */
public class DeviceSettingsProductFilterTest {

   private static final int TYPE = 0x0401;
   private static final int GEN1 = 0x0101;
   private static final int GEN2 = 0x0301;

   private DeviceSettingsContext settings;
   private DeviceDriverContext context;

   @Before
   public void setUp() {
      DeviceSettingsBlockContext block = new DeviceSettingsBlockContext();
      block.param(param("batteryReportInterval", 1, 2, 4, 70, products(GEN1)));
      block.param(param("supervisionTimeout", 2, 1, 5, 50, products(GEN1)));
      block.param(param("heartbeatInterval", 1, 1, 1, 70, products(GEN2)));
      block.param(param("retryLimit", 2, 1, 0, 5, products(GEN2)));
      block.param(param("shared", 9, 1, 0, 1, null));
      settings = block.build();
      context = createNiceMock(DeviceDriverContext.class);
   }

   @Test
   public void forProductKeepsOnlyThatProductsParams() {
      assertTrue(settings.hasProductFilters());
      assertEquals(names("batteryReportInterval", "supervisionTimeout", "shared"), names(settings.forProduct(TYPE, GEN1)));
      assertEquals(names("heartbeatInterval", "retryLimit", "shared"), names(settings.forProduct(TYPE, GEN2)));
      assertEquals(names("shared"), names(settings.forProduct(TYPE, 0x0201)));
      assertEquals(names("shared"), names(settings.forProduct(null, null)));
   }

   @Test
   public void unfilteredContextIsReturnedAsIs() {
      DeviceSettingsBlockContext block = new DeviceSettingsBlockContext();
      block.param(param("ledMode", 1, 1, 0, 3, null));
      DeviceSettingsContext plain = block.build();
      assertFalse(plain.hasProductFilters());
      assertSame(plain, plain.forProduct(null, null));
   }

   @Test
   public void reportIsReadAsTheDevicesOwnParam() throws Exception {
      expectProduct(TYPE, GEN2);
      context.setVariable("devsettings:heartbeatInterval", 30L);
      expectLastCall();
      replay(context);

      new DeviceSettingsConfigReportHandler(settings).handleEvent(context, DeviceSettingsSignedValueTest.report(1, 1, (byte) 30));
      verify(context);
   }

   @Test
   public void reportFromUnknownProductIsIgnored() throws Exception {
      Capture<String> keys = Capture.newInstance(CaptureType.ALL);
      expectProduct(TYPE, 0x0201);
      context.setVariable(capture(keys), anyObject());
      expectLastCall().anyTimes();
      replay(context);

      new DeviceSettingsConfigReportHandler(settings).handleEvent(context, DeviceSettingsSignedValueTest.report(1, 1, (byte) 30));
      assertTrue(keys.getValues().isEmpty());
   }

   @Test
   public void connectReadsOnlyTheDevicesParams() throws Exception {
      expectProduct(TYPE, GEN1);
      expect(context.getProtocolAddress()).andReturn(Address.protocolAddress("ZWAV", new byte[] { 0x02 })).anyTimes();
      Capture<ZWaveCommandMessage> gets = Capture.newInstance(CaptureType.ALL);
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), capture(gets), eq(-1));
      expectLastCall().anyTimes();
      replay(context);

      new DeviceSettingsConnectedHandler(settings).handleEvent(context, new DeviceConnectedEvent(null));

      List<Integer> paramNos = new ArrayList<>();
      for (ZWaveCommandMessage get : gets.getValues()) {
         paramNos.add(get.getCommand().toBytes()[2] & 0xFF);
      }
      assertEquals(Arrays.asList(1, 2, 9), paramNos);
   }

   @Test
   public void attributeListsOnlyTheDevicesParams() {
      expectProduct(TYPE, GEN2);
      replay(context);

      Map<String, Object> attrs = new DeviceSettingsGetAttributesProvider(settings).getAttributes(context, Collections.emptySet());
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> params = (List<Map<String, Object>>) attrs.get(DeviceSettingsCapability.ATTR_PARAMS);
      List<Object> ids = new ArrayList<>();
      for (Map<String, Object> p : params) {
         ids.add(p.get("paramId"));
      }
      assertEquals(Arrays.asList("heartbeatInterval", "retryLimit", "shared"), ids);
   }

   @Test
   public void setParamRejectsOtherProductsParam() throws Exception {
      expectProduct(TYPE, GEN1);
      Capture<MessageBody> response = Capture.newInstance();
      context.respondToPlatform(capture(response));
      expectLastCall();
      replay(context);

      MessageBody request = DeviceSettingsCapability.SetParamRequest.builder()
            .withParamId("heartbeatInterval").withValue("30").build();
      assertTrue(new DeviceSettingsSetParamHandler(settings).handleEvent(context, request));
      assertEquals("INVALID_PARAM", DeviceSettingsCapability.SetParamResponse.getStatus(response.getValue()));
   }

   @Test
   public void getParamRejectsOtherProductsParam() throws Exception {
      expectProduct(TYPE, GEN2);
      Capture<MessageBody> response = Capture.newInstance();
      context.respondToPlatform(capture(response));
      expectLastCall();
      replay(context);

      MessageBody request = DeviceSettingsCapability.GetParamRequest.builder().withParamId("supervisionTimeout").build();
      assertTrue(new DeviceSettingsGetParamHandler(settings).handleEvent(context, request));
      assertEquals("INVALID_PARAM", DeviceSettingsCapability.GetParamResponse.getStatus(response.getValue()));
   }

   @Test
   public void reusedNumberDoesNotResolveUntilNarrowed() {
      // gen 1 and gen 2 both use 1 and 2; only the shared param's 9 is unambiguous
      assertNull(settings.getByZwaveParamNo(1));
      assertNull(settings.getByZwaveParamNo(2));
      assertEquals("shared", settings.getByZwaveParamNo(9).getParamId());
      assertEquals("heartbeatInterval", settings.forProduct(TYPE, GEN2).getByZwaveParamNo(1).getParamId());
   }

   @Test
   public void narrowedContextIsReused() {
      assertSame(settings.forProduct(TYPE, GEN1), settings.forProduct(TYPE, GEN1));
      assertSame(settings.forProduct(null, null), settings.forProduct(null, 0x0101));
   }

   @Test
   public void connectSkipsReadsWhenDriverReadsLater() throws Exception {
      DeviceSettingsBlockContext block = new DeviceSettingsBlockContext();
      block.param(param("beeper", 3, 1, 0, 255, null));
      DeviceSettingsContext deferred = block.build(false);

      Capture<Object> attrs = Capture.newInstance(CaptureType.ALL);
      expect(context.setAttributeValue(EasyMock.<AttributeKey<Object>>anyObject(), capture(attrs))).andReturn(null).anyTimes();
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), anyObject(ZWaveCommandMessage.class), eq(-1));
      expectLastCall().andThrow(new AssertionError("no configuration get on connect")).anyTimes();
      replay(context);

      new DeviceSettingsConnectedHandler(deferred).handleEvent(context, new DeviceConnectedEvent(null));
      // the attribute is still published so clients see the params
      assertEquals(1, attrs.getValues().size());
   }

   @Test
   public void readAllSendsTheDevicesGets() throws Exception {
      expectProduct(TYPE, GEN2);
      expect(context.getProtocolAddress()).andReturn(Address.protocolAddress("ZWAV", new byte[] { 0x02 })).anyTimes();
      Capture<ZWaveCommandMessage> gets = Capture.newInstance(CaptureType.ALL);
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), capture(gets), eq(-1));
      expectLastCall().anyTimes();
      replay(context);

      DeviceSettingsUtil.readAll(context, settings);
      assertEquals(3, gets.getValues().size());
   }

   @Test(expected = IllegalArgumentException.class)
   public void productIdOutOfRangeIsRejected() {
      new DeviceSettingsBlockContext().param(param("x", 1, 1, 0, 1, Arrays.asList(Arrays.asList(0x4F440, 0x3032))));
   }

   @Test(expected = IllegalArgumentException.class)
   public void malformedProductsEntryIsRejected() {
      new DeviceSettingsBlockContext().param(param("x", 1, 1, 0, 1, Arrays.asList(Arrays.asList(TYPE))));
   }

   @Test(expected = IllegalArgumentException.class)
   public void emptyProductsListIsRejected() {
      new DeviceSettingsBlockContext().param(param("x", 1, 1, 0, 1, Collections.emptyList()));
   }

   @Test(expected = IllegalArgumentException.class)
   public void duplicateNameIsRejected() {
      DeviceSettingsBlockContext block = new DeviceSettingsBlockContext();
      block.param(param("x", 1, 1, 0, 1, products(GEN1)));
      block.param(param("x", 2, 1, 0, 1, products(GEN2)));
      block.build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void sameNumberTwiceOnOneProductIsRejected() {
      DeviceSettingsBlockContext block = new DeviceSettingsBlockContext();
      block.param(param("a", 1, 1, 0, 1, products(GEN1)));
      block.param(param("b", 1, 1, 0, 1, null));
      block.build();
   }

   private void expectProduct(int type, int id) {
      AttributeMap attrs = AttributeMap.newMap();
      attrs.set(AttributeKey.create(ZwaveConstants.ATTR_PRODUCTTYPE, Integer.class), type);
      attrs.set(AttributeKey.create(ZwaveConstants.ATTR_PRODUCTID, Integer.class), id);
      expect(context.getProtocolAttributes()).andReturn(attrs).anyTimes();
   }

   private static List<List<Integer>> products(int productId) {
      return Collections.singletonList(Arrays.asList(TYPE, productId));
   }

   private static Map<String, Object> param(String name, int id, int size, int min, int max, List<?> products) {
      Map<String, Object> args = new HashMap<>();
      args.put("name", name);
      args.put("id", id);
      args.put("size", size);
      args.put("type", "range");
      args.put("min", min);
      args.put("max", max);
      if (products != null) {
         args.put("products", products);
      }
      return args;
   }

   private static List<String> names(String... names) {
      return Arrays.asList(names);
   }

   private static List<String> names(DeviceSettingsContext ctx) {
      List<String> ids = new ArrayList<>();
      ctx.getParams().forEach(p -> ids.add(p.getParamId()));
      return ids;
   }
}
