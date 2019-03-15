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
package com.iris.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class Net {

   public static String toQueryString(Map<String,String> parameters){
	   return toQueryString(parameters, "");
   }

   public static String urlEncode(String val) {
      try {
         return URLEncoder.encode(val, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         //really? sigh...
         throw new RuntimeException(e);
      }
   }
   
   public static String toQueryString(Map<String,String> parameters, String keyPrefix){
      List<String>nvps=new ArrayList<String>();
      for(Map.Entry<String, String>e:parameters.entrySet()){
         nvps.add(String.format("%s=%s", urlEncode(keyPrefix+e.getKey()),urlEncode(e.getValue())));
      }
      return StringUtils.join(nvps, "&");
   }
}

