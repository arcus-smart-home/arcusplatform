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
package com.iris.oculus.util;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.client.event.Listener;

public class OperationAdapter<V> implements Listener<OperationEvent<V>> {
	private static final Logger logger = LoggerFactory.getLogger(OperationAdapter.class);

	@Override
	public void onEvent(OperationEvent<V> event) {
		switch(event.getType()) {
		case LOADING:
			onLoading();
			break;
		case LOADED:
			onLoaded(event.result().orElse(null));
			break;
		case ERROR:
			onError(event.error().get());
			break;
		default:
			onError(new IllegalArgumentException("Unexpected error type [" + event.getType() + "]"));
		}
	}

	protected void onLoading() {}
	
	protected void onLoaded(@Nullable V value) {}
	
	protected void onError(Throwable cause) {
		logger.warn("Unhandled operation error", cause);
	}
}

