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
package com.iris.alexa.message.v2;

public class Color {

   private double hue;
   private double saturation;
   private double brightness;

   public double getHue() {
      return hue;
   }

   public void setHue(double hue) {
      this.hue = hue;
   }

   public double getSaturation() {
      return saturation;
   }

   public void setSaturation(double saturation) {
      this.saturation = saturation;
   }

   public double getBrightness() {
      return brightness;
   }

   public void setBrightness(double brightness) {
      this.brightness = brightness;
   }

   @Override
   public String toString() {
      return "Color{" +
         "hue=" + hue +
         ", saturation=" + saturation +
         ", brightness=" + brightness +
         '}';
   }
}

