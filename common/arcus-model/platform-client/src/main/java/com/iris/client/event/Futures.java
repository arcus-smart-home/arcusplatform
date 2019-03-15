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

import com.google.common.base.Function;
import com.google.common.util.concurrent.SettableFuture;
import com.iris.client.util.Result;
import com.iris.client.util.Results;


/**
 * 
 */
public class Futures {

   public static <V> SettableClientFuture<V> settableFuture() {
      return new SettableClientFuture<V>();
   }
   
   public static <V> ClientFuture<V> succeededFuture(V value) {
      return resultFuture(Results.fromValue(value));
   }

   public static <V> ClientFuture<V> failedFuture(Throwable error) {
      return resultFuture(Results.<V>fromError(error));
   }

   public static <V> ClientFuture<V> resultFuture(Result<V> value) {
      return new DelegatingClientFuture<>(com.google.common.util.concurrent.Futures.immediateFuture(value));
   }

   public static <T, V> ClientFuture<T> transform(ClientFuture<V> input, final Function<V, T> transform) {
      final SettableClientFuture<T> transformed = new SettableClientFuture<T>();
      input.onCompletion(new Listener<Result<V>>() {
         @Override
         public void onEvent(Result<V> event) {
            if(event.isValue()) {
               try {
                  T value = transform.apply(event.getValue());
                  transformed.setValue(value);
               }
               catch(Exception e) {
                  transformed.setError(e);
               }
            }
            else {
               // error result has any generic type
               transformed.setResult((Result<T>) event);
            }
         }
      });
      return transformed;
   }

	public static <T, V> ClientFuture<T> chain(ClientFuture<V> first, final Function<V, ClientFuture<T>> next) {
		final ChainedClientFuture<T> chained = new ChainedClientFuture<>();
		first
      	.onCompletion(new Listener<Result<V>>() {
	         @Override
	         public void onEvent(Result<V> event) {
	            if(event.isValue()) {
	               try {
	                  next
	                  	.apply(event.getValue())
	                  	.onCompletion(chained);
	               }
	               catch(Exception e) {
	               	chained.onEvent(Results.<T>fromError(e));
	               }
	            }
	            else {
	               // error result has any generic type
	            	chained.onEvent((Result<T>) event);
	            }
	         }
	      });
      return chained;
	}
	
	private static class ChainedClientFuture<V> extends DelegatingClientFuture<V> implements Listener<Result<V>> {
		private SettableFuture<Result<V>> delegate = SettableFuture.<Result<V>>create();

		private ChainedClientFuture() {
		   this(SettableFuture.<Result<V>>create());
		}
		
		private ChainedClientFuture(SettableFuture<Result<V>> delegate) {
		   super(delegate);
		   this.delegate = delegate;
		}

		@Override
		public void onEvent(Result<V> event) {
			delegate.set(event);
		}

	}
}

