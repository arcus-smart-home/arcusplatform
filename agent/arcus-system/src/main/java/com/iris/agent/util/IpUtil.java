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
package com.iris.agent.util;

public class IpUtil {

   
   public static String genNetmask(int n) {
      String netmaskStr="";
      int netmask = 0;
      int i;
      
      for (i=0;i<n;i++) {
         netmask = netmask >> 1;
         netmask = netmask | 0x80000000;
      }      

      int a = ((netmask & 0xFF000000) >> 24) & 0x000000FF;
      int b = (netmask & 0x00FF0000) >> 16;
      int c = (netmask & 0x0000FF00) >> 8;
      int d = (netmask & 0x000000FF);
      
      netmaskStr = a + "." + b + "." + c + "." + d;
      
      return netmaskStr;
   }
      
}

