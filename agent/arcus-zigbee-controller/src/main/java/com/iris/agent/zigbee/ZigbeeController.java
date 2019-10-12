/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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
package com.iris.agent.zigbee;

import com.google.inject.Inject;
import com.iris.agent.addressing.HubAddressUtils;
import com.iris.agent.addressing.HubBridgeAddress;
import com.iris.agent.lifecycle.LifeCycle;
import com.iris.agent.lifecycle.LifeCycleListener;
import com.iris.agent.lifecycle.LifeCycleService;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.agent.router.Router;
import com.iris.agent.zigbee.ember.ZigbeeDriver;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.errors.Errors;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.netflix.governator.annotations.WarmUp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZigbeeController implements PortHandler, LifeCycleListener {
    private static final Logger logger = LoggerFactory.getLogger(ZigbeeController.class);

    public static final HubBridgeAddress ADDRESS = HubAddressUtils.bridge("zigbee", "ZBIG");

    private final AtomicBoolean needsFactoryReset = new AtomicBoolean(false);

    // Hub Message Router
    private final Router router;

    // Zigbee Driver/transport
    private final ZigbeeDriver driver;

    // The Hub port this controller is attached to.
    private Port port;

    /**
     * Constructs the ZWave controller with dependency injection.
     *
     * @param router Hub message router.
     */
    @Inject
    public ZigbeeController(Router router, ZigbeeDriver driver) {
        this.router = router;
        this.driver = driver;
    }

    /**
     * Starts the controller. Called after construction.
     * It hooks up the Zigbee controller to the agent router
     * and the agent life cycle service.
     */
    @WarmUp
    public void start() {
        logger.info("Starting Zigbee controller");
        port = router.connect("zigb", ADDRESS, this);
        LifeCycleService.addListener(this);
    }

    /**
     * Disconnect the controller from the agent router.
     */
    @PreDestroy
    public void stop() {
        if (port != null) {
            router.disconnect(port);
            port = null;
        }
    }

    //////////
    // LifeCycle Listener Implementation
    /////////
    @Override
    public void lifeCycleStateChanged(LifeCycle oldState, LifeCycle newState) {

    }

    @Override
    public void hubAccountIdUpdated(@Nullable UUID oldAcc, @Nullable UUID newAcc) {
        if (oldAcc == null && newAcc != null) {
            needsFactoryReset.set(true);
        }
    }

    @Override
    public void hubReset(LifeCycleService.Reset type) {
        if (type == LifeCycleService.Reset.FACTORY) {
            needsFactoryReset.set(true);
        }
    }

    @Override
    public void hubDeregistered() {
        try {
            //TODO: Anything?
        }
        catch (Exception ex) {
            logger.warn("Cloud not process hub removed: {}", ex.getMessage(), ex);
        }
    }

    ///////////////
    // Port Handler Implementation
    //////////////
    /**
     * Entry point for platform messages for the ZWave controller.
     */
    @Override
    @Nullable
    public Object recv(Port port, PlatformMessage message) throws Exception {
        logger.trace("Handling zwave platform message: {} -> {}", message, message.getValue());

       //TODO: Check for performing backup/restore

       String type = message.getMessageType();
       switch (type) {
          case HubCapability.PairingRequestRequest.NAME:
             return handlePairingRequest(message);

          case HubCapability.UnpairingRequestRequest.NAME:
             return handleUnpairingRequest(message);

          case com.iris.messages.ErrorEvent.MESSAGE_TYPE:
             logger.warn("Error received from platform: {}", message);
             return null;

          default:
             return Errors.unsupportedMessageType(message.getMessageType());
       }    }

    @Override
    public void recv(Port port, ProtocolMessage message) {
       if (!ZigbeeProtocol.NAMESPACE.equals((message.getMessageType()))) {
          return;
       }

       sendZigbeeProtocolMessage(message);
    }

   /**
    * Not used in this implementation.
    */
    @Override
    public void recv(Port port, Object message) {
       logger.trace("call to recv which is unused");
    }

   /**
    * Pairing request handler. This could be a request to start pairing or to
    * stop pairing depending on message contents. The pairing process singleton
    * is called to either start or stop pairing.
    *
    * The message type should be confirmed to be PairingRequest before calling
    * this method with that message.
    *
    * @param message PairingRequest message
    * @return always returns null
    */
   private Object handlePairingRequest(PlatformMessage message) throws Exception {

      MessageBody body = message.getValue();
      String action = HubCapability.PairingRequestRequest.getActionType(body);

      //TODO: Wait for bootstrapping to be finished.

      switch (action) {
         case HubCapability.PairingRequestRequest.ACTIONTYPE_START_PAIRING:
            long timeoutInMillis = HubCapability.PairingRequestRequest.getTimeout(body);
//            Pairing.INSTANCE.startPairing((int)(timeoutInMillis/1000));
            return null;
         case HubCapability.PairingRequestRequest.ACTIONTYPE_STOP_PAIRING:
//            Pairing.INSTANCE.stopPairing();
            return null;
         default:
            // TODO: Better Exception
            throw new Exception("Unknown pairing action: " + action);
      }
   }

   /**
    * Unpairing request handler. This could be a request to start unpairing or
    * to stop unpairing depending on message contents. The pairing process singleton
    * is called to either start or stop unpairing.
    *
    * The message type should be confirmed to be UnpairingRequest before calling
    * this method with that message.
    *
    * @param message UnpairingRequest message
    * @return always returns null
    */
   private Object handleUnpairingRequest(PlatformMessage message) throws Exception {
      MessageBody body = message.getValue();
      String action = HubCapability.UnpairingRequestRequest.getActionType(body);

      //TODO: Wait for bootstrapping to finish.

      switch (action) {
         case HubCapability.UnpairingRequestRequest.ACTIONTYPE_START_UNPAIRING:
            long timeoutInMillis = HubCapability.UnpairingRequestRequest.getTimeout(body);
//            Pairing.INSTANCE.startRemoval((int)(timeoutInMillis/1000));
            return null;
         case HubCapability.UnpairingRequestRequest.ACTIONTYPE_STOP_UNPAIRING:
//            Pairing.INSTANCE.stopRemoval();
            return null;
         default:
            // TODO: Better Exception
            throw new Exception("Unknown unpairing action: " + action);
      }
   }

   private void sendZigbeeProtocolMessage(ProtocolMessage msg) {
//      sendZigbeeProtocolMessage(msg, null);
   }

}