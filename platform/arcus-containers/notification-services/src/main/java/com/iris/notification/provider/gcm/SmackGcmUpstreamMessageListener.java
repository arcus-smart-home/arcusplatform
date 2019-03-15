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

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.notification.upstream.UpstreamNotificationResponder;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;

public class SmackGcmUpstreamMessageListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(SmackGcmUpstreamMessageListener.class);

    private final UpstreamNotificationResponder upstreamResponder;
    protected volatile boolean connectionDraining = false; // indicates connection is shutting down; no more connections

    public SmackGcmUpstreamMessageListener(UpstreamNotificationResponder errorResponder) {
        upstreamResponder = errorResponder;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void processPacket(Packet packet) throws NotConnectedException {
        Message incomingMessage = (Message) packet;
        SmackGcmPacketExtension gcmPacket = (SmackGcmPacketExtension) incomingMessage.getExtension(GcmServiceConstants.GCM_NAMESPACE);
        String json = gcmPacket.getJson();
        try {

            Map<String, Object> jsonObject = (Map<String, Object>) JSONValue.parseWithException(json);
            Object messageType = jsonObject.get("message_type");

            if ("ack".equals(messageType.toString())) {
                handleAckReceipt(jsonObject);
            } else if ("nack".equals(messageType.toString())) {
                handleNackReceipt(jsonObject);
            } else if ("control".equals(messageType.toString())) {
                handleControlMessage(jsonObject);
            } else if ("receipt".equals(messageType.toString())) {
                handleDeliveryReceipt(jsonObject);
            } else {
                logger.warn("Unrecognized message type " + messageType.toString());
            }

        } catch (ParseException e1) {
            logger.error("Error parsing JSON " + json, e1);
        } catch (Exception e2) {
            logger.error("Failed to process packet", e2);
        }
    }

    public boolean isConnectionDraining () {
        return connectionDraining;
    }

    protected void handleDeliveryReceipt(Map<String, Object> jsonObject) {
        String messageId = (String) jsonObject.get("message_id");

        // Delivery receipts should contain the outgoing message id (i.e., the one being confirmed) prefixed with the string
        // 'dr2:'
        if (!messageId.startsWith("dr2:")) {
            logger.error("Delivery receipt message from Google is malformed; message is not prefixed with 'dr2:' as it should be: " + messageId);
        } else {
            messageId = messageId.substring(4); // Strip 'dr2:' prefix from message id
            upstreamResponder.handleDeliveryReceipt(Notification.fromJsonString(messageId));
        }

    }

    protected void handleAckReceipt(Map<String, Object> jsonObject) {
        String messageId = (String) jsonObject.get("message_id");
        upstreamResponder.handleHandOff(Notification.fromJsonString(messageId));
    }

    protected void handleNackReceipt(Map<String, Object> jsonObject) {
        String messageId = (String) jsonObject.get("message_id");
        String error = (String) jsonObject.get("error");

        GcmErrorCode errorCode = GcmErrorCode.valueOf(error);
        Notification notification = Notification.fromJsonString(messageId);

        // Audit the error response
        logger.info("Failed to deliver GCM message due to " + error + " message:" + notification.toString());
        upstreamResponder.handleError(notification, !errorCode.isTerminalError, error);

        // If the error indicates that the device unregistered, then take appropriate action
        if (errorCode == GcmErrorCode.DEVICE_UNREGISTERED) {
            String from = (String) jsonObject.get("from");
            upstreamResponder.handleDeviceUnregistered(NotificationMethod.GCM, from);
        }
    }

    protected void handleControlMessage(Map<String, Object> jsonObject) {
        String controlType = (String) jsonObject.get("control_type");

        if (GcmErrorCode.CONNECTION_DRAINING.toString().equals(controlType)) {
            connectionDraining = true;
        }
    }
}

