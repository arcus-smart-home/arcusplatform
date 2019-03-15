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
package com.iris.messages.address;

import java.util.*;

import com.google.common.base.*;
import org.apache.commons.lang3.StringUtils;

import com.iris.Utils;
import com.iris.messages.MessageConstants;
import com.iris.util.IrisCollections;


/**
 *
 */
public class AddressMatchers {

   public static final AddressMatcher BROADCAST_MESSAGE_MATCHER = AddressMatchers.equals(Address.broadcastAddress());

   public static AddressMatcher equals(Address address) {
      return new AddressMatcherImpl(address.getNamespace(), address.getGroup(), address.getId(), address.isHubAddress());
   }

   public static Predicate<Address> fromCsvString(String csvMatchers) {
      List<AddressMatcher> components = new ArrayList<>();

      for (String adminAddress : Splitter.on(',').omitEmptyStrings().trimResults().split(csvMatchers)) {
         components.add(AddressMatchers.fromString(adminAddress));
      }

      switch (components.size()) {
         case 0:
            return Predicates.alwaysFalse();

         case 1:
            return components.get(0);

         default:
            return Predicates.or(components);
      }
   }

   public static AddressMatcher fromString(String matcher) {
      Preconditions.checkArgument(matcher != null && !matcher.isEmpty(), "matcher may not be empty");
      if(MessageConstants.BROADCAST.equals(matcher)) {
         return equals(Address.broadcastAddress());
      }
      
      String [] parts = StringUtils.split(matcher, ":");
      String namespace = parts[0];
      String group = parts.length > 1 ? parts[1] : null;
      String id = parts.length > 2 ? parts[2] : null;
      
      if("*".equals(namespace)) {
         throw new IllegalArgumentException("Namespace may not be a wildcard");
      }
      switch(parts.length) {
      case 2:
         if("*".equals(group)) {
            return new AddressMatcherImpl(namespace, WILDCARD, WILDCARD, false);
         }
         else {
            return equals(Address.fromString(matcher));
         }
      case 3:
         if("*".equals(group)) {
            Preconditions.checkArgument("*".equals(id), "If group is a wildcard, then id must also be a wildcard");
            return new AddressMatcherImpl(namespace, WILDCARD, WILDCARD, false);
         }
         else if("*".equals(id)) {
            return new AddressMatcherImpl(namespace, group, WILDCARD, false);
         }
         else {
            return equals(Address.fromString(matcher));
         }
      default:
         throw new IllegalArgumentException("Must be of the form [namespace:group] or [namespace:group:id]");
      }
   }
   
   public static AddressMatcher platformProtocolMatcher(String protocolName) {
      return new AddressMatcherImpl(MessageConstants.PROTOCOL, protocolName, WILDCARD, false);
   }

   public static AddressMatcher platformNamespace(String namespace) {
      return new AddressMatcherImpl(namespace, WILDCARD, WILDCARD, false);
   }

   public static AddressMatcher platformService(String namespace, Object service) {
      return new AddressMatcherImpl(namespace, service, WILDCARD, false);
   }

   public static AddressMatcher hubNamespace(String namespace) {
      return new AddressMatcherImpl(namespace, WILDCARD, WILDCARD, true);
   }

   public static Set<AddressMatcher> anyOf(Address... addresses) {
      return anyOf(IrisCollections.setOf(addresses));
   }

   public static Set<AddressMatcher> anyOf(Set<Address> addresses) {
      return setOfMatchers(new Function<Address,AddressMatcher>() {
         @Override
         public AddressMatcher apply(Address address) {
            return AddressMatchers.equals(address);
         }
      }, addresses);
   }

   public static Set<AddressMatcher> platformNamespaces(String... namespaces) {
      return platformNamespaces(IrisCollections.setOf(namespaces));
   }

   public static Set<AddressMatcher> platformNamespaces(Set<String> namespaces) {
      return setOfMatchers(new Function<String,AddressMatcher>() {
         @Override
         public AddressMatcher apply(String namespace) {
            return AddressMatchers.platformNamespace(namespace);
         }
      }, namespaces);
   }

   public static Set<AddressMatcher> platformServices(Address... addresses) {
      return platformServices(IrisCollections.setOf(addresses));
   }

   public static Set<AddressMatcher> platformServices(Set<Address> addresses) {
      return setOfMatchers(new Function<Address,AddressMatcher>() {
         @Override
         public AddressMatcher apply(Address input) {
            return AddressMatchers.platformService(input.getNamespace(), input.getGroup());
         }
      }, addresses);
   }

   public static Set<AddressMatcher> hubNamespaces(String... namespaces) {
      return hubNamespaces(IrisCollections.setOf(namespaces));
   }

   public static Set<AddressMatcher> hubNamespaces(Set<String> namespaces) {
      return setOfMatchers(new Function<String,AddressMatcher>() {
         @Override
         public AddressMatcher apply(String hubNamespace) {
            return AddressMatchers.hubNamespace(hubNamespace);
         }
      }, namespaces);
   }

   private static <I> Set<AddressMatcher> setOfMatchers(Function<I, AddressMatcher> fn, Collection<I> inputs) {
      if(inputs == null || inputs.isEmpty()) {
         return Collections.emptySet();
      }
      Set<AddressMatcher> matchers = new HashSet<AddressMatcher>(inputs.size());
      for(I input: inputs) {
         matchers.add(fn.apply(input));
      }
      return matchers;
   }
   
   public static boolean matchesAny(Set<Predicate<Address>>matchers,Address address){
      for(Predicate<Address> matcher:matchers){
         if(matcher.apply(address)){
            return true;
         }
      }
      return false;
   }
   
   private static boolean eq(Object o1, Object o2) {
      if(o1 == WILDCARD) {
         return true;
      }
      if(o1 instanceof byte[] && o2 instanceof byte[]) {
         return Arrays.equals((byte[])o1, (byte[]) o2);
      }
      return o1.equals(o2);
   }

   private static String repr(Object o) {
      if(o == WILDCARD) {
         return "*";
      }
      if(o instanceof String) {
         return (String) o;
      }
      if(o instanceof byte[]) {
         return Utils.b64Encode((byte[]) o);
      }
      return o.toString();
   }

   private static class AddressMatcherImpl implements AddressMatcher {
      private final String namespace;
      private final Object group;
      private final Object id;
      private final boolean hubAddress;

      AddressMatcherImpl(String namespace, Object group, Object id, boolean hubAddress) {
         this.namespace = namespace;
         this.group = group;
         this.id = id;
         this.hubAddress = hubAddress;
      }

      @Override
      public boolean apply(Address t) {
         return
               namespace.equals(t.getNamespace()) &&
               eq(group, t.getGroup()) &&
               eq(id, t.getId()) &&
               eq(hubAddress, t.isHubAddress());
      }

      @Override
      public String getNamespace() {
         return namespace;
      }

      @Override
      public Object getGroup() {
         return group;
      }

      @Override
      public Object getId() {
         return id;
      }

      @Override
      public boolean isHubAddress() {
         return hubAddress;
      }

      @Override
      public boolean isPlatformAddress() {
         return !hubAddress;
      }

      @Override
      public boolean isAnyGroup() {
         return group == null;
      }

      @Override
      public boolean isAnyId() {
         return id == null;
      }

      @Override
      public String toString() {
         return "AddressMatcher [" + namespace + ":" + repr(group) + ":" + repr(id) + "]";
      }

   }

   private static final Object WILDCARD = null;

}

