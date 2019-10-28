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
package com.iris.agent.zwave;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.zwave.client.ZWCallback;
import com.iris.agent.zwave.engine.EngineListener;
import com.iris.agent.zwave.engine.ZWaveEngineMsg;
import com.iris.agent.zwave.spy.ZWSpy;

public class ZWEngineListener implements EngineListener {
   private final static Logger logger = LoggerFactory.getLogger(ZWEngineListener.class);
   
   private final ZWCallback zwCallback;
   
   public ZWEngineListener(ZWCallback zwCallback) {
      this.zwCallback = zwCallback;
   }

   @Override
   public void onBootstrapSuccess(long homeId) {
      ZWServices.INSTANCE.getNetwork().initialize(homeId);
   }

   @Override
   public void onBootstrapFailure() {
      logger.error("ZWave Engine has failed bootstrap process.");
      //TODO: Alert platform that the hub has an issue.
   }

   @Override
   public void onNetworkResetting() {
      // TODO Auto-generated method stub
   }

   @Override
   public void onNetworkResettingFinished() {
      // TODO Auto-generated method stub
   }

   @Override
   public void onNetworkShutdown() {
      // TODO Auto-generated method stub
   }

   @Override
   public void onNotification(ZWaveEngineMsg msg) {
      ZWSpy.INSTANCE.unsolicited(msg);
      zwCallback.callback(msg);
   }

}

