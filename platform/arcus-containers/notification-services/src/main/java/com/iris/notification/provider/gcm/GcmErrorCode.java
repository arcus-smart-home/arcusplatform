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

public enum GcmErrorCode {

    /**
     * Message not formatted as valid Google Cloud Service JSON object; program error; should never happen.
     */
    INVALID_JSON(true),

    /**
     * Bogus registration id (application identifier); Iris system error; should never happen.
     */
    BAD_REGISTRATION(true),

    /**
     * Application was uninstalled from device or Google refreshed device ids; need to fetch new id from client.
     */
    DEVICE_UNREGISTERED(true),

    /**
     * Tried to send a malformed acknowledgement message; this system doesn't send ACKs so this should never occur.
     */
    BAD_ACK(true),

    /**
     * Google Cloud Services are down; retry later
     */
    SERVICE_UNAVAILABLE(false),

    /**
     * Internal Google error; retry later
     */
    INTERNAL_SERVER_ERROR(false),

    /**
     * Too many messages are being sent to the device; retry later
     */
    DEVICE_MESSAGE_RATE_EXCEEDED(false),

    /**
     * Google is rebalancing connections; stop using current connection and open a new one.
     */
    CONNECTION_DRAINING(false);

    public final boolean isTerminalError;

    private GcmErrorCode(boolean isTerminalError) {
        this.isTerminalError = isTerminalError;
    }

    public static boolean isTerminalErrorCode(String error) {
        return GcmErrorCode.valueOf(error.toUpperCase()).isTerminalError;
    }
}

