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
package com.iris.netty.server.session;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

import com.iris.Utils;

//TODO: This is a stand-in until we determine what exactly is used as the session id in the client
//      address. It may tie into authentication.
public class IrisNettyClientIds {
   private static final Logger logger = LoggerFactory.getLogger(IrisNettyClientIds.class);
   
   // FIXME this would probably be better as the cluster member id
   private final static int uniquifier = new Random().nextInt();
   private final static AtomicInteger currentId = new AtomicInteger(0);
   
   static {
      byte [] bytes = new byte[] {
            (byte) ((uniquifier >> 24) & 0xff),
            (byte) ((uniquifier >> 16) & 0xff),
            (byte) ((uniquifier >>  8) & 0xff),
            (byte) (uniquifier & 0xff)
      };
            
      logger.info("Using address prefix of {}", Utils.b64Encode(bytes).substring(0, 6));
   }
   
   @Deprecated
   public static long getAndIncrement() {
      return currentId.getAndIncrement();
   }
   
   public static String createId() {
      int id = currentId.incrementAndGet();
      byte [] bytes = new byte[] {
            (byte) ((uniquifier >> 24) & 0xff),
            (byte) ((uniquifier >> 16) & 0xff),
            (byte) ((uniquifier >>  8) & 0xff),
            (byte) (uniquifier & 0xff),
            (byte) ((id >> 24) & 0xff),
            (byte) ((id >> 16) & 0xff),
            (byte) ((id >>  8) & 0xff),
            (byte) (id & 0xff)
      };
      return Utils.b64Encode(bytes).substring(0, 11); // strip the = sign because its annoying and we never decode this value
   }
}

