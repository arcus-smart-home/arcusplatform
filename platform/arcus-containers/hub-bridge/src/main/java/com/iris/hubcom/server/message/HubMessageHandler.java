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
package com.iris.hubcom.server.message;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.HdrHistogram.DoubleHistogram;
import org.HdrHistogram.DoubleRecorder;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.bus.PlatformBusService;
import com.iris.bridge.bus.ProtocolBusService;
import com.iris.bridge.hub.HubLogger;
import com.iris.bridge.server.message.DeviceMessageHandler;
import com.iris.bridge.server.netty.BridgeMdcUtil;
import com.iris.bridge.server.session.ClientToken;
import com.iris.bridge.server.session.Session;
import com.iris.core.messaging.kafka.KafkaOpsConfig;
import com.iris.core.messaging.kafka.KafkaSerializers;
import com.iris.core.metricsreporter.reporter.IrisMetricsFormat;
import com.iris.hubcom.authz.HubMessageFilter;
import com.iris.hubcom.server.session.HubSession;
import com.iris.hubcom.server.session.HubSession.State;
import com.iris.info.IrisApplicationInfo;
import com.iris.io.Deserializer;
import com.iris.io.json.JSON;
import com.iris.messages.HubMessage;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.metrics.IrisHubMetrics;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.protocol.ProtocolMessage;
import com.iris.util.MdcContext.MdcContextReference;

import io.netty.buffer.ByteBuf;

@Singleton
public class HubMessageHandler implements Closeable, DeviceMessageHandler<ByteBuf> {
   private final static Logger logger = LoggerFactory.getLogger(HubMessageHandler.class);
   private static final long HUB_METRICS_VALIDITY_TIME_MS = TimeUnit.MINUTES.toMillis(5);

   private static final String METRICS_TOPIC = "metrics";

   private final HubLogger hubLogger;
   private final KafkaProducer<Void, JsonObject> irisMetricsSender;

   private final ConcurrentMap<String,DoubleRecorder> fastAggregatedMetrics;
   private final ConcurrentMap<String,DoubleRecorder> mediumAggregatedMetrics;
   private final ConcurrentMap<String,DoubleRecorder> slowAggregatedMetrics;

   private final PlatformBusService platformBusService;
   private final ProtocolBusService protocolBusService;
   private final HubMessageFilter filter;
   private final Deserializer<PlatformMessage> platformDeserializer = JSON.createDeserializer(PlatformMessage.class);
   private final Deserializer<ProtocolMessage> protocolDeserializer = JSON.createDeserializer(ProtocolMessage.class);
   private final DirectMessageExecutor directExecutor;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public HubMessageHandler(
         PlatformBusService platformBusService,
         ProtocolBusService protocolBusService,
         HubMessageFilter filter,
         DirectMessageExecutor directExecutor,
         HubLogger hubLogger,
         KafkaOpsConfig config,
         PlacePopulationCacheManager populationCacheMgr
   ) {

      this.platformBusService = platformBusService;
      this.protocolBusService = protocolBusService;
      this.filter = filter;
      this.directExecutor = directExecutor;
      this.irisMetricsSender = new KafkaProducer<>(config.toNuProducerProperties(), KafkaSerializers.voidSerializer(), KafkaSerializers.jsonSerializer());
      this.populationCacheMgr = populationCacheMgr;
      
      this.fastAggregatedMetrics = new ConcurrentHashMap<>();
      this.mediumAggregatedMetrics = new ConcurrentHashMap<>();
      this.slowAggregatedMetrics = new ConcurrentHashMap<>();
      this.hubLogger = hubLogger;

      ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor();
      es.scheduleAtFixedRate(new AggregatedMetricsReporter(fastAggregatedMetrics), IrisHubMetrics.HUB_METRIC_FAST_PERIOD_NS, IrisHubMetrics.HUB_METRIC_FAST_PERIOD_NS, TimeUnit.NANOSECONDS);
      es.scheduleAtFixedRate(new AggregatedMetricsReporter(mediumAggregatedMetrics), IrisHubMetrics.HUB_METRIC_MEDIUM_PERIOD_NS, IrisHubMetrics.HUB_METRIC_MEDIUM_PERIOD_NS, TimeUnit.NANOSECONDS);
      es.scheduleAtFixedRate(new AggregatedMetricsReporter(slowAggregatedMetrics), IrisHubMetrics.HUB_METRIC_SLOW_PERIOD_NS, IrisHubMetrics.HUB_METRIC_SLOW_PERIOD_NS, TimeUnit.NANOSECONDS);
   }

   @PreDestroy @Override
   public void close() throws IOException {
      // we opened it, we need to close it
      irisMetricsSender.close();
   }

   @Override
   public ByteBuf handleMessage(Session session, ByteBuf byteBuf) {
      HubMessage msgObj = byteBufToObject(byteBuf);
      if(logger.isTraceEnabled()) {
        logger.trace("Received hub message type: [{}] message: [{}]", msgObj.getType(), new String(msgObj.getPayload(), Charsets.UTF_8));
      }
      switch (msgObj.getType()) {
      case PLATFORM:
         PlatformMessage platformMsg = bytesToPlatform(msgObj.getPayload(), session.getActivePlace());
         if(filter.acceptFromHub((HubSession) session, platformMsg)) {
            PlatformMessage copy = ensureTimestamp(session, platformMsg);
            try (MdcContextReference context = BridgeMdcUtil.captureAndInitializeContext(session,copy)) {
               directExecutor.handle(session, copy);

               logger.trace("Placing platform message on platform bus of type [{}] for place [{}] and population [{}]", copy.getMessageType(), copy.getPlaceId(), copy.getPopulation());
               platformBusService.placeMessageOnPlatformBus(copy);
            }
         } else {
            logger.debug("Refusing platform message [{}] from hub [{}]", platformMsg, session);
         }
         break;

      case PROTOCOL:
         ProtocolMessage protocolMsg = bytesToProtocol(msgObj.getPayload(), session.getActivePlace());
         if(filter.acceptFromHub((HubSession) session, protocolMsg)) {
            ProtocolMessage protCopy = ensureTimestamp(session, protocolMsg);
            logger.trace("Placing protocol message on protocol bus");
            protocolBusService.placeMessageOnProtocolBus(protCopy);
         } else {
            logger.debug("Refusing protocol message [{}] from hub [{}]", protocolMsg, session);
         }
         break;

      case LOG:
         hubLogger.log(msgObj, session.getActivePlace(), session.getClientToken().getRepresentation());
         break;

      case METRICS:
         JsonObject metrics = bytesToMetrics(msgObj.getPayload(), session.getActivePlace());
         handleMetricsMessage(metrics, session.getActivePlace(), session.getClientToken());
         break;

      default:
         logger.error("Unrecognized message type [{}]", msgObj.getType());
      }

      return null;
   }

   private void handleMetricsMessage(JsonObject metrics, String placeId, ClientToken token) {
      long ts = metrics.get("ts").getAsLong();
      if (System.currentTimeMillis() > (ts + HUB_METRICS_VALIDITY_TIME_MS)) {
         logger.info("discarding metrics message from hub that is too old: timestamp={}, hub={}", ts, token.getRepresentation());
         return;
      }

      String type = metrics.get("mt").getAsString();
      if (StringUtils.isBlank(type)) {
         logger.info("discarding unknown metrics message from hub: type={}, hub={}", type, token.getRepresentation());
         return;
      }


      switch (type) {
      case "tag":
         handleTaggedMetricsReport(metrics, placeId, token, ts);
         break;
      case "agg":
         handleAggregatedMetricsReport(metrics, placeId, token, ts);
         break;
      default:
         logger.info("discarding unknown metrics message from hub: type={}, hub={}", type, token.getRepresentation());
         return;
      }
   }

   private void handleAggregatedMetricsReport(JsonObject metrics, String placeId, ClientToken token, long ts) {
      JsonElement elem = metrics.get("gauges");
      JsonArray gauges = (elem == null || elem.isJsonNull()) ? null : elem.getAsJsonArray();
      if (gauges == null || gauges.isJsonNull()) {
         return;
      }

      for (JsonElement element : gauges) {
         JsonObject gauge = element.getAsJsonObject();

         String name = gauge.get("n").getAsString();
         String interval = gauge.get("i").getAsString();

         DoubleRecorder recorder;
         switch (interval) {
         case "f":
            recorder = fastAggregatedMetrics.computeIfAbsent(name, (n) -> new DoubleRecorder(2));
            break;
         case "m":
            recorder = mediumAggregatedMetrics.computeIfAbsent(name, (n) -> new DoubleRecorder(2));
            break;
         case "s":
            recorder = slowAggregatedMetrics.computeIfAbsent(name, (n) -> new DoubleRecorder(2));
            break;
         default:
            continue;
         }

         recorder.recordValue(gauge.get("v").getAsDouble());
      }
   }

   private void handleTaggedMetricsReport(JsonObject metrics, String placeId, ClientToken token, long ts) {
      JsonObject metricMessage = new JsonObject();
      metricMessage.addProperty("ts", ts);
      metricMessage.addProperty("hst", token.getRepresentation());
      metricMessage.addProperty("svc", "hub-agent");
      metricMessage.add("ctn", metrics.get("model"));
      metricMessage.add("svr", metrics.get("osver"));

      JsonElement elem = metrics.get("gauges");
      JsonArray gauges = (elem == null || elem.isJsonNull()) ? null : elem.getAsJsonArray();
      if (gauges != null && !gauges.isJsonNull()) {
         JsonArray metricGauges = new JsonArray();
         for (JsonElement element : gauges) {
            JsonObject gauge = element.getAsJsonObject();

            JsonObject g = IrisMetricsFormat.toJsonGauge(
               gauge.get("n"),
               gauge.get("v"),
               ImmutableList.of()
            );

            metricGauges.add(g);
         }
         metricMessage.add("gauges", metricGauges);
      }

      elem = metrics.get("counters");
      JsonArray counters = (elem == null || elem.isJsonNull()) ? null : elem.getAsJsonArray();
      if (counters != null && !counters.isJsonNull()) {
         JsonArray metricCounters = new JsonArray();
         for (JsonElement element : counters) {
            JsonObject counter = element.getAsJsonObject();

            JsonObject c = IrisMetricsFormat.toJson(
               counter.get("n"),
               counter.get("v"),
               ImmutableList.of()
            );

            metricCounters.add(c);
         }
         metricMessage.add("counters", metricCounters);
      }

      elem = metrics.get("histograms");
      JsonArray histograms = (elem == null || elem.isJsonNull()) ? null : elem.getAsJsonArray();
      if (histograms != null && !histograms.isJsonNull()) {
         JsonArray metricHistograms = new JsonArray();
         for (JsonElement element : histograms) {
            JsonObject histogram = element.getAsJsonObject();

            JsonObject c = IrisMetricsFormat.toJson(
               histogram.get("n"),
               histogram.get("count"),
               histogram.get("min"),
               histogram.get("max"),
               histogram.get("mean"),
               histogram.get("stddev"),
               histogram.get("p50"),
               histogram.get("p75"),
               histogram.get("p95"),
               histogram.get("p98"),
               histogram.get("p99"),
               histogram.get("p999"),
               ImmutableList.of()
            );

            metricHistograms.add(c);
         }
         metricMessage.add("histograms", metricHistograms);
      }

      elem = metrics.get("timers");
      JsonArray timers = (elem == null || elem.isJsonNull()) ? null : elem.getAsJsonArray();
      if (timers != null && !timers.isJsonNull()) {
         JsonArray metricTimers = new JsonArray();
         for (JsonElement element : timers) {
            JsonObject timer = element.getAsJsonObject();

            JsonObject c = IrisMetricsFormat.toJson(
               timer.get("n"),
               timer.get("count"),
               timer.get("min"),
               timer.get("max"),
               timer.get("mean"),
               timer.get("stddev"),
               timer.get("p50"),
               timer.get("p75"),
               timer.get("p95"),
               timer.get("p98"),
               timer.get("p99"),
               timer.get("p999"),
               ImmutableList.of()
            );

            metricTimers.add(c);
         }
         metricMessage.add("timers", metricTimers);
      }

      elem = metrics.get("meters");
      JsonArray meters = (elem == null || elem.isJsonNull()) ? null : elem.getAsJsonArray();
      if (meters != null && !meters.isJsonNull()) {
         JsonArray metricMeters = new JsonArray();
         for (JsonElement element : meters) {
            JsonObject meter = element.getAsJsonObject();

            JsonObject c = IrisMetricsFormat.toJson(
               meter.get("n"),
               meter.get("count"),
               meter.get("mean"),
               meter.get("m1"),
               meter.get("m5"),
               meter.get("m15"),
               ImmutableList.of()
            );

            metricMeters.add(c);
         }
         metricMessage.add("meters", metricMeters);
      }

      irisMetricsSender.send(new ProducerRecord<Void, JsonObject>(METRICS_TOPIC, metricMessage));
   }

   private PlatformMessage ensureTimestamp(Session session, PlatformMessage msg) {
      PlatformMessage.Builder builder = PlatformMessage.builder(msg);
      if(MessageConstants.MSG_HUB_CONNECTED_EVENT.equals(msg.getMessageType())) {
         builder.withTimeToLive((int) TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS));
      }
      if(Capability.EVENT_GET_ATTRIBUTES_RESPONSE.equals(msg.getMessageType())) {
         HubSession hubSession = (HubSession) session;

         Map<String, Object> attributes = new HashMap<>(msg.getValue().getAttributes());
         attributes.put(HubConnectionCapability.ATTR_LASTCHANGE, hubSession.getLastStateChange());
         if(hubSession.getState() == State.AUTHORIZED) {
            attributes.put(HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_ONLINE);
         }
         else {
            attributes.put(HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_HANDSHAKE);
         }

         builder.withPayload(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, attributes);
      }
      return
         builder
            .withClientTime(msg.getTimestamp())
            .withTimestamp(new Date())
            .withPlaceId(session.getActivePlace())		
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(session.getActivePlace()))
            .create();
   }

   private ProtocolMessage ensureTimestamp(Session session, ProtocolMessage msg) {
      return ProtocolMessage.builder(msg)
            .withClientTime(msg.getTimestamp())
            .withTimestamp(new Date())
            .withPlaceId(session.getActivePlace())  
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(session.getActivePlace()))
            .create();
   }

   private JsonObject bytesToMetrics(byte[] payload, String placeId) {
      String spayload = new String(payload, StandardCharsets.UTF_8);

      JsonParser parser = new JsonParser();
      JsonElement elem = parser.parse(spayload);
      return elem.getAsJsonObject();
   }

   private PlatformMessage bytesToPlatform(byte[] payload, String placeId) {
     PlatformMessage message = platformDeserializer.deserialize(payload);
     return
           PlatformMessage
              .builder(message)
              .withPlaceId(placeId)
              .create();
   }

   private ProtocolMessage bytesToProtocol(byte[] payload, String placeId) {
     ProtocolMessage message = protocolDeserializer.deserialize(payload);
     return
           ProtocolMessage
              .builder(message)
              .withPlaceId(placeId)
              .create();
   }

   private HubMessage byteBufToObject(ByteBuf byteBuf) {
      byte[] bytes;
      if (byteBuf.hasArray()) {
         bytes = byteBuf.array();
      }
      else {
         bytes = new byte[byteBuf.readableBytes()];
         byteBuf.getBytes(byteBuf.readerIndex(), bytes);
      }

      HubMessage msgObj = JSON.createDeserializer(HubMessage.class).deserialize(bytes);
      return msgObj;
   }

   private final class AggregatedMetricsReporter implements Runnable {
      private final Map<String,DoubleRecorder> metrics;
      private final Map<String,DoubleHistogram> recycle;

      public AggregatedMetricsReporter(Map<String, DoubleRecorder> metrics) {
         this.metrics = metrics;
         this.recycle = new HashMap<>();
      }

      @Override
      public void run() {
         try {
            JsonObject metricMessage = new JsonObject();
            metricMessage.addProperty("ts", System.currentTimeMillis());
            metricMessage.addProperty("hst", IrisApplicationInfo.getHostName());
            metricMessage.addProperty("svc", "hub-agent");
            metricMessage.addProperty("ctn", IrisApplicationInfo.getContainerName());
            metricMessage.addProperty("svr", IrisApplicationInfo.getApplicationVersion());

            JsonArray histograms = new JsonArray();
            for (Map.Entry<String,DoubleRecorder> metric : metrics.entrySet()) {
               String name = metric.getKey();
               DoubleRecorder recorder = metric.getValue();

               DoubleHistogram hist = recorder.getIntervalHistogram(recycle.get(name));
               recycle.put(name, hist);


               JsonObject h = IrisMetricsFormat.toJson(
                  name,
                  hist.getTotalCount(),
                  hist.getMinValue(),
                  hist.getMaxValue(),
                  hist.getMean(),
                  hist.getStdDeviation(),
                  hist.getValueAtPercentile(0.50),
                  hist.getValueAtPercentile(0.75),
                  hist.getValueAtPercentile(0.95),
                  hist.getValueAtPercentile(0.98),
                  hist.getValueAtPercentile(0.99),
                  hist.getValueAtPercentile(0.999),
                  ImmutableList.of()
               );

               histograms.add(h);
            }

            metricMessage.add("histograms", histograms);
            irisMetricsSender.send(new ProducerRecord<Void, JsonObject>(METRICS_TOPIC, metricMessage));
         } catch (Exception ex) {
            logger.warn("failed to report aggregated metrics:", ex);
         }
      }
   }
}

