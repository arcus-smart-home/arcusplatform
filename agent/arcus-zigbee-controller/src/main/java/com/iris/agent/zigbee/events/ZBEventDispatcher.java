/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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

package com.iris.agent.zigbee.events;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ZBEventDispatcher {

   public static final ZBEventDispatcher INSTANCE = new ZBEventDispatcher();

   private ZBEventDispatcher() {}

   private final Set<ZBEventListener> listeners = new CopyOnWriteArraySet<>();

   public void dispatch(final ZBEvent event) {
      listeners.forEach(l -> l.onZBEvent(event));
   }

   public void register(final ZBEventListener listener) {
      listeners.add(listener);
   }

   public void unregister(final ZBEventListener listener) {
      listeners.remove(listener);
   }
}
