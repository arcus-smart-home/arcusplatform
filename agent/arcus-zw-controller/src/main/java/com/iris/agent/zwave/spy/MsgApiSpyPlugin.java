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
package com.iris.agent.zwave.spy;

import javax.servlet.http.HttpServletRequest;

import com.iris.agent.http.servlet.SpyApiServlet;
import com.iris.agent.spy.SpyPlugIn;

public class MsgApiSpyPlugin implements SpyPlugIn {

   @Override
   public Object apply(HttpServletRequest input) {
      StringBuffer sb = new StringBuffer();
      ZWSpy.INSTANCE.getZipMessages().forEach(m -> sb.append(m).append("\n"));
      return SpyApiServlet.pack(sb.toString());
   }

   @Override
   public boolean showLink() {
      return false;
   }

   @Override
   public String pageName() {
      return "zipmsgapi";
   }

   @Override
   public String title() {
      return null;
   }

}

