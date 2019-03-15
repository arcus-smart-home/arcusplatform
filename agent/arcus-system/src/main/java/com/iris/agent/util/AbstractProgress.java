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
package com.iris.agent.util;

import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class AbstractProgress<T> implements Progress<T> {
   private final CopyOnWriteArraySet<ProgressMonitor<? super T>> monitors = new CopyOnWriteArraySet<>();
   private final Comparator<T> comparator;
   private final T complete;

   protected AbstractProgress(T complete, Comparator<T> comparator) {
      this.complete = complete;
      this.comparator = comparator;
   }

   @Override
   public void addProgressMonitor(ProgressMonitor<? super T> monitor) {
      monitors.add(monitor);
   }

   @Override
   public boolean removeProgressMonitor(ProgressMonitor<? super T> monitor) {
      return monitors.remove(monitor);
   }

   public void fireProgressUpdated(T update) {
      for (ProgressMonitor<? super T> monitor : monitors) {
         monitor.onProgressChange(this, update);
      }
   }

   public void fireProgressComplete() {
      for (ProgressMonitor<? super T> monitor : monitors) {
         monitor.onProgressComplete(this);
      }
   }

   @Override
   public boolean isComplete() {
      return comparator.compare(progress(), complete()) >= 0;
   }

   @SuppressWarnings("null")
   @Override
   public T complete() {
      return complete;
   }
}

