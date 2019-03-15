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
package com.iris.driver.groovy.zwave;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.driver.groovy.reflex.ReflexContext;
import com.iris.driver.groovy.reflex.ReflexForwardContext;
import com.iris.driver.groovy.reflex.ReflexMatchContext;
import com.iris.driver.groovy.reflex.ReflexUtil;
import com.iris.driver.reflex.ReflexAction;
import com.iris.driver.reflex.ReflexActionSendProtocol;
import com.iris.driver.reflex.ReflexMatch;
import com.iris.driver.reflex.ReflexMatchLifecycle;
import com.iris.driver.reflex.ReflexMatchPollRate;
import com.iris.driver.reflex.ReflexMatchRegex;
import com.iris.protocol.zwave.model.ZWaveCommand;

public class ZWaveConfigContext extends GroovyObjectSupport {
   private static final Logger LOGGER = LoggerFactory.getLogger(ZWaveConfigContext.class);

   private long offlineTimeout = Long.MAX_VALUE;
   private List<ReflexContext> reflexes = new ArrayList<>(1);

   public ZWaveConfigContext() {
   }

   public void processReflexes(DriverBinding binding) {
      for (ReflexContext context : reflexes) {
         binding.getBuilder().addReflexDefinition(context.getDefinition());
      }

      if (offlineTimeout != Long.MAX_VALUE) {
         binding.getBuilder().withOfflineTimeout(offlineTimeout);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Configuration of Z-Wave offline timeouts
   /////////////////////////////////////////////////////////////////////////////
   
   public void offlineTimeout(long seconds) {
      this.offlineTimeout = (seconds > 0) ? seconds : Long.MAX_VALUE;
   }
   
   public void offlineTimeout(long timeout, TimeUnit unit) {
      long secs = unit.toSeconds(timeout);
      if (timeout != 0 && secs == 0) secs = 1;
      offlineTimeout(secs);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Configuration of Z-Wave polling
   /////////////////////////////////////////////////////////////////////////////
   
   public void poll(ReflexAndClosure reflex) {
      ReflexContext ctx = new PollReflexContext();
      reflexes.add(ctx);

      reflex.config.setDelegate(ctx);
      reflex.config.call();
   }

   public final class PollReflexContext extends ReflexMatchContext {
      public void on(ReflexMatchLifecycle.Type when) {
         addMatch(new ReflexMatchLifecycle(when));
      }

      public void after(long time) {
         after(time, TimeUnit.SECONDS);
      }

      public void after(long time, TimeUnit unit) {
         addMatch(new ReflexMatchPollRate(time,unit));
      }

      @Override
      public Object getProperty(String name) {
         if (name == null) {
            return super.getProperty(name);
         }

         ZWaveReflex.ZWaveCommandClassContext ccCtx = ZWaveReflex.INSTANCE.create(name);
         if (ccCtx != null) {
            return ccCtx;
         }

         try {
            return ReflexMatchLifecycle.Type.valueOf(name.toUpperCase());
         } catch (IllegalArgumentException ex) {
            // ignore
         }

         return super.getProperty(name);
      }

      @Override
      protected void addFinalAction(ReflexAction action) {
         boolean hasPollRate = false;
         for (ReflexMatch match : getMatches()) {
            if (match instanceof ReflexMatchPollRate) {
               hasPollRate = true;
               break;
            }
         }

         if (hasPollRate && !(action instanceof ReflexActionSendProtocol)) {
            GroovyValidator.error("zwave poll reflexes with polling rates only support send protocol message actions");
            return;
         }

         super.addFinalAction(action);
      }

      @Override
      public ProtocolClosureProcessor getProtocolSendProcessor() {
         return new ZWaveReflex.ZWaveSendProcessor() {
            @Override
            public void processPollCommand(ZWaveCommand command) {
               addAction(new ReflexActionSendProtocol(ReflexActionSendProtocol.Type.ZWAVE, command.toBytes()));
            }
         };
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Configuration of hub local reflexes
   /////////////////////////////////////////////////////////////////////////////

   public void match(ReflexAndClosure reflex) {
      ReflexContext ctx = new MatchReflexContext();
      reflexes.add(ctx);

      reflex.config.setDelegate(ctx);
      reflex.config.call();
   }

   public static final class MatchReflexContext extends ReflexForwardContext {
      @Override
      public Object getProperty(String name) {
         if ("_".equals(name)) {
            return ReflexUtil.WILDCARD;
         }

         if ("nodeinfo".equals(name)) {
            return new ZWaveReflex.ZWaveNodeInfoContext(this);
         }

         ZWaveReflex.ZWaveCommandClassContext ccCtx = ZWaveReflex.INSTANCE.create(name);
         if (ccCtx != null) {
            return ccCtx;
         }

         return super.getProperty(name);
      }

      @Override
      public ProtocolClosureProcessor getProtocolMatchProcessor() {
         return new ZWaveReflex.ZWaveMatchProcessor() {
            @Override
            public void processMatchString(String match) {
               addMatch(new ReflexMatchRegex(match));
            }
         };
      }

      @Override
      public ProtocolClosureProcessor getProtocolSendProcessor() {
         return new ZWaveReflex.ZWaveSendProcessor() {
            @Override
            public void processPollCommand(ZWaveCommand command) {
               addAction(new ReflexActionSendProtocol(ReflexActionSendProtocol.Type.ZWAVE, command.toBytes()));
            }
         };
      }
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Configuration of hub local reflexes
   /////////////////////////////////////////////////////////////////////////////
   
   public Object reflex(Closure<?> closure) {
      return new ReflexAndClosure(closure);
   }

   public static final class ReflexAndClosure {
      private final Closure<?> config;

      public ReflexAndClosure(Closure<?> config) {
         this.config = config;
      }
   }
}

