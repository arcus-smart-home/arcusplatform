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



import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.platform.AbstractPlatformMessageListener;
import com.iris.core.platform.IntraServiceMessageBus;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.ProductCatalogService.ReloadEvent;
import com.iris.messages.service.ProductCatalogService.ReloadSuccessResponseEvent;
import com.iris.messages.service.ProductCatalogService.ReportProdCatVersionEvent;
import com.iris.messages.service.ProductCatalogService.ReportProdCatVersionResponseEvent;
import com.iris.messages.services.PlatformConstants;


/**
 * Listen for the reload event on the bus.  When the event is heard, look for and load the appropriate version of prodcat
 */
@Singleton
public class ProductCatalogReloadListener extends AbstractPlatformMessageListener {

   private static final Logger logger = LoggerFactory.getLogger(ProductCatalogReloadListener.class);
   public static final String GENERIC_MESSAGE_BUS_ACTOR_ADDRESS = "GenericMessageBusActorAddress";
   public static final Address PRODUCTCATALOG_GENERIC_MESSAGE_BUS_SRC_ADDRESS = Address.fromString(MessageConstants.SERVICE + ":" + PlatformConstants.SERVICE_PRODUCTCATALOG + ":");

   
   @Inject
   @Named(GENERIC_MESSAGE_BUS_ACTOR_ADDRESS)
   private Address act;

   private final ProductCatalogManager prodCatManager;
   private Runnable afterReloadFunction = () -> {}; // simple callback used for informing tests that the listener completed its job

   @Inject
   public ProductCatalogReloadListener(IntraServiceMessageBus bus, ProductCatalogManager manager) {
      super(bus);
      this.prodCatManager = manager;
   }

   @Override
   protected void onStart() {
      super.onStart();
      addListeners(Address.broadcastAddress());
   }
   
   @Override
   protected void handleEvent(PlatformMessage message) throws Exception {
      if (ReloadEvent.NAME.equals(message.getMessageType())) {
         onReloadMessage(message);
      }
      if (ReportProdCatVersionEvent.NAME.equals(message.getMessageType())) {
    	  onReportProdCatVersion(message);
      }
   }

   private void onReloadMessage(PlatformMessage message) {
      Integer version = ReloadEvent.getProdCatVersion(message.getValue());
      logger.debug("Received product catelog reload message on intraservice topic from [{}] for product catalog version [{}]", message.getSource(), version);
      try {
         this.prodCatManager.loadCatalogs(version);
         sendSuccessEvent(message);
      }
      catch (RuntimeException e) {
         sendErrorEvent(message.getPlaceId(), e);
      }
      finally {
         this.afterReloadFunction.run();
      }
   }
   
   private void sendErrorEvent(@Nullable String placeId, RuntimeException e) {
      ErrorEvent event = Errors.fromException(e);
      PlatformMessage msg = PlatformMessage.buildEvent(event, PRODUCTCATALOG_GENERIC_MESSAGE_BUS_SRC_ADDRESS).withActor(this.act).withPlaceId(placeId).create(); // send the src address of the parent service
      getMessageBus().send(msg);
   }
   
   private void sendSuccessEvent(PlatformMessage message) {
      MessageBody payload = ReloadSuccessResponseEvent.builder().withProdCatPath(this.prodCatManager.getProductCatalogPath()).build();
      PlatformMessage msg = PlatformMessage.broadcast(message).from(PRODUCTCATALOG_GENERIC_MESSAGE_BUS_SRC_ADDRESS).withActor(this.act).withPayload(payload).create();
      getMessageBus().send(msg);
   }
   
   private void onReportProdCatVersion(PlatformMessage message) {
      MessageBody payload = ReportProdCatVersionResponseEvent.builder().withProdCatPath(this.prodCatManager.getProductCatalogPath()).build();
      PlatformMessage msg = PlatformMessage.broadcast(message).from(PRODUCTCATALOG_GENERIC_MESSAGE_BUS_SRC_ADDRESS).withActor(this.act).withPayload(payload).create();
      getMessageBus().send(msg);
   }

   /**
    * Used for asynchronous tests.  Called after loadCatalogs completes to ensure tests will wait and then run.
    * 
    * @param afterReload
    */
   void setAfterReloadFunction(Runnable afterReload) {
      this.afterReloadFunction = afterReload;
   }

}

