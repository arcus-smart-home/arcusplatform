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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for working with subscriptions.
 */
public class Subscriptions {
   private static final Logger LOGGER = LoggerFactory.getLogger(Subscriptions.class);

   private static final Subscription EmptySubscription = new Subscription() {
      @Override
      public void remove() {
         // no-op
      }
   };
   
   public static Subscription empty() {
      return EmptySubscription;
   }
   
   /**
    * Turns a collection of subscriptions into  a single subscription.  Invoking
    * remove() will remove all subscriptions.
    * @param subscriptions
    * @return
    */
   public static Subscription marshal(final Iterable<Subscription> subscriptions) {
      return new Subscription() {
         
         @Override
         public void remove() {
            for(Subscription subscription: subscriptions) {
               try {
                  subscription.remove();
               }
               catch(Exception e) {
                  LOGGER.warn("Unable to remove subscription [{}]", subscription, e);
               }
            }
         }
      };
   }
   
   public static Subscription remove(@Nullable Subscription subscription) {
      if(subscription != null) {
         subscription.remove();
      }
      return empty();
   }
}

