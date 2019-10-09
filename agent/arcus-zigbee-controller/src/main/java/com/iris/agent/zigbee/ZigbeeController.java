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
import com.iris.messages.PlatformMessage;
import com.iris.protocol.ProtocolMessage;
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

    // The Hub port this controller is attached to.
    private Port port;

    /**
     * Constructs the ZWave controller with dependency injection.
     *
     * @param router Hub message router.
     */
    @Inject
    public ZigbeeController(Router router) {
        this.router = router;
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
    // Port Handler Implemntation
    //////////////
    /**
     * Entry point for platform messages for the ZWave controller.
     */
    @Override
    @Nullable
    public Object recv(Port port, PlatformMessage message) throws Exception {
        logger.trace("Handling zwave platform message: {} -> {}", message, message.getValue());

        return null;
    }

    @Override
    public void recv(Port port, ProtocolMessage message) {

    }

    @Override
    public void recv(Port port, Object message) {

    }
}