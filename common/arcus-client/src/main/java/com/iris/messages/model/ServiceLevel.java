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
package com.iris.messages.model;

public enum ServiceLevel {
   BASIC, PREMIUM, PREMIUM_FREE, PREMIUM_PROMON, PREMIUM_PROMON_FREE, PREMIUM_ANNUAL, PREMIUM_PROMON_ANNUAL;
   
   public static ServiceLevel fromString(String serviceLevel) {
      if ("premium".equalsIgnoreCase(serviceLevel)) {
         return PREMIUM;
      }
      if ("basic".equalsIgnoreCase(serviceLevel)) {
         return BASIC;
      }
      if ("premium_free".equalsIgnoreCase(serviceLevel)) {
         return PREMIUM_FREE;
      }
      if("premium_promon".equalsIgnoreCase(serviceLevel)) {
         return PREMIUM_PROMON;
      }
      if("premium_promon_free".equalsIgnoreCase(serviceLevel)) {
         return PREMIUM_PROMON_FREE;
      }
      if("premium_annual".equalsIgnoreCase(serviceLevel)) {
         return PREMIUM_ANNUAL;
      }
      if("premium_promon_annual".equalsIgnoreCase(serviceLevel)) {
         return PREMIUM_PROMON_ANNUAL;
      }

      throw new IllegalArgumentException("The string " + serviceLevel + " is an invalid service level.");
   }

   public boolean isPremiumOrPromon(){
      return isPremiumOrPromon(this);
   }
   
   public static boolean isPremiumOrPromon(String serviceLevel){
      return (serviceLevel !=null) && fromString(serviceLevel).isPremiumOrPromon();
   }
   
   public static boolean isPromon(String serviceLevel) {
	   return (serviceLevel !=null) && fromString(serviceLevel).isPromon();
   }

   public static boolean isNonBasicFree(ServiceLevel level) {
      if(level == null) {
         return false;
      }
      return level.isNonBasicFree();
   }

   public boolean isNonBasicFree() {
      switch(this) {
         case PREMIUM_FREE:
         case PREMIUM_PROMON_FREE:
            return true;
         default:
            return false;
      }
   }
   
   public boolean isAnnual() {
      return isAnnual(this);
   }

   public static boolean isAnnual(ServiceLevel level) {
      switch (level) {
         case PREMIUM_ANNUAL:
         case PREMIUM_PROMON_ANNUAL:
            return true;
         default:
            return false;
      }
   }

   public static boolean isJustBasic(ServiceLevel level) {
      if (level == null) {
         return false;
      }
      return BASIC.equals(level);
   }

   public static boolean isPromon(ServiceLevel level) {
      if (level == null) {
         return false;
      }
      return level.isPromon();
   }

   public static boolean isPremiumNotPromon(ServiceLevel level){
      switch(level) {
         case PREMIUM:
         case PREMIUM_FREE:
         case PREMIUM_ANNUAL:
            return true;
         default:
            return false;
      }
   }   
   
   public boolean isPromon() {
      switch(this) {
         case PREMIUM_PROMON:
         case PREMIUM_PROMON_FREE:
         case PREMIUM_PROMON_ANNUAL:
            return true;
         default:
            return false;
      }
   }
   
   public static boolean isPremiumOrPromon(ServiceLevel level){
      if(level==null){
         return false;
      }
      switch(level) {
         case PREMIUM:
         case PREMIUM_FREE:
         case PREMIUM_PROMON:
         case PREMIUM_PROMON_FREE:
         case PREMIUM_ANNUAL:
         case PREMIUM_PROMON_ANNUAL:
            return true;
         default:
            return false;
      }
   }

   public static boolean isPaid(ServiceLevel level) {
      if(level == null) {
         return false;
      }
      return level.isPaid();
   }

   public boolean isPaid() {
      switch(this) {
         case PREMIUM:
         case PREMIUM_PROMON:
         case PREMIUM_ANNUAL:
         case PREMIUM_PROMON_ANNUAL:
            return true;
         default:
            return false;
      }
   }
}

