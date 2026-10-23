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
package com.iris.notification.provider.gcm;

import java.util.Map;

import com.iris.platform.notification.Notification;

/**
 * The seam for Android push.
 *
 * The only implementation used to be SmackGcmSender, which spoke GCM's XMPP
 * protocol to gcm.googleapis.com:5235. Google shut that endpoint down in 2024,
 * so it was removed and NoopGcmSender is all that is left.
 *
 * TODO: an FCM implementation goes here. FCM HTTP v1 is a plain HTTPS POST to
 * fcm.googleapis.com/v1/projects/{project}/messages:send, authenticated with an
 * OAuth2 token minted from a service-account key rather than the old
 * gcm.apikey/gcm.senderid pair. Its errors (UNREGISTERED, INVALID_ARGUMENT,
 * QUOTA_EXCEEDED, UNAVAILABLE) do not match the XMPP error set the old code
 * mapped, so the retry and device-unregistration handling in GCMProvider wants
 * rechecking against them rather than assuming the old behaviour carries over.
 */
public interface GcmSender {
    public void sendMessage(Notification notification, String toDevice, Map<String, Object> payload);
}

