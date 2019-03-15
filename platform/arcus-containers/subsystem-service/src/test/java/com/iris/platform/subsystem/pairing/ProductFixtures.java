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
package com.iris.platform.subsystem.pairing;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.messages.service.BridgeService;
import com.iris.model.Version;
import com.iris.prodcat.Input;
import com.iris.prodcat.Input.InputType;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogEntry.BatterySize;
import com.iris.prodcat.ProductCatalogEntry.Cert;
import com.iris.prodcat.Step;
import com.iris.prodcat.Step.StepType;

// FIXME push down to ModelFixtures
public class ProductFixtures {
   public static ProductCatalogEntryBuilder buildProduct() {
      return new ProductCatalogEntryBuilder();
   }

   public static Map<String, String> ipcdForm() {
      return ImmutableMap.of("IPCD:sn", "serialNumber", "IPCD:v1devicetype", "devicetype");
   }
   
   public static class ProductCatalogEntryBuilder {
      private ProductCatalogEntry entry = new ProductCatalogEntry();

      private ProductCatalogEntryBuilder() {
         entry.setAdded(new Date());
         entry.setAppRequired(false);
         entry.setCanDiscover(true);
         entry.setBlacklisted(false);
         entry.setCategories(ImmutableList.of("test"));
         entry.setProtoFamily("mock");
         entry.setProtoSpec("test");
      }
      
      public ProductCatalogEntryBuilder browseable() {
         entry.setCanBrowse(true);
         entry.setCanSearch(true);
         return this;
      }
      
      public ProductCatalogEntryBuilder notBrowseable() {
         entry.setCanBrowse(true);
         entry.setCanSearch(true);
         return this;
      }

      public ProductCatalogEntryBuilder bridged() {
         withProtoFamily("Proprietary");
         withDevRequired("aeda43");
         withPairingMode(ProductCatalogEntry.PairingMode.BRIDGED_DEVICE);
         Step step = new Step();
         step.setText("Pair it");
         step.setType(StepType.TEXT);
         withPair(ImmutableList.of(step));
         return this;
      }

      public ProductCatalogEntryBuilder ipcd() {
         withProtoFamily("IPCD");
         Input serialNumber = new Input();
         serialNumber.setName("IPCD:sn");
         serialNumber.setLabel("Serial Number");
         serialNumber.setRequired(true);
         serialNumber.setType(InputType.TEXT);
         Input model = new Input();
         model.setName("IPCD:v1devicetype");
         model.setRequired(true);
         model.setType(InputType.HIDDEN);
         model.setValue("test");
         Step step = new Step();
         step.setText("Pair it");
         step.setType(StepType.INPUT);
         step.setInputs(ImmutableList.of(serialNumber, model));
         step.setTarget("BRDG::IPCD");
         step.setMessage(BridgeService.RegisterDeviceRequest.NAME);
         withPair(ImmutableList.of(step));
         return this;
      }
      
      public ProductCatalogEntryBuilder oauth() {
         withProtoFamily("Cloud-to-cloud");
         withManufacturer("Nest");
         withVendor("Nest");
         Step step = new Step();
         step.setText("Pair it");
         step.setType(StepType.TEXT);
         withPair(ImmutableList.of(step));
         return this;
      }
      
      public ProductCatalogEntryBuilder zigbee() {
         withProtoFamily("ZigBee");
         withHubRequired(true);
         Step step = new Step();
         step.setText("Pair it");
         step.setType(StepType.TEXT);
         withPair(ImmutableList.of(step));
         return this;
      }
      
      public ProductCatalogEntryBuilder zwave() {
         withProtoFamily("Z-Wave");
         withHubRequired(true);
         {
            Step step = new Step();
            step.setText("Cross Your Fingers");
            step.setType(StepType.TEXT);
            withPair(ImmutableList.of(step));
         }
         {
            Step step = new Step();
            step.setText("Abandon Hope All Ye Who Enter Here");
            step.setType(StepType.TEXT);
            withRemoval(ImmutableList.of(step));
         }
         return this;
      }
      
      public ProductCatalogEntryBuilder withId(String id) {
         entry.setId(id);
         return this;
      }

      public ProductCatalogEntryBuilder withName(String name) {
         entry.setName(name);
         return this;
      }

      public ProductCatalogEntryBuilder withShortName(String shortName) {
         entry.setShortName(shortName);
         return this;
      }

      public ProductCatalogEntryBuilder withDescription(String description) {
         entry.setDescription(description);
         return this;
      }

      public ProductCatalogEntryBuilder withManufacturer(String manufacturer) {
         entry.setManufacturer(manufacturer);
         return this;
      }

      public ProductCatalogEntryBuilder withVendor(String vendor) {
         entry.setVendor(vendor);
         return this;
      }

      public ProductCatalogEntryBuilder withAddDevImg(String addDevImg) {
         entry.setAddDevImg(addDevImg);
         return this;
      }

      public ProductCatalogEntryBuilder withCert(Cert cert) {
         entry.setCert(cert);
         return this;
      }

      public ProductCatalogEntryBuilder withCanBrowse(Boolean canBrowse) {
         entry.setCanBrowse(canBrowse);
         return this;
      }

      public ProductCatalogEntryBuilder withCanSearch(Boolean canSearch) {
         entry.setCanSearch(canSearch);
         return this;
      }

      public ProductCatalogEntryBuilder withHelpUrl(String helpUrl) {
         entry.setHelpUrl(helpUrl);
         return this;
      }

      public ProductCatalogEntryBuilder withPairVideoUrl(String pairVideoUrl) {
         entry.setPairVideoUrl(pairVideoUrl);
         return this;
      }

      public ProductCatalogEntryBuilder withInstructionsUrl(String instructionsUrl) {
         entry.setInstructionsUrl(instructionsUrl);
         return this;
      }

      public ProductCatalogEntryBuilder withBatteryPrimSize(BatterySize batteryPrimSize) {
         entry.setBatteryPrimSize(batteryPrimSize);
         return this;
      }

      public ProductCatalogEntryBuilder withBatteryPrimNum(Integer batteryPrimNum) {
         entry.setBatteryPrimNum(batteryPrimNum);
         return this;
      }

      public ProductCatalogEntryBuilder withBatteryBackSize(BatterySize batteryBackSize) {
         entry.setBatteryBackSize(batteryBackSize);
         return this;
      }

      public ProductCatalogEntryBuilder withBatteryBackNum(Integer batteryBackNum) {
         entry.setBatteryBackNum(batteryBackNum);
         return this;
      }

      public ProductCatalogEntryBuilder withKeywords(String keywords) {
         entry.setKeywords(keywords);
         return this;
      }

      public ProductCatalogEntryBuilder withOTA(Boolean oTA) {
         entry.setOTA(oTA);
         return this;
      }

      public ProductCatalogEntryBuilder withProtoFamily(String protoFamily) {
         entry.setProtoFamily(protoFamily);
         return this;
      }

      public ProductCatalogEntryBuilder withProtoSpec(String protoSpec) {
         entry.setProtoSpec(protoSpec);
         return this;
      }

      public ProductCatalogEntryBuilder withDriver(String driver) {
         entry.setDriver(driver);
         return this;
      }

      public ProductCatalogEntryBuilder withAdded(Date added) {
         entry.setAdded(added);
         return this;
      }

      public ProductCatalogEntryBuilder withLastChanged(Date lastChanged) {
         entry.setLastChanged(lastChanged);
         return this;
      }

      public ProductCatalogEntryBuilder withCategories(List<String> categories) {
         entry.setCategories(categories);
         return this;
      }

      public ProductCatalogEntryBuilder withPopulations(List<String> populations) {
         entry.setPopulations(populations);
         return this;
      }

      public ProductCatalogEntryBuilder withPair(List<Step> pair) {
         entry.setPair(pair);
         return this;
      }

      public ProductCatalogEntryBuilder withRemoval(List<Step> removal) {
         entry.setRemoval(removal);
         return this;
      }

      public ProductCatalogEntryBuilder withReset(List<Step> reset) {
         entry.setReset(reset);
         return this;
      }

      public ProductCatalogEntryBuilder withScreen(String screen) {
         entry.setScreen(screen);
         return this;
      }

      public ProductCatalogEntryBuilder withHubRequired(Boolean hubRequired) {
         entry.setHubRequired(hubRequired);
         return this;
      }

      public ProductCatalogEntryBuilder withPairingMode(ProductCatalogEntry.PairingMode pairingMode) {
         entry.setPairingMode(pairingMode);
         return this;
      }

      public ProductCatalogEntryBuilder withDevRequired(String devRequired) {
         entry.setDevRequired(devRequired);
         return this;
      }

      public ProductCatalogEntryBuilder withMinAppVersion(String minAppVersion) {
         entry.setMinAppVersion(minAppVersion);
         return this;
      }

      public ProductCatalogEntryBuilder withMinHubFirmware(Version minHubFirmware) {
         entry.setMinHubFirmware(minHubFirmware);
         return this;
      }

      public ProductCatalogEntryBuilder withCanDiscover(Boolean canDiscover) {
         entry.setCanDiscover(canDiscover);
         return this;
      }

      public ProductCatalogEntryBuilder withAppRequired(Boolean appRequired) {
         entry.setAppRequired(appRequired);
         return this;
      }

      public ProductCatalogEntryBuilder withInstallManualUrl(String installManualUrl) {
         entry.setInstallManualUrl(installManualUrl);
         return this;
      }

      public ProductCatalogEntryBuilder withBlacklisted(Boolean blacklisted) {
         entry.setBlacklisted(blacklisted);
         return this;
      }
      
      public ProductCatalogEntryBuilder withPairingIdleTimeoutMs(Integer timeout) {
         entry.setPairingIdleTimeoutMs(timeout);
         return this;
      }
      
      public ProductCatalogEntryBuilder withPairingTimeoutMs(Integer timeout) {
         entry.setPairingTimeoutMs(timeout);
         return this;
      }

      public ProductCatalogEntry build() {
         return entry;
      }
      
   }
}

