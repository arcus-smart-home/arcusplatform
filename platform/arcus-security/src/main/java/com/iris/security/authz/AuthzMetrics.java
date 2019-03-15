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
package com.iris.security.authz;

import com.codahale.metrics.Counter;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public class AuthzMetrics {
   private final Counter authorized;
   private final Counter unauthorizedNoPlace;
   private final Counter unauthorizedNotOwner;
   private final Counter unauthorizedWrongPerson;
   private final Counter unauthorizedWrongPlace;
   private final Counter unauthorizedSupportRequest;
   
   public AuthzMetrics() {
      this(IrisMetrics.metrics("bridge.authz"));
   }
   
   public AuthzMetrics(IrisMetricSet metrics) {
      authorized = metrics.counter("authorized");
      unauthorizedNoPlace = metrics.counter("unauthorized.noplace");
      unauthorizedNotOwner = metrics.counter("unauthorized.notowner");
      unauthorizedWrongPerson = metrics.counter("unauthorized.wrongperson");
      unauthorizedWrongPlace = metrics.counter("unauthorized.wrongplace");
      unauthorizedSupportRequest = metrics.counter("unauthorized.supportrequest");
   }

   public void onAuthorized() {
      authorized.inc();
   }
   
   public void onUnauthorizedNoPlace() {
      unauthorizedNoPlace.inc();
   }
   
   public void onUnauthorizedNonAccountHolder() {
      unauthorizedNotOwner.inc();
   }

   public void onUnauthorizedWrongPerson() {
      unauthorizedWrongPerson.inc();
   }

	public void onUnauthorizedWrongPlace() {
		unauthorizedWrongPlace.inc();
	}

	public void onUnauthorizedSupport() {
		unauthorizedSupportRequest.inc();
	}
   
}

