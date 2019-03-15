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
package com.iris.capability.attribute.transform;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableSet;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.device.model.AttributeDefinition;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.MobileDeviceCapability;
import com.iris.messages.model.MobileDevice;

@SuppressWarnings("deprecation")
public class MobileDeviceAttributesTransformer extends ReflectiveBeanAttributesTransformer<MobileDevice> {

   // Used to filer out all digits [0-9]
   private static final Pattern pattern = Pattern.compile("\\D+");
   private static final String DEFAULT_VERSION = "UNKNOWN";
   private static final double DIAGONAL_TABLET_THRESHOLD = 6.5;

   public MobileDeviceAttributesTransformer(CapabilityRegistry registry) {
      super(
            registry,
            ImmutableSet.of(MobileDeviceCapability.NAMESPACE, Capability.NAMESPACE),
            MobileDevice.class);
   }

   @Override
   protected Object getValue(MobileDevice bean, AttributeDefinition definition) throws Exception {
      if (MobileDeviceCapability.ATTR_APPVERSION.equals(definition.getName()) && StringUtils.isEmpty(bean.getAppVersion())){
         return DEFAULT_VERSION;
      }else if (MobileDeviceCapability.ATTR_NAME.equals(definition.getName()) && StringUtils.isEmpty(bean.getName())){
         return friendlyName(bean);
      }else{
         return super.getValue(bean, definition);
      }
   }

   private String friendlyName(MobileDevice md) {
      // guard against devices not playing well and inserting null values
      Optional<String> deviceVendor = Optional.ofNullable(md.getDeviceVendor());
      Optional<String> formFactor = Optional.ofNullable(md.getFormFactor());
      Optional<String> resolution = Optional.ofNullable(md.getResolution());

      // Android Logic From Here on Out
      if (!deviceVendor.isPresent()){
         return AndroidDeviceType.GENERIC.friendlyName;
      }
      
      // apple consistently reports vendor fields
      if ("apple".equals(deviceVendor.get().toLowerCase())){
         return AppleDeviceType.getTypeForFactor(formFactor.orElse(DEFAULT_VERSION)).friendlyName;
      }

      OptionalDouble maxRes = getResolutionCoordinate(resolution, Mflag.MAX);
      OptionalDouble minRes = getResolutionCoordinate(resolution, Mflag.MIN);

      // if all three fields are present
      if (formFactor.isPresent() && maxRes.isPresent() && minRes.isPresent()){
         OptionalDouble maxLength = getApproxLengthInches(DPI.getDPI(formFactor.get()), maxRes.getAsDouble());
         OptionalDouble minLength = getApproxLengthInches(DPI.getDPI(formFactor.get()), minRes.getAsDouble());
         OptionalDouble diagonal = getDiagonalLength(maxLength, minLength);
         return AndroidDeviceType.getTypeBasedOnSizeInches(diagonal).friendlyName;
      }

      // all else fails
      return AndroidDeviceType.GENERIC.friendlyName;
   }

   /*
    * Calculate approximate length in inches based on denisty bucket If we ever
    * get physical ppi from the manufacturer we can shed the density bucket and
    * use the true density
    */
   private static OptionalDouble getApproxLengthInches(DPI dpi, double pixels) {
      if (dpi.equals(DPI.UNKNOWN) || pixels <= 0.0){
         return OptionalDouble.empty();
      }
      return OptionalDouble.of(pixels / dpi.getDpi());
   }

   /*
    * Given an arbitrary string of mixed numbers and string this will pull out
    * the largest or smallest number in the string: e.g. if using Mflag.MIN,
    * 1024x3224 --> 3224, 383838by534x65 --> 383838 etc...
    */
   private static OptionalDouble getResolutionCoordinate(Optional<String> str, Mflag mm) {
      if (str.isPresent()){
         DoubleStream dStream = pattern.splitAsStream(str.get()).filter(s -> !StringUtils.isBlank(s)).mapToDouble(Double::parseDouble);
         // return min or max depending on flag
         return mm.equals(Mflag.MIN) ? dStream.min() : dStream.max(); 
      }

      return OptionalDouble.empty();
   }

   private static OptionalDouble getDiagonalLength(OptionalDouble dim1, OptionalDouble dim2) {
      if (dim1.isPresent() && dim2.isPresent()){
         return OptionalDouble.of((Math.sqrt(Math.pow(dim1.getAsDouble(), 2) + Math.pow(dim2.getAsDouble(), 2))));
      }

      return OptionalDouble.empty();
   }

   private static enum Mflag {
      MIN, MAX;
   }

   /* TMT - normally I would put enums at top but these are a bit lengthy */
   public enum DPI {
      LDPI("LDPI", 120.0),
      MDPI("MDPI", 160.0),
      HDPI("HDPI", 240.0),
      XDPI("XHDPI", 320.0),
      XXDPI("XXHDPI", 480.0),
      XXXDPI("XXXHDPI", 640.0),
      UNKNOWN("unknown", 1.0);

      private final String denistyBucket;
      private final double dpi;

      private DPI(String denistyBucket, double dpi) {
         this.denistyBucket = denistyBucket;
         this.dpi = dpi;
      }

      public double getDpi() {
         return dpi;
      }

      static DPI getDPI(String des) {
         for (DPI dpi : DPI.values()){
            if (dpi.denistyBucket.equals(des.toUpperCase())){
               return dpi;
            }
         }
         return UNKNOWN;
      }

   } // Enum

   public enum AndroidDeviceType {
      GENERIC("Android Device"),
      TABLET("Android Tablet"),
      PHONE("Android Phone");

      private final String friendlyName;

      private AndroidDeviceType(String friendlyName) {
         this.friendlyName = friendlyName;
      }

      // calculate type based on diagonal screen length approximation
      static AndroidDeviceType getTypeBasedOnSizeInches(OptionalDouble diagonalLength) {

         if (!diagonalLength.isPresent()){
            return AndroidDeviceType.GENERIC;
         }

         if (diagonalLength.getAsDouble() >= DIAGONAL_TABLET_THRESHOLD){
            return AndroidDeviceType.TABLET;
         }else{
            return AndroidDeviceType.PHONE;
         }
      }

      public String getFriendlyName() {
         return friendlyName;
      }

   }

   public enum AppleDeviceType {
      GENERIC(DEFAULT_VERSION, "Apple Device"),
      IPAD("tablet", "iPad"),
      IPHONE("phone", "iPhone");

      private final String formFactor;
      private final String friendlyName;

      private AppleDeviceType(String formFactor, String friendlyName) {
         this.formFactor = formFactor;
         this.friendlyName = friendlyName;
      }

      static AppleDeviceType getTypeForFactor(String formFactor) {
         for (AppleDeviceType deviceType : AppleDeviceType.values()){
            if (deviceType.formFactor.equals(formFactor)){
               return deviceType;
            }
         }
         return AppleDeviceType.GENERIC;
      }

      public String getFriendlyName() {
         return friendlyName;
      }
   }
}

