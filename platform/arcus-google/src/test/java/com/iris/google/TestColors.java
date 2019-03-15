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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestColors {

   @Test
   public void testBlack() {
      Colors.RGB c = new Colors.RGB(0, 0, 0);
      assertEquals(0, c.toInt());
      assertEquals(new Colors.HSB(0, 0, 0), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

   @Test
   public void testWhite() {
      Colors.RGB c = new Colors.RGB(255,255, 255);
      assertEquals(16777215, c.toInt());
      assertEquals(new Colors.HSB(0, 0, 100), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

   @Test
   public void testRed() {
      Colors.RGB c = new Colors.RGB(255,0, 0);
      assertEquals(16711680, c.toInt());
      assertEquals(new Colors.HSB(0, 100, 100), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

   @Test
   public void testLime() {
      Colors.RGB c = new Colors.RGB(0,255, 0);
      assertEquals(65280, c.toInt());
      assertEquals(new Colors.HSB(120, 100, 100), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

   @Test
   public void testBlue() {
      Colors.RGB c = new Colors.RGB(0, 0, 255);
      assertEquals(255, c.toInt());
      assertEquals(new Colors.HSB(240, 100, 100), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

   @Test
   public void testYellow() {
      Colors.RGB c = new Colors.RGB(255, 255, 0);
      assertEquals(16776960, c.toInt());
      assertEquals(new Colors.HSB(60, 100, 100), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

   @Test
   public void testCyan() {
      Colors.RGB c = new Colors.RGB(0, 255, 255);
      assertEquals(65535, c.toInt());
      assertEquals(new Colors.HSB(180, 100, 100), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

   @Test
   public void testMagenta() {
      Colors.RGB c = new Colors.RGB(255, 0, 255);
      assertEquals(16711935, c.toInt());
      assertEquals(new Colors.HSB(300, 100, 100), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

   @Test
   public void testSilver() {
      Colors.RGB c = new Colors.RGB(192, 192, 192);
      assertEquals(12632256, c.toInt());
      assertEquals(new Colors.HSB(0, 0, 75), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

   @Test
   public void testGray() {
      Colors.RGB c = new Colors.RGB(128, 128, 128);
      assertEquals(8421504, c.toInt());
      assertEquals(new Colors.HSB(0, 0, 50), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

   @Test
   public void testMaroon() {
      Colors.RGB c = new Colors.RGB(128, 0, 0);
      assertEquals(8388608, c.toInt());
      assertEquals(new Colors.HSB(0, 100, 50), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

   @Test
   public void testOlive() {
      Colors.RGB c = new Colors.RGB(128, 128, 0);
      assertEquals(8421376, c.toInt());
      assertEquals(new Colors.HSB(60, 100, 50), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

   @Test
   public void testGreen() {
      Colors.RGB c = new Colors.RGB(0, 128, 0);
      assertEquals(32768, c.toInt());
      assertEquals(new Colors.HSB(120, 100, 50), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

   @Test
   public void testPurple() {
      Colors.RGB c = new Colors.RGB(128, 0, 128);
      assertEquals(8388736, c.toInt());
      assertEquals(new Colors.HSB(300, 100, 50), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

   @Test
   public void testTeal() {
      Colors.RGB c = new Colors.RGB(0, 128, 128);
      assertEquals(32896, c.toInt());
      assertEquals(new Colors.HSB(180, 100, 50), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

   @Test
   public void testNavy() {
      Colors.RGB c = new Colors.RGB(0, 0, 128);
      assertEquals(128, c.toInt());
      assertEquals(new Colors.HSB(240, 100, 50), c.toHSB());
      assertEquals(c, c.toHSB().toRGB());
   }

}

