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
package com.iris.agent.logging;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

public final class IrisAgentLogging {
   private static final String DEFAULT_LEVEL = "debug";

   private IrisAgentLogging() {
   }

   public static ch.qos.logback.classic.Level getLogLevel(String level) {
      if ("trace".equalsIgnoreCase(level)) return ch.qos.logback.classic.Level.TRACE;
      if ("debug".equalsIgnoreCase(level)) return ch.qos.logback.classic.Level.DEBUG;
      if ("info".equalsIgnoreCase(level)) return ch.qos.logback.classic.Level.INFO;
      if ("warn".equalsIgnoreCase(level)) return ch.qos.logback.classic.Level.WARN;
      if ("error".equalsIgnoreCase(level)) return ch.qos.logback.classic.Level.ERROR;
      return ch.qos.logback.classic.Level.INFO;
   }

   public static IrisAgentAppender getInMemoryAppender() {
      Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
      return (com.iris.agent.logging.IrisAgentAppender)logger.getAppender("MEMORY");
   }

   public static void setRootLogLevel(String level) {
      Logger log = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
      if (log != null) {
         log.setLevel(getLogLevel(level));
      }
   }

   public static void setAgentLogLevel(String level) {
      Logger log = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.iris.agent");
      if (log != null) {
         log.setLevel(getLogLevel(level));
      }
   }

   public static void setZigbeeLogLevel(String level) {
      Logger log = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.iris.agent.zigbee");
      if (log != null) {
         log.setLevel(getLogLevel(level));
      }
   }

   public static void setZWaveLogLevel(String level) {
      Logger log = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.iris.agent.zwave");
      if (log != null) {
         log.setLevel(getLogLevel(level));
      }
   }

   public static void setBleLogLevel(String level) {
      Logger log = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.iris.agent.ble");
      if (log != null) {
         log.setLevel(getLogLevel(level));
      }
   }

   public static void setSercommLogLevel(String level) {
      Logger log = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.iris.agent.controller.sercomm");
      if (log != null) {
         log.setLevel(getLogLevel(level));
      }
   }

   public static void setupInitialLogging() {
      Logger rootLog = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

      String rootType = System.getenv("IRIS_AGENT_LOGTYPE");
      if ("dev".equalsIgnoreCase(rootType)) {
         rootLog.detachAppender("STDOUT");
      } else {
         rootLog.detachAppender("DEV");
      }

      resetLogLevels();
   }

   public static void resetLogLevels() {
      String rootLevel = System.getenv("IRIS_AGENT_ROOT_LOGLVL");
      setRootLogLevel(rootLevel != null ? rootLevel : DEFAULT_LEVEL);

      String agentLevel = System.getenv("IRIS_AGENT_AGENT_LOGLVL");
      setAgentLogLevel(agentLevel != null ? agentLevel : DEFAULT_LEVEL);

      String zigbeeLevel = System.getenv("IRIS_AGENT_ZIGBEE_LOGLVL");
      setZigbeeLogLevel(zigbeeLevel != null ? zigbeeLevel : DEFAULT_LEVEL);

      String zwaveLevel = System.getenv("IRIS_AGENT_ZWAVE_LOGLVL");
      setZWaveLogLevel(zwaveLevel != null ? zwaveLevel : DEFAULT_LEVEL);

      String bleLevel = System.getenv("IRIS_AGENT_BLE_LOGLVL");
      setBleLogLevel(bleLevel != null ? bleLevel : DEFAULT_LEVEL);

      String sercommLevel = System.getenv("IRIS_AGENT_SERCOMM_LOGLVL");
      setSercommLogLevel(sercommLevel != null ? sercommLevel : DEFAULT_LEVEL);
   }

   public static String getInMemoryLogs(boolean compress) {
      Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
      IrisAgentAppender appender = (com.iris.agent.logging.IrisAgentAppender)logger.getAppender("MEMORY");
      if (appender == null) {
         return "";
      }

      String logs = appender.getLogs();
      if (!compress) {
         return logs;
      }

      byte[] data = logs.getBytes(StandardCharsets.UTF_8);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (GZIPOutputStream os = new GZIPOutputStream(baos)) {
         os.write(data);
      } catch (Exception ex) {
      }

      byte[] compressed = baos.toByteArray();
      return Base64.encodeBase64String(compressed);
   }
}

