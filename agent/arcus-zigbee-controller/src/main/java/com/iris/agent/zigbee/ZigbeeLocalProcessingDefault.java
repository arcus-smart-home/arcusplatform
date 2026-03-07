/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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
package com.iris.agent.zigbee;

import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.zigbee.ember.ZigbeeDriver;
import com.iris.agent.zigbee.node.ZBNode;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.protoc.runtime.ProtocMessage;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.zcl.General;
import com.iris.protocol.zigbee.zdp.Bind;
import com.iris.protocol.zwave.Protocol;
import com.zsmartsystems.zigbee.IeeeAddress;
import com.zsmartsystems.zigbee.ZigBeeEndpoint;
import com.zsmartsystems.zigbee.ZigBeeNetworkManager;
import com.zsmartsystems.zigbee.ZigBeeNode;
import com.zsmartsystems.zigbee.zcl.ZclAttribute;
import com.zsmartsystems.zigbee.zcl.ZclCluster;

import rx.Observable;

public class ZigbeeLocalProcessingDefault implements ZigbeeLocalProcessing {
   private static final Logger logger = LoggerFactory.getLogger(ZigbeeLocalProcessingDefault.class);

   @Override
   public boolean isOffline(Address addr) {
      ZBNode node = resolveNode(addr);
      return node == null || !node.isOnline();
   }

   @Override
   public void setOfflineTimeout(Address addr, long offlineTimeout) {
      ZBNode node = resolveNode(addr);
      if (node != null) {
         node.setOfflineTimeout((int) offlineTimeout);
         ZBServices.INSTANCE.getNetwork().saveNode(node);
      }
   }

   @Override
   public Observable<?> send(Address addr, Protocol.Message msg) {
      // Note: The interface signature uses Protocol.Message (Z-Wave type) which appears
      // to be a copy-paste error in the original interface. For now, return empty.
      logger.warn("send() called with Z-Wave Protocol.Message type on ZigBee local processing");
      return Observable.empty();
   }

   @Override
   public void addScheduledPoll(Address addr, long period, TimeUnit unit, Collection<byte[]> payloads) {
      logger.debug("addScheduledPoll not yet fully implemented");
   }

   @Override
   public long getNodeEui64(Address addr) {
      ZBNode node = resolveNode(addr);
      return node != null ? node.getIeeeAddr() : 0;
   }

   @Override
   public long eui64() {
      ZigbeeDriver driver = ZBServices.INSTANCE.getDriver();
      return driver != null ? driver.getCoordinatorEui64() : 0;
   }

   @Override
   public Observable<Bind.ZdpBindRsp> bind(long eui64, short profile, byte endpoint, short cluster, boolean server) {
      return Observable.create(sub -> {
         try {
            ZigBeeNetworkManager mgr = getNetworkManager();
            if (mgr == null) {
               sub.onError(new IllegalStateException("Network manager not available"));
               return;
            }

            IeeeAddress addr = new IeeeAddress(String.format("%016X", eui64));
            ZigBeeNode zsNode = mgr.getNode(addr);
            if (zsNode == null) {
               sub.onError(new IllegalStateException("Node not found: " + addr));
               return;
            }

            ZigBeeEndpoint ep = zsNode.getEndpoint(endpoint & 0xFF);
            if (ep == null) {
               sub.onError(new IllegalStateException("Endpoint not found: " + (endpoint & 0xFF)));
               return;
            }

            ZclCluster zclCluster = server ? ep.getInputCluster(cluster & 0xFFFF) : ep.getOutputCluster(cluster & 0xFFFF);
            if (zclCluster != null) {
               java.util.concurrent.Future<com.zsmartsystems.zigbee.CommandResult> future =
                     zclCluster.bind(mgr.getLocalIeeeAddress(), 1);
               try {
                  future.get(30, TimeUnit.SECONDS);
                  Bind.ZdpBindRsp rsp = Bind.ZdpBindRsp.builder()
                        .setStatus(0)
                        .create();
                  sub.onNext(rsp);
                  sub.onCompleted();
               } catch (Exception bindEx) {
                  sub.onError(bindEx);
               }
            } else {
               sub.onError(new IllegalStateException("Cluster not found: " + (cluster & 0xFFFF)));
            }
         } catch (Exception ex) {
            sub.onError(ex);
         }
      });
   }

   @Override
   public Observable<General.ZclWriteAttributesResponse> write(long eui64, short profile, byte endpoint,
         short cluster, Map<Short, ZclData> attrs) {
      return Observable.create(sub -> {
         try {
            ZigBeeNetworkManager mgr = getNetworkManager();
            if (mgr == null) {
               sub.onError(new IllegalStateException("Network manager not available"));
               return;
            }

            IeeeAddress addr = new IeeeAddress(String.format("%016X", eui64));
            ZigBeeNode zsNode = mgr.getNode(addr);
            if (zsNode == null) {
               sub.onError(new IllegalStateException("Node not found: " + addr));
               return;
            }

            ZigBeeEndpoint ep = zsNode.getEndpoint(endpoint & 0xFF);
            if (ep == null) {
               sub.onError(new IllegalStateException("Endpoint not found"));
               return;
            }

            ZclCluster zclCluster = ep.getInputCluster(cluster & 0xFFFF);
            if (zclCluster == null) {
               sub.onError(new IllegalStateException("Cluster not found"));
               return;
            }

            for (Map.Entry<Short, ZclData> entry : attrs.entrySet()) {
               ZclAttribute attribute = zclCluster.getAttribute(entry.getKey() & 0xFFFF);
               if (attribute != null) {
                  attribute.writeValue(entry.getValue().getDataValue());
               }
            }

            General.ZclWriteAttributesResponse rsp = General.ZclWriteAttributesResponse.builder()
                  .setAttributes(new General.ZclWriteAttributeStatus[0])
                  .create();
            sub.onNext(rsp);
            sub.onCompleted();
         } catch (Exception ex) {
            sub.onError(ex);
         }
      });
   }

   @Override
   public Observable<General.ZclWriteAttributesResponse> write(long eui64, short profile, byte endpoint,
         short cluster, General.ZclWriteAttributeRecord[] attrs) {
      return Observable.create(sub -> {
         try {
            ZigBeeNetworkManager mgr = getNetworkManager();
            if (mgr == null) {
               sub.onError(new IllegalStateException("Network manager not available"));
               return;
            }

            IeeeAddress addr = new IeeeAddress(String.format("%016X", eui64));
            ZigBeeNode zsNode = mgr.getNode(addr);
            if (zsNode == null) {
               sub.onError(new IllegalStateException("Node not found: " + addr));
               return;
            }

            ZigBeeEndpoint ep = zsNode.getEndpoint(endpoint & 0xFF);
            if (ep == null) {
               sub.onError(new IllegalStateException("Endpoint not found"));
               return;
            }

            ZclCluster zclCluster = ep.getInputCluster(cluster & 0xFFFF);
            if (zclCluster == null) {
               sub.onError(new IllegalStateException("Cluster not found"));
               return;
            }

            for (General.ZclWriteAttributeRecord record : attrs) {
               ZclAttribute attribute = zclCluster.getAttribute(record.getAttributeIdentifier() & 0xFFFF);
               if (attribute != null && record.getAttributeData() != null) {
                  attribute.writeValue(record.getAttributeData().getDataValue());
               }
            }

            General.ZclWriteAttributesResponse rsp = General.ZclWriteAttributesResponse.builder()
                  .setAttributes(new General.ZclWriteAttributeStatus[0])
                  .create();
            sub.onNext(rsp);
            sub.onCompleted();
         } catch (Exception ex) {
            sub.onError(ex);
         }
      });
   }

   @Override
   public Observable<General.ZclReadAttributesResponse> read(long eui64, short profile, byte endpoint,
         short cluster, Collection<Short> attrs) {
      short[] attrArray = new short[attrs.size()];
      int i = 0;
      for (Short a : attrs) {
         attrArray[i++] = a;
      }
      return read(eui64, profile, endpoint, cluster, attrArray);
   }

   @Override
   public Observable<General.ZclReadAttributesResponse> read(long eui64, short profile, byte endpoint,
         short cluster, short[] attrs) {
      return Observable.create(sub -> {
         try {
            ZigBeeNetworkManager mgr = getNetworkManager();
            if (mgr == null) {
               sub.onError(new IllegalStateException("Network manager not available"));
               return;
            }

            IeeeAddress addr = new IeeeAddress(String.format("%016X", eui64));
            ZigBeeNode zsNode = mgr.getNode(addr);
            if (zsNode == null) {
               sub.onError(new IllegalStateException("Node not found: " + addr));
               return;
            }

            ZigBeeEndpoint ep = zsNode.getEndpoint(endpoint & 0xFF);
            if (ep == null) {
               sub.onError(new IllegalStateException("Endpoint not found"));
               return;
            }

            ZclCluster zclCluster = ep.getInputCluster(cluster & 0xFFFF);
            if (zclCluster == null) {
               sub.onError(new IllegalStateException("Cluster not found"));
               return;
            }

            General.ZclReadAttributeRecord[] records = new General.ZclReadAttributeRecord[attrs.length];
            for (int j = 0; j < attrs.length; j++) {
               ZclAttribute attribute = zclCluster.getAttribute(attrs[j] & 0xFFFF);
               Object value = attribute != null ? attribute.readValue(0) : null;

               records[j] = General.ZclReadAttributeRecord.builder()
                     .setAttributeIdentifier(attrs[j])
                     .setStatus((byte) (value != null ? 0 : 1))
                     .create();
            }

            General.ZclReadAttributesResponse rsp = General.ZclReadAttributesResponse.builder()
                  .setAttributes(records)
                  .create();
            sub.onNext(rsp);
            sub.onCompleted();
         } catch (Exception ex) {
            sub.onError(ex);
         }
      });
   }

   @Override
   public Observable<Boolean> zcl(long eui64, short profile, byte endpoint, short cluster,
         ProtocMessage req, boolean fromServer, boolean clusterSpecific, boolean disableDefaultResponse) {
      // TODO: serialize ProtocMessage into raw ZCL frame and send via sendApsFrame.
      // Currently a no-op — keypad hub drivers (CentraLite, GreatStar, Alertme) that
      // call this will not transmit arm responses, zone enrollment, or reporting config.
      logger.warn("zcl() not yet implemented — dropping command to {} cluster 0x{} ep {}",
            Long.toHexString(eui64), Integer.toHexString(cluster & 0xFFFF), endpoint);
      return Observable.just(false);
   }

   @Override
   public Observable<Boolean> zclmsp(long eui64, int manuf, short profile, short endpoint, short cluster,
         int cmd, byte[] data, boolean fromServer, boolean clusterSpecific, boolean disableDefaultResponse) {
      // TODO: build manufacturer-specific ZCL frame and send via sendApsFrame.
      // Currently a no-op — keypad hub drivers that call this for chime commands
      // (manuf 0x104E, cluster 0xFC04) will not transmit.
      logger.warn("zclmsp() not yet implemented — dropping command to {} manuf=0x{} cluster=0x{} cmd=0x{}",
            Long.toHexString(eui64), Integer.toHexString(manuf), Integer.toHexString(cluster & 0xFFFF), Integer.toHexString(cmd));
      return Observable.just(false);
   }

   private ZBNode resolveNode(Address addr) {
      Object id = addr.getId();
      if (id instanceof ProtocolDeviceId) {
         return ZBServices.INSTANCE.getNetwork().getNode((ProtocolDeviceId) id);
      }
      return null;
   }

   private ZigBeeNetworkManager getNetworkManager() {
      ZigbeeDriver driver = ZBServices.INSTANCE.getDriver();
      return driver != null ? driver.getNetworkManager() : null;
   }
}
