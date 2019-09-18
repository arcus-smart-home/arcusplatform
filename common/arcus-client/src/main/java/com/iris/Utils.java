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
package com.iris;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;


public final class Utils {
   public static final Charset UTF_8 = Charset.forName(CharEncoding.UTF_8);
   public static final Object [] EMPTY_ARRAY = new Object [] {};
   public static final byte [] EMPTY_BYTE_ARRAY = new byte [] {};

   private Utils() {
   }

   public static String getNamespace(String name) {
      assertNotNull(name, "name may not be null");
      String [] parts = name.split("\\:", 2);
      if(parts.length != 2) {
         throw new IllegalArgumentException(name + " is not namespaced");
      }
      return parts[0];
   }

   public static boolean isNamespaced(String name) {
      return name != null && name.indexOf(':') > 0;
   }

   public static String namespace(String namespace, String name) {
      assertNotNull(namespace, "namespace may not be null");
      assertNotNull(name, "name may not be null");
      assertTrue(namespace.indexOf(':') == -1, "namespace may not contain ':'");
      return namespace + ':' + name;
   }

   public static void assertNotNull(Object argument) throws IllegalArgumentException {
      assertNotNull(argument, null);
   }

   public static void assertNotNull(Object argument, Object message) throws IllegalArgumentException {
      if(argument == null) {
         throw new IllegalArgumentException(message == null ? "may not be null" : message.toString());
      }
   }

   public static void assertNotEmpty(String value) {
      assertNotEmpty(value, null);
   }

   public static void assertNotEmpty(String value, Object message) {
      if(value == null || value.length() == 0) {
         throw new IllegalArgumentException(message == null ? "may not be empty" : message.toString());
      }
   }
   
   public static void assertNotEmpty(Map<?,?> map) {
	      assertNotEmpty(map, null);
	}
	
	public static void  assertNotEmpty(Map<?,?> map,  Object message){
	   if(map == null || map.isEmpty()) {
	       throw new IllegalArgumentException(message == null ? "may not be empty" : message.toString());
	    }	   
	} 
	
   public static void assertNotEmpty(Collection<?> collection) {
      assertNotEmpty(collection, null);
   }

   public static void  assertNotEmpty(Collection<?> collection,  Object message){
      if(collection == null || collection.isEmpty()) {
          throw new IllegalArgumentException(message == null ? "may not be empty" : message.toString());
       }    
   }    

   public static void assertTrue(boolean expression) throws IllegalArgumentException {
      assertTrue(expression, null);
   }

   public static void assertTrue(boolean expression, Object message) throws IllegalArgumentException {
      if(!expression) {
         throw new IllegalArgumentException(message == null ? "expected assertion to be true, was false" : message.toString());
      }
   }

   public static void assertFalse(boolean expression) throws IllegalArgumentException {
      assertFalse(expression, null);
   }

   public static void assertFalse(boolean expression, Object message) throws IllegalArgumentException {
      if(expression) {
         throw new IllegalArgumentException(message == null ? "expected assertion to be false, was true" : message.toString());
      }
   }

   public static <K, V> Map<K, V> unmodifiableCopy(Map<K, V> source) {
      if(source == null || source.isEmpty()) {
         return Collections.emptyMap();
      }
      if(source instanceof LinkedHashMap) { // maintain sorting
         return Collections.unmodifiableMap(new LinkedHashMap<K, V>( source ));
      }
      return Collections.unmodifiableMap(new HashMap<K, V>(source));
   }

   public static final String serializeType(Type type) {
      StringBuilder sb = new StringBuilder();
      doSerializeType(sb, type);
      return sb.toString();
   }

   public static final Type deserializeType(String type) {
      return doDeserializeType(type);
   }

   public static String b64Encode(byte[] binary) {
      int remainder = binary.length % 3;
      int resultLength = (binary.length / 3) * 4;
      if(remainder != 0) {
         resultLength += 4;
      }
      char[] encoded = new char[resultLength];
      for(int i=0, o=0; i<(binary.length-2); i+=3, o+=4) {
         byte i0 = binary[i];
         byte i1 = binary[i + 1];
         byte i2 = binary[i + 2];
         encoded[o] = lookup((i0 >> 2) & 0x3f);
         encoded[o+1] = lookup(((i0 << 4) & 0x30) | ((i1 >> 4) & 0x0f));
         encoded[o+2] = lookup(((i1 << 2) & 0x3c) | ((i2 >> 6) & 0x03));
         encoded[o+3] = lookup(i2 & 0x3f);
      }
      if(remainder == 2) {
         byte i0 = binary[binary.length - 2];
         byte i1 = binary[binary.length - 1];
         encoded[encoded.length - 4] = lookup((i0 >> 2) & 0x3f);
         encoded[encoded.length - 3] = lookup(((i0 << 4) & 0x30) | ((i1 >> 4) & 0x0f));
         encoded[encoded.length - 2] = lookup((i1 << 2) & 0x3c);
         encoded[encoded.length - 1] = '=';
      }
      if(remainder == 1) {
         byte i0 = binary[binary.length - 1];
         encoded[encoded.length - 4] = lookup((i0 >> 2) & 0x3f);
         encoded[encoded.length - 3] = lookup((i0 << 4) & 0x30);
         encoded[encoded.length - 2] = '=';
         encoded[encoded.length - 1] = '=';
      }
      return new String(encoded);
   }

   public static byte [] b64Decode(CharSequence encoded) {
      if(encoded.length() % 4 != 0) {
         throw new IllegalArgumentException("Invalid base64 input, must have a multiple of 4 characters");
      }

      int resultLength = (encoded.length() / 4) * 3;
      if(encoded.charAt(encoded.length() - 1) == '=') {
         resultLength--;
      }
      if(encoded.charAt(encoded.length() - 2) == '=') {
         resultLength--;
      }
      byte [] decoded = new byte[resultLength];
      for(int i=3,o=2; o<resultLength; i+=4,o+=3) {
         int i0 = reverseLookup(encoded.charAt(i - 3));
         int i1 = reverseLookup(encoded.charAt(i - 2));
         int i2 = reverseLookup(encoded.charAt(i - 1));
         int i3 = reverseLookup(encoded.charAt(i));

         decoded[o - 2] = (byte)( (i0 << 2) | (i1 >> 4) );
         decoded[o - 1] = (byte)( ((i1 << 4) & 0xf0) | (i2 >> 2) );
         decoded[o]     = (byte)( ((i2 << 6) & 0xc0) | i3 );
      }
      if(resultLength % 3 == 2) {
         int i0 = reverseLookup(encoded.charAt(encoded.length() - 4));
         int i1 = reverseLookup(encoded.charAt(encoded.length() - 3));
         int i2 = reverseLookup(encoded.charAt(encoded.length() - 2));
         decoded[resultLength - 2] = (byte)( (i0 << 2) | (i1 >> 4) );
         decoded[resultLength - 1] = (byte)( ((i1 << 4) & 0xf0) | (i2 >> 2) );
      }
      if(resultLength % 3 == 1) {
         int i0 = reverseLookup(encoded.charAt(encoded.length() - 4));
         int i1 = reverseLookup(encoded.charAt(encoded.length() - 3));
         decoded[resultLength - 1] = (byte)( (i0 << 2) | (i1 >> 4) );
      }
      return decoded;
   }

   // TODO move this somewhere else?
   public static final byte[] hash(String value) {
      try {
         MessageDigest md = MessageDigest.getInstance("SHA-1");
         return md.digest(value.getBytes(UTF_8));
      }
      catch (NoSuchAlgorithmException e) {
         throw new UnsupportedOperationException("This VM does not support SHA-1 hash");
      }
   }

   public static final byte[] hash(InputStream is) throws IOException {
      try {
         byte [] buffer = new byte[1024];
         int len;
         MessageDigest md = MessageDigest.getInstance("SHA1");
         while((len = is.read(buffer)) > -1) {
            md.update(buffer, 0, len);
         }
         return md.digest();
      }
      catch(NoSuchAlgorithmException e) {
         throw new UnsupportedOperationException("This VM does not support SHA-1 hash");
      }
   }

   public static final String shortHash(InputStream is) throws IOException {
      return Utils.b64Encode( hash(is) ).substring(0, 27);
   }

   private static void doSerializeType(StringBuilder out, Type value) {
      if(value instanceof Class) {
         out.append(((Class<?>) value).getName());
      }
      else if(value instanceof ParameterizedType) {
         ParameterizedType pt = (ParameterizedType) value;
         doSerializeType(out, pt.getRawType());

         Type[] typeArgs = pt.getActualTypeArguments();
         if(typeArgs.length > 0) {
            out.append("<");
            for(int i=0; i<typeArgs.length; i++) {
               if(i > 0) {
                  out.append(",");
               }
               doSerializeType(out, typeArgs[i]);
            }
            out.append(">");
         }
      }
      else {
         throw new IllegalArgumentException("Unserializable type: " + value);
      }
   }

   private static char lookup(int value) {
      return BASE64_LOOKUP[value];
   }

   private static int reverseLookup(char value) {
      switch(value) {
      case 'A': return 0;
      case 'B': return 1;
      case 'C': return 2;
      case 'D': return 3;
      case 'E': return 4;
      case 'F': return 5;
      case 'G': return 6;
      case 'H': return 7;
      case 'I': return 8;
      case 'J': return 9;
      case 'K': return 10;
      case 'L': return 11;
      case 'M': return 12;
      case 'N': return 13;
      case 'O': return 14;
      case 'P': return 15;
      case 'Q': return 16;
      case 'R': return 17;
      case 'S': return 18;
      case 'T': return 19;
      case 'U': return 20;
      case 'V': return 21;
      case 'W': return 22;
      case 'X': return 23;
      case 'Y': return 24;
      case 'Z': return 25;
      case 'a': return 26;
      case 'b': return 27;
      case 'c': return 28;
      case 'd': return 29;
      case 'e': return 30;
      case 'f': return 31;
      case 'g': return 32;
      case 'h': return 33;
      case 'i': return 34;
      case 'j': return 35;
      case 'k': return 36;
      case 'l': return 37;
      case 'm': return 38;
      case 'n': return 39;
      case 'o': return 40;
      case 'p': return 41;
      case 'q': return 42;
      case 'r': return 43;
      case 's': return 44;
      case 't': return 45;
      case 'u': return 46;
      case 'v': return 47;
      case 'w': return 48;
      case 'x': return 49;
      case 'y': return 50;
      case 'z': return 51;
      case '0': return 52;
      case '1': return 53;
      case '2': return 54;
      case '3': return 55;
      case '4': return 56;
      case '5': return 57;
      case '6': return 58;
      case '7': return 59;
      case '8': return 60;
      case '9': return 61;
      case '+': return 62;
      case '/': return 63;
      default: throw new IllegalArgumentException("Invalid base 64 character " + value);
      }
   }

   private static final char [] BASE64_LOOKUP = new char[] {
      'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O',
      'P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d',
      'e','f','g','h','i','j','k','l','m','n','o','p','q','r','s',
      't','u','v','w','x','y','z','0','1','2','3','4','5','6','7',
      '8','9','+','/'
   };

   private static Type doDeserializeType(String name) {
      int startIdx = name.indexOf('<');
      if(startIdx == -1) {
         return classForName(name);
      }
      else if(name.charAt(name.length() - 1) == '>') {
         Class<?> rawType = classForName(name.substring(0, startIdx));
         String [] typeNames = StringUtils.split(name.substring(startIdx+1, name.length() - 1), ',');
         Type [] typeParameters = new Type[typeNames.length];
         for(int i=0; i<typeNames.length; i++) {
            typeParameters[i] = doDeserializeType(typeNames[i]);
         }
         return TypeUtils.parameterize(rawType, typeParameters);
      }
      else {
         throw new IllegalArgumentException("Invalid class name, missing close '>': " + name);
      }
   }

   private static Class<?> classForName(String name) {
      try {
         return Class.forName(name);
      }
      catch (ClassNotFoundException e) {
         throw new IllegalArgumentException("Unrecognized class name: " + name, e);
      }
   }

   private static final String KEY_ALGORITHM = "AES";
   private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
   private static final String SRN_ALGORITHM = "SHA1PRNG";

   private static SecretKey createSecretKey(String secret) {
      try {
         byte[] encoded = Utils.b64Decode(secret);
         return new SecretKeySpec(encoded, KEY_ALGORITHM);
      } catch(Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Deprecated
   public static byte[] aesEncrypt(String secretStr, byte[] decrypted) {
      if(decrypted == null || decrypted.length == 0) {
         return null;
      }

      try {
         SecretKey secret = createSecretKey(secretStr);
         Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);

         // initialize iv parameter spec
         byte[] iv = new byte[cipher.getBlockSize()];
         SecureRandom random = SecureRandom.getInstance(SRN_ALGORITHM);
         random.nextBytes(iv);
         IvParameterSpec ivParam = new IvParameterSpec(iv);

         cipher.init(Cipher.ENCRYPT_MODE, secret, ivParam);

         // encrypt the string
         byte[] encrypted = cipher.doFinal(decrypted);

         // tack the iv onto the beginning of the encrypted data, for use decrypting
         byte[] fullEncrypted = new byte[iv.length + encrypted.length];
         System.arraycopy(iv, 0, fullEncrypted, 0, iv.length);
         System.arraycopy(encrypted, 0, fullEncrypted, iv.length, encrypted.length);

         return fullEncrypted;

      } catch(Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Deprecated
   public static byte[] aesDecrypt(String secretStr, byte[] encrypted) {
      if(encrypted == null || encrypted.length == 0) {
         return null;
      }

      try {
         SecretKey secret = createSecretKey(secretStr);
         Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);

         // pull out the iv
         byte[] iv = new byte[cipher.getBlockSize()];
         System.arraycopy(encrypted, 0, iv, 0, iv.length);
         IvParameterSpec ivParam = new IvParameterSpec(iv);

         byte[] data = new byte[encrypted.length - iv.length];
         System.arraycopy(encrypted, iv.length, data, 0, data.length);

         cipher.init(Cipher.DECRYPT_MODE, secret, ivParam);
         return cipher.doFinal(data);
      } catch(Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Deprecated
   public static String aesEncrypt(String secretStr, String decrypted) {
      if(StringUtils.isBlank(decrypted)) {
         return null;
      }

      try {
         byte[] encryptedBytes = aesEncrypt(secretStr, decrypted.getBytes("UTF-8"));
         return Utils.b64Encode(encryptedBytes);
      } catch(Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Deprecated
   public static String aesDecrypt(String secretStr, String encrypted) {
      if(StringUtils.isBlank(encrypted)) {
         return null;
      }

      try {
         byte[] encryptedBytes = Utils.b64Decode(encrypted);
         return new String(aesDecrypt(secretStr, encryptedBytes), "UTF-8");
      } catch(Exception e) {
         throw new RuntimeException(e);
      }
   }

   public static boolean setIf(String key, Object value, Map<String,Object> map) {
      if(value == null) {
         return false;
      }
      map.put(key, value);
      return true;
   }

   public static Map<String, String> coerceToStringMap(Map<String, ?> value) {
      if(value == null || value.isEmpty()) {
         return null;
      }

      Map<String, String> result = new HashMap<>(value.size());
      for(Map.Entry<String, ?> entry: value.entrySet()) {
         result.put(entry.getKey(), coerceToString(entry.getValue()));
      }
      return result;
   }

   public static String coerceToString(Object value) {
      if(value == null) {
         return null;
      }

      if(value instanceof Date) {
         return String.valueOf(((Date) value).getTime());
      }
      return value.toString();
   }

   private static final String TOKEN_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

   public static String randomTokenString(int length) {
      StringBuilder token = new StringBuilder();
      SecureRandom rand = new SecureRandom();

      for(int i = 0; i < length; i++) {
         token.append(TOKEN_CHARS.charAt(rand.nextInt(TOKEN_CHARS.length())));
      }

      return token.toString();
   }

}

