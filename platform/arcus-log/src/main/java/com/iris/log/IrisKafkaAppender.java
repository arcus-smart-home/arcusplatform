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
package com.iris.log;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.jdt.annotation.Nullable;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public class IrisKafkaAppender<E> extends UnsynchronizedAppenderBase<E> implements Callback {
   private final Serializer<String> keySerializer = new StringSerializer();
   private final Serializer<String> valueSerializer = new StringSerializer();
   private final IrisMetricSet metrics = IrisMetrics.metrics("iris.log");
   private final Counter logCount = metrics.counter("count");
   private final Counter logDropped = metrics.counter("dropped");
   private final Timer logTime = metrics.timer("time");

   private final IrisStdioHijack hijack;

   /////////////////////////////////////////////////////////////////////////////
   // The default settings for the Kafka log appender are:
   // * Acks = 0
   //    Logs are not critical, so enable substantially higher performance
   //    by enabling a fire and forget mode. This setting also means that
   //    retries will never be peformed.
   //
   // * Block On Buffer Full = False
   //    Making sure every single log makes it to the log server is not
   //    as important as ensuring the platform remains functional even
   //    when the kafka servers for logging are down.
   //
   // * Compression = Snappy
   //    Snappy compression is beneficial for producers and consumers since
   //    the amount of data transferred over the network is reduced. There
   //    is a CPU utilization penalty for this, but our services don't look
   //    to be CPU bound very often.
   //
   // * BatchSize = 128 KB, Linger Ms = 1000
   //    Logs are not real time crictical, but we would like the see them
   //    relatively fast. Larger batch sizes are better because they result
   //    in a lower number of requests to the servers and result in better
   //    compression ratios. Batch data up into 128 KB chunks or for upto
   //    1 second, which ever comes first.
   /////////////////////////////////////////////////////////////////////////////

   private String clientId;
   private String bootstrapServers;
   private String topic;
   private String acks = "0";
   private String compressionType = "snappy";

   private Integer sendBufferBytes = 128 * 1024;
   private Integer recvBufferBytes = 32 * 1024;
   private Integer retries = 0;
   private Integer batchSize = 128 * 1024;
   private Integer maxRequestSize = 1 * 1024 * 1024;
   private Integer timeoutMs = 30 * 1000;
   private Boolean blockOnBufferFull = false;

   private Long bufferMemory = 32L * 1024L * 1024L;
   private Long lingerMs = 1 * 1000L;
   private Long metadataFetchTimeoutMs = 60L * 1000L;
   private Long metadataMaxAgeMs = 5L * 60L * 1000L;
   private Long reconnectBackoffMs = 10L;
   private Long retryBackoffMs = 100L;

   private Layout<E> layout;

   // This field is marked volatile because it uses a double checked locking
   // pattern to lazily initialize the producer on startup.
   private volatile KafkaProducer<String,String> kafka;

   public IrisKafkaAppender() {
      this.hijack = new IrisStdioHijack();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Logback Lifecycle API
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public void start() {
      if (isStarted()) {
         return;
      }

      if (topic == null || topic.isEmpty()) {
         throw new IllegalStateException("topic not configured");
      }

      if (bootstrapServers == null || bootstrapServers.trim().isEmpty()) {
         throw new IllegalStateException("bootstrap servers not configured");
      }

      if (layout == null) {
         throw new IllegalStateException("layout not configured");
      }

      hijack.start();
      super.start();
   }

   @Override
   public void stop() {
      if (!isStarted()) {
         return;
      }

      KafkaProducer<String,String> kp = kafka;
      if (kp != null) {
         kp.close();
      }

      kafka = null;
      hijack.stop();
      super.stop();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Logback Appender API
   /////////////////////////////////////////////////////////////////////////////

   public Layout<E> getLayout() {
      return layout;
   }

   public void setLayout(Layout<E> layout) {
      this.layout = layout;
   }

   public String getTopic() {
      return topic;
   }

   public void setTopic(String topic) {
      this.topic = topic;
   }

   @Override
   protected void append(E event) {
      final Timer.Context context = logTime.time();

      try {
         String msg = layout.doLayout(event);
         getKafka().send(new ProducerRecord<String,String>(topic,msg), this);
      } finally {
         context.stop();
      }
   }

   @Override
   public void onCompletion(RecordMetadata metadata, Exception e) {
      if (e == null) {
         logCount.inc();
      } else {
         logDropped.inc();
         e.printStackTrace(hijack.getRealOutputStream());
      }
   }

   private KafkaProducer<String,String> getKafka() {
      // Double checked locking pattern because this is called a lot and because we need
      // to lazily initialize the producer since kafka itself logs through this logger.
      KafkaProducer<String,String> result = this.kafka;
      if (result == null) {
         synchronized (this) {
            result = this.kafka;
            if (result == null) {
               this.kafka = new KafkaProducer<String,String>(getKafkaProducerProperties(), keySerializer, valueSerializer);
               result = this.kafka;
            }
         }
      }

      return result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Kafka Configuration
   /////////////////////////////////////////////////////////////////////////////

   private Properties getKafkaProducerProperties() {
      Properties props = new Properties();
      putIf(props, "bootstrap.servers", expandHostAndPort(getBootstrapServers()));
      putIf(props, "acks", getAcks());
      putIf(props, "buffer.memory", getBufferMemory());
      putIf(props, "compression.type", getCompressionType());
      putIf(props, "retries", getRetries());
      putIf(props, "batch.size", getBatchSize());
      putIf(props, "client.id", getClientId());
      putIf(props, "linger.ms", getLingerMs());
      putIf(props, "max.request.size", getMaxRequestSize());
      putIf(props, "send.buffer.bytes", getSendBufferBytes());
      putIf(props, "receive.buffer.bytes", getRecvBufferBytes());
      putIf(props, "timeout.ms", getTimeoutMs());
      putIf(props, "block.on.buffer.full", getBlockOnBufferFull());
      putIf(props, "metadata.fetch.timeout.ms", getMetadataFetchTimeoutMs());
      putIf(props, "metadata.max.age.ms", getMetadataMaxAgeMs());
      putIf(props, "reconnect.backoff.ms", getReconnectBackoffMs());
      putIf(props, "retry.backoff.ms", getRetryBackoffMs());
      return props;
   }

   public String getClientId() {
      return clientId;
   }

   public void setClientId(String clientId) {
      this.clientId = clientId;
   }

   public String getBootstrapServers() {
      return bootstrapServers;
   }

   public void setBootstrapServers(String bootstrapServers) {
      this.bootstrapServers = bootstrapServers;
   }

   public String getAcks() {
      return acks;
   }

   public void setAcks(String acks) {
      this.acks = acks;
   }

   public String getCompressionType() {
      return compressionType;
   }

   public void setCompressionType(String compressionType) {
      this.compressionType = compressionType;
   }

   public Long getBufferMemory() {
      return bufferMemory;
   }

   public void setBufferMemory(Long bufferMemory) {
      this.bufferMemory = bufferMemory;
   }

   public Integer getRetries() {
      return retries;
   }

   public void setRetries(Integer retries) {
      this.retries = retries;
   }

   public Integer getBatchSize() {
      return batchSize;
   }

   public void setBatchSize(Integer batchSize) {
      this.batchSize = batchSize;
   }

   public Long getLingerMs() {
      return lingerMs;
   }

   public void setLingerMs(Long lingerMs) {
      this.lingerMs = lingerMs;
   }

   public Integer getMaxRequestSize() {
      return maxRequestSize;
   }

   public void setMaxRequestSize(Integer maxRequestSize) {
      this.maxRequestSize = maxRequestSize;
   }

   public Integer getTimeoutMs() {
      return timeoutMs;
   }

   public void setTimeoutMs(Integer timeoutMs) {
      this.timeoutMs = timeoutMs;
   }

   public Boolean getBlockOnBufferFull() {
      return blockOnBufferFull;
   }

   public void setBlockOnBufferFull(Boolean blockOnBufferFull) {
      this.blockOnBufferFull = blockOnBufferFull;
   }

   public Long getMetadataFetchTimeoutMs() {
      return metadataFetchTimeoutMs;
   }

   public void setMetadataFetchTimeoutMs(Long metadataFetchTimeoutMs) {
      this.metadataFetchTimeoutMs = metadataFetchTimeoutMs;
   }

   public Long getMetadataMaxAgeMs() {
      return metadataMaxAgeMs;
   }

   public void setMetadataMaxAgeMs(Long metadataMaxAgeMs) {
      this.metadataMaxAgeMs = metadataMaxAgeMs;
   }

   public Long getReconnectBackoffMs() {
      return reconnectBackoffMs;
   }

   public void setReconnectBackoffMs(Long reconnectBackoffMs) {
      this.reconnectBackoffMs = reconnectBackoffMs;
   }

   public Long getRetryBackoffMs() {
      return retryBackoffMs;
   }

   public void setRetryBackoffMs(Long retryBackoffMs) {
      this.retryBackoffMs = retryBackoffMs;
   }


   public Integer getSendBufferBytes() {
      return sendBufferBytes;
   }

   public void setSendBufferBytes(Integer sendBufferBytes) {
      this.sendBufferBytes = sendBufferBytes;
   }

   public Integer getRecvBufferBytes() {
      return recvBufferBytes;
   }

   public void setRecvBufferBytes(Integer recvBufferBytes) {
      this.recvBufferBytes = recvBufferBytes;
   }

   private Boolean putIf(Properties props, String name, @Nullable Number value) {
      return putIf(props, name, toString(value));
   }

   private Boolean putIf(Properties props, String name, @Nullable Boolean value) {
      return putIf(props, name, (value == null) ? null : (value ? "true" : "false"));
   }

   private Boolean putIf(Properties props, String name, @Nullable String value) {
      if(value == null) {
         return false;
      }

      props.put(name, value);
      return true;
   }

   @Nullable
   private String toString(@Nullable Object value) {
      if(value == null) {
         return null;
      }
      return String.valueOf(value);
   }

   private String expandHostAndPort(String commaDelimitedList) {
      List<String> contactPoints = new ArrayList<>();

      String[] cps = commaDelimitedList.split(",");
      for (int i = 0; i < cps.length; i++) {
         try {
            URI uri = URI.create("http://" + cps[i].trim());
            InetAddress[] addrs = InetAddress.getAllByName(uri.getHost());

            for (InetAddress addr : addrs) {
               String broker = addr.getHostAddress();
               if (uri.getPort() >= 0) {
                  broker = broker + ":" + uri.getPort();
               } else {
                  broker = broker + ":9092";
               }

               contactPoints.add(broker);
            }
         } catch (Exception ex) {
            // ignore so we can use any working addresses
            // that are available.
         }
      }

      if (contactPoints.isEmpty()) {
         throw new RuntimeException("Unable to configure Kafka cluster, hosts could not be resolved: " + commaDelimitedList);
      }

      StringBuilder bld = new StringBuilder();
      for (String cp : contactPoints) {
         if (cp == null) continue;

         if (bld.length() != 0) bld.append(',');
         bld.append(cp);
      }

      return bld.toString();
   }
}

