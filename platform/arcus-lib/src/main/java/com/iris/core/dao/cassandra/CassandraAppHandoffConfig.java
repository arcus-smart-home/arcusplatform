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
package com.iris.core.dao.cassandra;

import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class CassandraAppHandoffConfig {
	@Inject(optional=true) @Named("app.handoff.token.ttl.sec")
	private long ttlSec = TimeUnit.MINUTES.toSeconds(5);
	@Inject(optional=true) @Named("app.handoff.token.length")
	private int tokenLength = 64;
	
	public long getTtlSec() {
		return ttlSec;
	}
	
	public void setTtlSec(long ttlSec) {
		this.ttlSec = ttlSec;
	}
	
	public int getTokenLength() {
		return tokenLength;
	}
	
	public void setTokenLength(int tokenLength) {
		this.tokenLength = tokenLength;
	}

}


