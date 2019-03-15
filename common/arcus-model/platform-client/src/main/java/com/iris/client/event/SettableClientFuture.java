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
package com.iris.client.event;

import java.util.concurrent.Executor;

import com.google.common.util.concurrent.SettableFuture;
import com.iris.client.util.Result;
import com.iris.client.util.Results;

/**
 *
 */
public class SettableClientFuture<V> extends DelegatingClientFuture<V> {
	private SettableFuture<Result<V>> delegate = SettableFuture.<Result<V>>create();

	public SettableClientFuture() {
	   this(SettableFuture.<Result<V>>create());
	}
	
	protected SettableClientFuture(Executor executor) {
	   this(SettableFuture.<Result<V>>create(), executor);
	}
	
	private SettableClientFuture(SettableFuture<Result<V>> delegate) {
	   super(delegate);
	   this.delegate = delegate;
	}
	
   private SettableClientFuture(SettableFuture<Result<V>> delegate, Executor executor) {
      super(delegate, executor);
      this.delegate = delegate;
   }
   
	public void setValue(V value) {
		setResult(Results.fromValue(value));
	}

	public void setError(Throwable error) {
		setResult(Results.<V>fromError(error));
	}

	public void setResult(Result<V> result) {
		delegate.set(result);
	}

}

