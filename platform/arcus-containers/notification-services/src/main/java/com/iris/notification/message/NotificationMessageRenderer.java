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
package com.iris.notification.message;

import java.util.Map;

import com.iris.messages.model.BaseEntity;
import com.iris.messages.model.Person;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;

public interface NotificationMessageRenderer {

    public String renderMessage(Notification notification, NotificationMethod method, Person recipient, Map<String, BaseEntity<?, ?>> additionalEntityParams);

    public Map<String, String> renderMultipartMessage(Notification notification, NotificationMethod method, Person recipient, Map<String, BaseEntity<?, ?>> additionalEntityParams);
}

