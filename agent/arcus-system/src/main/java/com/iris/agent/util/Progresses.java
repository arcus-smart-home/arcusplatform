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

public final class Progresses {
   public static AbstractProgress<Integer> create(int max) {
      return new IntegerProgress(max, new ComparableComparator<Integer>());
   }

   public static AbstractProgress<Long> create(long max) {
      return new LongProgress(max, new ComparableComparator<Long>());
   }

   public static AbstractProgress<Double> create(double max) {
      return new DoubleProgress(max, new ComparableComparator<Double>());
   }

   private static final class IntegerProgress extends AbstractProgress<Integer> {
      private int progress;

      private IntegerProgress(int max, Comparator<Integer> comparator) {
         super(max, comparator);
         this.progress = 0;
      }

      @Override
      public void fireProgressUpdated(Integer update) {
         this.progress += update;
         if (this.progress > complete()) {
            this.progress = complete();
         }

         super.fireProgressUpdated(update);
      }

      @Override
      public void fireProgressComplete() {
         this.progress = complete();
         super.fireProgressComplete();
      }

      @Override
      public Integer progress() {
         return progress;
      }
   }

   private static final class LongProgress extends AbstractProgress<Long> {
      private long progress;

      private LongProgress(long max, Comparator<Long> comparator) {
         super(max, comparator);
         this.progress = 0;
      }

      @Override
      public void fireProgressUpdated(Long update) {
         this.progress += update;
         if (this.progress > complete()) {
            this.progress = complete();
         }

         super.fireProgressUpdated(update);
      }

      @Override
      public void fireProgressComplete() {
         this.progress = complete();
         super.fireProgressComplete();
      }

      @Override
      public Long progress() {
         return progress;
      }
   }

   private static final class DoubleProgress extends AbstractProgress<Double> {
      private double progress;

      private DoubleProgress(double max, Comparator<Double> comparator) {
         super(max, comparator);
         this.progress = 0.0;
      }

      @Override
      public void fireProgressUpdated(Double update) {
         this.progress += update;
         if (this.progress > complete()) {
            this.progress = complete();
         }

         super.fireProgressUpdated(update);
      }

      @Override
      public void fireProgressComplete() {
         this.progress = complete();
         super.fireProgressComplete();
      }

      @Override
      public Double progress() {
         return progress;
      }
   }
}

