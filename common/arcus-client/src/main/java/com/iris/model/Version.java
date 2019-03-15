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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.iris.Utils;

/**
 * An immutable version.  Supports major, minor and an optional
 * qualifier.  When a qualifier is specified it means this is
 * a "special" version, in general when trying to find the "newest version"
 * it should always be assumed that a no-qualifier version is better than a version
 * with a qualifier.  Versions with qualifiers should only be returned when specifically
 * seached by qualifier.
 */
public class Version implements Comparable<Version> {

   public static final Version UNVERSIONED = new Version(0);

   // note that the .+ for the qualifier is more permissive than we allow
   // but the error messages generated when we pass the qualifier with invalid characters
   // in is better than the "doesn't match pattern" error message
   private static final Pattern p = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-(.+))?$");
   private static final int MAX_QUALIFIER_LENGTH = 64;

   public static Version fromRepresentation(String version) throws IllegalArgumentException {
      Matcher m = p.matcher(version);
      if(!m.matches()) {
         throw new IllegalArgumentException("Invalid version format, should be of the form: <major>.<minor>.<patch>.<build>-<qualifier> where <major> and <minor> are non-negative integers, <patch> and <build> are optional non-negative integers and <qualifier> may contain the letters a through z, A through Z, and the underscore character (_).");
      }

      int major = Integer.parseInt(m.group(1));
      int minor = Integer.parseInt(m.group(2));
      Integer patch = m.group(3) == null ? null : Integer.parseInt(m.group(3));
      Integer build = m.group(4) == null ? null : Integer.parseInt(m.group(4));
      String qualifier = m.group(5);
      return new Version(major, minor, patch, build, qualifier, version);
   }

   /**
    * Finds the newest version with no qualifier.
    * @param versions
    * @return The newest version with the given qualifer, or {@code null}
    * if no version with the given qualifier is in the iteration.  An empty
    * iterator will always return {@code null}.
    */
   @Nullable
   public static Version findNewest(Iterable<Version> versions) {
      return findNewest(versions, "");
   }

   /**
    * Finds the newest version with the specified qualifier. Note that
    * a {@code qualifier == ""} is equivalent {@link #findNewest(Iterable)}.
    * @param versions
    * @param qualifier
    * @return The newest version with the given qualifer, or {@code null}
    * if no version with the given qualifier is in the iteration.  An empty
    * iterator will always return {@code null}.
    */
   @Nullable
   public static Version findNewest(Iterable<Version> versions, String qualifier) {
      Utils.assertNotNull(versions);
      qualifier = qualifier == null ? "" : qualifier;
      Version newest = null;
      for(Version v: versions) {
         if(qualifier.equals(v.getQualifier())) {
            if(newest == null) {
               newest = v;
            }
            else if(newest.compareTo(v) > 0) {
               newest = v;
            }
         }
      }
      return newest;
   }

   // transient is a hint for hashcode and equals
   private final transient int major;
   private final transient int minor;
   private final transient Integer patch;
   private final transient Integer build;
   private final transient String qualifier;
   private final String representation;

   public Version(int major) {
      this(major, 0, null, null);
   }

   public Version(int major, int minor) {
      this(major, minor, null, null);
   }

   public Version(int major, int minor, @Nullable String qualifier) {
      this(major, minor, qualifier, null);
   }

   private Version(int major, int minor, @Nullable String qualifier, String representation) {
      this(major, minor, null, null, qualifier, representation);
   }

   Version(int major, int minor, @Nullable Integer patch, @Nullable Integer build, @Nullable String qualifier, String representation) {
      Utils.assertTrue(major > -1, "The major version must be greater than -1");
      Utils.assertTrue(minor > -1, "The minor version must be greater than -1");
      Utils.assertTrue(qualifier == null || qualifier.length() < MAX_QUALIFIER_LENGTH, "The qualifier must be less than " + MAX_QUALIFIER_LENGTH + " characters long");
      Utils.assertTrue(qualifier == null || StringUtils.containsOnly(qualifier, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_"), "The qualifier may contain only the letters a through z and the underscore character");
      this.major = major;
      this.minor = minor;
      this.patch = patch;
      this.build = build;
      this.qualifier = qualifier == null ? "" : qualifier;
      if(representation == null) {
         StringBuilder sb = new StringBuilder().append(this.major).append(".").append(this.minor);
         if(this.patch != null) {
            sb.append(".").append(this.patch);
         }
         if(this.build != null) {
            sb.append(".").append(this.build);
         }
         if(!this.qualifier.isEmpty()) {
            sb.append("-").append(this.qualifier);
         }
         this.representation = sb.toString();
      }
      else {
         this.representation = representation;
      }
   }

   public int getMajor() {
      return major;
   }

   public int getMinor() {
      return minor;
   }

   public boolean hasQualifier() {
      return !qualifier.isEmpty();
   }

   public String getQualifier() {
      return qualifier;
   }

   public String getRepresentation() {
      return representation;
   }

   public boolean hasPatch() {
      return patch != null;
   }

   public Integer getPatch() {
      return patch;
   }

   public boolean hasBuild() {
      return build != null;
   }

   public Integer getBuild() {
      return build;
   }

   @Override
   public int compareTo(Version o) {
      if(o == null) {
         return 1;
      }
      int major1 = getMajor(), major2 = o.getMajor();
      if(major1 != major2) {
         return major2 - major1;
      }
      int minor1 = getMinor(), minor2 = o.getMinor();
      if(minor1 != minor2) {
         return minor2 - minor1;
      }

      int patch1 = hasPatch() ? getPatch() : 0;
      int patch2 = o.hasPatch() ? o.getPatch() : 0;
      if(patch1 != patch2) {
         return patch2 - patch1;
      }

      int build1 = hasBuild() ? getBuild() : 0;
      int build2 = o.hasBuild() ? o.getBuild() : 0;

      if(build1 != build2) {
         return build2 - build1;
      }

      String q1 = hasQualifier() ? getQualifier() : null;
      String q2 = o.hasQualifier() ? o.getQualifier() : null;
      // this is a bit weird, since we would like 'beta' to be greater
      // than 'alpha' we reverse the comparison here.
      // of course no qualifier means production ready, so null out no qualifier
      // and always sort it to the front
      return ObjectUtils.compare(q2, q1, true);
   }

   @Override
   public String toString() {
      return "Version [" + getRepresentation() + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((representation == null) ? 0 : representation.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Version other = (Version) obj;
      if (representation == null) {
         if (other.representation != null) return false;
      }
      else if (!representation.equals(other.representation)) return false;
      return true;
   }

}

