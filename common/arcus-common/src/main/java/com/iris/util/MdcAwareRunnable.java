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

import java.util.Map;

import org.slf4j.MDC;

import com.google.common.collect.ImmutableMap;
import com.iris.util.MdcContext.MdcContextReference;

/**
 * 
 */
public class MdcAwareRunnable implements Runnable {
   
   public static MdcAwareRunnable wrap(Runnable delegate) {
      return new MdcAwareRunnable(delegate, MDC.getCopyOfContextMap());
   }
   
   public static MdcAwareRunnable wrap(Runnable delegate, Map<String, String> mdc) {
      return new MdcAwareRunnable(delegate, mdc);
   }
   
   private static Map<String, String> getContext(Map<String, String> mdc) {
      return mdc == null ? ImmutableMap.<String, String>of() : mdc;
   }
   
   private final Runnable delegate;
   private final Map<String, String> mdc;

   protected MdcAwareRunnable(Runnable delegate, Map<String, String> mdc) {
      this.delegate = delegate;
      this.mdc = getContext(mdc);
   }

   /* (non-Javadoc)
    * @see java.lang.Runnable#run()
    */
   @Override
   public void run() {
      try(MdcContextReference context = MdcContext.captureMdcContext()) {
         MDC.setContextMap(mdc);
         delegate.run();
      }
   }

}

