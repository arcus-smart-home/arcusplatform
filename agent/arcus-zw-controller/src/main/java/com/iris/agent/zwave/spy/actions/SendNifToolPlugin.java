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
package com.iris.agent.zwave.spy.actions;

import javax.servlet.http.HttpServletRequest;

import com.iris.agent.spy.SpyPlugIn;
import com.iris.agent.zwave.ZWServices;
import com.iris.agent.zwave.spy.ZWSpy;

public class SendNifToolPlugin implements SpyPlugIn {

   @Override
   public Object apply(HttpServletRequest input) {
      ZWServices.INSTANCE.getNetwork().requentSendNif();
      ZWSpy.INSTANCE.toolUsed("Request controller to send NIF");
      return "";
   }

   @Override
   public boolean showLink() {
      return false;
   }

   @Override
   public String pageName() {
      return "zipsendnif";
   }

   @Override
   public String title() {
      return null;
   }

}


