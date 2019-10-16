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
package com.iris.agent.reflex.drivers;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.reflex.ReflexController;
import com.iris.agent.util.RxIris;
import com.iris.messages.address.Address;
import com.iris.protoc.runtime.ProtocMessage;
import com.iris.protoc.runtime.ProtocUtil;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.protocol.zigbee.zcl.Constants;
import com.iris.protocol.zigbee.zcl.General;
import com.iris.protocol.zigbee.zdp.Bind.ZdpBindRsp;

import rx.Observable;
import rx.Subscriber;

public abstract class AbstractZigbeeHubDriver extends AbstractHubDriver {
   private static final Logger log = LoggerFactory.getLogger(AbstractZigbeeHubDriver.class);

   public static final short HA_PROFILE_ID = (short)0x0104;
   public static final short AME_PROFILE_ID = (short)0xC216;
   public static final byte AME_ENDPOINT_ID = 0x02;
   public static final short AME_ATTR_CLUSTER_ID = (short)0x00C0;

   protected final long eui64;

   public AbstractZigbeeHubDriver(ReflexController parent, Address addr) {
      super(parent, addr);
      this.eui64 = parent.zigbee().getNodeEui64(addr);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Protocol Message Handling
   /////////////////////////////////////////////////////////////////////////////
   
   protected boolean handleAttributeUpdated(short profile, byte endpoint, short cluster, short id, ZclData data) {
      return false;
   }
   
   protected void finishAttributesUpdated() {
   }

   protected @Nullable ZclData getAttributeData(short attr) {
      log.warn("unknown attribute: {}", ProtocUtil.toHexString(attr));
      return null;
   }
   
   protected @Nullable ProtocMessage handleReadAttributes(General.ZclReadAttributes req) {
      short[] attrs = req.getAttributes();
      General.ZclReadAttributeRecord[] rec = new General.ZclReadAttributeRecord[attrs.length];

      int i = 0;
      for (short attr : attrs) {
         ZclData data = getAttributeData(attr);
         rec[i++] = General.ZclReadAttributeRecord.builder()
            .setStatus(data != null ? 0 : Constants.ZB_STATUS_NOT_FOUND)
            .setAttributeIdentifier(attr)
            .setAttributeData(data)
            .create();
      }

      return General.ZclReadAttributesResponse.builder()
         .setAttributes(rec)
         .create();
   }

   protected boolean handleReadAttributes(ZigbeeMessage.Zcl zcl) {
      General.ZclReadAttributes req = General.ZclReadAttributes.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, zcl.getPayload());
      ProtocMessage rsp = handleReadAttributes(req);

      if (rsp != null) {
         zclrsp(zcl, rsp).subscribe(RxIris.SWALLOW_ALL);
      } else {
         zcldefrsp(zcl, Constants.ZB_STATUS_UNSUP_GENERAL_COMMAND);
      }

      return rsp != null;
   }
   
   protected boolean handleReadAttributesResponse(ZigbeeMessage.Zcl zcl) {
      boolean handled = false;

      General.ZclReadAttributesResponse rsp = General.ZclReadAttributesResponse.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, zcl.getPayload());
      for (General.ZclReadAttributeRecord rec : rsp.getAttributes()) {
         handled |= handleAttributeUpdated(zcl.rawProfileId(), zcl.rawEndpoint(), zcl.rawClusterId(), rec.rawAttributeIdentifier(), rec.getAttributeData());
      }

      zcldefrsp(zcl, handled ? 0x00 : Constants.ZB_STATUS_UNSUP_GENERAL_COMMAND);
      finishAttributesUpdated();
      return handled;
   }
   
   protected boolean handleReportAttributes(ZigbeeMessage.Zcl zcl) {
      boolean handled = false;

      General.ZclReportAttributes rpts = General.ZclReportAttributes.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, zcl.getPayload());
      for (General.ZclAttributeReport rpt : rpts.getAttributes()) {
         handled |= handleAttributeUpdated(zcl.rawProfileId(), zcl.rawEndpoint(), zcl.rawClusterId(), rpt.rawAttributeIdenifier(), rpt.getAttributeData());
      }

      zcldefrsp(zcl, handled ? 0x00 : Constants.ZB_STATUS_UNSUP_GENERAL_COMMAND);
      finishAttributesUpdated();
      return handled;
   }
   
   protected boolean handleConfigureReportingResponse(ZigbeeMessage.Zcl zcl) {
      return false;
   }
   
   protected int handleWriteAttribute(int attr, ZclData data) {
      log.warn("cannot write unknown attribute: {}", ProtocUtil.toHexString(attr));
      return Constants.ZB_STATUS_NOT_FOUND;
   }

   protected @Nullable ProtocMessage handleWriteAttributes(General.ZclWriteAttributes req) {
      General.ZclWriteAttributeRecord[] recs = req.getAttributes();
      List<General.ZclWriteAttributeStatus> rsp = new ArrayList<>();
      for (General.ZclWriteAttributeRecord rec : recs) {
         int sta = handleWriteAttribute(rec.getAttributeIdentifier(), rec.getAttributeData());
         if (sta != 0) {
            rsp.add(
               General.ZclWriteAttributeStatus.builder()
                  .setStatus(sta)
                  .setAttributeIdentifier(rec.getAttributeIdentifier())
                  .create()
            );
         }
      }

      if (rsp.isEmpty()) {
            rsp.add(
               General.ZclWriteAttributeStatus.builder()
                  .setStatus(0)
                  .setAttributeIdentifier(0)
                  .create()
            );
      }

      return General.ZclWriteAttributesResponse.builder()
         .setAttributes(rsp.toArray(new General.ZclWriteAttributeStatus[rsp.size()]))
         .create();
   }

   protected boolean handleWriteAttributes(ZigbeeMessage.Zcl zcl) {
      General.ZclWriteAttributes req = General.ZclWriteAttributes.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, zcl.getPayload());
      ProtocMessage rsp = handleWriteAttributes(req);

      if (rsp != null) {
         zclrsp(zcl, rsp).subscribe(RxIris.SWALLOW_ALL);
      }

      return rsp != null;
   }
   
   protected boolean handleWriteAttributesResponse(ZigbeeMessage.Zcl zcl) {
      return false;
   }
   
   protected boolean handleZclGeneralFromServer(ZigbeeMessage.Zcl zcl) {
      switch (zcl.getZclMessageId()) {
      case General.ZclReadAttributes.ID:
         return handleReadAttributes(zcl);
      case General.ZclReadAttributesResponse.ID:
         return handleReadAttributesResponse(zcl);
      case General.ZclReportAttributes.ID:
         return handleReportAttributes(zcl);
      case General.ZclConfigureReportingResponse.ID:
         return handleConfigureReportingResponse(zcl);
      case General.ZclWriteAttributes.ID:
         return handleWriteAttributes(zcl);
      case General.ZclWriteAttributesResponse.ID:
         return handleWriteAttributesResponse(zcl);
      default:
         return false;
      }
   }

   protected boolean handleZclGeneralFromClient(ZigbeeMessage.Zcl zcl) {
      return false;
   }
   
   protected boolean handleZclClusterSpecificFromServer(ZigbeeMessage.Zcl zcl) {
      return false;
   }
   
   protected boolean handleZclClusterSpecificFromClient(ZigbeeMessage.Zcl zcl) {
      return false;
   }
   
   protected boolean handleZclManufSpecific(ZigbeeMessage.Zcl zcl) {
      return false;
   }
   
   protected boolean handleZcl(ZigbeeMessage.Zcl zcl) {
      int flags = zcl.getFlags();
      if ((flags & ZigbeeMessage.Zcl.MANUFACTURER_SPECIFIC) != 0) {
         return handleZclManufSpecific(zcl);
      }

      if ((flags & ZigbeeMessage.Zcl.CLUSTER_SPECIFIC) != 0) {
         if ((flags & ZigbeeMessage.Zcl.FROM_SERVER) != 0) {
            return handleZclClusterSpecificFromServer(zcl);
         } else {
            return handleZclClusterSpecificFromClient(zcl);
         }
      } else {
         if ((flags & ZigbeeMessage.Zcl.FROM_SERVER) != 0) {
            return handleZclGeneralFromServer(zcl);
         } else {
            return handleZclGeneralFromClient(zcl);
         }
      }
   }

   protected boolean handleZcl(byte[] msg) {
      ZigbeeMessage.Zcl zmsg = ZigbeeMessage.Zcl.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, msg);
      return handleZcl(zmsg);
   }
   
   protected boolean handleZdp(ZigbeeMessage.Zdp zdp) {
      return false;
   }

   protected boolean handleZdp(byte[] msg) {
      ZigbeeMessage.Zdp zmsg = ZigbeeMessage.Zdp.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, msg);
      return handleZdp(zmsg);
   }
   
   @Override
   protected boolean handle(String type, byte[] msg) {
      if (!ZigbeeProtocol.NAMESPACE.equals(type)) {
         return false;
      }

      ZigbeeMessage.Protocol pmsg = ZigbeeMessage.Protocol.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, msg);
      switch (pmsg.getType()) {
      case ZigbeeMessage.Zcl.ID:
         return handleZcl(pmsg.getPayload());

      case ZigbeeMessage.Zdp.ID:
         return handleZdp(pmsg.getPayload());

      default:
         return false;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Zigbee Driver APIs
   /////////////////////////////////////////////////////////////////////////////

   public long hubEui64() {
      return parent.zigbee().eui64();
   }

   public Observable<ZdpBindRsp> bind(short profile, byte endpoint, short cluster, boolean server) {
      return parent.zigbee().bind(eui64, profile, endpoint, cluster, server);
   }

   public Observable<ZdpBindRsp> bind(Binding... bindings) {
      List<Observable<ZdpBindRsp>> req = new ArrayList<>();
      for (Binding binding : bindings) {
         req.add(bind(binding.profile, binding.endpoint, binding.cluster, binding.server));
      }

      return Observable.concat(req);
   }
   
   public Observable<ZdpBindRsp> bind(Iterable<Binding> bindings) {
      List<Observable<ZdpBindRsp>> req = new ArrayList<>();
      for (Binding binding : bindings) {
         req.add(bind(binding.profile, binding.endpoint, binding.cluster, binding.server));
      }

      return Observable.concat(req);
   }
 
   public Observable<Boolean> bindAndExpectSuccess(short profile, byte endpoint, short cluster, boolean server) {
      return bind(profile, endpoint, cluster, server).lift(ZdpBindExpectSuccess.INSTANCE);
   }

   public Observable<Boolean> bindAndExpectSuccess(Binding... bindings) {
      return bind(bindings).lift(ZdpBindExpectSuccess.INSTANCE);
   }
   
   public Observable<Boolean> bindAndExpectSuccess(Iterable<Binding> bindings) {
      return bind(bindings).lift(ZdpBindExpectSuccess.INSTANCE);
   }

   public Observable<General.ZclWriteAttributesResponse> write(short profile, byte endpoint, short cluster, Map<Short,ZclData> attrs) {
      return parent.zigbee().write(eui64, profile, endpoint, cluster, attrs);
   }

   public Observable<General.ZclWriteAttributesResponse> write(short profile, byte endpoint, short cluster, General.ZclWriteAttributeRecord[] attrs) {
      return parent.zigbee().write(eui64, profile, endpoint, cluster, attrs);
   }

   public Observable<General.ZclReadAttributesResponse> read(short profile, byte endpoint, short cluster, Collection<Short> attrs) {
      return parent.zigbee().read(eui64, profile, endpoint, cluster, attrs);
   }

   public Observable<General.ZclReadAttributesResponse> read(short profile, byte endpoint, short cluster, short[] attrs) {
      return parent.zigbee().read(eui64, profile, endpoint, cluster, attrs);
   }

   public Observable<Boolean> zcl(short profile, byte endpoint, short cluster,
      ProtocMessage req, boolean fromServer, boolean clusterSpecific, boolean disableDefaultResponse) {
      return parent.zigbee().zcl(eui64, profile, endpoint, cluster, req, fromServer, clusterSpecific, disableDefaultResponse);
   }

   public Observable<Boolean> zclmsp(int manuf, short profile, short endpoint, short cluster,
      int cmd, byte[] data, boolean fromServer, boolean clusterSpecific, boolean disableDefaultResponse) {
      return parent.zigbee().zclmsp(eui64, manuf, profile, endpoint, cluster,
         cmd, data, fromServer, clusterSpecific, disableDefaultResponse);
   }

   public Observable<Boolean> zclrsp(ZigbeeMessage.Zcl req, ProtocMessage rsp) {
      boolean fs = (req.getFlags() & ZigbeeMessage.Zcl.FROM_SERVER) == 0;
      boolean cs = (req.getFlags() & ZigbeeMessage.Zcl.CLUSTER_SPECIFIC) == 1;
      return zcl(req.rawProfileId(), req.rawEndpoint(), req.rawClusterId(), rsp, fs, cs, true);
   }

   public Observable<Boolean> zcldefrsp(ZigbeeMessage.Zcl req, int status) {
      boolean ddr = (req.getFlags() & ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE) != 0;
      if (ddr) {
         return Observable.just(true);
      }

      boolean fs = (req.getFlags() & ZigbeeMessage.Zcl.FROM_SERVER) == 0;
      return zcl(req.rawProfileId(), req.rawEndpoint(), req.rawClusterId(), General.ZclDefaultResponse.builder()
         .setCommandIdentifier(req.getZclMessageId())
         .setStatus(status)
         .create(), 
         fs, false, true
      );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Helpers
   /////////////////////////////////////////////////////////////////////////////
   
   public <T,V> Observable<V> markDoneOnComplete(Variable<T> var, T done, Observable<V> obs) {
      return obs.doOnCompleted(new rx.functions.Action0() {
         @Override
         public void call() {
            log.info("completed: {}", var.getKey());
            set(var, done);
         }
      });
   }

   public <T> void subscribeAndLogResults(Logger log, String header, Observable<?> obs) {
      obs.subscribe(new RxIris.Observer<Object>() {
         @Override public void processNext(Object t) { }
         @Override public void processError(Throwable e) { log.info("{}: {}", header, e.getMessage(), e); }
         @Override public void processCompleted() { log.info("{}: complete", header); }
      });
   }

   /////////////////////////////////////////////////////////////////////////////
   // Helper Classes
   /////////////////////////////////////////////////////////////////////////////
   
   protected static final class ZdpBindExpectSuccess extends RxIris.Operator<Boolean,ZdpBindRsp> {
      private static final ZdpBindExpectSuccess INSTANCE = new ZdpBindExpectSuccess();

      @Override
      public Subscriber<? super ZdpBindRsp> run(Subscriber<? super Boolean> s) {
         return new RxIris.Subscriber<ZdpBindRsp>() {
            @Override
            public void processNext(ZdpBindRsp t) {
               byte sta = t.rawStatus();
               if (sta != 0) {
                  processError(new Exception("binding failed: " + ProtocUtil.toHexString(sta)));
               }
            }

            @Override
            public void processError(Throwable e) {
               if (!s.isUnsubscribed()) {
                  s.onError(e);
               }
            }

            @Override
            public void processCompleted() {
               if (!s.isUnsubscribed()) {
                  s.onNext(true);
                  s.onCompleted();
               }
            }
         };
      }
   }
   
   protected static final class Binding {
      private final short profile;
      private final byte endpoint;
      private final short cluster;
      private final boolean server;

      public Binding(short profile, byte endpoint, short cluster, boolean server) {
         this.profile = profile;
         this.endpoint = endpoint;
         this.cluster = cluster;
         this.server = server;
      }
   }
}

