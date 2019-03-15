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
package com.iris.oculus.modules.device.mockaction;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// Quick and dirty utility for generating a bunch of mock actions from a simple file format.

public class MockGen {
   
   private final static String FILENAME="/home/erik/iris/workspace/tools/oculus/src/dist/mockimport.txt";
   private final static String OUTPUTFILE="/home/erik/mock.json";
   
   public static void main(String[] args) {
      NavigableMap<String,List<MockAction>> actionMap = new TreeMap<>();
      String line = null;
      String currentCap = null;
      List<Step> currentSteps = null;
      String currentAction = null;
      try (BufferedReader br = new BufferedReader(new FileReader(FILENAME))) {
         while ((line = br.readLine()) != null) {
            System.out.println("Processing Line: " + line);
            if (line.startsWith("#")) {
               if (currentCap != null && currentAction != null && currentSteps != null && currentSteps.size() > 0) {
                  MockAction ma = new MockAction();
                  ma.setName(currentAction);
                  ma.setSteps(currentSteps);
                  actionMap.computeIfAbsent(currentCap, (c) -> new ArrayList<>()).add(ma);
                  currentAction = null;
                  currentSteps = null;
               }
               currentCap = line.substring(1).trim().toLowerCase();
            }
            else if (line.startsWith("::")) {
               String s = line.substring(2).trim();
               String[] fields = s.split(",");
               Step step = new Step();
               step.setType("attr");
               step.setAttr(currentCap + ":" + fields[0].trim().toLowerCase());
               step.setValue(fields[1].trim());
               currentSteps.add(step);
            }
            else if (line.startsWith(":")) {
               if (currentCap != null && currentAction != null && currentSteps != null && currentSteps.size() > 0) {
                  MockAction ma = new MockAction();
                  ma.setName(currentAction);
                  ma.setSteps(currentSteps);
                  actionMap.computeIfAbsent(currentCap, (c) -> new ArrayList<>()).add(ma);
               }
               currentAction = line.substring(1).trim();
               currentSteps = new ArrayList<>();
            }
         }
      } catch (FileNotFoundException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      
      if (currentCap != null && currentAction != null && currentSteps != null && currentSteps.size() > 0) {
         MockAction ma = new MockAction();
         ma.setName(currentAction);
         ma.setSteps(currentSteps);
         actionMap.computeIfAbsent(currentCap, (c) -> new ArrayList<>()).add(ma);
      }
      
      MockActions mas = new MockActions();
      mas.setActionMap(actionMap);
      
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      String json = gson.toJson(mas);
      try (FileWriter fw = new FileWriter(OUTPUTFILE)) {
         fw.write(json);
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }
}

