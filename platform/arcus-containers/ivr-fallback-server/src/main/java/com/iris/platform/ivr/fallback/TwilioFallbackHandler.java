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
package com.iris.platform.ivr.fallback;

import com.codahale.metrics.Counter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.RequestHandlerImpl;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;


@Singleton
@HttpGet("/ivr/fallback?*")
public class TwilioFallbackHandler extends RequestHandlerImpl {

	private static final String SERVICE_NAME="twilio.fallback.handler";
	private static final IrisMetricSet METRICS = IrisMetrics.metrics(SERVICE_NAME);
	private static final Counter FALLBACK_REQUEST_COUNTER = METRICS.counter("request.fallback.count");

	@Inject
	public TwilioFallbackHandler(AlwaysAllow alwaysAllow) {
		super(alwaysAllow, new FallbackResponder("fallback.xml", FALLBACK_REQUEST_COUNTER));
	}
}

