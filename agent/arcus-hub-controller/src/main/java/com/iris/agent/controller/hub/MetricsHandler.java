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
package com.iris.agent.controller.hub;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.CachedGauge;
import com.codahale.metrics.Counter;
import com.codahale.metrics.DerivativeGauge;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.exec.ExecService;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.metrics.MetricsService;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.agent.storage.StorageService;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubMetricsCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.type.HubMetric;
import com.iris.metrics.IrisHubMetrics;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.protocol.ProtocolMessage;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

// TODO: This class should support spinning up multiple metric reporters
//       that have different collected metric sets, different reporing
//       intervals, etc.
enum MetricsHandler implements PortHandler {
   INSTANCE;

   private static final Logger log = LoggerFactory.getLogger(MetricsHandler.class);
   private static final Logger METRICS_LOGGER = LoggerFactory.getLogger("metrics");

   // The rate at which we check if we need to send an aggregated report and the rates at which
   // fast, medium, and slow aggregated metrics are sent. These must be multiple of each other
   // going from top to bottom (i.e. fast must be a multiple of check, medium must be a multiple
   // of fast, etc.)
   private static final long AGGREGATED_METRICS_REPORTING_INTERVAL = IrisHubMetrics.HUB_METRIC_FAST_PERIOD_NS;
   private static final long AGGREGATED_METRICS_FAST_REPORTING_INTERVAL = IrisHubMetrics.HUB_METRIC_FAST_PERIOD_NS;
   private static final long AGGREGATED_METRICS_MEDIUM_REPORTING_INTERVAL = IrisHubMetrics.HUB_METRIC_MEDIUM_PERIOD_NS;
   private static final long AGGREGATED_METRICS_SLOW_REPORTING_INTERVAL = IrisHubMetrics.HUB_METRIC_SLOW_PERIOD_NS;

   // Number of entries to store for each metric that we keep around for
   // long term. The current setting is to allow 1440 samples in the buffer
   // allowing for over 24 hours of historical data when sampled once every
   // 60 seconds.
   //
   // The following metrics are currently kept long term:
   //    * metric timestamps                    8 bytes/entry
   //    * hub.system.mem.free                  8 bytes/entry
   //    * hub.system.disk.data.free            8 bytes/entry
   //    * hub.system.net.pri.recv              8 bytes/entry
   //    * hub.system.net.pri.sent              8 bytes/entry
   //    * hub.system.net.sec.recv              8 bytes/entry
   //    * hub.system.net.sec.sent              8 bytes/entry
   //    * hub.system.load.1min                 2 bytes/entry
   //    * hub.system.cpu.user                  1 byte/entry
   //    * hub.system.cpu.system                1 byte/entry
   //    * hub.system.cpu.iowait                1 byte/entry
   //    * hub.system.cpu.idle                  1 byte/entry
   //    * 
   //    * hub.process.agent.mem.heap.used      8 bytes/entry
   //    * hub.process.agent.mem.nonheap.used   8 bytes/entry
   //    ----------------------------------------------
   //      total                                78 bytes/entry
   //
   // This current setup consumes approximately 110 KB of memory for storage.
   private static final int LONGTERM_BUFFER_SIZE = 1440;
   private final long LONGTERM_SAMPLE_FREQUENCY = TimeUnit.SECONDS.toMillis(60);

   private final SystemInfo system = new SystemInfo();
   private final HardwareAbstractionLayer hal = system.getHardware();
   private final OperatingSystem os = system.getOperatingSystem();
   private final CentralProcessor cpu = hal.getProcessor();

   private final IrisMetricSet SYSTEM_METRICS = IrisMetrics.metrics("hub.system");
   private final IrisMetricSet PROCESS_METRICS = IrisMetrics.metrics("hub.process");

   private final Map<String,MetricsJob> jobs = new HashMap<>();
   private final LongTermMetricSampler longTermSampler;
   private final AggregatedMetricsReporter aggMetricReporter;

   private final File dataPath = StorageService.getFile("data:///");

   private final HubAttributesService.Attribute<List<String>> METRICSJOBS = HubAttributesService.computedList(String.class, HubMetricsCapability.ATTR_METRICSJOBS, () -> ImmutableList.copyOf(jobs.keySet()));

   private MetricsHandler() {
      //////////////////////////////////////////////////////////////////////////
      // Register the system level metrics with the metrics registry
      //////////////////////////////////////////////////////////////////////////
      
      SYSTEM_METRICS.gauge("mem.total", sysMemoryTotal);
      SYSTEM_METRICS.gauge("mem.free", sysMemoryFree);

      SYSTEM_METRICS.gauge("load.1min", sysLoadAverage1Min);
      SYSTEM_METRICS.gauge("load.5min", sysLoadAverage5Min);
      SYSTEM_METRICS.gauge("load.15min", sysLoadAverage15Min);

      SYSTEM_METRICS.gauge("cpu.user", sysCpuUser);
      SYSTEM_METRICS.gauge("cpu.nice", sysCpuNice);
      SYSTEM_METRICS.gauge("cpu.system", sysCpuSystem);
      SYSTEM_METRICS.gauge("cpu.idle", sysCpuIdle);
      SYSTEM_METRICS.gauge("cpu.iowait", sysCpuIoWait);
      SYSTEM_METRICS.gauge("cpu.irq", sysCpuIrq);
      SYSTEM_METRICS.gauge("cpu.softirq", sysCpuSoftIrq);

      SYSTEM_METRICS.gauge("net.pri.recv", sysPrimaryNetworkRecv);
      SYSTEM_METRICS.gauge("net.pri.sent", sysPrimaryNetworkSent);
      SYSTEM_METRICS.gauge("net.pri.inerr", sysPrimaryNetworkInErr);
      SYSTEM_METRICS.gauge("net.pri.outerr", sysPrimaryNetworkOutErr);
      SYSTEM_METRICS.gauge("net.sec.recv", sysSecondaryNetworkRecv);
      SYSTEM_METRICS.gauge("net.sec.sent", sysSecondaryNetworkSent);
      SYSTEM_METRICS.gauge("net.sec.inerr", sysSecondaryNetworkInErr);
      SYSTEM_METRICS.gauge("net.sec.outerr", sysSecondaryNetworkOutErr);

      SYSTEM_METRICS.gauge("disk.data.read", sysDataDiskRead);
      SYSTEM_METRICS.gauge("disk.data.write", sysDataDiskWrite);
      SYSTEM_METRICS.gauge("disk.data.time", sysDataDiskTime);
      SYSTEM_METRICS.gauge("disk.data.total", sysDataDiskTotal);
      SYSTEM_METRICS.gauge("disk.data.free", sysDataDiskFree);
   
      PROCESS_METRICS.gauge("agent.rss", procAgentRss);
      PROCESS_METRICS.gauge("agent.vsz", procAgentVsz);
      PROCESS_METRICS.gauge("agent.time.kernel", procAgentKernelTime);
      PROCESS_METRICS.gauge("agent.time.user", procAgentUserTime);
      PROCESS_METRICS.gauge("agent.disk.read", procAgentDiskRead);
      PROCESS_METRICS.gauge("agent.disk.write", procAgentDiskWrite);

      PROCESS_METRICS.gauge("agent.mem.heap.max", procAgentHeapUsageMax);
      PROCESS_METRICS.gauge("agent.mem.heap.used", procAgentHeapUsageUsed);
      PROCESS_METRICS.gauge("agent.mem.heap.committed", procAgentHeapUsageCommitted);
      PROCESS_METRICS.gauge("agent.mem.nonheap.max", procAgentNonHeapUsageMax);
      PROCESS_METRICS.gauge("agent.mem.nonheap.used", procAgentNonHeapUsageUsed);
      PROCESS_METRICS.gauge("agent.mem.nonheap.committed", procAgentNonHeapUsageCommitted);

      PROCESS_METRICS.gauge("agent.mem.code.cache.max", procAgentMemoryCodeCacheMax);
      PROCESS_METRICS.gauge("agent.mem.code.cache.used", procAgentMemoryCodeCacheUsed);
      PROCESS_METRICS.gauge("agent.mem.meta.space.max", procAgentMemoryMetaSpaceMax);
      PROCESS_METRICS.gauge("agent.mem.meta.space.used", procAgentMemoryMetaSpaceUsed);
      PROCESS_METRICS.gauge("agent.mem.eden.space.max", procAgentMemoryEdenSpaceMax);
      PROCESS_METRICS.gauge("agent.mem.eden.space.used", procAgentMemoryEdenSpaceUsed);
      PROCESS_METRICS.gauge("agent.mem.survivor.space.max", procAgentMemorySurvivorSpaceMax);
      PROCESS_METRICS.gauge("agent.mem.survivor.space.used", procAgentMemorySurvivorSpaceUsed);
      PROCESS_METRICS.gauge("agent.mem.tenured.max", procAgentMemoryTenuredMax);
      PROCESS_METRICS.gauge("agent.mem.tenured.used", procAgentMemoryTenuredUsed);

      PROCESS_METRICS.gauge("agent.threads.total", procAgentThreadCount);
      PROCESS_METRICS.gauge("agent.threads.runnable", procAgentThreadRunnableCount);
      PROCESS_METRICS.gauge("agent.threads.blocked", procAgentThreadBlockedCount);
      PROCESS_METRICS.gauge("agent.threads.waiting", procAgentThreadWaitingCount);
      PROCESS_METRICS.gauge("agent.threads.timed_waiting", procAgentThreadTimedWaitingCount);

      PROCESS_METRICS.gauge("agent.gc.compilationtime", procAgentCompilationTime);
      PROCESS_METRICS.gauge("agent.gc.copy.count", procAgentGcCopyCount);
      PROCESS_METRICS.gauge("agent.gc.copy.time", procAgentGcCopyTime);
      PROCESS_METRICS.gauge("agent.gc.marksweep.count", procAgentGcMarkSweepCount);
      PROCESS_METRICS.gauge("agent.gc.marksweep.time", procAgentGcMarkSweepTime);

      //////////////////////////////////////////////////////////////////////////
      // Setup and start the long term metrics store
      //////////////////////////////////////////////////////////////////////////
      
      LongTermMetricSampler sampler = new LongTermMetricSamplerBuilder()
         .addLongMetric("hub.system.mem.free", sysMemoryFree)
         .addFixed6x10Metric("hub.system.load.1min", sysLoadAverage1Min)
         .addPercentageMetric("hub.system.cpu.user", sysCpuUserStore)
         .addPercentageMetric("hub.system.cpu.system", sysCpuSystemStore)
         .addPercentageMetric("hub.system.cpu.iowait", sysCpuIoWaitStore)
         .addPercentageMetric("hub.system.cpu.idle", sysCpuIdleStore)
         .addLongMetric("hub.system.disk.data.free", sysDataDiskFree)
         .addLongMetric("hub.system.net.pri.recv", sysPrimaryNetworkRecv)
         .addLongMetric("hub.system.net.pri.sent", sysPrimaryNetworkSent)
         .addLongMetric("hub.system.net.sec.recv", sysSecondaryNetworkRecv)
         .addLongMetric("hub.system.net.sec.sent", sysSecondaryNetworkSent)
         .addLongMetric("hub.process.agent.mem.heap.used", procAgentHeapUsageUsed)
         .addLongMetric("hub.process.agent.mem.nonheap.used", procAgentNonHeapUsageUsed)
         .build(LONGTERM_BUFFER_SIZE);

      ExecService.periodic().scheduleAtFixedRate(sampler, LONGTERM_SAMPLE_FREQUENCY, LONGTERM_SAMPLE_FREQUENCY, TimeUnit.MILLISECONDS);
      this.longTermSampler = sampler;

      //////////////////////////////////////////////////////////////////////////
      // Register the system level aggregated metrics
      //////////////////////////////////////////////////////////////////////////
      
      MetricsService.registerAggregatedMetricFast("hub.system.mem.free", sysMemoryFree);
      MetricsService.registerAggregatedMetricFast("hub.system.load.15min", sysLoadAverage15Min);
      MetricsService.registerAggregatedMetricFast("hub.system.cpu.user", sysCpuUserAgg);
      MetricsService.registerAggregatedMetricFast("hub.system.cpu.system", sysCpuSystemAgg);
      MetricsService.registerAggregatedMetricFast("hub.system.cpu.iowait", sysCpuIoWaitAgg);
      MetricsService.registerAggregatedMetricFast("hub.system.cpu.idle", sysCpuIdleAgg);

      MetricsService.registerAggregatedMetricMedium("hub.system.net.pri.inerr", sysPrimaryNetworkInErr);
      MetricsService.registerAggregatedMetricMedium("hub.system.net.pri.outerr", sysPrimaryNetworkOutErr);

      MetricsService.registerAggregatedMetricSlow("hub.system.disk.data.free", sysDataDiskFree);

      this.aggMetricReporter = new AggregatedMetricsReporter();
   }

   void start(Port parent) {
      parent.delegate(
         this,
         HubMetricsCapability.StartMetricsJobRequest.NAME,
         HubMetricsCapability.EndMetricsJobsRequest.NAME,
         HubMetricsCapability.ListMetricsRequest.NAME,
         HubMetricsCapability.GetMetricsJobInfoRequest.NAME,
         HubMetricsCapability.GetStoredMetricsRequest.NAME
      );

      aggMetricReporter.start();
   }

   void shutdown() {
      aggMetricReporter.stop();
      for (MetricsJob job : jobs.values()) {
         job.stop();
      }
   }

   @Nullable
   @Override
   public Object recv(Port port, PlatformMessage message) throws Exception {
      switch (message.getMessageType()) {
      case HubMetricsCapability.StartMetricsJobRequest.NAME:
         return handleStartMetricsJob(message.getValue());

      case HubMetricsCapability.EndMetricsJobsRequest.NAME:
         return handleEndMetricsJob(message.getValue());

      case HubMetricsCapability.ListMetricsRequest.NAME:
         return handleListMetrics(message.getValue());

      case HubMetricsCapability.GetMetricsJobInfoRequest.NAME:
         return handleGetMetricsJobInfo(message.getValue());

      case HubMetricsCapability.GetStoredMetricsRequest.NAME:
         return handleGetStoredMetrics(message.getValue());

      default:
         throw new Exception("cannot handle message");
      }
   }

   @Override
   public void recv(Port port, ProtocolMessage message) {
   }

   @Override
   public void recv(Port port, Object message) {
   }

   private Object handleStartMetricsJob(MessageBody msg) {
      MetricsJob job = new MetricsJob(msg);
      MetricsJob old = jobs.put(job.name, job);
      if (old != null) {
         old.stop();
      }

      METRICSJOBS.poke();
      job.start();

      return HubMetricsCapability.StartMetricsJobResponse.instance();
   }

   private Object handleEndMetricsJob(MessageBody msg) {
      MetricsJob old = jobs.remove(HubMetricsCapability.EndMetricsJobsRequest.getJobname(msg));
      if (old != null) {
         old.stop();
         METRICSJOBS.poke();
      }

      return HubMetricsCapability.EndMetricsJobsResponse.instance();
   }

   private Object handleListMetrics(MessageBody msg) {
      String regex = HubMetricsCapability.ListMetricsRequest.getRegex(msg);
      if (StringUtils.isBlank(regex)) {
         regex = ".*";
      }

      Pattern p = Pattern.compile("^" + regex + "$");
      Predicate<String> matcher = (m) -> p.matcher(m).matches();

      ImmutableList.Builder<String> bld = ImmutableList.builder();
      for (String name : IrisMetrics.registry().getNames()) {
         if (matcher.test(name)) {
            bld.add(name);
         }
      }

      return HubMetricsCapability.ListMetricsResponse.builder()
         .withMetrics(bld.build())
         .build();
   }

   private Object handleGetMetricsJobInfo(MessageBody msg) {
      String jobName = HubMetricsCapability.GetMetricsJobInfoRequest.getJobname(msg);

      MetricsJob job = jobs.get(jobName);
      if (job == null) {
         return Errors.invalidParam("jobname");
      }

      long remainingNs = job.end - System.nanoTime();
      long remainingMs = Math.max(0, TimeUnit.NANOSECONDS.toMillis(remainingNs));
      return HubMetricsCapability.GetMetricsJobInfoResponse.builder()
         .withPeriodMs(job.period)
         .withRemainingDurationMs(remainingMs)
         .build();
   }

   private Object handleGetStoredMetrics(MessageBody msg) throws IOException {
      String json = JSON.toJson(longTermSampler.metrics());

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (GZIPOutputStream out = new GZIPOutputStream(baos)) {
         IOUtils.write(json, out, StandardCharsets.UTF_8);
      }
      
      String result = Base64.encodeBase64String(baos.toByteArray());
      return HubMetricsCapability.GetStoredMetricsResponse.builder()
         .withMetrics(result)
         .build();
   }

   ///////////////////////////////////////////////////////////////////////////////
   // System Memory Metrics
   ///////////////////////////////////////////////////////////////////////////////

   private Gauge<GlobalMemory> sysGlobalMemory = new Gauge<GlobalMemory>() {
      @Override
      public GlobalMemory getValue() {
         return hal.getMemory();
      }
   };

   private Gauge<Long> sysMemoryTotal = new DerivativeGauge<GlobalMemory,Long>(sysGlobalMemory) {
      @Override
      public @Nullable Long transform(@Nullable GlobalMemory mem) {
         return (mem == null) ? null : mem.getTotal();
      }
   };

   private Gauge<Long> sysMemoryFree = new DerivativeGauge<GlobalMemory,Long>(sysGlobalMemory) {
      @Override
      public @Nullable Long transform(@Nullable GlobalMemory mem) {
         return (mem == null) ? null : mem.getAvailable();
      }
   };

   ///////////////////////////////////////////////////////////////////////////////
   // System CPU Metrics
   ///////////////////////////////////////////////////////////////////////////////
   
   private Gauge<CentralProcessor> sysCentralProcessor = new Gauge<CentralProcessor>() {
      @Override
      public CentralProcessor getValue() {
         return cpu;
      }
   };

   private Gauge<double[]> sysLoadAverages = new CachedDerivativeGauge<CentralProcessor,double[]>(sysCentralProcessor, 950, TimeUnit.MILLISECONDS) {
      @Override
      public @Nullable double[] transform(@Nullable CentralProcessor cpu) {
         return (cpu == null) ? null : cpu.getSystemLoadAverage(3);
      }
   };

   private Gauge<Double> sysLoadAverage1Min = new DerivativeGauge<double[],Double>(sysLoadAverages) {
      @Override
      public @Nullable Double transform(@Nullable double[] load) {
         return (load == null) ? null : load[0];
      }
   };

   private Gauge<Double> sysLoadAverage5Min = new DerivativeGauge<double[],Double>(sysLoadAverages) {
      @Override
      public @Nullable Double transform(@Nullable double[] load) {
         return (load == null) ? null : load[1];
      }
   };

   private Gauge<Double> sysLoadAverage15Min = new DerivativeGauge<double[],Double>(sysLoadAverages) {
      @Override
      public @Nullable Double transform(@Nullable double[] load) {
         return (load == null) ? null : load[2];
      }
   };

   private static final class CpuTicksGauge extends CachedDerivativeGauge<CentralProcessor,CpuTicks> {
      private @Nullable long[] prevCpuTicks = null;

      public CpuTicksGauge(Gauge<CentralProcessor> processor, long cacheTime, TimeUnit unit) {
         super(processor, cacheTime, unit);
      }

      @Override
      public @Nullable CpuTicks transform(@Nullable CentralProcessor cpu) {
         long[] ticks = (cpu == null) ? null : cpu.getSystemCpuLoadTicks();
         long[] prev = prevCpuTicks;

         if (prev != null && ticks != null) {
            long user = ticks[TickType.USER.getIndex()] - prev[TickType.USER.getIndex()];
            long nice = ticks[TickType.NICE.getIndex()] - prev[TickType.NICE.getIndex()];
            long sys = ticks[TickType.SYSTEM.getIndex()] - prev[TickType.SYSTEM.getIndex()];
            long idle = ticks[TickType.IDLE.getIndex()] - prev[TickType.IDLE.getIndex()];
            long iowait = ticks[TickType.IOWAIT.getIndex()] - prev[TickType.IOWAIT.getIndex()];
            long irq = ticks[TickType.IRQ.getIndex()] - prev[TickType.IRQ.getIndex()];
            long softirq = ticks[TickType.SOFTIRQ.getIndex()] - prev[TickType.SOFTIRQ.getIndex()];
            long steal = ticks[TickType.STEAL.getIndex()] - prev[TickType.STEAL.getIndex()];
            double total = user + nice + sys + idle + iowait + irq + softirq + steal;

            double cpuUser = user / total;
            double cpuNice = nice / total;
            double cpuSystem = sys / total;
            double cpuIdle = idle / total;
            double cpuIoWait = iowait / total;
            double cpuIrq = irq / total;
            double cpuSoftIrq = softirq / total;
            double cpuSteal = steal / total;

            prevCpuTicks = ticks;
            return new CpuTicks(cpuUser, cpuNice, cpuSystem, cpuIdle, cpuIoWait, cpuIrq, cpuSoftIrq, cpuSteal);
         }

         prevCpuTicks = ticks;
         return null;
      }
   };
   
   private static final class CpuUserGauge extends DerivativeGauge<CpuTicks,Double> {
      public CpuUserGauge(Gauge<CpuTicks> ticks) {
         super(ticks);
      }

      @Override
      public @Nullable Double transform(@Nullable CpuTicks ticks) {
         return (ticks == null) ? null : ticks.cpuUser;
      }
   }
   
   private static final class CpuNiceGauge extends DerivativeGauge<CpuTicks,Double> {
      public CpuNiceGauge(Gauge<CpuTicks> ticks) {
         super(ticks);
      }

      @Override
      public @Nullable Double transform(@Nullable CpuTicks ticks) {
         return (ticks == null) ? null : ticks.cpuNice;
      }
   }
   
   private static final class CpuSystemGauge extends DerivativeGauge<CpuTicks,Double> {
      public CpuSystemGauge(Gauge<CpuTicks> ticks) {
         super(ticks);
      }

      @Override
      public @Nullable Double transform(@Nullable CpuTicks ticks) {
         return (ticks == null) ? null : ticks.cpuSystem;
      }
   }
   
   private static final class CpuIdleGauge extends DerivativeGauge<CpuTicks,Double> {
      public CpuIdleGauge(Gauge<CpuTicks> ticks) {
         super(ticks);
      }

      @Override
      public @Nullable Double transform(@Nullable CpuTicks ticks) {
         return (ticks == null) ? null : ticks.cpuIdle;
      }
   }
   
   private static final class CpuIoWaitGauge extends DerivativeGauge<CpuTicks,Double> {
      public CpuIoWaitGauge(Gauge<CpuTicks> ticks) {
         super(ticks);
      }

      @Override
      public @Nullable Double transform(@Nullable CpuTicks ticks) {
         return (ticks == null) ? null : ticks.cpuIoWait;
      }
   }
   
   private static final class CpuIrqGauge extends DerivativeGauge<CpuTicks,Double> {
      public CpuIrqGauge(Gauge<CpuTicks> ticks) {
         super(ticks);
      }

      @Override
      public @Nullable Double transform(@Nullable CpuTicks ticks) {
         return (ticks == null) ? null : ticks.cpuIrq;
      }
   }
   
   private static final class CpuSoftIrqGauge extends DerivativeGauge<CpuTicks,Double> {
      public CpuSoftIrqGauge(Gauge<CpuTicks> ticks) {
         super(ticks);
      }

      @Override
      public @Nullable Double transform(@Nullable CpuTicks ticks) {
         return (ticks == null) ? null : ticks.cpuSoftIrq;
      }
   }

   private Gauge<CpuTicks> sysCpuTicks = new CpuTicksGauge(sysCentralProcessor, 950, TimeUnit.MILLISECONDS);
   private Gauge<CpuTicks> sysCpuTicksAgg = new CpuTicksGauge(sysCentralProcessor, 950, TimeUnit.MILLISECONDS);
   private Gauge<CpuTicks> sysCpuTicksStore = new CpuTicksGauge(sysCentralProcessor, 950, TimeUnit.MILLISECONDS);

   private Gauge<Double> sysCpuUser = new CpuUserGauge(sysCpuTicks);
   private Gauge<Double> sysCpuNice = new CpuNiceGauge(sysCpuTicks);
   private Gauge<Double> sysCpuSystem = new CpuSystemGauge(sysCpuTicks);
   private Gauge<Double> sysCpuIdle = new CpuIdleGauge(sysCpuTicks);
   private Gauge<Double> sysCpuIoWait = new CpuIoWaitGauge(sysCpuTicks);
   private Gauge<Double> sysCpuIrq = new CpuIrqGauge(sysCpuTicks);
   private Gauge<Double> sysCpuSoftIrq = new CpuSoftIrqGauge(sysCpuTicks);

   private Gauge<Double> sysCpuUserAgg = new CpuUserGauge(sysCpuTicksAgg);
   private Gauge<Double> sysCpuNiceAgg = new CpuNiceGauge(sysCpuTicksAgg);
   private Gauge<Double> sysCpuSystemAgg = new CpuSystemGauge(sysCpuTicksAgg);
   private Gauge<Double> sysCpuIdleAgg = new CpuIdleGauge(sysCpuTicksAgg);
   private Gauge<Double> sysCpuIoWaitAgg = new CpuIoWaitGauge(sysCpuTicksAgg);
   private Gauge<Double> sysCpuIrqAgg = new CpuIrqGauge(sysCpuTicksAgg);
   private Gauge<Double> sysCpuSoftIrqAgg = new CpuSoftIrqGauge(sysCpuTicksAgg);

   private Gauge<Double> sysCpuUserStore = new CpuUserGauge(sysCpuTicksStore);
   private Gauge<Double> sysCpuNiceStore = new CpuNiceGauge(sysCpuTicksStore);
   private Gauge<Double> sysCpuSystemStore = new CpuSystemGauge(sysCpuTicksStore);
   private Gauge<Double> sysCpuIdleStore = new CpuIdleGauge(sysCpuTicksStore);
   private Gauge<Double> sysCpuIoWaitStore = new CpuIoWaitGauge(sysCpuTicksStore);
   private Gauge<Double> sysCpuIrqStore = new CpuIrqGauge(sysCpuTicksStore);
   private Gauge<Double> sysCpuSoftIrqStore = new CpuSoftIrqGauge(sysCpuTicksStore);

   private static final class CpuTicks {
      private final double cpuUser;
      private final double cpuNice;
      private final double cpuSystem;
      private final double cpuIdle;
      private final double cpuIoWait;
      private final double cpuIrq;
      private final double cpuSoftIrq;
      private final double cpuSteal;

      public CpuTicks(double cpuUser, double cpuNice, double cpuSystem, double cpuIdle, double cpuIoWait, double cpuIrq, double cpuSoftIrq, double cpuSteal) {
         this.cpuUser = cpuUser;
         this.cpuNice = cpuNice;
         this.cpuSystem = cpuSystem;
         this.cpuIdle = cpuIdle;
         this.cpuIoWait = cpuIoWait;
         this.cpuIrq = cpuIrq;
         this.cpuSoftIrq = cpuSoftIrq;
         this.cpuSteal = cpuSteal;
      }
   }

   ///////////////////////////////////////////////////////////////////////////////
   // System Disk Metrics
   ///////////////////////////////////////////////////////////////////////////////
   
   private Gauge<Map<String,HWDiskStore>> sysDiskStore = new CachedGauge<Map<String,HWDiskStore>>(950, TimeUnit.MILLISECONDS) {
      @Override
      public Map<String,HWDiskStore> loadValue() {
         ImmutableMap.Builder<String,HWDiskStore> result = ImmutableMap.builder();
         for (HWDiskStore disk : hal.getDiskStores()) {
            result.put(disk.getName(), disk);
         }

         return result.build();
      }
   };

   private Gauge<HWDiskStore> sysDataDisk = new DerivativeGauge<Map<String,HWDiskStore>,HWDiskStore>(sysDiskStore) {
      @Override
      public @Nullable HWDiskStore transform(@Nullable Map<String,HWDiskStore> networks) {
         return (networks == null) ? null : networks.get(IrisHal.getDataDiskName());
      }
   };

   private Gauge<Long> sysDataDiskRead = new DerivativeGauge<HWDiskStore,Long>(sysDataDisk) {
      @Override
      public @Nullable Long transform(@Nullable HWDiskStore disk) {
         return (disk == null) ? 0 : disk.getReadBytes();
      }
   };

   private Gauge<Long> sysDataDiskWrite = new DerivativeGauge<HWDiskStore,Long>(sysDataDisk) {
      @Override
      public @Nullable Long transform(@Nullable HWDiskStore disk) {
         return (disk == null) ? 0 : disk.getWriteBytes();
      }
   };

   private Gauge<Long> sysDataDiskTime = new DerivativeGauge<HWDiskStore,Long>(sysDataDisk) {
      @Override
      public @Nullable Long transform(@Nullable HWDiskStore disk) {
         return (disk == null) ? 0 : disk.getTransferTime();
      }
   };

   private Gauge<Long> sysDataDiskTotal = new DerivativeGauge<HWDiskStore,Long>(sysDataDisk) {
      @Override
      public @Nullable Long transform(@Nullable HWDiskStore disk) {
         return dataPath.getTotalSpace();
      }
   };

   private Gauge<Long> sysDataDiskFree = new DerivativeGauge<HWDiskStore,Long>(sysDataDisk) {
      @Override
      public @Nullable Long transform(@Nullable HWDiskStore disk) {
         return dataPath.getUsableSpace();
      }
   };

   ///////////////////////////////////////////////////////////////////////////////
   // System Network Metrics
   ///////////////////////////////////////////////////////////////////////////////
   
   private Gauge<Map<String,NetworkIF>> sysNetworks = new CachedGauge<Map<String,NetworkIF>>(950, TimeUnit.MILLISECONDS) {
      @Override
      public Map<String,NetworkIF> loadValue() {
         ImmutableMap.Builder<String,NetworkIF> result = ImmutableMap.builder();
         for (NetworkIF nif : hal.getNetworkIFs()) {
            result.put(nif.getName(), nif);
         }

         return result.build();
      }
   };

   private Gauge<NetworkIF> sysPrimaryNetwork = new DerivativeGauge<Map<String,NetworkIF>,NetworkIF>(sysNetworks) {
      @Override
      public @Nullable NetworkIF transform(@Nullable Map<String,NetworkIF> networks) {
         return (networks == null) ? null : networks.get(IrisHal.getPrimaryNetworkInterfaceName());
      }
   };

   private Gauge<Long> sysPrimaryNetworkRecv = new DerivativeGauge<NetworkIF,Long>(sysPrimaryNetwork) {
      @Override
      public @Nullable Long transform(@Nullable NetworkIF network) {
         return (network == null) ? 0 : network.getBytesRecv();
      }
   };

   private Gauge<Long> sysPrimaryNetworkSent = new DerivativeGauge<NetworkIF,Long>(sysPrimaryNetwork) {
      @Override
      public @Nullable Long transform(@Nullable NetworkIF network) {
         return (network == null) ? 0 : network.getBytesSent();
      }
   };

   private Gauge<Long> sysPrimaryNetworkInErr = new DerivativeGauge<NetworkIF,Long>(sysPrimaryNetwork) {
      @Override
      public @Nullable Long transform(@Nullable NetworkIF network) {
         return (network == null) ? 0 : network.getInErrors();
      }
   };

   private Gauge<Long> sysPrimaryNetworkOutErr = new DerivativeGauge<NetworkIF,Long>(sysPrimaryNetwork) {
      @Override
      public @Nullable Long transform(@Nullable NetworkIF network) {
         return (network == null) ? 0 : network.getOutErrors();
      }
   };

   private Gauge<NetworkIF> sysSecondaryNetwork = new DerivativeGauge<Map<String,NetworkIF>,NetworkIF>(sysNetworks) {
      @Override
      public @Nullable NetworkIF transform(@Nullable Map<String,NetworkIF> networks) {
         return (networks == null) ? null : networks.get(IrisHal.getSecondaryNetworkInterfaceName());
      }
   };

   private Gauge<Long> sysSecondaryNetworkRecv = new DerivativeGauge<NetworkIF,Long>(sysSecondaryNetwork) {
      @Override
      public @Nullable Long transform(@Nullable NetworkIF network) {
         return (network == null) ? 0 : network.getBytesRecv();
      }
   };

   private Gauge<Long> sysSecondaryNetworkSent = new DerivativeGauge<NetworkIF,Long>(sysSecondaryNetwork) {
      @Override
      public @Nullable Long transform(@Nullable NetworkIF network) {
         return (network == null) ? 0 : network.getBytesSent();
      }
   };

   private Gauge<Long> sysSecondaryNetworkInErr = new DerivativeGauge<NetworkIF,Long>(sysSecondaryNetwork) {
      @Override
      public @Nullable Long transform(@Nullable NetworkIF network) {
         return (network == null) ? 0 : network.getInErrors();
      }
   };

   private Gauge<Long> sysSecondaryNetworkOutErr = new DerivativeGauge<NetworkIF,Long>(sysSecondaryNetwork) {
      @Override
      public @Nullable Long transform(@Nullable NetworkIF network) {
         return (network == null) ? 0 : network.getOutErrors();
      }
   };

   ///////////////////////////////////////////////////////////////////////////////
   // Agent Process Metrics
   ///////////////////////////////////////////////////////////////////////////////

   private Gauge<OSProcess> procAgent = new CachedGauge<OSProcess>(950, TimeUnit.MILLISECONDS) {
      @Override
      public OSProcess loadValue() {
         return os.getProcess(os.getProcessId());
      }
   };

   private Gauge<Long> procAgentRss = new DerivativeGauge<OSProcess,Long>(procAgent) {
      @Override
      public @Nullable Long transform(@Nullable OSProcess proc) {
         return (proc == null) ? null : proc.getResidentSetSize();
      }
   };

   private Gauge<Long> procAgentVsz = new DerivativeGauge<OSProcess,Long>(procAgent) {
      @Override
      public @Nullable Long transform(@Nullable OSProcess proc) {
         return (proc == null) ? null : proc.getVirtualSize();
      }
   };

   private Gauge<Long> procAgentKernelTime = new DerivativeGauge<OSProcess,Long>(procAgent) {
      @Override
      public @Nullable Long transform(@Nullable OSProcess proc) {
         return (proc == null) ? null : proc.getKernelTime();
      }
   };

   private Gauge<Long> procAgentUserTime = new DerivativeGauge<OSProcess,Long>(procAgent) {
      @Override
      public @Nullable Long transform(@Nullable OSProcess proc) {
         return (proc == null) ? null : proc.getUserTime();
      }
   };

   private Gauge<Long> procAgentDiskRead = new DerivativeGauge<OSProcess,Long>(procAgent) {
      @Override
      public @Nullable Long transform(@Nullable OSProcess proc) {
         return (proc == null) ? null : proc.getBytesRead();
      }
   };

   private Gauge<Long> procAgentDiskWrite = new DerivativeGauge<OSProcess,Long>(procAgent) {
      @Override
      public @Nullable Long transform(@Nullable OSProcess proc) {
         return (proc == null) ? null : proc.getBytesWritten();
      }
   };

   ///////////////////////////////////////////////////////////////////////////////
   // Agent Memory Metrics
   ///////////////////////////////////////////////////////////////////////////////
   
   private final MemoryMXBean javaMemoryBean = ManagementFactory.getMemoryMXBean();
   private final ThreadMXBean javaThreadBean = ManagementFactory.getThreadMXBean();
   private final CompilationMXBean javaCompilationBean = ManagementFactory.getCompilationMXBean();
   private final List<GarbageCollectorMXBean> javaGcBeans = ManagementFactory.getGarbageCollectorMXBeans();
   private final List<MemoryPoolMXBean> javaMemoryPoolBeans = ManagementFactory.getMemoryPoolMXBeans();

   private Gauge<MemoryUsage> procAgentHeapUsage = new CachedGauge<MemoryUsage>(950, TimeUnit.MILLISECONDS) {
      @Override
      public @Nullable MemoryUsage loadValue() {
         return javaMemoryBean.getHeapMemoryUsage();
      }
   };

   private Gauge<MemoryUsage> procAgentNonHeapUsage = new CachedGauge<MemoryUsage>(950, TimeUnit.MILLISECONDS) {
      @Override
      public @Nullable MemoryUsage loadValue() {
         return javaMemoryBean.getNonHeapMemoryUsage();
      }
   };

   private Gauge<Long> procAgentHeapUsageMax = new DerivativeGauge<MemoryUsage,Long>(procAgentHeapUsage) {
      @Override
      public @Nullable Long transform(@Nullable MemoryUsage usage) {
         return (usage == null) ? null : usage.getMax();
      }
   };

   private Gauge<Long> procAgentHeapUsageUsed = new DerivativeGauge<MemoryUsage,Long>(procAgentHeapUsage) {
      @Override
      public @Nullable Long transform(@Nullable MemoryUsage usage) {
         return (usage == null) ? null : usage.getUsed();
      }
   };

   private Gauge<Long> procAgentHeapUsageCommitted = new DerivativeGauge<MemoryUsage,Long>(procAgentHeapUsage) {
      @Override
      public @Nullable Long transform(@Nullable MemoryUsage usage) {
         return (usage == null) ? null : usage.getCommitted();
      }
   };

   private Gauge<Long> procAgentNonHeapUsageMax = new DerivativeGauge<MemoryUsage,Long>(procAgentNonHeapUsage) {
      @Override
      public @Nullable Long transform(@Nullable MemoryUsage usage) {
         return (usage == null) ? null : usage.getMax();
      }
   };

   private Gauge<Long> procAgentNonHeapUsageUsed = new DerivativeGauge<MemoryUsage,Long>(procAgentNonHeapUsage) {
      @Override
      public @Nullable Long transform(@Nullable MemoryUsage usage) {
         return (usage == null) ? null : usage.getUsed();
      }
   };

   private Gauge<Long> procAgentNonHeapUsageCommitted = new DerivativeGauge<MemoryUsage,Long>(procAgentNonHeapUsage) {
      @Override
      public @Nullable Long transform(@Nullable MemoryUsage usage) {
         return (usage == null) ? null : usage.getCommitted();
      }
   };

   private Gauge<Long> procAgentCompilationTime = new Gauge<Long>() {
      @Override
      public @Nullable Long getValue() {
         return javaCompilationBean.getTotalCompilationTime();
      }
   };

   private Gauge<MemInfo> procAgentMemInfo = new CachedGauge<MemInfo>(950, TimeUnit.MILLISECONDS) {
      @Override
      public @Nullable MemInfo loadValue() {
         long codeCacheUsed = 0;
         long codeCacheMax = 0;
         long edenSpaceUsed = 0;
         long edenSpaceMax = 0;
         long metaSpaceUsed = 0;
         long metaSpaceMax = 0;
         long survivorSpaceUsed = 0;
         long survivorSpaceMax = 0;
         long tenuredUsed = 0;
         long tenuredMax = 0;

         for (MemoryPoolMXBean bean : javaMemoryPoolBeans) {
            switch (bean.getName()) {
            case "Code Cache":
               MemoryUsage ccusage = bean.getUsage();
               codeCacheUsed = ccusage.getUsed();
               codeCacheMax = ccusage.getMax();
               break;
            case "Metaspace":
               MemoryUsage msusage = bean.getUsage();
               metaSpaceUsed = msusage.getUsed();
               metaSpaceMax = msusage.getMax();
               break;
            case "Eden Space":
               MemoryUsage esusage = bean.getUsage();
               edenSpaceUsed = esusage.getUsed();
               edenSpaceMax = esusage.getMax();
               break;
            case "Survivor Space":
               MemoryUsage ssusage = bean.getUsage();
               survivorSpaceUsed = ssusage.getUsed();
               survivorSpaceMax = ssusage.getMax();
               break;
            case "Tenured Gen":
               MemoryUsage tnusage = bean.getUsage();
               tenuredUsed = tnusage.getUsed();
               tenuredMax = tnusage.getMax();
               break;
            default:
               // ignore
               break;
            }
         }

         return new MemInfo(codeCacheUsed, codeCacheMax, edenSpaceUsed, edenSpaceMax, metaSpaceUsed, metaSpaceMax, survivorSpaceUsed, survivorSpaceMax, tenuredUsed, tenuredMax);
      }
   };

   private Gauge<Long> procAgentMemoryCodeCacheUsed = new DerivativeGauge<MemInfo,Long>(procAgentMemInfo) {
      @Override
      public @Nullable Long transform(@Nullable MemInfo info) {
         return (info == null) ? null : info.codeCacheUsed;
      }
   };

   private Gauge<Long> procAgentMemoryCodeCacheMax = new DerivativeGauge<MemInfo,Long>(procAgentMemInfo) {
      @Override
      public @Nullable Long transform(@Nullable MemInfo info) {
         return (info == null) ? null : info.codeCacheMax;
      }
   };

   private Gauge<Long> procAgentMemoryMetaSpaceUsed = new DerivativeGauge<MemInfo,Long>(procAgentMemInfo) {
      @Override
      public @Nullable Long transform(@Nullable MemInfo info) {
         return (info == null) ? null : info.metaSpaceUsed;
      }
   };

   private Gauge<Long> procAgentMemoryMetaSpaceMax = new DerivativeGauge<MemInfo,Long>(procAgentMemInfo) {
      @Override
      public @Nullable Long transform(@Nullable MemInfo info) {
         return (info == null) ? null : info.metaSpaceMax;
      }
   };

   private Gauge<Long> procAgentMemoryEdenSpaceUsed = new DerivativeGauge<MemInfo,Long>(procAgentMemInfo) {
      @Override
      public @Nullable Long transform(@Nullable MemInfo info) {
         return (info == null) ? null : info.edenSpaceUsed;
      }
   };

   private Gauge<Long> procAgentMemoryEdenSpaceMax = new DerivativeGauge<MemInfo,Long>(procAgentMemInfo) {
      @Override
      public @Nullable Long transform(@Nullable MemInfo info) {
         return (info == null) ? null : info.edenSpaceMax;
      }
   };

   private Gauge<Long> procAgentMemorySurvivorSpaceUsed = new DerivativeGauge<MemInfo,Long>(procAgentMemInfo) {
      @Override
      public @Nullable Long transform(@Nullable MemInfo info) {
         return (info == null) ? null : info.survivorSpaceUsed;
      }
   };

   private Gauge<Long> procAgentMemorySurvivorSpaceMax = new DerivativeGauge<MemInfo,Long>(procAgentMemInfo) {
      @Override
      public @Nullable Long transform(@Nullable MemInfo info) {
         return (info == null) ? null : info.survivorSpaceMax;
      }
   };

   private Gauge<Long> procAgentMemoryTenuredUsed = new DerivativeGauge<MemInfo,Long>(procAgentMemInfo) {
      @Override
      public @Nullable Long transform(@Nullable MemInfo info) {
         return (info == null) ? null : info.tenuredUsed;
      }
   };

   private Gauge<Long> procAgentMemoryTenuredMax = new DerivativeGauge<MemInfo,Long>(procAgentMemInfo) {
      @Override
      public @Nullable Long transform(@Nullable MemInfo info) {
         return (info == null) ? null : info.tenuredMax;
      }
   };

   private Gauge<GcInfo> procAgentGcInfo = new CachedGauge<GcInfo>(950, TimeUnit.MILLISECONDS) {
      @Override
      public @Nullable GcInfo loadValue() {
         long copyCount = 0;
         long copyTime = 0;
         long markSweepCount = 0;
         long markSweepTime = 0;
         for (GarbageCollectorMXBean bean : javaGcBeans) {
            switch (bean.getName()) {
            case "Copy":
               copyCount = bean.getCollectionCount();
               copyTime = bean.getCollectionTime();
               break;
            case "MarkSweep":
               markSweepCount = bean.getCollectionCount();
               markSweepTime = bean.getCollectionTime();
               break;
            default:
               // ignore
               break;
            }
         }

         return new GcInfo(copyCount, copyTime, markSweepCount, markSweepTime);
      }
   };

   private Gauge<Long> procAgentGcCopyCount = new DerivativeGauge<GcInfo,Long>(procAgentGcInfo) {
      @Override
      public @Nullable Long transform(@Nullable GcInfo info) {
         return (info == null) ? null : info.copyCollectionCount;
      }
   };

   private Gauge<Long> procAgentGcCopyTime = new DerivativeGauge<GcInfo,Long>(procAgentGcInfo) {
      @Override
      public @Nullable Long transform(@Nullable GcInfo info) {
         return (info == null) ? null : info.copyCollectionTime;
      }
   };

   private Gauge<Long> procAgentGcMarkSweepCount = new DerivativeGauge<GcInfo,Long>(procAgentGcInfo) {
      @Override
      public @Nullable Long transform(@Nullable GcInfo info) {
         return (info == null) ? null : info.markSweepCollectionCount;
      }
   };

   private Gauge<Long> procAgentGcMarkSweepTime = new DerivativeGauge<GcInfo,Long>(procAgentGcInfo) {
      @Override
      public @Nullable Long transform(@Nullable GcInfo info) {
         return (info == null) ? null : info.markSweepCollectionTime;
      }
   };

   private Gauge<ThreadInfo[]> procAgentThreadInfos = new CachedGauge<ThreadInfo[]>(950, TimeUnit.MILLISECONDS) {
      @Override
      public @Nullable ThreadInfo[] loadValue() {
         return javaThreadBean.getThreadInfo(javaThreadBean.getAllThreadIds());
      }
   };

   private Gauge<ThreadStates> procAgentThreadStates = new DerivativeGauge<ThreadInfo[],ThreadStates>(procAgentThreadInfos) {
      @Override
      public @Nullable ThreadStates transform(@Nullable ThreadInfo[] infos) {
         int total = 0;
         int runnable = 0;
         int blocked = 0;
         int waiting = 0;
         int timedwaiting = 0;

         if (infos != null) {
            total = infos.length;
            for (ThreadInfo info : infos) {
               switch (info.getThreadState()) {
               case RUNNABLE:
                  runnable++;
                  break;
               case BLOCKED:
                  blocked++;
                  break;
               case WAITING:
                  waiting++;
                  break;
               case TIMED_WAITING:
                  timedwaiting++;
                  break;
               default:
                  // ignore
                  break;
               }
            }
         }
         
         return new ThreadStates(total, runnable, blocked, waiting, timedwaiting);
      }
   };

   private Gauge<Integer> procAgentThreadCount = new DerivativeGauge<ThreadStates,Integer>(procAgentThreadStates) {
      @Override
      public @Nullable Integer transform(@Nullable ThreadStates states) {
         return (states == null) ? 0 : states.total;
      }
   };

   private Gauge<Integer> procAgentThreadRunnableCount = new DerivativeGauge<ThreadStates,Integer>(procAgentThreadStates) {
      @Override
      public @Nullable Integer transform(@Nullable ThreadStates states) {
         return (states == null) ? 0 : states.runnable;
      }
   };

   private Gauge<Integer> procAgentThreadBlockedCount = new DerivativeGauge<ThreadStates,Integer>(procAgentThreadStates) {
      @Override
      public @Nullable Integer transform(@Nullable ThreadStates states) {
         return (states == null) ? 0 : states.blocked;
      }
   };

   private Gauge<Integer> procAgentThreadWaitingCount = new DerivativeGauge<ThreadStates,Integer>(procAgentThreadStates) {
      @Override
      public @Nullable Integer transform(@Nullable ThreadStates states) {
         return (states == null) ? 0 : states.waiting;
      }
   };

   private Gauge<Integer> procAgentThreadTimedWaitingCount = new DerivativeGauge<ThreadStates,Integer>(procAgentThreadStates) {
      @Override
      public @Nullable Integer transform(@Nullable ThreadStates states) {
         return (states == null) ? 0 : states.timedwaiting;
      }
   };

   private static final class MemInfo {
      private final long codeCacheUsed;
      private final long codeCacheMax;
      private final long edenSpaceUsed;
      private final long edenSpaceMax;
      private final long metaSpaceUsed;
      private final long metaSpaceMax;
      private final long survivorSpaceUsed;
      private final long survivorSpaceMax;
      private final long tenuredUsed;
      private final long tenuredMax;

      public MemInfo(long codecacheused, long codecachemax, long edenspaceused, long edenspacemax, long metaspaceused, long metaspacemax, long survivorspaceused, long survivorspacemax, long tenuredused, long tenuredmax) {
         codeCacheUsed = codecacheused;
         codeCacheMax = codecachemax;
         edenSpaceUsed = edenspaceused;
         edenSpaceMax = edenspacemax;
         metaSpaceUsed = metaspaceused;
         metaSpaceMax = metaspacemax;
         survivorSpaceUsed = survivorspaceused;
         survivorSpaceMax = survivorspacemax;
         tenuredUsed = tenuredused;
         tenuredMax = tenuredmax;
      }
   }

   private static final class GcInfo {
      private final long copyCollectionCount;
      private final long copyCollectionTime;
      private final long markSweepCollectionCount;
      private final long markSweepCollectionTime;

      public GcInfo(long copyCollectionCount, long copyCollectionTime, long markSweepCollectionCount, long markSweepCollectionTime) {
         this.copyCollectionCount = copyCollectionCount;
         this.copyCollectionTime = copyCollectionTime;
         this.markSweepCollectionCount = markSweepCollectionCount;
         this.markSweepCollectionTime = markSweepCollectionTime;
      }
   }

   private static final class ThreadStates {
      private final int total;
      private final int runnable;
      private final int blocked;
      private final int waiting;
      private final int timedwaiting;

      public ThreadStates(int total, int runnable, int blocked, int waiting, int timedwaiting) {
         this.total = total;
         this.runnable = runnable;
         this.blocked = blocked;
         this.waiting = waiting;
         this.timedwaiting = timedwaiting;
      }
   }

   ///////////////////////////////////////////////////////////////////////////////
   // Helper Classes
   ///////////////////////////////////////////////////////////////////////////////
   
   private static abstract class CachedDerivativeGauge<F,T> extends CachedGauge<T> {
      private final Gauge<F> base;

      private CachedDerivativeGauge(Gauge<F> base, long time, TimeUnit unit) {
         super(time, unit);
         this.base = base;
      }

      @Override
      public T loadValue() {
         return transform(base.getValue());

      }

      protected abstract T transform(F value);
   }

   ///////////////////////////////////////////////////////////////////////////////
   // Long Term Metrics Buffers
   ///////////////////////////////////////////////////////////////////////////////

   private static abstract class LongTermMetric<T extends Number> {
      protected final Gauge<T> gauge;
      private final String name;
      protected T previous;

      protected LongTermMetric(String name, Gauge<T> gauge, T def) {
         this.name = name;
         this.gauge = gauge;
         this.previous = def;
      }

      public String getName() {
         return name;
      }

      public void sample() {
         T latest = gauge.getValue();
         if (latest == null) {
            latest = previous;
         }

         sample(latest);
         previous = latest;
      }

      public int getSize() {
         return getBuffer().getSize();
      }

      public int getCapacity() {
         return getBuffer().getCapacity();
      }

      public int getStartIndex() {
         return getBuffer().getStartIndex();
      }

      protected abstract void sample(T value);
      public abstract AbstractMetricBuffer getBuffer();

      public abstract T getValue(int idx);
      public abstract void appendValue(StringBuilder bld, int idx);
   }

   private static final class ByteLongTermMetric extends LongTermMetric<Byte> {
      private final ByteMetricBuffer buffer;

      public ByteLongTermMetric(String name, Gauge<? extends Number> gauge, int capacity) {
         super(name, new ToByteGauge(gauge), (byte)0);
         this.buffer = new ByteMetricBuffer(capacity);
      }

      @Override
      protected void sample(Byte value) {
         buffer.add(value);
      }

      @Override
      public Byte getValue(int idx) {
         return buffer.get(idx);
      }

      @Override
      public void appendValue(StringBuilder bld, int idx) {
         bld.append(buffer.get(idx));
      }

      @Override
      public AbstractMetricBuffer getBuffer() {
         return buffer;
      }
   }

   private static final class ShortLongTermMetric extends LongTermMetric<Short> {
      private final ShortMetricBuffer buffer;

      public ShortLongTermMetric(String name, Gauge<? extends Number> gauge, int capacity) {
         super(name, new ToShortGauge(gauge), (short)0);
         this.buffer = new ShortMetricBuffer(capacity);
      }

      @Override
      protected void sample(Short value) {
         buffer.add(value);
      }

      @Override
      public Short getValue(int idx) {
         return buffer.get(idx);
      }

      @Override
      public void appendValue(StringBuilder bld, int idx) {
         bld.append(buffer.get(idx));
      }

      @Override
      public AbstractMetricBuffer getBuffer() {
         return buffer;
      }
   }

   private static final class IntLongTermMetric extends LongTermMetric<Integer> {
      private final IntMetricBuffer buffer;

      public IntLongTermMetric(String name, Gauge<? extends Number> gauge, int capacity) {
         super(name, new ToIntGauge(gauge), 0);
         this.buffer = new IntMetricBuffer(capacity);
      }

      @Override
      protected void sample(Integer value) {
         buffer.add(value);
      }

      @Override
      public Integer getValue(int idx) {
         return buffer.get(idx);
      }

      @Override
      public void appendValue(StringBuilder bld, int idx) {
         bld.append(buffer.get(idx));
      }

      @Override
      public AbstractMetricBuffer getBuffer() {
         return buffer;
      }
   }

   private static final class LongLongTermMetric extends LongTermMetric<Long> {
      private final LongMetricBuffer buffer;

      public LongLongTermMetric(String name, Gauge<? extends Number> gauge, int capacity) {
         super(name, new ToLongGauge(gauge), 0L);
         this.buffer = new LongMetricBuffer(capacity);
      }

      @Override
      protected void sample(Long value) {
         buffer.add(value);
      }

      @Override
      public Long getValue(int idx) {
         return buffer.get(idx);
      }

      @Override
      public void appendValue(StringBuilder bld, int idx) {
         bld.append(buffer.get(idx));
      }

      @Override
      public AbstractMetricBuffer getBuffer() {
         return buffer;
      }
   }

   private static final class FloatLongTermMetric extends LongTermMetric<Float> {
      private final FloatMetricBuffer buffer;

      public FloatLongTermMetric(String name, Gauge<? extends Number> gauge, int capacity) {
         super(name, new ToFloatGauge(gauge), 0.0f);
         this.buffer = new FloatMetricBuffer(capacity);
      }

      @Override
      protected void sample(Float value) {
         buffer.add(value);
      }

      @Override
      public Float getValue(int idx) {
         return buffer.get(idx);
      }

      @Override
      public void appendValue(StringBuilder bld, int idx) {
         bld.append(buffer.get(idx));
      }

      @Override
      public AbstractMetricBuffer getBuffer() {
         return buffer;
      }
   }

   private static final class DoubleLongTermMetric extends LongTermMetric<Double> {
      private final DoubleLikeMetricBuffer buffer;

      public DoubleLongTermMetric(String name, Gauge<? extends Number> gauge, int capacity) {
         this(name, gauge, new DoubleMetricBuffer(capacity));
      }

      public DoubleLongTermMetric(String name, Gauge<? extends Number> gauge, DoubleLikeMetricBuffer buffer) {
         super(name, new ToDoubleGauge(gauge), 0.0);
         this.buffer = buffer;
      }

      @Override
      protected void sample(Double value) {
         buffer.add(value);
      }

      @Override
      public Double getValue(int idx) {
         return buffer.get(idx);
      }

      @Override
      public void appendValue(StringBuilder bld, int idx) {
         bld.append(buffer.get(idx));
      }

      @Override
      public AbstractMetricBuffer getBuffer() {
         return buffer;
      }
   }

   private static abstract class ToSaturatingGauge<T> implements Gauge<T> {
      private final Gauge<? extends Number> delegate;
      private final long min;
      private final long max;

      public ToSaturatingGauge(Gauge<? extends Number> delegate, long min, long max) {
         this.delegate = delegate;
         this.min = min;
         this.max = max;
      }

      @Override
      public T getValue() {
         Number num = delegate.getValue();
         if (num == null) {
            return null;
         }

         long value = num.longValue();
         long result;
         if (value < min) {
            result = min;
         } else if (value > max) {
            result = max;
         } else {
            result = value;
         }

         return convert(result);
      }

      protected abstract T convert(long value);
   }

   private static final class ToByteGauge extends ToSaturatingGauge<Byte> {
      public ToByteGauge(Gauge<? extends Number> delegate) {
         super(delegate, Byte.MIN_VALUE, Byte.MAX_VALUE);
      }

      @Override
      public Byte convert(long value) {
         return (byte)value;
      }
   }

   private static final class ToShortGauge extends ToSaturatingGauge<Short> {
      public ToShortGauge(Gauge<? extends Number> delegate) {
         super(delegate, Short.MIN_VALUE, Short.MAX_VALUE);
      }

      @Override
      public Short convert(long value) {
         return (short)value;
      }
   }

   private static final class ToIntGauge extends ToSaturatingGauge<Integer> {
      public ToIntGauge(Gauge<? extends Number> delegate) {
         super(delegate, Integer.MIN_VALUE, Integer.MAX_VALUE);
      }

      @Override
      public Integer convert(long value) {
         return (int)value;
      }
   }

   private static final class ToLongGauge implements Gauge<Long> {
      private final Gauge<? extends Number> delegate;

      public ToLongGauge(Gauge<? extends Number> delegate) {
         this.delegate = delegate;
      }

      @Override
      public Long getValue() {
         Number num = delegate.getValue();
         return (num == null) ? null : num.longValue();
      }
   }

   private static final class ToFloatGauge implements Gauge<Float> {
      private final Gauge<? extends Number> delegate;

      public ToFloatGauge(Gauge<? extends Number> delegate) {
         this.delegate = delegate;
      }

      @Override
      public Float getValue() {
         Number num = delegate.getValue();
         return (num == null) ? null : num.floatValue();
      }
   }

   private static final class ToDoubleGauge implements Gauge<Double> {
      private final Gauge<? extends Number> delegate;

      public ToDoubleGauge(Gauge<? extends Number> delegate) {
         this.delegate = delegate;
      }

      @Override
      public Double getValue() {
         Number num = delegate.getValue();
         return (num == null) ? null : num.doubleValue();
      }
   }

   private static abstract class AbstractMetricBuffer {
      protected int idx = 0;
      protected int size = 0;

      public int getSize() {
         return size;
      }

      public int getStartIndex() {
         int c = getCapacity();
         return (size < c) ? 0 : idx;
      }

      protected void next() {
         int c = getCapacity();
         idx = (idx + 1) % c;
         size = Math.min(size+1, c);
      }

      public abstract int getCapacity();
   }

   private static final class ByteMetricBuffer extends AbstractMetricBuffer {
      private final byte[] buffer;

      public ByteMetricBuffer(int capacity) {
         this.buffer = new byte[capacity];
      }

      public void add(byte value) {
         buffer[idx] = value;
         next();
      }

      public byte get(int idx) {
         return buffer[idx];
      }

      @Override
      public int getCapacity() {
         return buffer.length;
      }
   }

   private static final class ShortMetricBuffer extends AbstractMetricBuffer {
      private final short[] buffer;

      public ShortMetricBuffer(int capacity) {
         this.buffer = new short[capacity];
      }

      public void add(short value) {
         buffer[idx] = value;
         next();
      }

      public short get(int idx) {
         return buffer[idx];
      }

      @Override
      public int getCapacity() {
         return buffer.length;
      }
   }

   private static final class IntMetricBuffer extends AbstractMetricBuffer {
      private final int[] buffer;

      public IntMetricBuffer(int capacity) {
         this.buffer = new int[capacity];
      }

      public void add(int value) {
         buffer[idx] = value;
         next();
      }

      public int get(int idx) {
         return buffer[idx];
      }

      @Override
      public int getCapacity() {
         return buffer.length;
      }
   }

   private static final class LongMetricBuffer extends AbstractMetricBuffer {
      private final long[] buffer;

      public LongMetricBuffer(int capacity) {
         this.buffer = new long[capacity];
      }

      public void add(long value) {
         buffer[idx] = value;
         next();
      }

      public long get(int idx) {
         return buffer[idx];
      }

      @Override
      public int getCapacity() {
         return buffer.length;
      }
   }

   private static final class FloatMetricBuffer extends AbstractMetricBuffer {
      private final float[] buffer;

      public FloatMetricBuffer(int capacity) {
         this.buffer = new float[capacity];
      }

      public void add(float value) {
         buffer[idx] = value;
         next();
      }

      public float get(int idx) {
         return buffer[idx];
      }

      @Override
      public int getCapacity() {
         return buffer.length;
      }
   }

   private static abstract class DoubleLikeMetricBuffer extends AbstractMetricBuffer {
      public abstract void add(double value);
      public abstract double get(int idx);
   }

   private static final class PercentageMetricBuffer extends DoubleLikeMetricBuffer {
      private final int MAX = 255;
      private final byte[] buffer;

      public PercentageMetricBuffer(int capacity) {
         this.buffer = new byte[capacity];
      }

      public void add(double value) {
         byte val = (byte)Math.max(0,Math.min(MAX, (int)Math.round(MAX*value)));
         buffer[idx] = val;
         next();
      }

      public double get(int idx) {
         return ((double)(buffer[idx] & 0xFF)) / MAX;
      }

      @Override
      public int getCapacity() {
         return buffer.length;
      }
   }

   private static final class ByteFixedMetricBuffer extends DoubleLikeMetricBuffer {
      private static final int MAX = Byte.MAX_VALUE;
      private final byte[] buffer;
      private final double fixed;

      public ByteFixedMetricBuffer(int capacity, int shift) {
         this.buffer = new byte[capacity];
         this.fixed = (1 << shift);
      }

      public void add(double value) {
         byte val = (byte)Math.max(0, Math.min(MAX, (int)Math.round(value*fixed)));
         buffer[idx] = val;
         next();
      }

      public double get(int idx) {
         return (buffer[idx] & 0xFF) / fixed;
      }

      @Override
      public int getCapacity() {
         return buffer.length;
      }
   }

   private static final class ShortFixedMetricBuffer extends DoubleLikeMetricBuffer {
      private static final int MAX = Short.MAX_VALUE;
      private final short[] buffer;
      private final double fixed;

      public ShortFixedMetricBuffer(int capacity, int shift) {
         this.buffer = new short[capacity];
         this.fixed = (1 << shift);
      }

      public void add(double value) {
         short val = (short)Math.max(0, Math.min(MAX, (int)Math.round(value*fixed)));
         buffer[idx] = val;
         next();
      }

      public double get(int idx) {
         return (buffer[idx] & 0xFFFF) / fixed;
      }

      @Override
      public int getCapacity() {
         return buffer.length;
      }
   }

   private static final class DoubleMetricBuffer extends DoubleLikeMetricBuffer {
      private final double[] buffer;

      public DoubleMetricBuffer(int capacity) {
         this.buffer = new double[capacity];
      }

      public void add(double value) {
         buffer[idx] = value;
         next();
      }

      public double get(int idx) {
         return buffer[idx];
      }

      @Override
      public int getCapacity() {
         return buffer.length;
      }
   }

   private static final class LongTermMetricSampler implements Runnable {
      private final List<LongTermMetric<?>> longTermMetrics;
      private final LongLongTermMetric times;

      public LongTermMetricSampler(List<LongTermMetric<?>> longTermMetrics, int capacity) {
         this.longTermMetrics = longTermMetrics;
         this.times = new LongLongTermMetric("sample.times", new Gauge<Long>() {
            @Override
            public Long getValue() {
               return System.currentTimeMillis();
            }
         }, capacity);
      }

      @Override
      public synchronized void run() {
         try {
            times.sample();
            for (LongTermMetric<?> metric : longTermMetrics) {
               metric.sample();
            }
         } catch (Exception ex) {
            log.warn("failed to sample metrics:", ex);
         }
      }

      private synchronized List<Map<String,Object>> metrics() {
         int sz = times.getSize();
         int cp = times.getCapacity();
         int si = times.getStartIndex();

         ImmutableList.Builder<Map<String,Object>> bld = ImmutableList.builder();
         addMetric(bld, times, sz, cp, si);
         for (LongTermMetric<?> metric : longTermMetrics) {
            addMetric(bld, metric, sz, cp, si);
         }

         return bld.build();
      }

      private void addMetric(ImmutableList.Builder<Map<String,Object>> bld, LongTermMetric<?> metric, int sz, int cp, int si) {
         ImmutableList.Builder<Double> samples = ImmutableList.builder();
         for (int i = 0; i < sz; ++i) {
            Number sample = metric.getValue(((si+i) % cp));
            samples.add(sample.doubleValue());
         }

         HubMetric hm = new HubMetric();
         hm.setName(metric.getName());
         hm.setSamples(samples.build());
         bld.add(hm.toMap());
      }

      private synchronized void dump() {
         try {
            int sz = times.getSize();
            int cp = times.getCapacity();
            int si = times.getStartIndex();

            StringBuilder bld = new StringBuilder();
            bld.append("times: ");
            getDump(si, sz, cp, times, bld);
            bld.append("\n");

            for (LongTermMetric<?> metric : longTermMetrics) {
               bld.append(metric.getName()).append(": ");
               getDump(si, sz, cp, metric, bld); 
               bld.append("\n");
            }

            METRICS_LOGGER.warn("long term metrics:\n{}", bld);
         } catch (Exception ex) {
            log.warn("failed to dump metric data:", ex);
         }
      }

      private String getDump(int si, int sz, int cp, LongTermMetric<?> metric) {
         StringBuilder bld = new StringBuilder();
         getDump(si, sz, cp, metric, bld);
         return bld.toString();
      }

      private void getDump(int si, int sz, int cp, LongTermMetric<?> metric, StringBuilder bld) {
         boolean first = true;
         for (int i = 0; i < sz; ++i) {
            if (!first) bld.append(',');
            metric.appendValue(bld, (si+i) % cp);
            first = false;
         }
      }
   }

   private static final class LongTermMetricSamplerBuilder {
      private static enum Type {
         BYTE,
         SHORT,
         INT,
         LONG,
         FLOAT,
         DOUBLE,
         PERCENT,

         Q4_4,
         Q6_10,
         Q8_8,
      }

      private final Map<String,Type> metricsType = new LinkedHashMap<>();
      private final Map<String,Gauge<?>> metricsGauge = new LinkedHashMap<>();

      public LongTermMetricSamplerBuilder addByteMetric(String name, Gauge<? extends Number> gauge) {
         metricsType.put(name, Type.BYTE);
         metricsGauge.put(name, gauge);
         return this;
      }

      public LongTermMetricSamplerBuilder addShortMetric(String name, Gauge<? extends Number> gauge) {
         metricsType.put(name, Type.SHORT);
         metricsGauge.put(name, gauge);
         return this;
      }

      public LongTermMetricSamplerBuilder addIntMetric(String name, Gauge<? extends Number> gauge) {
         metricsType.put(name, Type.INT);
         metricsGauge.put(name, gauge);
         return this;
      }

      public LongTermMetricSamplerBuilder addLongMetric(String name, Gauge<? extends Number> gauge) {
         metricsType.put(name, Type.LONG);
         metricsGauge.put(name, gauge);
         return this;
      }

      public LongTermMetricSamplerBuilder addFloatMetric(String name, Gauge<? extends Number> gauge) {
         metricsType.put(name, Type.FLOAT);
         metricsGauge.put(name, gauge);
         return this;
      }

      public LongTermMetricSamplerBuilder addDoubleMetric(String name, Gauge<? extends Number> gauge) {
         metricsType.put(name, Type.DOUBLE);
         metricsGauge.put(name, gauge);
         return this;
      }

      public LongTermMetricSamplerBuilder addFixed4x4Metric(String name, Gauge<? extends Number> gauge) {
         metricsType.put(name, Type.Q4_4);
         metricsGauge.put(name, gauge);
         return this;
      }

      public LongTermMetricSamplerBuilder addFixed6x10Metric(String name, Gauge<? extends Number> gauge) {
         metricsType.put(name, Type.Q6_10);
         metricsGauge.put(name, gauge);
         return this;
      }

      public LongTermMetricSamplerBuilder addFixed8x8Metric(String name, Gauge<? extends Number> gauge) {
         metricsType.put(name, Type.Q8_8);
         metricsGauge.put(name, gauge);
         return this;
      }

      public LongTermMetricSamplerBuilder addPercentageMetric(String name, Gauge<? extends Number> gauge) {
         metricsType.put(name, Type.PERCENT);
         metricsGauge.put(name, gauge);
         return this;
      }

      public LongTermMetricSampler build(int capacity) {
         ImmutableList.Builder<LongTermMetric<?>> longTermMetrics = ImmutableList.builder();
         for (Map.Entry<String,Type> entry : metricsType.entrySet()) {
            String name = entry.getKey();
            Gauge<?> gauge = metricsGauge.get(name);
            switch (entry.getValue()) {
            case BYTE:
               longTermMetrics.add(new ByteLongTermMetric(name, (Gauge<? extends Number>)gauge, capacity));
               break;
            case SHORT:
               longTermMetrics.add(new ShortLongTermMetric(name, (Gauge<? extends Number>)gauge, capacity));
               break;
            case INT:
               longTermMetrics.add(new IntLongTermMetric(name, (Gauge<? extends Number>)gauge, capacity));
               break;
            case LONG:
               longTermMetrics.add(new LongLongTermMetric(name, (Gauge<? extends Number>)gauge, capacity));
               break;
            case FLOAT:
               longTermMetrics.add(new FloatLongTermMetric(name, (Gauge<? extends Number>)gauge, capacity));
               break;
            case DOUBLE:
               longTermMetrics.add(new DoubleLongTermMetric(name, (Gauge<? extends Number>)gauge, capacity));
               break;
            case Q4_4:
               longTermMetrics.add(new DoubleLongTermMetric(name, (Gauge<? extends Number>)gauge, new ByteFixedMetricBuffer(capacity, 4)));
               break;
            case Q6_10:
               longTermMetrics.add(new DoubleLongTermMetric(name, (Gauge<? extends Number>)gauge, new ShortFixedMetricBuffer(capacity, 10)));
               break;
            case Q8_8:
               longTermMetrics.add(new DoubleLongTermMetric(name, (Gauge<? extends Number>)gauge, new ShortFixedMetricBuffer(capacity, 8)));
               break;
            case PERCENT:
               longTermMetrics.add(new DoubleLongTermMetric(name, (Gauge<? extends Number>)gauge, new PercentageMetricBuffer(capacity)));
               break;
            default:
               throw new RuntimeException("unknown long term metrics type: " + entry.getValue());
            }
         }

         return new LongTermMetricSampler(longTermMetrics.build(), capacity);
      }
   }

   ///////////////////////////////////////////////////////////////////////////////
   // Metric Jobs and Reporting
   ///////////////////////////////////////////////////////////////////////////////
   
   private static void addGaugeValue(JsonArray jsonGauges, String name, Object value, @Nullable String interval) {
      if ((value instanceof Long) || (value instanceof Integer) || (value instanceof Byte) || (value instanceof Short)) {
         JsonObject obj = new JsonObject();
         obj.addProperty("n", name);
         obj.addProperty("v", ((Number)value).longValue());
         if (interval != null) {
            obj.addProperty("i", interval);
         }

         jsonGauges.add(obj);
      } else if (value instanceof Number) {
         JsonObject obj = new JsonObject();
         obj.addProperty("n", name);
         obj.addProperty("v", ((Number)value).doubleValue());
         if (interval != null) {
            obj.addProperty("i", interval);
         }

         jsonGauges.add(obj);
      } else if (value instanceof Map) {
         for (Map.Entry<Object,Object> mentry : ((Map<Object,Object>)value).entrySet()) {
            String mname = name + "." + String.valueOf(mentry.getKey());
            addGaugeValue(jsonGauges, mname, mentry.getValue(), interval);
         }
      } else {
         log.warn("cannot report gauge '{}' of type: {} ({})", name, value.getClass(), value);
      }
   }

   private static abstract class MetricReporter {
      public @Nullable JsonArray reportGauges(@Nullable JsonArray existingJsonGauges, Map<String, Gauge> gauges) {
         return reportGauges(existingJsonGauges, gauges, null);
      }

      public @Nullable JsonArray reportGauges(@Nullable JsonArray existingJsonGauges, Map<String, Gauge> gauges, @Nullable String interval) {
         JsonArray jsonGauges = existingJsonGauges;
         for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue().getValue();
            if (value != null) {
               if (jsonGauges == null) {
                  jsonGauges = new JsonArray();
               }

               addGaugeValue(jsonGauges, name, value, interval);
            }
         }

         return jsonGauges;
      }

      public @Nullable JsonArray reportCounters(@Nullable JsonArray existingJsonCounters, Map<String, Counter> counters) {
         JsonArray jsonCounters = existingJsonCounters;
         for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            if (jsonCounters == null) {
               jsonCounters = new JsonArray();
            }

            String name = entry.getKey();
            long count = entry.getValue().getCount();

            JsonObject obj = new JsonObject();
            obj.addProperty("n", name);
            obj.addProperty("v", count);

            jsonCounters.add(obj);
         }

         return jsonCounters;
      }

      public @Nullable JsonArray reportHistograms(@Nullable JsonArray existingJsonHistograms, Map<String, Histogram> histograms) {
         JsonArray jsonHistograms = existingJsonHistograms;
         for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            String name = entry.getKey();
            Snapshot snap = entry.getValue().getSnapshot();

            if (jsonHistograms == null) {
               jsonHistograms = new JsonArray();
            }

            JsonObject obj = new JsonObject();
            obj.addProperty("n", name);
            obj.addProperty("count", entry.getValue().getCount());
            obj.addProperty("min", snap.getMin());
            obj.addProperty("max", snap.getMax());
            obj.addProperty("mean", snap.getMean());
            obj.addProperty("stddev", snap.getStdDev());
            obj.addProperty("p50", snap.getMedian());
            obj.addProperty("p75", snap.get75thPercentile());
            obj.addProperty("p95", snap.get95thPercentile());
            obj.addProperty("p98", snap.get98thPercentile());
            obj.addProperty("p99", snap.get99thPercentile());
            obj.addProperty("p999", snap.get999thPercentile());

            jsonHistograms.add(obj);
         }

         return jsonHistograms;
      }

      public @Nullable JsonArray reportMeters(@Nullable JsonArray existingJsonMeters, Map<String, Meter> meters) {
         JsonArray jsonMeters = existingJsonMeters;
         for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            String name = entry.getKey();
            Meter meter = entry.getValue();

            if (jsonMeters == null) {
               jsonMeters = new JsonArray();
            }

            JsonObject obj = new JsonObject();
            obj.addProperty("n", name);
            obj.addProperty("count", meter.getCount());
            obj.addProperty("mean", meter.getMeanRate());
            obj.addProperty("m1", meter.getOneMinuteRate());
            obj.addProperty("m5", meter.getFiveMinuteRate());
            obj.addProperty("m15", meter.getFifteenMinuteRate());

            jsonMeters.add(obj);
         }

         return jsonMeters;
      }

      public @Nullable JsonArray reportTimers(@Nullable JsonArray existingJsonTimers, Map<String, Timer> timers) {
         JsonArray jsonTimers = existingJsonTimers;
         for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            String name = entry.getKey();
            Snapshot snap = entry.getValue().getSnapshot();

            if (jsonTimers == null) {
               jsonTimers = new JsonArray();
            }

            JsonObject obj = new JsonObject();
            obj.addProperty("n", name);
            obj.addProperty("count", entry.getValue().getCount());
            obj.addProperty("min", snap.getMin());
            obj.addProperty("max", snap.getMax());
            obj.addProperty("mean", snap.getMean());
            obj.addProperty("stddev", snap.getStdDev());
            obj.addProperty("p50", snap.getMedian());
            obj.addProperty("p75", snap.get75thPercentile());
            obj.addProperty("p95", snap.get95thPercentile());
            obj.addProperty("p98", snap.get98thPercentile());
            obj.addProperty("p99", snap.get99thPercentile());
            obj.addProperty("p999", snap.get999thPercentile());

            jsonTimers.add(obj);
         }

         return jsonTimers;
      }

      public void report(Map<String, Gauge> gauges, Map<String, Counter> counters, Map<String, Histogram> histograms, Map<String, Meter> meters, Map<String, Timer> timers) {
         long timestamp = System.currentTimeMillis();

         JsonArray jsonGauges = reportGauges(null, gauges);
         JsonArray jsonCounters = reportCounters(null, counters);
         JsonArray jsonHistograms = reportHistograms(null, histograms);
         JsonArray jsonMeters = reportMeters(null, meters);
         JsonArray jsonTimers = reportTimers(null, timers);

         report(timestamp, jsonGauges, jsonCounters, jsonHistograms, jsonMeters, jsonTimers);
      }

      public void report(long timestamp, @Nullable JsonArray jsonGauges, @Nullable JsonArray jsonCounters, @Nullable JsonArray jsonHistograms, @Nullable JsonArray jsonMeters, @Nullable JsonArray jsonTimers) {
         report(timestamp, jsonGauges, jsonCounters, jsonHistograms, jsonMeters, jsonTimers, getType());
      }

      public void report(long timestamp, @Nullable JsonArray jsonGauges, @Nullable JsonArray jsonCounters, @Nullable JsonArray jsonHistograms, @Nullable JsonArray jsonMeters, @Nullable JsonArray jsonTimers, String type) {
         JsonObject report = new JsonObject();
         report.addProperty("mt", type);
         report.addProperty("ts", timestamp);
         report.addProperty("osver", IrisHal.getOperatingSystemVersion());
         report.addProperty("agver", IrisHal.getAgentVersion());
         report.addProperty("model", IrisHal.getModel());

         if (jsonGauges != null) {
            report.add("gauges", jsonGauges);
         }

         if (jsonCounters != null) {
            report.add("counters", jsonCounters);
         }

         if (jsonHistograms != null) {
            report.add("histograms", jsonHistograms);
         }

         if (jsonMeters != null) {
            report.add("meters", jsonMeters);
         }

         if (jsonTimers != null) {
            report.add("timers", jsonTimers);
         }

         MetricsService.notify(report);
      }

      protected abstract String getType();
   }

   private static final class AggregatedMetricsReporter extends MetricReporter implements Runnable {
      private Map<String, Gauge> fastGauges = ImmutableMap.of();
      private Map<String, Gauge> mediumGauges = ImmutableMap.of();
      private Map<String, Gauge> slowGauges = ImmutableMap.of();

      private int aggregatedMetricsChange = -1;
      private volatile boolean stop;

      private long lastFastReport = Long.MIN_VALUE;
      private long lastMediumReport = Long.MIN_VALUE;
      private long lastSlowReport = Long.MIN_VALUE;

      public void start() {
         ExecService.periodic().scheduleAtFixedRate(this, TimeUnit.MINUTES.toNanos(1), AGGREGATED_METRICS_REPORTING_INTERVAL, TimeUnit.NANOSECONDS);
      }

      public void stop() {
         this.stop = true;
      }

      @Override
      public void run() {
         if (stop) {
            return;
         }

         try {
            updateGaugesIfNeeded();

            long now = System.nanoTime();

            if (lastSlowReport == Long.MIN_VALUE || (now-lastSlowReport) > AGGREGATED_METRICS_SLOW_REPORTING_INTERVAL) {
               lastSlowReport = now;
               lastMediumReport = now;
               lastFastReport = now;

               JsonArray gauges = null;
               gauges = reportGauges(gauges, fastGauges, "f");
               gauges = reportGauges(gauges, mediumGauges, "m");
               gauges = reportGauges(gauges, slowGauges, "s");

               report(now, gauges, null, null, null, null);
            } else if (lastMediumReport == Long.MIN_VALUE || (now-lastMediumReport) > AGGREGATED_METRICS_MEDIUM_REPORTING_INTERVAL) {
               lastMediumReport = now;
               lastFastReport = now;

               JsonArray gauges = null;
               gauges = reportGauges(gauges, fastGauges, "f");
               gauges = reportGauges(gauges, mediumGauges, "m");

               report(now, gauges, null, null, null, null);
            } else  if (lastFastReport == Long.MIN_VALUE || (now-lastFastReport) > AGGREGATED_METRICS_FAST_REPORTING_INTERVAL) {
               lastFastReport = now;

               JsonArray gauges = null;
               gauges = reportGauges(gauges, fastGauges, "f");

               report(now, gauges, null, null, null, null);
            }
         } catch (Exception ex) {
            log.warn("error while processing aggregated metrics", ex);
         }
      }

      @Override
      public String getType() {
         return "agg";
      }

      private void updateGaugesIfNeeded() {
         int agmc = MetricsService.getAggregatedMetricsChange();
         if (aggregatedMetricsChange == agmc) {
            return;
         }

         aggregatedMetricsChange = agmc;
         fastGauges = getGauges(MetricsService.getAggregatedMetricFast().entrySet());
         mediumGauges = getGauges(MetricsService.getAggregatedMetricMedium().entrySet());
         slowGauges = getGauges(MetricsService.getAggregatedMetricSlow().entrySet());
      }

      private void reportGauges(Map<String,Gauge>... gauges) {
         switch (gauges.length) {
         case 0:
            // nothing to do
            break;

         case 1:
            report(gauges[0], ImmutableMap.of(), ImmutableMap.of(), ImmutableMap.of(), ImmutableMap.of());
            break;

         default:
            ImmutableMap.Builder<String,Gauge> bld = ImmutableMap.builder();
            for (Map<String,Gauge> next : gauges) {
               bld.putAll(next);
            }
         
            report(bld.build(), ImmutableMap.of(), ImmutableMap.of(), ImmutableMap.of(), ImmutableMap.of());
            break;
         }
      }

      private Map<String,Gauge> getGauges(Iterable<Map.Entry<String,Gauge<?>>> gauges) {
         ImmutableMap.Builder<String,Gauge> bld = ImmutableMap.builder();

         Map<String,Gauge> registered = IrisMetrics.registry().getGauges();
         for (Map.Entry<String,Gauge<?>> entry : gauges) {
            Gauge gauge = entry.getValue();
            if (gauge == null) {
               log.warn("registered aggregated metric '{}' is not found or is not a gauge", entry.getKey());
               continue;
            }

            bld.put(entry.getKey(), gauge);
         }

         return bld.build();
      }
   }

   private static final class MetricsJob extends MetricReporter implements Runnable {
      private final String name;
      private final long period;
      private final long end;
      private volatile boolean stop;
      
      private final Map<String, Gauge> gauges;
      private final Map<String, Counter> counters;
      private final Map<String, Histogram> histograms;
      private final Map<String, Meter> meters;
      private final Map<String, Timer> timers;

      private MetricsJob(MessageBody msg) {
         this.name = HubMetricsCapability.StartMetricsJobRequest.getJobname(msg);
         this.period = HubMetricsCapability.StartMetricsJobRequest.getPeriodMs(msg);
         long duration = HubMetricsCapability.StartMetricsJobRequest.getDurationMs(msg);
         this.end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(duration);

         List<String> metrics = HubMetricsCapability.StartMetricsJobRequest.getMetrics(msg);
         Predicate<String> matcher;
         if (metrics == null || metrics.isEmpty()) {
            matcher = (m) -> true;
         } else {
            matcher = (m) -> false;
            if (metrics != null && !metrics.isEmpty()) {
               for (String metric : metrics) {
                  Pattern p = Pattern.compile("^" + metric + "$");
                  matcher = matcher.or((m) -> p.matcher(m).matches()); 
               }
            }
         }

         ImmutableMap.Builder<String,Gauge> gauges = ImmutableMap.builder();
         ImmutableMap.Builder<String,Counter> counters = ImmutableMap.builder();
         ImmutableMap.Builder<String,Histogram> histograms = ImmutableMap.builder();
         ImmutableMap.Builder<String,Meter> meters = ImmutableMap.builder();
         ImmutableMap.Builder<String,Timer> timers = ImmutableMap.builder();

         for (Map.Entry<String,Gauge> entry : IrisMetrics.registry().getGauges().entrySet()) {
            if (matcher.test(entry.getKey())) {
               gauges.put(entry.getKey(), entry.getValue());
            }
         }

         for (Map.Entry<String,Counter> entry : IrisMetrics.registry().getCounters().entrySet()) {
            if (matcher.test(entry.getKey())) {
               counters.put(entry.getKey(), entry.getValue());
            }
         }

         for (Map.Entry<String,Histogram> entry : IrisMetrics.registry().getHistograms().entrySet()) {
            if (matcher.test(entry.getKey())) {
               histograms.put(entry.getKey(), entry.getValue());
            }
         }

         for (Map.Entry<String,Meter> entry : IrisMetrics.registry().getMeters().entrySet()) {
            if (matcher.test(entry.getKey())) {
               meters.put(entry.getKey(), entry.getValue());
            }
         }

         for (Map.Entry<String,Timer> entry : IrisMetrics.registry().getTimers().entrySet()) {
            if (matcher.test(entry.getKey())) {
               timers.put(entry.getKey(), entry.getValue());
            }
         }

         this.gauges = gauges.build();
         this.counters = counters.build();
         this.histograms = histograms.build();
         this.meters = meters.build();
         this.timers = timers.build();
      }

      public void start() {
         ExecService.periodic().schedule(this, 0, TimeUnit.MILLISECONDS);
      }

      public void stop() {
         this.stop = true;
      }

      @Override
      public void run() {
         if (stop || System.nanoTime() > end) {
            if (MetricsHandler.INSTANCE.jobs.remove(name, this)) {
               MetricsHandler.INSTANCE.METRICSJOBS.poke();
            }
            return;
         }

         try {
            report(gauges, counters, histograms, meters, timers);
         } finally {
            ExecService.periodic().schedule(this, period, TimeUnit.MILLISECONDS);
         }
      }

      @Override
      public String getType() {
         return "tag";
      }
   }
}

