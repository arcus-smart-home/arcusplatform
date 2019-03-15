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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.binary.Base64;

import com.iris.agent.hal.IrisHal;
import com.iris.agent.spy.SpyService;
import com.iris.io.json.JSON;

/**
 * Serves up data in JSON form for Ajax calls from webpages.
 * 
 * @author Erik Larson
 *
 */
public class SpyApiServlet extends AbstractSpyServlet {
   private static final long serialVersionUID = -8339237275174978535L;
   
   public static final String GET_PROCESS_LOAD = "getload";
   public static final String GET_PLATFORM_MSGS = "getplatmsg";
   public static final String GET_LED_STATE = "getled";
   
   public SpyApiServlet() {
      addPage(GET_PROCESS_LOAD, r -> pack(decodeLoad()));
      addPage(GET_LED_STATE, r -> pack(IrisHal.getLedState()));
      addPage(GET_PLATFORM_MSGS, r-> getMessages());
   }
   
   @Override
   protected String contentType(String pageName) {
      return "application/json";
   }
   
   @Override
   protected String transform(String pageName, Object value) {
      return value != null ? JSON.toJson(value) : "";
   }
   
   public static SimpleData pack(Object value) {
      return new SimpleData(value != null ? value.toString() : "");
   }
   
   private String decodeLoad() {    
      StringBuffer sb = new StringBuffer();
      byte[] compressedLoad = Base64.decodeBase64(IrisHal.getLoad());
      try (BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(compressedLoad))))) {
         br.lines().forEach(l -> sb.append(l).append("\n"));
      } catch (IOException e) {
         // Swallow Exception For Now
         sb.append("Error Decoding Load");
      }
      return sb.toString();
   }
   
   private SimpleMessages getMessages() {
      return new SimpleMessages( 
            SpyService.INSTANCE.incomingPlatformMsgs().map(m -> JSON.toJson(m)).collect(Collectors.toCollection(() -> new ArrayList<>())),
            SpyService.INSTANCE.outgoingPlatformMsgs().map(m -> JSON.toJson(m)).collect(Collectors.toCollection(() -> new ArrayList<>()))
            );
   }
   
   private static class SimpleMessages {
      @SuppressWarnings("unused")
      List<String> incoming;
      @SuppressWarnings("unused")
      List<String> outgoing;
      
      SimpleMessages(List<String> incoming, List<String> outgoing) {
         Collections.reverse(incoming);
         Collections.reverse(outgoing);
         this.incoming = incoming; 
         this.outgoing = outgoing;
      }
   }
   
   private static class SimpleData {
      @SuppressWarnings("unused")
      String data;
      
      SimpleData(String data) {
         this.data = data;
      }
   }
}

