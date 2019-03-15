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
/**
 *
 */
package com.iris.driver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;
import com.iris.device.model.CommandDefinition;
import com.iris.device.model.EventDefinition;
import com.iris.driver.config.DriverConfigurationStateMachine;
import com.iris.driver.reflex.ReflexDefinition;
import com.iris.driver.reflex.ReflexDriver;
import com.iris.driver.reflex.ReflexDriverDefinition;
import com.iris.driver.reflex.ReflexRunMode;
import com.iris.messages.model.DriverId;
import com.iris.messages.type.Population;
import com.iris.model.Version;
import com.iris.util.IrisCollections;

/**
 *
 */
public class DeviceDriverDefinition {   

   public static DeviceDriverDefinition.Builder builder() {
      return new Builder();
   }

   private final DriverId id;
   private final String commit;
   private final String hash;
   private final String description;
   private final Set<AttributeDefinition> attributes;
   private final Set<CommandDefinition> commands;
   private final Set<EventDefinition> events;
   private final Set<CapabilityDefinition> capabilities;
   private final Set<String> populations;
   private final ReflexDriverDefinition reflexes;
   private final DriverConfigurationStateMachine configuration;

   DeviceDriverDefinition(
         String name,
         String description,
         Version version,
         String commit,
         String hash,
         Set<AttributeDefinition> attributes,
         Set<CommandDefinition> commands,
         Set<EventDefinition> events,
         Set<CapabilityDefinition> capabilities,
         ReflexRunMode mode,
         List<ReflexDefinition> reflexes,
         List<String> populations,
         DriverConfigurationStateMachine configuration,
         long offlineTimeout
   ) {
      this.id = new DriverId(name, version);
      this.description = description;
      this.commit = commit;
      this.hash = hash;
      this.attributes = IrisCollections.unmodifiableCopy(attributes);
      this.commands = IrisCollections.unmodifiableCopy(commands);
      this.events = IrisCollections.unmodifiableCopy(events);
      this.capabilities = IrisCollections.unmodifiableCopy(capabilities);
      this.populations = populations!=null?ImmutableSet.copyOf(populations):null;
      this.reflexes = new ReflexDriverDefinition(name, version, hash, offlineTimeout, capabilities, mode, reflexes);
      this.configuration = configuration;
   }

   public DriverId getId() {
      return id;
   }

   public String getName() {
      return id.getName();
   }

   public Version getVersion() {
      return id.getVersion();
   }

   public boolean isVersioned() {
      return !Version.UNVERSIONED.equals(id.getVersion());
   }

   public String getDescription() {
      return description;
   }

   public Set<String> getPopulations() {
   	if(populations != null) {
   		return populations;
   	}else{
   		return ImmutableSet.<String>of();
   	}
   }

   public Set<AttributeDefinition> getAttributes() {
      return attributes;
   }

   public int getMinimumRequiredReflexVersion() {
      return ReflexDriver.getMinimumRequiredReflexVersion(reflexes.getReflexes(), reflexes.getDfa());
   }

   /**
    * @return the commit
    */
   @Nullable
   public String getCommit() {
      return commit;
   }

   /**
    * @return the hash
    */
   @Nullable
   public String getHash() {
      return hash;
   }

   public Set<CommandDefinition> getCommands() {
      return commands;
   }

   public Set<EventDefinition> getEvents() {
      return events;
   }

   public Set<CapabilityDefinition> getCapabilities() {
      return capabilities;
   }

   public ReflexDriverDefinition getReflexes() {
      return reflexes;
   }

   public DriverConfigurationStateMachine getConfiguration() {
      return configuration;
   }

   public long getOfflineTimeout() {
      return reflexes.getOfflineTimeout();
   }

   @Override
   public String toString() {
      return "DeviceDriverDefinition [id=" + id + ", description="
            + description + ", attributes=" + attributes + ", commands="
            + commands + ", events=" + events + ", capabilities="
            + capabilities + "]";
   }

   public static class Builder {
      private String name;
      private String description;
      private Version version = Version.UNVERSIONED;
      private String commit;
      private String hash;
      private Set<AttributeDefinition> attributes = new LinkedHashSet<>();
      private Set<CommandDefinition> commands = new LinkedHashSet<>();
      private Set<EventDefinition> events = new LinkedHashSet<>();
      private Set<CapabilityDefinition> capabilities = new LinkedHashSet<>();
      private ReflexRunMode reflexRunMode = ReflexRunMode.defaultMode();
      private List<ReflexDefinition> reflexes = new ArrayList<>();
      private List<String> populations = null;
      private DriverConfigurationStateMachine configuration = null;
      private long offlineTimeout = Long.MAX_VALUE;

      /**
       * Live view of the associated attributes so that DeviceDriverBuilder
       * can prune the set for what's actually implemented.
       */
      Set<AttributeDefinition> attributes() {
         return attributes;
      }

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      public Builder withDescription(String description) {
         this.description = description;
         return this;
      }

      public Builder withVersion(Version version) {
         this.version = version;
         return this;
      }

      public Builder withCommit(String commit) {
         this.commit = commit;
         return this;
      }
      
      public Builder withHash(String hash) {
         this.hash = hash;
         return this;
      }
      
      public Builder addAttribute(AttributeDefinition attribute) {
         this.attributes.add(attribute);
         return this;
      }

      public Builder addAttributes(Collection<AttributeDefinition> attributes) {
         this.attributes.addAll(attributes);
         return this;
      }
      
      public Builder addCommand(CommandDefinition command) {
         this.commands.add(command);
         return this;
      }

      public Builder addEvent(EventDefinition event) {
         this.events.add(event);
         return this;
      }

      public Builder addCapability(CapabilityDefinition capability) {
         this.attributes.addAll(capability.getAttributes().values());
         this.commands.addAll(capability.getCommands().values());
         this.events.addAll(capability.getEvents().values());
         this.capabilities.add(capability);
         return this;
      }

      public Builder addCapabilities(Collection<CapabilityDefinition> capabilities) {
         for(CapabilityDefinition capability: capabilities) {
            addCapability(capability);
         }
         return this;
      }

      public Builder withReflexRunMode(ReflexRunMode mode) {
         this.reflexRunMode = mode;
         return this;
      }

      public Builder addReflex(ReflexDefinition reflex) {
         this.reflexes.add(reflex);
         return this;
      }

      public Builder addReflexes(Collection<ReflexDefinition> reflexes) {
         for(ReflexDefinition reflex : reflexes) {
            addReflex(reflex);
         }
         return this;
      }

      public Builder addConfigurationStateMachine(DriverConfigurationStateMachine configuration) {
         this.configuration = configuration;
         return this;
      }

      public Builder withOfflineTimeout(long offlineTimeout) {
         this.offlineTimeout = offlineTimeout;
         return this;
      }

      public Builder withPopulations(List<String> populations) {
         this.populations = populations;
         return this;
      }

      public DeviceDriverDefinition create() {
         Preconditions.checkArgument(StringUtils.isNotEmpty(name), "Must specify a name");
         return new DeviceDriverDefinition(name, description, version, commit, hash, attributes, commands, events, capabilities, reflexRunMode, reflexes, populations, configuration, offlineTimeout);
      }

   }

}

