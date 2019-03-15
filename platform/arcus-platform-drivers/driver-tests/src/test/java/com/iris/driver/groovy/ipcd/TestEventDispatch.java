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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.iris.protocol.ipcd.message.model.IpcdEvent;

public class TestEventDispatch extends IpcdHandlersTestCase {

   public TestEventDispatch() {
      super("IpcdEventDispatchHandler.driver");
   }
   
   @Test
   public void testMatchOnConnect() {
      IpcdEvent event = new IpcdEvent();
      event.setDevice(IpcdFixtures.getDevice());
      event.setEvents(Arrays.asList("OnConnect"));
      sendMessage(event);

      Assert.assertEquals("Event Landed", context.getVariable("event"));
      Assert.assertEquals("OnConnect event handler called", context.getVariable("onconnect"));
      Assert.assertEquals(null, context.getVariable("onboot"));
      Assert.assertEquals(null, context.getVariable("onupdate"));
   }
   
   @Test
   public void testMatchOnBoot() {
      IpcdEvent event = new IpcdEvent();
      event.setDevice(IpcdFixtures.getDevice());
      event.setEvents(Arrays.asList("OnBoot"));
      sendMessage(event);

      Assert.assertEquals("Event Landed", context.getVariable("event"));
      Assert.assertEquals(null, context.getVariable("onconnect"));
      Assert.assertEquals("OnBoot event handler called", context.getVariable("onboot"));
      Assert.assertEquals(null, context.getVariable("onupdate"));
   }
   
   @Test
   public void testMatchOnNotMatched() {
      IpcdEvent event = new IpcdEvent();
      event.setDevice(IpcdFixtures.getDevice());
      event.setEvents(Arrays.asList("OnNotMatched"));
      sendMessage(event);

      Assert.assertEquals("Event Landed", context.getVariable("event"));
      Assert.assertEquals(null, context.getVariable("onconnect"));
      Assert.assertEquals(null, context.getVariable("onboot"));
      Assert.assertEquals(null, context.getVariable("onupdate"));
   }
   
   @Test
   public void testMatchWithBlankEvent() {
      IpcdEvent event = new IpcdEvent();
      event.setDevice(IpcdFixtures.getDevice());
      event.setEvents(Collections.emptyList());
      sendMessage(event);

      Assert.assertEquals("Event Landed", context.getVariable("event"));
      Assert.assertEquals(null, context.getVariable("onconnect"));
      Assert.assertEquals(null, context.getVariable("onboot"));
      Assert.assertEquals(null, context.getVariable("onupdate"));
   }
   
   @Test
   public void testMatchOnUpdate() {
      IpcdEvent event = new IpcdEvent();
      event.setDevice(IpcdFixtures.getDevice());
      event.setEvents(Arrays.asList("OnUpdate"));
      sendMessage(event);

      Assert.assertEquals("Event Landed", context.getVariable("event"));
      Assert.assertEquals(null, context.getVariable("onconnect"));
      Assert.assertEquals(null, context.getVariable("onboot"));
      Assert.assertEquals("OnUpdate event handler called", context.getVariable("onupdate"));
   }
   
   @Test
   public void testMatchOnConnectAndOnBoot() {
      IpcdEvent event = new IpcdEvent();
      event.setDevice(IpcdFixtures.getDevice());
      event.setEvents(Arrays.asList("OnConnect", "OnBoot"));
      sendMessage(event);

      Assert.assertEquals("Event Landed", context.getVariable("event"));
      Assert.assertEquals("OnConnect event handler called", context.getVariable("onconnect"));
      Assert.assertEquals("OnBoot event handler called", context.getVariable("onboot"));
      Assert.assertEquals(null, context.getVariable("onupdate"));
   }
   
   @Test
   public void testMatchOnConnectOnUpdateAndOnBoot() {
      IpcdEvent event = new IpcdEvent();
      event.setDevice(IpcdFixtures.getDevice());
      event.setEvents(Arrays.asList("OnConnect", "OnUpdate", "OnBoot"));
      sendMessage(event);

      Assert.assertEquals("Event Landed", context.getVariable("event"));
      Assert.assertEquals("OnConnect event handler called", context.getVariable("onconnect"));
      Assert.assertEquals("OnBoot event handler called", context.getVariable("onboot"));
      Assert.assertEquals("OnUpdate event handler called", context.getVariable("onupdate"));
   }
   
   @Test
   public void testMatchOnConnectOnUpdateAndOnBootWithOthers() {
      IpcdEvent event = new IpcdEvent();
      event.setDevice(IpcdFixtures.getDevice());
      event.setEvents(Arrays.asList("OnConnect", "OnUpdate", "OnBoot", "OnDownloaded", "OnVixen"));
      sendMessage(event);

      Assert.assertEquals("Event Landed", context.getVariable("event"));
      Assert.assertEquals("OnConnect event handler called", context.getVariable("onconnect"));
      Assert.assertEquals("OnBoot event handler called", context.getVariable("onboot"));
      Assert.assertEquals("OnUpdate event handler called", context.getVariable("onupdate"));
   }
}

