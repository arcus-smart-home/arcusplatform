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
package com.iris.prodcat;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.capability.definition.Definition;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.definition.ObjectDefinition;
import com.iris.capability.definition.ServiceDefinition;
import com.iris.core.messaging.memory.InMemoryIntraServiceMessageBus;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.ClasspathDefinitionRegistry;
import com.iris.messages.service.ProductCatalogService.ReloadEvent;
import com.iris.messages.service.ProductCatalogService.ReloadSuccessResponseEvent;
import com.iris.messages.services.PlatformConstants;
import com.iris.resource.Resource;
import com.iris.resource.Resources;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Modules;


@Modules({ InMemoryMessageModule.class })
public class TestProductCatalogReloadListener extends IrisMockTestCase {

   @Inject
   private InMemoryIntraServiceMessageBus intraServiceBus;

   @Inject
   private ProductCatalogReloadListener listener;

   @Inject
   private ProductCatalogManager prodCatManager;
   
   @Provides
   @Singleton
   @Named(ProductCatalogReloadListener.GENERIC_MESSAGE_BUS_ACTOR_ADDRESS)
   public Address provideMessageBusActorAddress() {
      return Address.fromString("SERV:prodcat:Reload");
   }

   @Before
   public void setUp() throws Exception {
      super.setUp();
      reset(); // clear out any startup calls on the mocks
   }

   @Test
   public void testReloadProdCat() throws URISyntaxException, InterruptedException, TimeoutException {
      Resource catalog = Resources.getResource("classpath:/notanotherfolder/");
      this.prodCatManager.setCatalogResource(catalog);

      CountDownLatch lock = setupLock(); // prep for async call

      assertEquals("classpath:/notanotherfolder/test104.xml", this.prodCatManager.getProductCatalogPath()); // test initial load of 'latest' version

      MessageBody payload = ReloadEvent.builder().withProdCatVersion(8).build(); // ask for a particular version

      PlatformMessage message = PlatformMessage.createBroadcast(payload, Address.fromString("SERV:junit:"));

      this.intraServiceBus.send(message);

      lock.await(2000, TimeUnit.MILLISECONDS); // wait for the new prodcat
      
      PlatformMessage reload = this.intraServiceBus.take(); // the message just sent
      PlatformMessage success = this.intraServiceBus.take(); // the success event  
      
      assertNotNull(success);
      assertFalse(success.isError());
      assertEquals(ProductCatalogReloadListener.PRODUCTCATALOG_GENERIC_MESSAGE_BUS_SRC_ADDRESS, success.getSource());
      assertEquals(Address.fromString("SERV:prodcat:Reload"), success.getActor());

      MessageBody event = success.getValue();
      assertEquals(ReloadSuccessResponseEvent.NAME, event.getMessageType());
      assertEquals("classpath:/notanotherfolder/test8.xml", event.getAttributes().get(ReloadSuccessResponseEvent.ATTR_PRODCATPATH));

      assertEquals("classpath:/notanotherfolder/test8.xml", this.prodCatManager.getProductCatalogPath()); // test the new version was loaded

      lock = setupLock(); // we burned our last lock, make a new one.

      payload = ReloadEvent.builder().build(); // ask for the 'latest' version

      message = PlatformMessage.createBroadcast(payload, Address.fromString("SERV:junit:"));

      this.intraServiceBus.send(message);

      lock.await(2000, TimeUnit.MILLISECONDS); // wait for the new prodcat

      assertEquals("classpath:/notanotherfolder/test104.xml", this.prodCatManager.getProductCatalogPath()); // test for 'latest' version
   }

   @Test
   public void testReloadProdCatFileError() throws URISyntaxException, InterruptedException, TimeoutException {
      CountDownLatch lock = setupLock(); // prep for async call

      assertEquals("classpath:/product_catalog.xml", this.prodCatManager.getProductCatalogPath()); // test initial load of 'latest' version

      MessageBody payload = ReloadEvent.builder().withProdCatVersion(8).build(); // ask for a particular version

      PlatformMessage message = PlatformMessage.createBroadcast(payload, Address.fromString("SERV:junit:"));

      this.intraServiceBus.send(message);

      lock.await(2000, TimeUnit.MILLISECONDS); // wait for the new prodcat

      PlatformMessage relaod = this.intraServiceBus.take(); // the message just sent
      PlatformMessage error = this.intraServiceBus.take(); // the error returned 

      assertNotNull(error);
      assertTrue(error.isError());
      assertEquals(ProductCatalogReloadListener.PRODUCTCATALOG_GENERIC_MESSAGE_BUS_SRC_ADDRESS, error.getSource());
      assertEquals(Address.fromString("SERV:prodcat:Reload"), error.getActor());

      ErrorEvent event = (ErrorEvent) error.getValue();
      assertTrue(event.getMessage().contains("not a directory"));

      assertEquals("classpath:/product_catalog.xml", this.prodCatManager.getProductCatalogPath()); // test the new version was loaded
   }

   @Test
   public void testReloadProdCatVersionError() throws URISyntaxException, InterruptedException, TimeoutException {
      Resource catalog = Resources.getResource("classpath:/notanotherfolder/");
      this.prodCatManager.setCatalogResource(catalog);
      CountDownLatch lock = setupLock(); // prep for async call

      assertEquals("classpath:/notanotherfolder/test104.xml", this.prodCatManager.getProductCatalogPath()); // test initial load of 'latest' version

      MessageBody payload = ReloadEvent.builder().withProdCatVersion(22).build(); // ask for a particular version

      PlatformMessage message = PlatformMessage.createBroadcast(payload, Address.fromString("SERV:junit:"));
      
      this.intraServiceBus.send(message);

      lock.await(2000, TimeUnit.MILLISECONDS); // wait for the new prodcat

      PlatformMessage reload = this.intraServiceBus.take(); // the message just sent
      PlatformMessage error = this.intraServiceBus.take(); // the error returned 

      assertNotNull(error);
      assertTrue(error.isError());
      assertEquals(ProductCatalogReloadListener.PRODUCTCATALOG_GENERIC_MESSAGE_BUS_SRC_ADDRESS, error.getSource());
      assertEquals(Address.fromString("SERV:prodcat:Reload"), error.getActor());

      ErrorEvent event = (ErrorEvent) error.getValue();
      assertTrue(event.getMessage().contains("Requested product catalog version"));

      assertEquals("classpath:/notanotherfolder/test104.xml", this.prodCatManager.getProductCatalogPath()); // test the new version was loaded
   }

   /**
    * Test to ensure the reload event exists in the services registry
    */
   @Test
   public void testProdCatGetServiceReloadEvent() {
      DefinitionRegistry registry = ClasspathDefinitionRegistry.instance();
      ServiceDefinition prodcat = registry.getService(PlatformConstants.SERVICE_PRODUCTCATALOG);
      assertNotNull(prodcat);
      assertEventExist(prodcat, ReloadEvent.NAME);
   }
   
   @Test
   public void testProdCatGetServiceReloadSuccessEvent() {
      DefinitionRegistry registry = ClasspathDefinitionRegistry.instance();
      ServiceDefinition prodcat = registry.getService(PlatformConstants.SERVICE_PRODUCTCATALOG);
      assertNotNull(prodcat);
      assertEventExist(prodcat, ReloadSuccessResponseEvent.NAME);
   }

   private void assertEventExist(ObjectDefinition def, String expected) {
      assertFoundInList(def.getEvents(), stripOffPrefix(def.getNamespace(), expected));
   }

   private void assertFoundInList(List<? extends Definition> defList, String expectedName) {
      if (defList != null) {
         for (Definition e : defList) {
            if (expectedName.equals(e.getName())) {
               return;
            }
         }
      }
      fail(expectedName + " cannot be found.");

   }

   private String stripOffPrefix(String prefix, String name) {
      return name.substring(prefix.length() + 1);
   }

   /**
    * Setup a lock that we can use to wait on the success of the listener.
    * 
    * @return The lock being used by the listener.
    * @throws InterruptedException
    */
   private CountDownLatch setupLock() throws InterruptedException {
      CountDownLatch lock = new CountDownLatch(1);

      this.listener.setAfterReloadFunction(() -> {
         lock.countDown();
      });

      return lock;
   }

}

