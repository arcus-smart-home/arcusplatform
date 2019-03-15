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
package com.iris.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class TestVersion {

   @Test
   public void testFromRepresentionNoQualifier() {
      Version v = Version.fromRepresentation("1.0");
      assertEquals(1, v.getMajor());
      assertEquals(0, v.getMinor());
      assertEquals(false, v.hasQualifier());
      assertEquals(false, v.hasPatch());
      assertEquals(false, v.hasBuild());
      assertEquals("", v.getQualifier());
      assertEquals("1.0", v.getRepresentation());
      assertEquals(new Version(1, 0), v);

      v = Version.fromRepresentation("1.0.0");
      assertEquals(1, v.getMajor());
      assertEquals(0, v.getMinor());
      assertEquals(false, v.hasQualifier());
      assertEquals(true, v.hasPatch());
      assertEquals(Integer.valueOf(0), v.getPatch());
      assertEquals(false, v.hasBuild());
      assertEquals("", v.getQualifier());
      assertEquals("1.0.0", v.getRepresentation());
      assertEquals(new Version(1, 0, 0, null, null, "1.0.0"), v);

      v = Version.fromRepresentation("1.0.0.0");
      assertEquals(1, v.getMajor());
      assertEquals(0, v.getMinor());
      assertEquals(false, v.hasQualifier());
      assertEquals(true, v.hasPatch());
      assertEquals(Integer.valueOf(0), v.getPatch());
      assertEquals(true, v.hasBuild());
      assertEquals(Integer.valueOf(0), v.getBuild());
      assertEquals("", v.getQualifier());
      assertEquals("1.0.0.0", v.getRepresentation());
      assertEquals(new Version(1, 0, 0, 0, null, "1.0.0.0"), v);
   }

   @Test
   public void testFromRepresentionWithQualifier() {
      Version v = Version.fromRepresentation("7.104-SNAPSHOT_NIGHTLY");
      assertEquals(7, v.getMajor());
      assertEquals(104, v.getMinor());
      assertEquals(true, v.hasQualifier());
      assertEquals("SNAPSHOT_NIGHTLY", v.getQualifier());
      assertEquals("7.104-SNAPSHOT_NIGHTLY", v.getRepresentation());
      assertEquals(new Version(7, 104, "SNAPSHOT_NIGHTLY"), v);

      v = Version.fromRepresentation("1.0.0-SNAPSHOT_NIGHTLY");
      assertEquals(1, v.getMajor());
      assertEquals(0, v.getMinor());
      assertEquals(true, v.hasQualifier());
      assertEquals(true, v.hasPatch());
      assertEquals(Integer.valueOf(0), v.getPatch());
      assertEquals(false, v.hasBuild());
      assertEquals("SNAPSHOT_NIGHTLY", v.getQualifier());
      assertEquals("1.0.0-SNAPSHOT_NIGHTLY", v.getRepresentation());
      assertEquals(new Version(1, 0, 0, null, "SNAPSHOT_NIGHTLY", "1.0.0-SNAPSHOT_NIGHTLY"), v);

      v = Version.fromRepresentation("1.0.0.0-SNAPSHOT_NIGHTLY");
      assertEquals(1, v.getMajor());
      assertEquals(0, v.getMinor());
      assertEquals(true, v.hasQualifier());
      assertEquals(true, v.hasPatch());
      assertEquals(Integer.valueOf(0), v.getPatch());
      assertEquals(true, v.hasBuild());
      assertEquals(Integer.valueOf(0), v.getBuild());
      assertEquals("SNAPSHOT_NIGHTLY", v.getQualifier());
      assertEquals("1.0.0.0-SNAPSHOT_NIGHTLY", v.getRepresentation());
      assertEquals(new Version(1, 0, 0, 0, "SNAPSHOT_NIGHTLY", "1.0.0.0-SNAPSHOT_NIGHTLY"), v);
   }

   @Test
   public void testInvalidRepresentations() {
      assertInvalidRepresentation("1");
      assertInvalidRepresentation("1..0");
      assertInvalidRepresentation("1.0-");
      assertInvalidRepresentation("1.0--qualifier");
      assertInvalidRepresentation("1.0-some spaces in this");
      assertInvalidRepresentation("1.0--qualifier1-qualifier2");
      assertInvalidRepresentation("1.0-*");
      assertInvalidRepresentation("1.0-1234567890123456789012345678901234567890123456789012345678901234");
   }

   @Test
   public void testConstructor1() {
      Version v = new Version(1);
      assertEquals(1, v.getMajor());
      assertEquals(0, v.getMinor());
      assertEquals(false, v.hasQualifier());
      assertEquals("", v.getQualifier());
      assertEquals("1.0", v.getRepresentation());
      assertEquals(v, Version.fromRepresentation(v.getRepresentation()));
   }

   @Test
   public void testConstructor2() {
      Version v = new Version(7, 104);
      assertEquals(7, v.getMajor());
      assertEquals(104, v.getMinor());
      assertEquals(false, v.hasQualifier());
      assertEquals("", v.getQualifier());
      assertEquals("7.104", v.getRepresentation());
      assertEquals(v, Version.fromRepresentation(v.getRepresentation()));
   }

   @Test
   public void testConstructor3() {
      Version v = new Version(7, 104, "SNAPSHOT");
      assertEquals(7, v.getMajor());
      assertEquals(104, v.getMinor());
      assertEquals(true, v.hasQualifier());
      assertEquals("SNAPSHOT", v.getQualifier());
      assertEquals("7.104-SNAPSHOT", v.getRepresentation());
      assertEquals(v, Version.fromRepresentation(v.getRepresentation()));
   }

   @Test
   public void testSort() {
      List<Version> input =
            Arrays.asList(
                  Version.fromRepresentation("1.0-alpha"),
                  Version.fromRepresentation("1.0-beta"),
                  Version.fromRepresentation("1.0"),
                  Version.fromRepresentation("1.1-gamma"),
                  Version.fromRepresentation("1.1"),
                  Version.fromRepresentation("1.2"),
                  Version.fromRepresentation("2.104")
           );

      // the expected list is the reverse of the input
      List<Version> expected = new ArrayList<>(input);
      Collections.reverse(expected);

      List<Version> actual = new ArrayList<>(input);
      Collections.sort(actual);

      assertEquals(expected, actual);
   }

   @Test
   public void testFindNewest() {
      List<Version> versions =
            Arrays.asList(
                  Version.fromRepresentation("1.0-alpha"),
                  Version.fromRepresentation("1.0"),
                  Version.fromRepresentation("1.1-alpha"),
                  Version.fromRepresentation("1.1"),
                  Version.fromRepresentation("1.2"),
                  Version.fromRepresentation("2.104"),
                  Version.fromRepresentation("3.0-beta")
           );

      assertEquals(Version.fromRepresentation("2.104"), Version.findNewest(versions));
      assertEquals(Version.fromRepresentation("1.1-alpha"), Version.findNewest(versions, "alpha"));
      assertEquals(Version.fromRepresentation("3.0-beta"), Version.findNewest(versions, "beta"));
      assertEquals(null, Version.findNewest(versions, "gamma"));
   }

   @Test
   public void testFindNewestWithPatch() {
      List<Version> versions =
            Arrays.asList(
                  Version.fromRepresentation("1.0-alpha"),
                  Version.fromRepresentation("1.0.0-alpha"),
                  Version.fromRepresentation("1.0.1-alpha")
           );

      assertEquals(Version.fromRepresentation("1.0.1-alpha"), Version.findNewest(versions, "alpha"));
   }

   @Test
   public void testFindNewestWithPatchAndBuild() {
      List<Version> versions =
            Arrays.asList(
                  Version.fromRepresentation("1.0-alpha"),
                  Version.fromRepresentation("1.0.0.0-alpha"),
                  Version.fromRepresentation("1.0.0.1-alpha")
           );

      assertEquals(Version.fromRepresentation("1.0.0.1-alpha"), Version.findNewest(versions, "alpha"));
   }
   
   @Test
   public void testCompareTo() {
      assertTrue(Version.fromRepresentation("2.0.1.045").compareTo(Version.fromRepresentation("2.0.1.038")) < 0);
      assertTrue(Version.fromRepresentation("2.0.1.045").compareTo(Version.fromRepresentation("2.0.1.045")) == 0);
      assertTrue(Version.fromRepresentation("2.0.1.045").compareTo(Version.fromRepresentation("2.0.1.058")) > 0);
   }

   private void assertInvalidRepresentation(String representation) {
      try {
         Version.fromRepresentation(representation);
         Assert.fail("Allowed invalid representation [" + representation + "]");
      }
      catch(IllegalArgumentException e) {
         // expected
      }
   }

}

