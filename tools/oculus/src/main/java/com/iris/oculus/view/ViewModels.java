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
package com.iris.oculus.view;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import com.iris.client.model.Model;
import com.iris.client.model.Store;

public class ViewModels {

   public static <M extends Model> ViewModel<M> from(Store<M> store) {
      return new StoreViewModel<>(store);
   }

   public static <M> ViewModel<M> from(Collection<M> models) {
      return new SimpleViewModel<>(models);
   }

   public static <M extends Model> ViewModel<M> sort(Store<M> store, Comparator<? super M> sortBy) {
      return sort(from(store), sortBy);
   }

   public static <M> ViewModel<M> sort(Collection<M> models, Comparator<? super M> sortBy) {
      return sort(from(models), sortBy);
   }

   public static <M> ViewModel<Optional<M>> optional(ViewModel<M> view) {
      return new OptionalViewModel<>(view);
   }

   public static <M> ViewModel<M> sort(ViewModel<M> view, Comparator<? super M> sortBy) {
      FilteredViewModel<M> sorted = new FilteredViewModel<>(view);
      sorted.sortBy(sortBy);
      return sorted;
   }
}

