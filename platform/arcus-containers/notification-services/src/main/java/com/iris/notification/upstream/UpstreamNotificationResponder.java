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
package com.iris.notification.upstream;

import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;

/**
 * Responds to upstream notifications; that is, messages that originate from notification providers typically as a result of an
 * error, a device that has been unregistered (app uninstalled) or confirmation that the notification message was delivered to the
 * requested device.
 *
 */
public interface UpstreamNotificationResponder {

    /**
     * Responds to an error generated in the process of sending/delivering a notification message to a client.
     *
     * @param notification
     *            The notification object representing the message being delivered.
     * @param isRecoverable
     *            True to indicate that the system may retry sending this message later; false if the message should not be
     *            retried.
     * @param message
     *            The error message; format and contents specific to each error condition.
     */
    public void handleError(Notification notification, boolean isRecoverable, String message);

    public void handleHandOff(Notification notification);

    /**
     * Responds to a delivery receipt; indication that the provided notification was successfully delivered to the device.
     *
     * @param notification
     *            The notification message object that was successfully delivered.
     */
    public void handleDeliveryReceipt(Notification notification);

    /**
     * Responds to a notice that the given device identifier/token is no longer registered with the provider gateway. This error
     * indicates that the customer uninstalled the Iris app, or that the provider has refreshed their id/token. In either case,
     * the notification service must stop using the expired id/token and begin using a new one.
     *
     * @param method
     *            The NotificationMethod associated with this device identifier (i.e., APNS or GCM).
     * @param deviceIdentifier
     *            The expired device identifier/token that should no longer be used.
     */
    public void handleDeviceUnregistered(NotificationMethod method, String deviceIdentifier);

}

