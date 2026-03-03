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
package com.iris.platform.rule.catalog.selector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.SimpleModel;

public class TestInstanceSelectorGenerator {

   private SimpleContext context;
   private SimpleModel deviceA;
   private SimpleModel deviceB;
   private SimpleModel deviceNoInstances;

   @Before
   public void setUp() {
      UUID placeId = UUID.randomUUID();
      context = new SimpleContext(placeId, Address.platformService("rule"), LoggerFactory.getLogger("test"));

      // Device A: buttons "main", "top-left", "top-right"
      deviceA = createDevice("devA");
      Map<String, Set<String>> instancesA = new HashMap<>();
      instancesA.put("main", new HashSet<>(Arrays.asList("but", "swit")));
      instancesA.put("top-left", new HashSet<>(Arrays.asList("but")));
      instancesA.put("top-right", new HashSet<>(Arrays.asList("but")));
      deviceA.setAttribute(Capability.ATTR_INSTANCES, instancesA);
      context.putModel(deviceA);

      // Device B: buttons "up", "down"
      deviceB = createDevice("devB");
      Map<String, Set<String>> instancesB = new HashMap<>();
      instancesB.put("up", new HashSet<>(Arrays.asList("but")));
      instancesB.put("down", new HashSet<>(Arrays.asList("but")));
      deviceB.setAttribute(Capability.ATTR_INSTANCES, instancesB);
      context.putModel(deviceB);

      // Device with no button instances
      deviceNoInstances = createDevice("devC");
      deviceNoInstances.setAttribute(Capability.ATTR_INSTANCES, Collections.emptyMap());
      context.putModel(deviceNoInstances);
   }

   @Test
   public void testIsSatisfiableWithButtonDevices() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("but");
      assertTrue(generator.isSatisfiable(context));
   }

   @Test
   public void testIsSatisfiableWithNoMatchingDevices() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("dim");
      assertFalse(generator.isSatisfiable(context));
   }

   @Test
   public void testGenerateReturnsAllInstances() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("but");
      Selector selector = generator.generate(context);

      ListSelector list = (ListSelector) selector;
      List<String> values = optionValues(list);
      assertEquals(5, values.size());
      assertTrue(values.contains("main"));
      assertTrue(values.contains("top-left"));
      assertTrue(values.contains("top-right"));
      assertTrue(values.contains("up"));
      assertTrue(values.contains("down"));
      assertNull(list.getDependsOn());
   }

   @Test
   public void testGenerateNoDependsOnIgnoresSelections() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("but");
      Map<String, String> selections = new HashMap<>();
      selections.put("device", deviceA.getAddress().getRepresentation());

      Selector selector = generator.generate(context, selections);

      // Without dependsOn, selections are ignored — returns all instances
      ListSelector list = (ListSelector) selector;
      assertEquals(5, optionValues(list).size());
      assertNull(list.getDependsOn());
   }

   @Test
   public void testGenerateWithDependsOnNoSelectionReturnsAll() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("but", "device");
      Selector selector = generator.generate(context, Collections.emptyMap());

      // No selection provided — returns all instances for backward compat
      ListSelector list = (ListSelector) selector;
      List<String> values = optionValues(list);
      assertEquals(5, values.size());
      assertNotNull(list.getDependsOn());
      assertEquals("device", list.getDependsOn());
   }

   @Test
   public void testGenerateWithDependsOnNullSelectionsReturnsAll() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("but", "device");
      Selector selector = generator.generate(context, null);

      // Null selections — returns all instances for backward compat
      ListSelector list = (ListSelector) selector;
      assertEquals(5, optionValues(list).size());
      assertEquals("device", list.getDependsOn());
   }

   @Test
   public void testGenerateWithDependsOnDeviceASelected() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("but", "device");
      Map<String, String> selections = new HashMap<>();
      selections.put("device", deviceA.getAddress().getRepresentation());

      Selector selector = generator.generate(context, selections);

      ListSelector list = (ListSelector) selector;
      List<String> values = optionValues(list);
      assertEquals(3, values.size());
      assertTrue(values.contains("main"));
      assertTrue(values.contains("top-left"));
      assertTrue(values.contains("top-right"));
      assertEquals("device", list.getDependsOn());
   }

   @Test
   public void testGenerateWithDependsOnDeviceBSelected() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("but", "device");
      Map<String, String> selections = new HashMap<>();
      selections.put("device", deviceB.getAddress().getRepresentation());

      Selector selector = generator.generate(context, selections);

      ListSelector list = (ListSelector) selector;
      List<String> values = optionValues(list);
      assertEquals(2, values.size());
      assertTrue(values.contains("up"));
      assertTrue(values.contains("down"));
      assertEquals("device", list.getDependsOn());
   }

   @Test
   public void testGenerateWithDependsOnDeviceNoInstances() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("but", "device");
      Map<String, String> selections = new HashMap<>();
      selections.put("device", deviceNoInstances.getAddress().getRepresentation());

      Selector selector = generator.generate(context, selections);

      ListSelector list = (ListSelector) selector;
      assertTrue(optionValues(list).isEmpty());
      assertEquals("device", list.getDependsOn());
   }

   @Test
   public void testGenerateWithDependsOnUnknownDevice() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("but", "device");
      Map<String, String> selections = new HashMap<>();
      selections.put("device", Address.platformDriverAddress(UUID.randomUUID()).getRepresentation());

      Selector selector = generator.generate(context, selections);

      // Unknown device — return empty options with hint
      ListSelector list = (ListSelector) selector;
      assertTrue(optionValues(list).isEmpty());
      assertEquals("device", list.getDependsOn());
   }

   @Test
   public void testGenerateFiltersToCapabilityNamespace() {
      // Only "main" has "swit" in its capability set
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("swit");
      Selector selector = generator.generate(context);

      ListSelector list = (ListSelector) selector;
      List<String> values = optionValues(list);
      assertEquals(1, values.size());
      assertTrue(values.contains("main"));
   }

   @Test
   public void testToDisplayLabel() {
      assertEquals("Button 1", InstanceSelectorGenerator.toDisplayLabel("button1"));
      assertEquals("Button 4", InstanceSelectorGenerator.toDisplayLabel("button4"));
      assertEquals("Outlet 2", InstanceSelectorGenerator.toDisplayLabel("outlet2"));
      assertEquals("Relay 1", InstanceSelectorGenerator.toDisplayLabel("relay1"));
      assertEquals("Main", InstanceSelectorGenerator.toDisplayLabel("main"));
      assertEquals("Up", InstanceSelectorGenerator.toDisplayLabel("up"));
      assertEquals("Down", InstanceSelectorGenerator.toDisplayLabel("down"));
      assertEquals("Square", InstanceSelectorGenerator.toDisplayLabel("square"));
      assertEquals("Top Left", InstanceSelectorGenerator.toDisplayLabel("top-left"));
      assertEquals("Bottom Right", InstanceSelectorGenerator.toDisplayLabel("bottom-right"));
   }

   @Test
   public void testGenerateProducesFriendlyLabels() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("but");
      Selector selector = generator.generate(context);

      ListSelector list = (ListSelector) selector;
      List<String> labels = optionLabels(list);
      assertTrue(labels.contains("Main"));
      assertTrue(labels.contains("Top Left"));
      assertTrue(labels.contains("Top Right"));
      assertTrue(labels.contains("Up"));
      assertTrue(labels.contains("Down"));
   }

   @Test
   public void testValidateValidInstance() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("but", "device");
      Map<String, Object> variables = new HashMap<>();
      variables.put("device", deviceA.getAddress().getRepresentation());
      variables.put("button", "main");

      String error = generator.validate(context, "main", variables);
      assertNull(error);
   }

   @Test
   public void testValidateInvalidInstance() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("but", "device");
      Map<String, Object> variables = new HashMap<>();
      variables.put("device", deviceB.getAddress().getRepresentation());
      variables.put("button", "main");

      // "main" doesn't exist on deviceB
      String error = generator.validate(context, "main", variables);
      assertNotNull(error);
   }

   @Test
   public void testValidateNoDependsOnSkipsValidation() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("but");
      Map<String, Object> variables = new HashMap<>();
      variables.put("device", deviceA.getAddress().getRepresentation());

      String error = generator.validate(context, "nonexistent", variables);
      assertNull(error);
   }

   @Test
   public void testValidateInstanceOnDeviceWithNoInstances() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("but", "device");
      Map<String, Object> variables = new HashMap<>();
      variables.put("device", deviceNoInstances.getAddress().getRepresentation());
      variables.put("button", "button1");

      // deviceNoInstances has empty instances — button1 doesn't exist
      String error = generator.validate(context, "button1", variables);
      assertNotNull(error);
   }

   @Test
   public void testValidateNoDeviceSelectedSkipsValidation() {
      InstanceSelectorGenerator generator = new InstanceSelectorGenerator("but", "device");
      Map<String, Object> variables = new HashMap<>();

      String error = generator.validate(context, "main", variables);
      assertNull(error);
   }

   private SimpleModel createDevice(String id) {
      SimpleModel model = new SimpleModel();
      model.setAttribute(Capability.ATTR_TYPE, "dev");
      model.setAttribute(Capability.ATTR_ID, id);
      model.setAttribute(Capability.ATTR_ADDRESS, Address.platformDriverAddress(UUID.nameUUIDFromBytes(id.getBytes())).getRepresentation());
      return model;
   }

   private List<String> optionValues(ListSelector selector) {
      List<String> values = new ArrayList<>();
      for (Option o : selector.getOptions()) {
         values.add((String) o.getValue());
      }
      return values;
   }

   private List<String> optionLabels(ListSelector selector) {
      List<String> labels = new ArrayList<>();
      for (Option o : selector.getOptions()) {
         labels.add(o.getLabel());
      }
      return labels;
   }
}
