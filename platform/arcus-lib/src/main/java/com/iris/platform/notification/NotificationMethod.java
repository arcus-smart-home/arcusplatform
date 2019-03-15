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
package com.iris.platform.notification;

import static com.iris.platform.notification.NotificationFormat.HTML;
import static com.iris.platform.notification.NotificationFormat.JSON;
import static com.iris.platform.notification.NotificationFormat.TEXT;

public enum NotificationMethod {
    IVR      (TEXT),
    EMAIL    (HTML),
    PUSH     (JSON),
    GCM      (JSON),
    APNS     (JSON),
    WEBHOOK  (TEXT),
    SMS      (TEXT),
    LOG      (TEXT);
    
    private final NotificationFormat format;
    
    NotificationMethod(NotificationFormat format) {
   	 this.format = format;
    }
    
    public NotificationFormat format() { return this.format; }
}

