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
package com.iris.google;

import org.apache.commons.math3.util.Precision;

public class Colors {

   private Colors() {
   }

   public static class HSB {

      private final int hue;
      private final float saturation;
      private final float brightness;

      public HSB(int hue, int saturationPercent, int brightnessPercent) {
         this(hue, ((float) saturationPercent) / 100, ((float) brightnessPercent) / 100);
      }

      public HSB(int hue, float saturation, float brightness) {
         this.hue = hue;
         this.saturation = saturation;
         this.brightness = brightness;
      }

      public RGB toRGB() {
         // http://www.rapidtables.com/convert/color/hsv-to-rgb.htm
         float c = brightness * saturation;
         float x = c * (1 - Math.abs(((hue / 60) % 2) - 1));
         float m = brightness - c;

         float rp = 0.0f;
         float gp = 0.0f;
         float bp = 0.0f;

         if(hue >= 0 && hue < 60) {
            rp = c; gp = x;
         } else if(hue >= 60 && hue < 120) {
            rp = x; gp = c;
         } else if(hue >= 120 && hue < 180) {
            gp = c; bp = x;
         } else if(hue >= 180 && hue < 240) {
            bp = x; gp = c;
         } else if(hue >= 240 && hue < 300) {
            rp = x; bp = c;
         } else if(hue >= 300 && hue < 360) {
            rp = c; bp = x;
         }

         int r = (int) Math.ceil((rp + m) * 255);
         int g = (int) Math.ceil((gp + m) * 255);
         int b = (int) Math.ceil((bp + m) * 255);

         return new RGB(r, g, b);
      }

      public int hue() { return hue; }
      public float brightness() { return brightness; }
      public int brightnessPercent() { return (int) (brightness * 100); }
      public float saturation() { return saturation; }
      public int saturationPercent() { return (int) (saturation * 100); }

      public String toString() {
         return "hsb(" + hue() + ", " + saturationPercent() + "%, " + brightnessPercent() + "%)";
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         HSB hsb = (HSB) o;

         if (hue != hsb.hue) return false;
         if (saturationPercent() != hsb.saturationPercent()) return false;
         return brightnessPercent() == hsb.brightnessPercent();
      }

      @Override
      public int hashCode() {
         int result = hue;
         result = 31 * result + saturationPercent();
         result = 31 * result + brightnessPercent();
         return result;
      }
   }

   public static class RGB {

      private final int r;
      private final int g;
      private final int b;

      public RGB(int r, int g, int b) {
         this.r = r;
         this.g = g;
         this.b = b;
      }

      public RGB(int v) {
         r = (v & 0x00FF0000) >> 16;
         g = (v & 0x0000FF00) >> 8;
         b = (v & 0x000000FF);
      }

      public int toInt() {
         int rgb = r;
         rgb = (rgb << 8) + g;
         return (rgb << 8) + b;
      }

      public HSB toHSB() {
         // http://www.rapidtables.com/convert/color/rgb-to-hsv.htm
         float rp = ((float) r)/255;
         float gp = ((float) g)/255;
         float bp = ((float) b)/255;

         float cmax = Math.max(Math.max(rp, gp), bp);
         float cmin = Math.min(Math.min(rp, gp), bp);
         float d = cmax - cmin;
         float s = 0.0f;
         float h = 0.0f;

         if(cmax != 0) {
            s = d / cmax;
         }

         if(Precision.equals(cmax, bp)) {
            h = 60 * (((rp - gp) / d) + 4);
         } else if(Precision.equals(cmax, gp)) {
            h = 60 * (((bp - rp) / d) + 2);
         } else if(Precision.equals(cmax, rp)) {
            h = 60 * (((gp - bp) / d) % 6);
         }

         return new HSB((int) h, s, cmax);
      }

      public int red() { return r; }
      public int green() { return g; }
      public int blue() { return b; }

      public String toString() {
         return "rgb(" + r + ", "+ g + ", " + b + ')';
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         RGB rgb = (RGB) o;

         if (r != rgb.r) return false;
         if (g != rgb.g) return false;
         return b == rgb.b;
      }

      @Override
      public int hashCode() {
         int result = r;
         result = 31 * result + g;
         result = 31 * result + b;
         return result;
      }
   }
}

