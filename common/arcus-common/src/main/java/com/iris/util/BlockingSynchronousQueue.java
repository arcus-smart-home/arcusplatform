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
package com.iris.util;

import java.util.concurrent.SynchronousQueue;

/**
 * Overrides the non-blocking offer method to block so that things submitting work to the
 * executor block until there are threads available to run it.
 * 
 * Left at package scope since it generally breaks the contract of BlockingQueue.
 */
class BlockingSynchronousQueue extends SynchronousQueue<Runnable> {
   
   /* (non-Javadoc)
    * @see java.util.concurrent.SynchronousQueue#offer(java.lang.Object)
    */
   @Override
   public boolean offer(Runnable r) {
      try {
         put(r);
         return true;
      }
      catch (InterruptedException e) {
         return false;
      }
   }
   
}

