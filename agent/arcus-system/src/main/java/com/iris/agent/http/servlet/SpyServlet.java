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
package com.iris.agent.http.servlet;

import com.iris.agent.spy.SpyService;

public class SpyServlet extends AbstractSpyServlet {
   private static final long serialVersionUID = -8339237275174978535L;
   
   public static final String LOAD_PAGE = "load";
   public static final String PLAT_MSG_PAGE = "platmsg";
   public static final String LED_PAGE = "led";
   
   public SpyServlet() {
      addDefaultPage(r -> {
         return render("spy", SpyService.INSTANCE.getContext());
      });
      addPage(LOAD_PAGE, r -> render("spy_load", SpyService.INSTANCE.getContext()));
      addPage(PLAT_MSG_PAGE, r -> render("spy_plat_msg", SpyService.INSTANCE.getContext()));
      addPage(LED_PAGE, r -> render("spy_led", SpyService.INSTANCE.getContext()));
   }
}



