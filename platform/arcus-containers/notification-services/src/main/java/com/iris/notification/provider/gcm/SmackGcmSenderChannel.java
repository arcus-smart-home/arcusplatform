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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.notification.NotificationService;
import com.iris.notification.upstream.UpstreamNotificationResponder;
import com.iris.platform.notification.Notification;

/**
 * Smack XMPP-implementation of a Google Cloud Services server. Adapted from Google's example server software published at:
 * https://developer.android.com/google/gcm/ccs.html#smack.
 */
public class SmackGcmSenderChannel implements PingFailedListener {

    private static final IrisMetricSet METRICS = IrisMetrics.metrics(NotificationService.SERVICE_NAME);
    private static final Logger logger = LoggerFactory.getLogger(SmackGcmSenderChannel.class);

    private XMPPConnection connection;
    private PingManager pingManager;
    private final UpstreamNotificationResponder errorResponder;
    private final SmackGcmUpstreamMessageListener upstreamListener;
    private boolean channelClosed = false;

    private final Counter pingFailedCounter = METRICS.counter("gcm.smack.ping.failed.count");

    public SmackGcmSenderChannel(String googleApiKey, Long googleSenderId, int keepAliveInterval, UpstreamNotificationResponder errorResponder) {
        this.errorResponder = errorResponder;
        upstreamListener = new SmackGcmUpstreamMessageListener(errorResponder);

        try {
            ProviderManager.addExtensionProvider(GcmServiceConstants.GCM_ELEMENT_NAME, GcmServiceConstants.GCM_NAMESPACE, new SmackGcmPacketExtensionProvider());
            connect(googleSenderId, googleApiKey, keepAliveInterval);

        } catch (XMPPException | IOException | SmackException e) {
            // Nothing we can do about these...
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(Notification notification, String toDevice, Map<String, Object> payload) {

        if (upstreamListener.isConnectionDraining()) {
            throw new IllegalStateException("Cannot send message on this channel; connection is draining and not accepting any more messages.");
        }

        String messageId = notification.toJsonString();
        String message = SmackGcmSenderChannel.createJsonMessage(toDevice, messageId, payload);

        try {
            connection.sendPacket(new SmackGcmPacketExtension(message).toPacket());
        } catch (NotConnectedException e) {
            errorResponder.handleError(notification, true, e.toString());
            pingManager.unregisterPingFailedListener(this);
            channelClosed = true;
        }
    }

    public boolean hasClosed() {
        return channelClosed || upstreamListener.isConnectionDraining();
    }

    public void close() {
        try {
            pingManager.unregisterPingFailedListener(this);
            connection.disconnect();
        } catch (NotConnectedException e) {
            // Nothing to do
        }
    }

    /**
     * Creates a JSON encoded GCM message.
     *
     * @param to
     *            RegistrationId of the target device (Required).
     * @param messageId
     *            Unique messageId for which CCS sends an "ack/nack" (Required).
     * @param payload
     *            Message content intended for the application. (Optional).
     * @return JSON encoded GCM message.
     */
    protected static String createJsonMessage(String to, String messageId, Map<String, Object> payload) {
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("to", to);
        message.put("message_id", messageId);
        message.put("delivery_receipt_requested", true);
        for (String key : payload.keySet()) {
        	message.put(key, payload.get(key));
        }

        return JSONValue.toJSONString(message);
    }

    /**
     * Connects to GCM Cloud Connection Server using the supplied credentials.
     *
     * @param senderId
     *            Your GCM project number
     * @param apiKey
     *            API Key of your project
     */
    protected void connect(long senderId, String apiKey, int keepAliveInterval) throws XMPPException, IOException, SmackException {

        // Configure connection
        ConnectionConfiguration config = new ConnectionConfiguration(GcmServiceConstants.GCM_SERVER, GcmServiceConstants.GCM_PORT);
        config.setSecurityMode(SecurityMode.enabled);
        config.setReconnectionAllowed(true);
        config.setRosterLoadedAtLogin(false);
        config.setSendPresence(false);
        config.setSocketFactory(SSLSocketFactory.getDefault());

        // Create connection object and initiate connection
        connection = new XMPPTCPConnection(config);
        pingManager = PingManager.getInstanceFor(connection);
        pingManager.setPingInterval(keepAliveInterval);
        pingManager.registerPingFailedListener(this);
        connection.connect();

        // Register listener to log connection state events
        connection.addConnectionListener(new SmackLoggingConnectionListener());

        // Handle incoming messages (delivery receipts and Google control messages)
        connection.addPacketListener(upstreamListener, new PacketTypeFilter(Message.class));

        // Log in...
        connection.login(senderId + "@" + GcmServiceConstants.GCM_SERVER, apiKey);
    }

    @Override
    public void pingFailed() {
       pingFailedCounter.inc();
    }
}

