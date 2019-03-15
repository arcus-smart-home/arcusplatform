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
/**
 *
 */
package com.iris.util;

import java.io.Closeable;
import java.util.Map;

import org.slf4j.MDC;

import com.google.common.collect.ImmutableMap;


/**
 *
 */
public class MdcContext {
   public static final String MDC_PLACE           = "place";
   public static final String MDC_FROM            = "from";
   public static final String MDC_TO              = "to";
   public static final String MDC_BY              = "by"; // actor
   public static final String MDC_ID              = "id"; // correlation id
   public static final String MDC_TARGET          = "target"; // the thing doing the processing
   public static final String MDC_TYPE            = "type";
   public static final String MDC_IP              = "ip";
   public static final String MDC_USER_AGENT      = "agent";
   public static final String MDC_CLIENT_VERSION  = "clnt";
   public static final String MDC_LOCATION        = "loc";
   public static final String MDC_SERVICE_LEVEL   = "slvl";

   /**
    * Captures the current MDC context and restores it when
    * this resource is closed.
    * @return
    */
   public static MdcContextReference captureMdcContext() {
      return new MdcContextReference(MDC.getCopyOfContextMap());
   }

   public static final class MdcContextReference implements Closeable {
      private final Map<String, String> mdc;

      MdcContextReference(Map<String, String> mdc) {
         this.mdc = mdc;
      }

      public void restore() {
         if(mdc == null) {
            MDC.setContextMap(ImmutableMap.<String, String>of());
         }
         else {
            MDC.setContextMap(mdc);
         }
      }

      public void close() {
         restore();
      }
   }
}

