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
package com.iris.driver.groovy.zwave;

import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveNodeInfoMessage;

import groovy.lang.Closure;

public class OnZWaveNodeInfo extends Closure<Object> {
   private final EnvironmentBinding binding;
   
   public OnZWaveNodeInfo(EnvironmentBinding binding) {
      super(binding);
      this.setResolveStrategy(TO_SELF);
      this.binding = binding;
   }
   
   protected void doCall(Closure<?> closure) {
      ZWaveProtocolEventMatcher matcher = new ZWaveProtocolEventMatcher();
      matcher.setProtocolName(ZWaveProtocol.NAMESPACE);
      matcher.setMessageType(ZWaveNodeInfoMessage.TYPE);
      matcher.setHandler(DriverBinding.wrapAsHandler(closure));
      binding.getBuilder().addEventMatcher(matcher);
   }
}

