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
package com.iris.notification.provider.apns;

public enum ApnsErrorCode {

    /**
     * No error, no problem. Why is this defined as an error code in the API?
     */
    NO_ERROR(true),

    /**
     * General error occurred sending push notification.
     */
    PROCESSING_ERROR(false),

    /**
     * Message accepted, but connection is being closed. Create a new connection for future messages.
     */
    SHUTDOWN(false),

    /**
     * Unknown error occurred; retry later
     */
    UNKNOWN(false),

    /**
     * Token wasn't specified in the request; program error; should never occur
     */
    MISSING_TOKEN(true),

    /**
     * No app bundle id (bad certificate?); program error; should never occur
     */
    MISSING_TOPIC(true),

    /**
     * No message payload specified; program error; should never occur
     */
    MISSING_PAYLOAD(true),

    /**
     * Bogus token; Iris system error; should never occur
     */
    INVALID_TOKEN_SIZE(true),

    /**
     * Bogus bundle id; should never occur
     */
    INVALID_TOPIC_SIZE(true),

    /**
     * Notification message is too large; Iris system error
     */
    INVALID_PAYLOAD_SIZE(true),

    /**
     * Bogus or unknown token; Iris system error; should never occur
     */
    INVALID_TOKEN(true);

    public final boolean isTerminalError;

    private ApnsErrorCode(boolean isTerminalError) {
        this.isTerminalError = isTerminalError;
    }

    public static boolean isTerminalError(String error) {
        return ApnsErrorCode.valueOf(error.toUpperCase()).isTerminalError;
    }
}

