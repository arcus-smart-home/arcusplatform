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
package com.iris.common.rule.event;

/**
 * Allows a scheduled event to be cancelled.
 */
public interface ScheduledEventHandle {
   /**
    * Indicates the event is waiting to fire. This
    * will be false once it is cancelled or fired.
    * @return
    */
   boolean isPending();
  
   /**
    * Cancels the event from firing. This will
    * return true if the event is cancelled by this
    * call.  If it has already fired or been cancelled
    * previously this will return false.
    * @return
    */
   boolean cancel();
   
   /**
    * Returns true if the event passed in is the
    * instance this handled refers to. Useful for
    * tracking when a specific scheduled event
    * is fired.
    * @param event
    * @return
    */
   boolean isReferencedEvent(RuleEvent event);
}

