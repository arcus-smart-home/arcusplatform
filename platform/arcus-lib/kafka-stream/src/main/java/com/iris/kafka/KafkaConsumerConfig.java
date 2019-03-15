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
package com.iris.kafka;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;
import com.iris.kafka.KafkaLeaderReader.ScanSearch;

public class KafkaConsumerConfig {
   private HostAndPort broker = HostAndPort.fromParts("localhost", 9092);
   private List<HostAndPort> brokerOverrides = ImmutableList.of();
   private int soTimeoutMs = 1000;
   private int bufferSize = 64 * 1024;
   private String clientId;
   private int scanFetchSize = 64 * 1024;
   private int fetchSize = 512 * 1024;
   private long emptyWaitTime = TimeUnit.NANOSECONDS.convert(10, TimeUnit.MILLISECONDS);
   private long emptyWaitMaxTime = TimeUnit.NANOSECONDS.convert(1000, TimeUnit.MILLISECONDS);
   private long emptyExitTime = Long.MAX_VALUE;
   private Set<String> topics = ImmutableSet.of();
   private Optional<Instant> offset = Optional.empty();
   private Optional<ScanSearch<? extends Object>> searchOffset = Optional.empty();

   public KafkaConsumerConfig() {

   }

   public KafkaConsumerConfig(KafkaConsumerConfig config) {
      this.broker = config.getBroker();
      this.brokerOverrides = config.getBrokerOverrides();
      this.soTimeoutMs = config.getSoTimeoutMs();
      this.bufferSize = config.getBufferSize();
      this.clientId = config.getClientId();
      this.scanFetchSize = config.getScanFetchSize();
      this.fetchSize = config.getFetchSize();
      this.emptyWaitTime = config.getEmptyWaitTime();
      this.emptyWaitMaxTime = config.getEmptyWaitMaxTime();
      this.emptyExitTime = config.getEmptyExitTime();
      this.topics = ImmutableSet.copyOf(config.getTopics());
      this.offset = config.getOffset();
      this.searchOffset = config.getScanSearch();
   }

   public HostAndPort getBroker() {
      return broker;
   }

   public void setBroker(HostAndPort broker) {
      Preconditions.checkNotNull(broker, "broker may not be null");
      this.broker = broker;
   }

   /**
    * @return the host
    */
   public String getHost() {
      return broker.getHostText();
   }

   /**
    * @param host the host to set
    */
   public void setHost(String host) {
      this.broker = HostAndPort.fromParts(host, this.broker.getPort());
   }

   /**
    * @return the port
    */
   public int getPort() {
      return this.broker.getPortOrDefault(9092);
   }

   /**
    * @param port the port to set
    */
   public void setPort(int port) {
      this.broker = HostAndPort.fromParts(this.broker.getHostText(), port);
   }

   public List<HostAndPort> getBrokerOverrides() {
      return brokerOverrides;
   }

   public Optional<HostAndPort> getBrokerOverride(int brokerId) {
      Preconditions.checkArgument(brokerId > 0);
      if(brokerOverrides.size() < brokerId) {
         return Optional.empty();
      }
      else {
         return Optional.of(brokerOverrides.get(brokerId - 1));
      }
   }

   public void setBrokerOverrides(@Nullable List<HostAndPort> brokerOverrides) {
      this.brokerOverrides = brokerOverrides == null ? ImmutableList.of() : ImmutableList.copyOf(brokerOverrides);
   }

   /**
    * @return the soTimeoutMs
    */
   public int getSoTimeoutMs() {
      return soTimeoutMs;
   }

   /**
    * @param soTimeoutMs the soTimeoutMs to set
    */
   public void setSoTimeoutMs(int soTimeoutMs) {
      this.soTimeoutMs = soTimeoutMs;
   }

   /**
    * @return the bufferSize
    */
   public int getBufferSize() {
      return bufferSize;
   }

   /**
    * @param bufferSize the bufferSize to set
    */
   public void setBufferSize(int bufferSize) {
      this.bufferSize = bufferSize;
   }

   /**
    * @return the searchOffset
    */
   public Optional<ScanSearch<? extends Object>> getSearchOffset() {
      return searchOffset;
   }

   /**
    * @param searchOffset the searchOffset to set
    */
   public void setSearchOffset(Optional<ScanSearch<? extends Object>> searchOffset) {
      this.searchOffset = searchOffset;
   }

   /**
    * @return the clientId
    */
   public String getClientId() {
      return clientId;
   }

   /**
    * @param clientId the clientId to set
    */
   public void setClientId(String clientId) {
      this.clientId = clientId;
   }

   /**
    * @return the fetchSize
    */
   public int getFetchSize() {
      return fetchSize;
   }

   /**
    * @param fetchSize the fetchSize to set
    */
   public void setFetchSize(int fetchSize) {
      this.fetchSize = fetchSize;
   }

   public long getEmptyWaitTime() {
      return emptyWaitTime;
   }

   public void setEmptyWaitTime(long emptyWaitTime) {
      this.emptyWaitTime = emptyWaitTime;
   }

   public long getEmptyWaitMaxTime() {
      return emptyWaitMaxTime;
   }

   public void setEmptyWaitMaxTime(long emptyWaitMaxTime) {
      this.emptyWaitMaxTime = emptyWaitMaxTime;
   }

   public long getEmptyExitTime() {
      return emptyExitTime;
   }

   public void setEmptyExitTime(long emptyExitTime) {
      this.emptyExitTime = emptyExitTime;
   }

   /**
    * @return the topics
    */
   public Set<String> getTopics() {
      return topics;
   }

   /**
    * @param topics the topics to set
    */
   public void setTopics(Set<String> topics) {
      this.topics = ImmutableSet.copyOf(topics);
   }

   /**
    * @return the offset
    */
   public Optional<Instant> getOffset() {
      return offset;
   }

   /**
    * @param offset the offset to set
    */
   public void setOffset(Optional<Instant> offset) {
      this.offset = offset;
   }

   public void setOffset(@Nullable Instant offset) {
      this.offset = Optional.ofNullable(offset);
   }

   public int getScanFetchSize() {
      return scanFetchSize;
   }

   public void setScanFetchSize(int scanFetchSize) {
      this.scanFetchSize = scanFetchSize;
   }

   /**
    * @return the offset
    */
   public Optional<ScanSearch<? extends Object>> getScanSearch() {
      return searchOffset;
   }

   /**
    * @param offset the offset to set
    */
   public void setScanSearch(Optional<ScanSearch<? extends Object>> searchOffset) {
      this.searchOffset = searchOffset;
   }

   public void setScanSearch(@Nullable ScanSearch<? extends Object> searchOffset) {
      this.searchOffset = Optional.ofNullable(searchOffset);
   }

}

