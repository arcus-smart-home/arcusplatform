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
package com.iris.agent.scene;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.iris.agent.exec.ExecService;
import com.iris.messages.address.Address;
import com.iris.protocol.ProtocolMessage;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David Easley (deasley@btbcku.com) on 5/24/17.
 */
public class SceneService {
    private static final boolean IRIS_SCENE_SERVICE_ENABLED = System.getenv("IRIS_SCENE_SERVICE_DISABLE") == null;
    private static final Logger log = LoggerFactory.getLogger(SceneService.class);
    private static final Object START_LOCK = new Object();

    // The amount of time we will force ourselves to wait before considering the scene
    // complete when the enqueued count reaches 0. This prevents us from considering
    // the scene complete because there was a small delay while enqueuing messages.
    private static final long FINISH_SCENE_DWELL_TIME = TimeUnit.SECONDS.toNanos(5);

    // The amount of time we will allow before we force the scene to be considered
    // no longer enqueuing. This prevents us from never considering the scene complete because
    // the scene is firing faster than we can execute it.
    private static final long ENQUEING_SCENE_TIMEOUT = TimeUnit.SECONDS.toNanos(300);

    // The number of messages needed to be in a scene before we log about
    // the size of that scene and the granularity that we keep warning about it
    // after it exceed this size.
    private static final int SCENE_QUEUE_SIZE_WARN = 200;
    private static final int SCENE_QUEUE_SIZE_GRAN = 100;

    private static @Nullable SceneFactory sceneFactory;
    private static @Nullable Map<Address, SceneLog> sceneLogMap;

    public enum SceneProtocolSupport {
        ZWAVE(ZWavePrioritySupport.INSTANCE, "zwave", "scnzw", 300000, 1),
        ZIGBEE(ZigbeePrioritySupport.INSTANCE, "zigbee", "scnzb", 60000, 32);

        private @Nullable BlockingQueue<SceneEvent> queue;
        private @Nullable Semaphore ready;
        private volatile @Nullable Thread thread;
        private final ScenePrioritySupport prioritizer;
        private final String name;
        private final String threadName;
        private final long timeout;
        private final int concurrent;
        private int sequence;

        private SceneProtocolSupport(ScenePrioritySupport prioritizer, String name, String threadName, long timeoutMilliseconds, int concurrent) {
            this.prioritizer = prioritizer;
            this.name = name;
            this.threadName = threadName;
            this.timeout = timeoutMilliseconds;
            this.concurrent = concurrent;
            this.sequence = 0;
        }

        public void start() {
            Thread thr = thread;
            if (thr != null) {
                throw new RuntimeException("scene " + name + " service already started");
            }

            thr = new Thread(new SceneQueueChecker());
            thr.setName(threadName);
            thr.setDaemon(true);

            ready = new Semaphore(concurrent);
            queue = new PriorityBlockingQueue<>();

            thread = thr;
            thr.start();
        }

        public void shutdown() {
            Thread thr = thread;
            if (thr == null) {
                throw new RuntimeException("scene " + name + " service not started");
            }

            queue = null;
            ready = null;
            thread = null;
            thr.interrupt();
        }

        public void enqueue(SceneHandler hndlr, ProtocolMessage msg, Object port) {
            int priority = prioritizer.getMessagePriority(msg);
            SceneEvent tmpScene = sceneFactory.newScene(hndlr, msg, port, priority, sequence);
            sequence++;

            try {
                queue.put(tmpScene);
                logNewEvent(tmpScene.sceneId, tmpScene.destination);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.trace("scene {} service interrupted:", name, e);
            }
        }

        private final class SceneQueueChecker implements Runnable {
            @Override
            public void run() {
                Thread thr = thread;
                while (thr == thread) {
                    try {
                        SceneEvent tmpScene = queue.take();
                        if (!ready.tryAcquire(timeout, TimeUnit.MILLISECONDS)) {
                            ready.drainPermits();
                            ready.release(concurrent);
                            log.warn("failed to acquier semaphore to entering scene, sending anyways: semaphore={}, priority={}, reset semaphore ct={}", name, tmpScene.priority, ready.availablePermits());
                        }

                        rx.Observer tmpSub = new rx.Observer<Object>() {
                            @Override
                            public void onNext(Object o) {
                            }

                            @Override
                            public void onError(Throwable e) {
                                logEventComplete(tmpScene.sceneId, false);
                                ready.release();
                            }

                            @Override
                            public void onCompleted() {
                                logEventComplete(tmpScene.sceneId, true);
                                ready.release();
                            }
                        };

                        tmpScene.hndlr.recvScene(tmpScene.port, tmpScene.msg, tmpSub);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.trace("scene {} service interrupted:", name,  e);
                    } catch (Exception e) {
                       log.warn("failed during scene execution:", e);
                    }
                }
            }
        }
    }

    public interface ScenePrioritySupport {
        int getMessagePriority(ProtocolMessage msg);
    }

    public enum ZWavePrioritySupport implements ScenePrioritySupport {
        INSTANCE;

        @Override
        public int getMessagePriority(ProtocolMessage msg) {
            return ZWaveScene.calculatePriority(msg);
        }
    }

    public enum ZigbeePrioritySupport implements ScenePrioritySupport {
        INSTANCE;

        @Override
        public int getMessagePriority(ProtocolMessage msg) {
            return 0;   // For now, zigbee messages all have the same priority
        }
    }

    public static void start() {
        synchronized (START_LOCK) {
            sceneLogMap = new ConcurrentHashMap<>();
            sceneFactory = new SceneFactory();
            SceneProtocolSupport.ZWAVE.start();
            SceneProtocolSupport.ZIGBEE.start();
            log.warn("entering scene: scene service started");
        }
    }

    public static void shutdown() {
        synchronized (START_LOCK) {
            SceneProtocolSupport.ZWAVE.shutdown();
            SceneProtocolSupport.ZIGBEE.shutdown();
            sceneLogMap = null;
            sceneFactory = null;
            log.warn("entering scene: scene service shutdown");
        }

    }

    public static boolean isSceneMessage(ProtocolMessage msg) {
        if(IRIS_SCENE_SERVICE_ENABLED){
            Address act = msg.getActor();

            if(act != null) {
                if (act.toString().contains("scene")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getDestDevice(ProtocolMessage msg) {
        Address dest = msg.getDestination();
        if(dest != null){
            return dest.toString();
        }
        return "<unknown>";
    }

    private static class SceneEvent implements Comparable<SceneEvent> {

        public ProtocolMessage msg;
        public Address sceneId;
        public String destination;
        private SceneHandler hndlr;
        public Object port;
        private int priority;
        private int sequence;

        SceneEvent(SceneHandler hndlrIn, ProtocolMessage msgIn, Object portIn, int priorityIn, int sequenceIn) {
            msg = msgIn;
            sceneId = msgIn.getActor();
            destination = getDestDevice(msgIn);
            hndlr = hndlrIn;
            port = portIn;
            priority = priorityIn;
            sequence = sequenceIn;
        }

        @Override
        public int compareTo(SceneEvent o1) {
            // reversed since low-priority events are ordered  to the head of the queue
            int cmp = Integer.compare(o1.priority, this.priority);
            if (cmp != 0) {
               return cmp;
            }

            return Integer.compare(this.sequence, o1.sequence);
        }
    }

    private static final class SceneFactory {

        private SceneEvent newScene(SceneHandler hndlr, ProtocolMessage msg, Object port, int priority, int sequence) {

            return new SceneEvent(hndlr, msg, port, priority, sequence);
        }
    }

    private static class SceneLog implements Runnable{
        //While a SceneLog is in the BUILDING_SCENE state, the scene will still be able to execute events. The primary difference
        //between this state and the EXECUTING_SCENE state is the inability to mark the scene as having completed execution
        //until the enqueueingTimeout has elapsed (even if the eventCounter has reached 0). This prevents a scene from
        //being prematurely marked complete.
        public enum LoggingStates {BUILDING_SCENE, EXECUTING_SCENE, COMPLETED_SCENE}

        public Address sceneId;         // scene actor header's index into the place id
        public AtomicInteger eventCounter;
        public AtomicInteger totalEvents;
        public AtomicInteger failedEvents;
        public Set<String> devices;        //device destinations that are part of the scene
        public LoggingStates state;
        public long firstEventEnqueueTime;
        public long lastEventEnqueueTime;
        public long lastEventTransmitTime;

        SceneLog(Address id, String destDevice) {
            sceneId = id;
            devices = new HashSet<String>();
            if(!destDevice.equals("<unknown>")) {
                devices.add(destDevice);
            }
            state = LoggingStates.BUILDING_SCENE;
            eventCounter = new AtomicInteger();
            totalEvents = new AtomicInteger();
            failedEvents = new AtomicInteger();
            firstEventEnqueueTime = System.nanoTime();
            ExecService.periodic().schedule(this, FINISH_SCENE_DWELL_TIME, TimeUnit.NANOSECONDS);
        }

        @Override
        public void run() {
            Map<Address,SceneLog> slMap = sceneLogMap;
            if (slMap == null) {
               return;
            }

            if (!slMap.containsKey(sceneId)) {
                // scene must have already completed
                return;
            }

            if (state == LoggingStates.BUILDING_SCENE && eventCounter.get() == 0) {
                // the scene has completed
                state = LoggingStates.COMPLETED_SCENE;
                outputSceneLog(this);
                slMap.remove(sceneId, this);
                return;
            }

            long elapsed = System.nanoTime() - firstEventEnqueueTime;
            boolean timedout = elapsed >= ENQUEING_SCENE_TIMEOUT;
            if (timedout) {
               state = LoggingStates.EXECUTING_SCENE;
            } else {
               ExecService.periodic().schedule(this, FINISH_SCENE_DWELL_TIME, TimeUnit.NANOSECONDS);
            }
        }
    }

    private static void logNewEvent(Address id, String destDevice) {
       //Currently events can still be logged as part of an existing scene
       //in both the BUILDING_SCENE state the EXECUTING_SCENE state
        Map<Address,SceneLog> slMap = sceneLogMap;
        if (slMap == null) {
           return;
        }
       
        SceneLog slog = slMap.computeIfAbsent(id, (i) -> new SceneLog(id, destDevice));
        slog.eventCounter.incrementAndGet();
        int events = slog.totalEvents.incrementAndGet();
        slog.lastEventEnqueueTime = System.nanoTime();
        slog.devices.add(destDevice);

        if (events >= SCENE_QUEUE_SIZE_WARN && (events % SCENE_QUEUE_SIZE_GRAN == 0)) {
           log.info("large scene being executed: scene={}, total size={}", id, events);
        }
    }

    private static void logEventComplete(Address id, boolean success) {
        Map<Address,SceneLog> slMap = sceneLogMap;
        if (slMap == null) {
           return;
        }

        SceneLog slog = slMap.get(id);
        if (slog == null) {
            log.warn("entering scene: WARNING: no scene is being tracked with specified id");
            return;
        }

        slog.lastEventTransmitTime = System.nanoTime();
        int remaining = slog.eventCounter.decrementAndGet();
        long elapsed = System.nanoTime() - slog.firstEventEnqueueTime;

        if (!success){
            slog.failedEvents.incrementAndGet();
        }

        if (remaining != 0) {
           // there are parts of the scene still needing to execute
           return;
        }

        if (slog.state == SceneLog.LoggingStates.EXECUTING_SCENE ||
            (slog.state == SceneLog.LoggingStates.BUILDING_SCENE && elapsed >= FINISH_SCENE_DWELL_TIME)) {
            slog.state = SceneLog.LoggingStates.COMPLETED_SCENE;
            outputSceneLog(slog);
            slMap.remove(id);
        }
    }

    private static void outputSceneLog(SceneLog sLog) {
        long tDelta = sLog.lastEventTransmitTime - sLog.firstEventEnqueueTime;
        log.info("finished executing scene {}: time={}ms, state={}, total events={}, failed events={} , devices={}",
                 sLog.sceneId, TimeUnit.NANOSECONDS.toMillis(tDelta), sLog.state, sLog.totalEvents.get(), sLog.failedEvents.get(), sLog.devices.size());
    }
}

