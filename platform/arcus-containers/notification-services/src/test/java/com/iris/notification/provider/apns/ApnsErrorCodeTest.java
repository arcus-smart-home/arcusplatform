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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ApnsErrorCodeTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldntAcceptBogusErrorCode() throws Exception {
        assertTrue(ApnsErrorCode.isTerminalError("bogus"));
    }

    @Test
    public void shouldReturnTerminalErrorIndicator() throws Exception {
        assertTrue(ApnsErrorCode.isTerminalError("no_error"));
        assertFalse(ApnsErrorCode.isTerminalError("processing_error"));
        assertFalse(ApnsErrorCode.isTerminalError("shutdown"));
        assertFalse(ApnsErrorCode.isTerminalError("unknown"));
        assertTrue(ApnsErrorCode.isTerminalError("missing_token"));
        assertTrue(ApnsErrorCode.isTerminalError("missing_topic"));
        assertTrue(ApnsErrorCode.isTerminalError("missing_payload"));
        assertTrue(ApnsErrorCode.isTerminalError("invalid_token_size"));
        assertTrue(ApnsErrorCode.isTerminalError("invalid_topic_size"));
        assertTrue(ApnsErrorCode.isTerminalError("invalid_payload_size"));
        assertTrue(ApnsErrorCode.isTerminalError("invalid_token"));
    }

}

