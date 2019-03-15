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
package com.iris.agent.scene;

import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.Protocol;
import com.iris.protocol.zwave.ZWaveExternalProtocol;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author David Easley (deasley@btbcku.com) on 6/1/17.
 */
public class ZWaveScene {
    private static final Logger log = LoggerFactory.getLogger(SceneService.class);
    private static final ConcurrentMap<Byte,ConcurrentMap<Byte,Integer>> priorities = new ConcurrentHashMap<>();

    public static int calculatePriority(ProtocolMessage msg) {
        try {
            Protocol.Command cmdMsg = Protocol.Command.serde().fromBytes(ByteOrder.BIG_ENDIAN,
                                        msg.getValue(ZWaveExternalProtocol.INSTANCE).getPayload());
            byte cmdClassId = (byte)( ((byte)cmdMsg.getCommandClassId()) & 0xFF );
            byte cmdId = (byte)( ((byte)cmdMsg.getCommandId()) & 0xFF );

            ConcurrentMap<Byte,Integer> cmdPriorities = priorities.computeIfAbsent(cmdClassId, (cid) -> new ConcurrentHashMap<>());
            return cmdPriorities.computeIfAbsent(cmdId, (cmd) -> {
               String commandName = ZWaveAllCommandClasses.getClass(cmdClassId).get(cmdId).commandName;
               return (commandName.contains("set") && !commandName.contains("setup"))
                  ? 1 //if the command changes a device state, return a higher priority
                  : 0;
               });
        } catch (Exception e){
            log.warn("entering scene: error in decoding zwave message to assess priority, err:", e);
        }

        return 0;
    }
}

