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
package com.iris.agent.zwave.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.hal.IrisHal;
import com.iris.agent.zwave.ZWMsg;
import com.iris.agent.zwave.client.ZWClient;
import com.iris.agent.zwave.code.ZCmd;

import static com.iris.agent.zwave.engine.ZWaveSerialConstants.*;

/**
 * Z-Wave Serial API engine implementation.
 *
 * Communicates with the Z-Wave controller chip over UART using the
 * Z-Wave Serial API protocol (SOF/ACK/NAK/CAN framing).
 *
 * This engine handles:
 * - Bootstrap (open port, get home ID, get initial node list)
 * - Sending commands to nodes via FUNC_ID_ZW_SEND_DATA
 * - Receiving unsolicited callbacks (APPLICATION_COMMAND_HANDLER)
 * - Inclusion/exclusion mode
 * - Node info queries
 */
public class ZWaveSerialEngine implements ZWaveEngine {
   private static final Logger logger = LoggerFactory.getLogger(ZWaveSerialEngine.class);

   private final String portPath;
   private final Set<EngineListener> listeners = new CopyOnWriteArraySet<>();
   private final Map<Integer, ZWaveSerialClient> clients = new ConcurrentHashMap<>();
   private final Map<Integer, ZWaveEngineNodeInfo> nodeInfoCache = new ConcurrentHashMap<>();
   private final AtomicInteger callbackIdCounter = new AtomicInteger(1);
   private final AtomicReference<BlockingQueue<Object>> syncChannel = new AtomicReference<>();
   private final ReentrantLock sendLock = new ReentrantLock();

   private ZWaveSerialPort serialPort;
   private Thread readerThread;
   private volatile boolean running = false;

   // Network state
   private long homeId;
   private int controllerNodeId = 1;
   private String libraryVersion = "";
   private String libraryType = "";
   private boolean primaryController = true;
   private boolean staticController = false;
   private boolean bridgeController = false;
   private int sucNodeId = 0;

   // Node capabilities bitmap from SERIAL_API_GET_INIT_DATA
   private final boolean[] nodePresent = new boolean[232];

   public ZWaveSerialEngine(String portPath) {
      this.portPath = portPath;
   }

   @Override
   public void bootstrap() {
      logger.info("Bootstrapping Z-Wave serial engine on port {}", portPath);
      try {
         serialPort = new ZWaveSerialPort(portPath);
         serialPort.open();
         running = true;

         // Hardware reset the Z-Wave chip via GPIO to guarantee clean state
         if (IrisHal.resetZWaveChip()) {
            logger.info("Z-Wave chip hardware reset complete");
         } else {
            logger.warn("Z-Wave GPIO reset unavailable, falling back to serial soft reset");
            serialPort.writeByte(CAN);
            drainUntilQuiet();
            serialPort.write(ZWaveSerialFrame.request(FUNC_ID_SERIAL_API_SOFT_RESET).toBytes());
            Thread.sleep(1500);
         }
         drainUntilQuiet();

         // Bootstrap reads directly from serial port (no reader thread)
         performBootstrap();

         // Start reader thread only after bootstrap succeeds
         readerThread = new Thread(this::readerLoop, "zwave-serial-reader");
         readerThread.setDaemon(true);
         readerThread.start();
      } catch (Exception e) {
         logger.error("Failed to bootstrap Z-Wave serial engine", e);
         running = false;
         notifyBootstrapFailure();
      }
   }

   /**
    * Drain all pending controller frames until the line is quiet for 1 second.
    * ACKs any data frames so the controller releases its queue.
    */
   private void drainUntilQuiet() throws InterruptedException {
      logger.info("Draining pending Z-Wave controller frames...");
      int drained = 0;
      long lastActivity = System.currentTimeMillis();
      while (System.currentTimeMillis() - lastActivity < 1000) {
         Object msg = serialPort.poll(100, TimeUnit.MILLISECONDS);
         if (msg != null) {
            if (msg instanceof ZWaveSerialFrame) {
               serialPort.sendAck();
            }
            drained++;
            lastActivity = System.currentTimeMillis();
         }
      }
      logger.info("Startup drain complete, drained {} messages", drained);
   }

   private void performBootstrap() {
      try {
         // Step 1: Get memory/home ID
         ZWaveSerialFrame resp = sendAndWaitForResponse(
            ZWaveSerialFrame.request(FUNC_ID_ZW_MEMORY_GET_ID));
         if (resp != null && resp.getDataLength() >= 5) {
            homeId = ((0xFFL & resp.getDataByte(0)) << 24) |
                     ((0xFFL & resp.getDataByte(1)) << 16) |
                     ((0xFFL & resp.getDataByte(2)) << 8) |
                     (0xFFL & resp.getDataByte(3));
            controllerNodeId = 0xFF & resp.getDataByte(4);
            logger.info("Z-Wave Home ID: 0x{}, Controller Node: {}", String.format("%08X", homeId), controllerNodeId);
         } else {
            logger.error("Failed to get Z-Wave memory ID");
            notifyBootstrapFailure();
            return;
         }

         // Step 2: Get version
         resp = sendAndWaitForResponse(
            ZWaveSerialFrame.request(FUNC_ID_ZW_GET_VERSION));
         if (resp != null && resp.getDataLength() >= 12) {
            byte[] verBytes = new byte[11];
            for (int i = 0; i < 11; i++) {
               verBytes[i] = resp.getDataByte(i);
            }
            libraryVersion = new String(verBytes).trim();
            libraryType = String.valueOf(0xFF & resp.getDataByte(11));
            logger.info("Z-Wave Library: {} (type {})", libraryVersion, libraryType);
         }

         // Step 3: Get SUC node ID
         resp = sendAndWaitForResponse(
            ZWaveSerialFrame.request(FUNC_ID_ZW_GET_SUC_NODE_ID));
         if (resp != null && resp.getDataLength() >= 1) {
            sucNodeId = 0xFF & resp.getDataByte(0);
            logger.info("Z-Wave SUC Node ID: {}", sucNodeId);
         }

         // Step 4: Get controller capabilities
         resp = sendAndWaitForResponse(
            ZWaveSerialFrame.request(FUNC_ID_ZW_GET_CONTROLLER_CAPABILITIES));
         if (resp != null && resp.getDataLength() >= 1) {
            int caps = 0xFF & resp.getDataByte(0);
            primaryController = (caps & 0x04) == 0;
            staticController = (caps & 0x08) != 0;
            bridgeController = (caps & 0x80) != 0;
            logger.info("Z-Wave Controller - primary: {}, static: {}, bridge: {}",
               primaryController, staticController, bridgeController);
         }

         // Step 5: Get initial node list
         resp = sendAndWaitForResponse(
            ZWaveSerialFrame.request(FUNC_ID_SERIAL_API_GET_INIT_DATA));
         if (resp != null && resp.getDataLength() >= 34) {
            // Bytes [3..31] contain 29-byte node bitmask
            int offset = 3;
            for (int i = 0; i < NODE_BITMASK_LENGTH && (offset + i) < resp.getDataLength(); i++) {
               int bitmask = 0xFF & resp.getDataByte(offset + i);
               for (int bit = 0; bit < 8; bit++) {
                  int nodeId = (i * 8) + bit + 1;
                  if (nodeId <= 232 && (bitmask & (1 << bit)) != 0) {
                     nodePresent[nodeId - 1] = true;
                  }
               }
            }
            List<Integer> foundNodes = new ArrayList<>();
            for (int i = 0; i < 232; i++) {
               if (nodePresent[i]) {
                  foundNodes.add(i + 1);
               }
            }
            logger.info("Z-Wave nodes on network: {}", foundNodes);
         }

         // Bootstrap complete
         notifyBootstrapSuccess();

      } catch (Exception e) {
         logger.error("Error during Z-Wave bootstrap", e);
         notifyBootstrapFailure();
      }
   }

   /**
    * Send a frame and wait for the response frame (synchronous).
    * Handles ACK/NAK/CAN and retries.
    */
   private ZWaveSerialFrame sendAndWaitForResponse(ZWaveSerialFrame frame) {
      sendLock.lock();
      try {
         return sendAndWaitForResponseImpl(frame);
      } finally {
         sendLock.unlock();
      }
   }

   private ZWaveSerialFrame sendAndWaitForResponseImpl(ZWaveSerialFrame frame) {
      boolean useReaderThread = readerThread != null && readerThread.isAlive();
      LinkedBlockingQueue<Object> ch = null;
      if (useReaderThread) {
         ch = new LinkedBlockingQueue<>();
         syncChannel.set(ch);
      }
      try {
         int retries = 3;
         while (retries-- > 0) {
            // Drain any pending unsolicited data before sending
            drainPending(ch);

            if (logger.isDebugEnabled()) {
               byte[] rawBytes = frame.toBytes();
               StringBuilder hex = new StringBuilder();
               for (byte b : rawBytes) {
                  hex.append(String.format("%02X ", 0xFF & b));
               }
               logger.debug("TX: {}", hex.toString().trim());
            }
            serialPort.write(frame.toBytes());

            try {
               Object ack = poll(ch, ACK_TIMEOUT_MS);
               if (ack instanceof Byte) {
                  byte b = (Byte) ack;
                  if (b == ACK) {
                     Object resp = poll(ch, RESPONSE_TIMEOUT_MS);
                     if (resp instanceof ZWaveSerialFrame) {
                        ZWaveSerialFrame respFrame = (ZWaveSerialFrame) resp;
                        // Reader already ACK'd if via syncChannel
                        if (ch == null) {
                           serialPort.sendAck();
                        }
                        return respFrame;
                     } else {
                        logger.warn("Expected response frame, got: {}", resp);
                     }
                  } else if (b == NAK) {
                     logger.warn("NAK received, retrying ({} left)", retries);
                     continue;
                  } else if (b == CAN) {
                     logger.warn("CAN received, retrying ({} left)", retries);
                     drainAfterCan(ch);
                     continue;
                  }
               } else if (ack == null) {
                  logger.warn("Timeout waiting for ACK, retrying ({} left)", retries);
               } else if (ack instanceof ZWaveSerialFrame) {
                  // Unsolicited frame; reader already ACK'd if via syncChannel
                  if (ch == null) {
                     serialPort.sendAck();
                  }
                  handleUnsolicitedFrame((ZWaveSerialFrame) ack);
                  retries++;
               }
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               return null;
            }
         }
         logger.error("Failed to get response after retries for {}", frame);
         return null;
      } finally {
         syncChannel.set(null);
      }
   }

   /**
    * Send a frame, wait for ACK only (no response expected).
    */
   private boolean sendAndWaitForAck(ZWaveSerialFrame frame) {
      sendLock.lock();
      try {
         return sendAndWaitForAckImpl(frame);
      } finally {
         sendLock.unlock();
      }
   }

   private boolean sendAndWaitForAckImpl(ZWaveSerialFrame frame) {
      boolean useReaderThread = readerThread != null && readerThread.isAlive();
      LinkedBlockingQueue<Object> ch = null;
      if (useReaderThread) {
         ch = new LinkedBlockingQueue<>();
         syncChannel.set(ch);
      }
      try {
         int retries = 3;
         while (retries-- > 0) {
            drainPending(ch);
            serialPort.write(frame.toBytes());
            try {
               Object ack = poll(ch, ACK_TIMEOUT_MS);
               if (ack instanceof Byte) {
                  byte b = (Byte) ack;
                  if (b == ACK) {
                     return true;
                  } else if (b == CAN) {
                     logger.warn("Got CAN waiting for ACK, retrying ({} left)", retries);
                     drainAfterCan(ch);
                     continue;
                  } else if (b == NAK) {
                     logger.warn("Got NAK waiting for ACK, retrying ({} left)", retries);
                     continue;
                  }
               } else if (ack == null) {
                  logger.warn("Timeout waiting for ACK, retrying ({} left)", retries);
               }
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               return false;
            }
         }
         return false;
      } finally {
         syncChannel.set(null);
      }
   }

   /**
    * Poll from either the sync channel (when reader thread is active)
    * or directly from the serial port (during bootstrap).
    */
   private Object poll(BlockingQueue<Object> ch, long timeoutMs) throws InterruptedException {
      if (ch != null) {
         return ch.poll(timeoutMs, TimeUnit.MILLISECONDS);
      }
      return serialPort.poll(timeoutMs, TimeUnit.MILLISECONDS);
   }

   /**
    * Drain any pending messages before sending to avoid collisions
    * with unsolicited controller frames.
    */
   private void drainPending(BlockingQueue<Object> ch) {
      try {
         // Wait briefly for any in-flight controller data (e.g. SEND_DATA
         // callbacks from previous RF transmissions) to arrive
         Object stale;
         while ((stale = poll(ch, 50)) != null) {
            if (stale instanceof ZWaveSerialFrame) {
               // Reader already ACK'd; just handle it
               handleUnsolicitedFrame((ZWaveSerialFrame) stale);
            }
         }
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
   }

   /**
    * After receiving CAN, wait 150ms per Z-Wave spec, then receive and ACK
    * any pending controller frame to clear the collision before retrying.
    */
   private void drainAfterCan(BlockingQueue<Object> ch) {
      try {
         Thread.sleep(150);
         Object pending = poll(ch, 200);
         if (pending instanceof ZWaveSerialFrame) {
            // Reader already ACK'd if routed via syncChannel;
            // ACK here only if reading directly (bootstrap)
            if (ch == null) {
               serialPort.sendAck();
            }
            handleUnsolicitedFrame((ZWaveSerialFrame) pending);
         }
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
   }

   /**
    * Background reader thread that processes unsolicited inbound frames.
    * Routes messages to syncChannel when a synchronous send is active.
    */
   private void readerLoop() {
      logger.info("Z-Wave serial reader thread started");
      while (running) {
         try {
            Object msg = serialPort.poll(100, TimeUnit.MILLISECONDS);
            if (msg == null) {
               continue;
            }

            // Always ACK data frames immediately so the controller
            // doesn't retransmit and cause collisions
            if (msg instanceof ZWaveSerialFrame) {
               serialPort.sendAck();
            }

            // Route to sync channel if a synchronous send is waiting
            BlockingQueue<Object> ch = syncChannel.get();
            if (ch != null) {
               ch.offer(msg);
               continue;
            }

            // Handle unsolicited messages
            if (msg instanceof ZWaveSerialFrame) {
               handleUnsolicitedFrame((ZWaveSerialFrame) msg);
            } else if (msg instanceof Byte) {
               logger.debug("Received idle signal byte: 0x{}", String.format("%02X", 0xFF & (Byte) msg));
            }
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
         } catch (Exception e) {
            logger.error("Error in Z-Wave reader loop", e);
         }
      }
      logger.info("Z-Wave serial reader thread stopped");
   }

   /**
    * Handle an unsolicited frame from the Z-Wave controller.
    */
   private void handleUnsolicitedFrame(ZWaveSerialFrame frame) {
      if (!frame.isRequest()) {
         return; // Only process unsolicited requests (callbacks)
      }

      int funcId = 0xFF & frame.getFunctionId();
      switch (funcId) {
         case 0xFF & FUNC_ID_APPLICATION_COMMAND_HANDLER:
            handleApplicationCommand(frame);
            break;

         case 0xFF & FUNC_ID_ZW_SEND_DATA:
            handleSendDataCallback(frame);
            break;

         case 0xFF & FUNC_ID_ZW_ADD_NODE_TO_NETWORK:
            handleAddNodeCallback(frame);
            break;

         case 0xFF & FUNC_ID_ZW_REMOVE_NODE_FROM_NETWORK:
            handleRemoveNodeCallback(frame);
            break;

         default:
            logger.debug("Unhandled unsolicited frame: func=0x{}, data={} bytes",
               String.format("%02X", funcId), frame.getDataLength());
            break;
      }
   }

   /**
    * Handle APPLICATION_COMMAND_HANDLER callback.
    * Data format: status | nodeId | cmdLen | cmdClass | cmd | payload...
    */
   /**
    * Handle SEND_DATA callback.
    * Data format: callbackId | txStatus | ...routing info...
    * txStatus: 0x00=OK, 0x01=NO_ACK, 0x02=FAIL, 0x03=NOT_IDLE, 0x04=NOROUTE
    */
   private void handleSendDataCallback(ZWaveSerialFrame frame) {
      if (frame.getDataLength() < 2) {
         return;
      }
      int callbackId = 0xFF & frame.getDataByte(0);
      int txStatus = 0xFF & frame.getDataByte(1);
      if (txStatus != 0x00) {
         logger.warn("SEND_DATA callback {}: tx failed, status=0x{}",
            callbackId, String.format("%02X", txStatus));
      } else {
         logger.debug("SEND_DATA callback {}: tx OK", callbackId);
      }
   }

   private void handleApplicationCommand(ZWaveSerialFrame frame) {
      if (frame.getDataLength() < 4) {
         return;
      }
      int nodeId = 0xFF & frame.getDataByte(1);
      int cmdLen = 0xFF & frame.getDataByte(2);
      if (cmdLen <= 0 || frame.getDataLength() < 3 + cmdLen) {
         return;
      }
      byte[] payload = new byte[cmdLen];
      for (int i = 0; i < cmdLen; i++) {
         payload[i] = frame.getDataByte(3 + i);
      }

      ZWaveEngineMsg engineMsg = new ZWaveEngineMsg((int) homeId, nodeId, payload);
      notifyListeners(engineMsg);
   }

   /**
    * Handle add node to network callback.
    */
   private void handleAddNodeCallback(ZWaveSerialFrame frame) {
      if (frame.getDataLength() < 2) {
         return;
      }
      int status = 0xFF & frame.getDataByte(1);
      logger.debug("Add node callback status: 0x{}", String.format("%02X", status));

      // Notify listeners via engine message so the upper layers can process
      if (frame.getDataLength() > 2) {
         byte[] data = frame.getData();
         ZWaveEngineMsg msg = new ZWaveEngineMsg((int) homeId, 0, data);
         notifyListeners(msg);
      }
   }

   /**
    * Handle remove node from network callback.
    */
   private void handleRemoveNodeCallback(ZWaveSerialFrame frame) {
      if (frame.getDataLength() < 2) {
         return;
      }
      int status = 0xFF & frame.getDataByte(1);
      logger.debug("Remove node callback status: 0x{}", String.format("%02X", status));

      if (frame.getDataLength() > 2) {
         byte[] data = frame.getData();
         ZWaveEngineMsg msg = new ZWaveEngineMsg((int) homeId, 0, data);
         notifyListeners(msg);
      }
   }

   /**
    * Send a command to a specific node via FUNC_ID_ZW_SEND_DATA.
    * Frame: funcId(0x13) | nodeId | cmdLen | cmd... | txOptions | callbackId
    */
   void sendNodeCommand(int nodeId, byte[] cmdBytes, ZWMsg msg) {
      int callbackId = callbackIdCounter.getAndIncrement() & 0xFF;
      if (callbackId == 0) {
         callbackId = callbackIdCounter.getAndIncrement() & 0xFF;
      }

      byte[] data = new byte[cmdBytes.length + 4];
      data[0] = (byte) nodeId;
      data[1] = (byte) cmdBytes.length;
      System.arraycopy(cmdBytes, 0, data, 2, cmdBytes.length);
      data[2 + cmdBytes.length] = DEFAULT_TRANSMIT_OPTIONS;
      data[3 + cmdBytes.length] = (byte) callbackId;

      ZWaveSerialFrame frame = ZWaveSerialFrame.request(FUNC_ID_ZW_SEND_DATA, data);
      // SEND_DATA requires waiting for both ACK and RES frame
      ZWaveSerialFrame resp = sendAndWaitForResponse(frame);
      if (resp != null) {
         msg.finished();
      } else {
         msg.error(new Exception("Failed to send command to node " + nodeId));
      }
   }

   /**
    * Send a controller-level command (e.g., inclusion, node list, etc).
    */
   void sendControllerCommand(ZCmd cmd, ZWMsg msg) {
      byte[] cmdBytes = cmd.bytes();
      // Controller commands vary; send the raw bytes as a serial API frame
      // The first byte of the ZCmd is treated as the function ID for controller commands
      if (cmdBytes.length >= 2) {
         byte funcId = cmdBytes[0];
         byte[] data = new byte[cmdBytes.length - 1];
         System.arraycopy(cmdBytes, 1, data, 0, data.length);
         ZWaveSerialFrame frame = ZWaveSerialFrame.request(funcId, data);
         boolean sent = sendAndWaitForAck(frame);
         if (sent) {
            msg.finished();
         } else {
            msg.error(new Exception("Failed to send controller command"));
         }
      } else {
         msg.error(new Exception("Invalid controller command: too short"));
      }
   }

   // ========================
   // ZWaveEngine interface
   // ========================

   @Override
   public ZWClient getClient(int nodeId) {
      return clients.computeIfAbsent(nodeId, id -> new ZWaveSerialClient(id, this));
   }

   @Override
   public void addEngineListener(EngineListener listener) {
      listeners.add(listener);
   }

   @Override
   public void removeEngineListener(EngineListener listener) {
      listeners.remove(listener);
   }

   @Override
   public void shutdown() {
      logger.info("Shutting down Z-Wave serial engine");
      running = false;
      if (readerThread != null) {
         readerThread.interrupt();
         try {
            readerThread.join(5000);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
         }
      }
      if (serialPort != null) {
         serialPort.close();
      }
      clients.values().forEach(ZWaveSerialClient::shutdown);
      clients.clear();
      for (EngineListener l : listeners) {
         l.onNetworkShutdown();
      }
   }

   // ========================
   // Network Commands
   // ========================

   @Override
   public void hardReset(long homeId) {
      logger.warn("Performing Z-Wave hard reset (factory default)");
      ZWaveSerialFrame frame = ZWaveSerialFrame.request(FUNC_ID_ZW_SET_DEFAULT);
      sendAndWaitForAck(frame);
      for (EngineListener l : listeners) {
         l.onNetworkResetting();
      }
   }

   @Override
   public void softReset(long homeId) {
      logger.info("Performing Z-Wave soft reset");
      ZWaveSerialFrame frame = ZWaveSerialFrame.request(FUNC_ID_SERIAL_API_SOFT_RESET);
      serialPort.write(frame.toBytes());
      // Soft reset doesn't get ACK - chip resets immediately
   }

   @Override
   public void healNetworkNode(long homeId, int nodeId, boolean returnRouteInitialization) {
      logger.info("Healing network node {}", nodeId);
      ZWaveSerialFrame frame = ZWaveSerialFrame.request(
         FUNC_ID_ZW_REQUEST_NODE_NEIGHBOR_UPDATE,
         (byte) nodeId);
      sendAndWaitForAck(frame);
   }

   @Override
   public void healNetwork(long homeId, boolean returnRouteInitialization) {
      logger.info("Healing full Z-Wave network");
      for (int i = 0; i < 232; i++) {
         if (nodePresent[i] && (i + 1) != controllerNodeId) {
            healNetworkNode(homeId, i + 1, returnRouteInitialization);
         }
      }
   }

   @Override
   public int getControllerNodeId(long homeId) {
      return controllerNodeId;
   }

   @Override
   public int getSUCNodeId(long homeId) {
      return sucNodeId;
   }

   @Override
   public boolean isPrimaryController(long homeId) {
      return primaryController;
   }

   @Override
   public boolean isStaticController(long homeId) {
      return staticController;
   }

   @Override
   public boolean isBridgeControler(long homeId) {
      return bridgeController;
   }

   @Override
   public String getLibraryVersion(long homeId) {
      return libraryVersion;
   }

   @Override
   public String getLibraryType(long homeId) {
      return libraryType;
   }

   // ========================
   // Node Commands
   // ========================

   @Override
   public boolean refreshNodeInfo(long homeId, int nodeId) {
      ZWaveSerialFrame resp = sendAndWaitForResponse(
         ZWaveSerialFrame.request(FUNC_ID_ZW_REQUEST_NODE_INFO, (byte) nodeId));
      return resp != null;
   }

   @Override
   public boolean isNodeListeningDevice(long homeId, int nodeId) {
      ZWaveEngineNodeInfo info = getOrFetchNodeInfo(nodeId);
      return info != null && info.listening;
   }

   @Override
   public boolean isNodeFrequentListeningDevice(long homeId, int nodeId) {
      ZWaveEngineNodeInfo info = getOrFetchNodeInfo(nodeId);
      return info != null && info.frequentListening;
   }

   @Override
   public boolean isNodeBeamingDevice(long homeId, int nodeId) {
      ZWaveEngineNodeInfo info = getOrFetchNodeInfo(nodeId);
      return info != null && info.beaming;
   }

   @Override
   public boolean isNodeRoutingDevice(long homeId, int nodeId) {
      ZWaveEngineNodeInfo info = getOrFetchNodeInfo(nodeId);
      return info != null && info.routing;
   }

   @Override
   public boolean isNodeSecurityDevice(long homeId, int nodeId) {
      ZWaveEngineNodeInfo info = getOrFetchNodeInfo(nodeId);
      return info != null && info.security;
   }

   @Override
   public long getNodeMaxBaudRate(long homeId, int nodeId) {
      ZWaveEngineNodeInfo info = getOrFetchNodeInfo(nodeId);
      return info != null ? info.maxBaudRate : 0;
   }

   @Override
   public int getNodeVersion(long homeId, int nodeId) {
      ZWaveEngineNodeInfo info = getOrFetchNodeInfo(nodeId);
      return info != null ? info.version : 0;
   }

   @Override
   public int getNodeSecurity(long homeId, int nodeId) {
      ZWaveEngineNodeInfo info = getOrFetchNodeInfo(nodeId);
      return info != null ? info.securityByte : 0;
   }

   @Override
   public int getNodeBasic(long homeId, int nodeId) {
      ZWaveEngineNodeInfo info = getOrFetchNodeInfo(nodeId);
      return info != null ? info.basicType : 0;
   }

   @Override
   public int getNodeGeneric(long homeId, int nodeId) {
      ZWaveEngineNodeInfo info = getOrFetchNodeInfo(nodeId);
      return info != null ? info.genericType : 0;
   }

   @Override
   public int getNodeSpecific(long homeId, int nodeId) {
      ZWaveEngineNodeInfo info = getOrFetchNodeInfo(nodeId);
      return info != null ? info.specificType : 0;
   }

   @Override
   public String getNodeManufacturerId(long homeId, int nodeId) {
      // Manufacturer info requires sending a MANUFACTURER_SPECIFIC_GET command
      // and parsing the report. Return empty for now - the upper layers handle this.
      return "";
   }

   @Override
   public String getNodeProductType(long homeId, int nodeId) {
      return "";
   }

   @Override
   public String getNodeProductId(long homeId, int nodeId) {
      return "";
   }

   @Override
   public boolean isNodeInfoReceived(long homeId, int nodeId) {
      return nodeInfoCache.containsKey(nodeId);
   }

   @Override
   public boolean isNodeAwake(long homeId, int nodeId) {
      // Wake-up tracking not yet implemented at engine level
      return true;
   }

   @Override
   public boolean isNodeFailed(long homeId, int nodeId) {
      ZWaveSerialFrame resp = sendAndWaitForResponse(
         ZWaveSerialFrame.request(FUNC_ID_ZW_IS_FAILED_NODE_ID, (byte) nodeId));
      if (resp != null && resp.getDataLength() >= 1) {
         return resp.getDataByte(0) != 0;
      }
      return false;
   }

   // ========================
   // Associations
   // ========================

   @Override
   public int getNumGroups(long homeId, int nodeId) {
      // Association group count is obtained via ASSOCIATION_GROUPINGS_GET command class
      // which goes through the normal command pipeline, not the serial API directly.
      return 0;
   }

   @Override
   public long getAssociations(long homeId, int nodeId, int groupIdx, AtomicReference<int[]> associations) {
      // Association data is obtained via ASSOCIATION_GET command class
      return 0;
   }

   @Override
   public int getMaxAssociations(long homeId, int nodeId, int groupIdx) {
      return 0;
   }

   @Override
   public String getGroupLabel(long homeId, int nodeId, int groupIdx) {
      return "";
   }

   @Override
   public int addAssociation(long homeId, int nodeId, int groupIdx, int targetNodeId) {
      return 0;
   }

   @Override
   public int removeAssociation(long homeId, int nodeId, int groupIdx, int targetNodeId) {
      return 0;
   }

   // ========================
   // Internal helpers
   // ========================

   /**
    * Fetch node protocol info via FUNC_ID_ZW_GET_NODE_PROTOCOL_INFO.
    * Response: capabilities | frequentListening | reserved | basicType | genericType | specificType
    */
   private ZWaveEngineNodeInfo getOrFetchNodeInfo(int nodeId) {
      ZWaveEngineNodeInfo cached = nodeInfoCache.get(nodeId);
      if (cached != null) {
         return cached;
      }

      ZWaveSerialFrame resp = sendAndWaitForResponse(
         ZWaveSerialFrame.request(FUNC_ID_ZW_GET_NODE_PROTOCOL_INFO, (byte) nodeId));
      if (resp == null || resp.getDataLength() < 5) {
         return null;
      }

      ZWaveEngineNodeInfo info = new ZWaveEngineNodeInfo();
      int capabilities = 0xFF & resp.getDataByte(0);
      int flss = 0xFF & resp.getDataByte(1);

      info.listening = (capabilities & 0x80) != 0;
      info.routing = (capabilities & 0x40) != 0;
      info.maxBaudRate = (capabilities & 0x38) == 0x10 ? 40000 : 9600;
      info.version = (capabilities & 0x07) + 1;
      info.frequentListening = (flss & 0x60) != 0;
      info.beaming = (flss & 0x10) != 0;
      info.security = (flss & 0x01) != 0;
      info.securityByte = 0xFF & resp.getDataByte(2);
      info.basicType = 0xFF & resp.getDataByte(3);
      info.genericType = 0xFF & resp.getDataByte(4);
      info.specificType = resp.getDataLength() > 5 ? 0xFF & resp.getDataByte(5) : 0;

      nodeInfoCache.put(nodeId, info);
      return info;
   }

   private void notifyBootstrapSuccess() {
      logger.info("Z-Wave bootstrap complete, home ID: 0x{}", String.format("%08X", homeId));
      for (EngineListener l : listeners) {
         l.onBootstrapSuccess(homeId);
      }
   }

   private void notifyBootstrapFailure() {
      for (EngineListener l : listeners) {
         l.onBootstrapFailure();
      }
   }

   private void notifyListeners(ZWaveEngineMsg msg) {
      for (EngineListener l : listeners) {
         try {
            l.onNotification(msg);
         } catch (Exception e) {
            logger.error("Error in engine listener", e);
         }
      }
   }
}
