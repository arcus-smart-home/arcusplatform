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

public class ViewModelEvent {

   public static ViewModelAddedEvent added(int index) {
      return added(index, 1);
   }

   public static ViewModelAddedEvent added(int index, int count) {
      return new ViewModelAddedEvent(index, index + count - 1);
   }

   public static ViewModelUpdatedEvent updated(int index) {
      return updated(index, 1);
   }

   public static ViewModelUpdatedEvent updated(int index, int count) {
      return new ViewModelUpdatedEvent(index, index + count - 1);
   }

   public static ViewModelRemovedEvent removed(int index) {
      return removed(index, 1);
   }

   public static ViewModelRemovedEvent removed(int index, int count) {
      return new ViewModelRemovedEvent(index, index + count - 1);
   }

   public static ViewModelChangedEvent changed() {
      return new ViewModelChangedEvent();
   }

   private int start;
   private int end;

   private ViewModelEvent(int start, int end) {
      this.start = start;
      this.end = end;
   }

   public int getStart() {
      return start;
   }
   public int getEnd() {
      return end;
   }

   public static class ViewModelAddedEvent extends ViewModelEvent {
      private ViewModelAddedEvent(int start, int end) {
         super(start, end);
      }
   }

   public static class ViewModelUpdatedEvent extends ViewModelEvent {
      private ViewModelUpdatedEvent(int start, int end) {
         super(start, end);
      }
   }

   public static class ViewModelRemovedEvent extends ViewModelEvent {
      private ViewModelRemovedEvent(int start, int end) {
         super(start, end);
      }
   }

   public static class ViewModelChangedEvent extends ViewModelEvent {
      private ViewModelChangedEvent() {
         super(0, Integer.MAX_VALUE);
      }
   }

}

