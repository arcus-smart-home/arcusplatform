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
 * @author paul@couto-software.com
 *
 */

package com.iris.agent.upnp;

public interface IrisUpnpListener {
   void deviceAdded(String deviceType, String manufacturer, String model, String host, String uuid);
   void deviceRemoved(String deviceType, String manufacturer, String model, String host, String uuid);
   void deviceUpdated(String deviceType, String manufacturer, String model, String host, String uuid);
}

