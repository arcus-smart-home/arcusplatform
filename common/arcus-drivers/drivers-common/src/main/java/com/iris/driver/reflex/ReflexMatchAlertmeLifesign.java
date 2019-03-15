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
package com.iris.driver.reflex;

public final class ReflexMatchAlertmeLifesign implements ReflexMatch {
   private final int profile;
   private final int endpoint;
   private final int cluster;
   private final int setMask;
   private final int clrMask;

   public ReflexMatchAlertmeLifesign(int profile, int endpoint, int cluster, int setMask, int clrMask) {
      this.profile = profile;
      this.endpoint = endpoint;
      this.cluster = cluster;
      this.setMask = setMask;
      this.clrMask = clrMask;
   }

   public int getProfile() {
      return profile;
   }

   public int getEndpoint() {
      return endpoint;
   }

   public int getCluster() {
      return cluster;
   }

   public int getSetMask() {
      return setMask;
   }

   public int getClrMask() {
      return clrMask;
   }

   @Override
   public String toString() {
      return "ReflexMatchAlertmeLifesign [" + 
         "profile=" + profile + 
         ",endpoint=" + endpoint + 
         ",cluster=" + cluster + 
         ",set=" + setMask +
         ",clr=" + clrMask + 
      "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + clrMask;
      result = prime * result + cluster;
      result = prime * result + endpoint;
      result = prime * result + profile;
      result = prime * result + setMask;
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      ReflexMatchAlertmeLifesign other = (ReflexMatchAlertmeLifesign) obj;
      if (clrMask != other.clrMask)
         return false;
      if (cluster != other.cluster)
         return false;
      if (endpoint != other.endpoint)
         return false;
      if (profile != other.profile)
         return false;
      if (setMask != other.setMask)
         return false;
      return true;
   }
}

