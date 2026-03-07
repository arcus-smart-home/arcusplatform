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
package com.iris.agent.zwave.engine;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Factory that creates a ZWaveSerialEngine using the configured serial port.
 */
public class ZWaveSerialEngineFactory implements ZWaveEngineFactory {

   private final String portPath;

   @Inject
   public ZWaveSerialEngineFactory(@Named("iris.zwave.port") String portPath) {
      this.portPath = portPath;
   }

   @Override
   public ZWaveEngine createEngine() {
      return new ZWaveSerialEngine(portPath);
   }
}
