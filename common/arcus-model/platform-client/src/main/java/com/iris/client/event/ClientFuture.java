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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.base.Function;
import com.iris.client.util.Result;

/**
 * An extension of {@link Future} that allows event listeners
 * to be added.
 */
public interface ClientFuture<V> extends Future<V> {

   /**
    * This returns {@code true} when the future is done
    * and an error result has been set.  If {@code isDone() == false}
    * this will also always be false.  If {@code isDone() == true}
    * and {@code isError() == true} than get() will throw an
    * {@link ExecutionException}.
    * @return
    */
   public boolean isError();

   public ClientFuture<V> onSuccess(Listener<V> handler);

   public ClientFuture<V> onFailure(Listener<Throwable> error);

   public ClientFuture<V> onCompletion(Listener<Result<V>> value);

   public <O> ClientFuture<O> transform(Function<V, O> transform);
   
   public <O> ClientFuture<O> chain(Function<V, ClientFuture<O>> next);
}

