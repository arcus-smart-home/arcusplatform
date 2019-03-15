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

import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules(AttributeMapTransformModule.class)
public class TestProtocolDrivenAttributeMapTransformer extends IrisTestCase {
   /*
   
   @Inject
   private ProtocolDrivenAttributeMapTransformer transformer;
   
   @SuppressWarnings("unchecked")
   @Test
   public void testTransformFromMapToAttributeMap() {
      assertNull(transformer.transformToAttributeMap(null));
      AttributeMap map = transformer.transformToAttributeMap(Collections.<String,Object>emptyMap());
      assertTrue(map.isEmpty());
      
      Map<String, Object> attributes = new HashMap<>();
      Set<Byte> bytes = new HashSet<>();
      attributes.put(ZWaveProtocol.ATTR_MANUFACTURER, 4);
      attributes.put(ZWaveProtocol.ATTR_PRODUCTID, 8);
      attributes.put(ZWaveProtocol.ATTR_PRODUCTTYPE, 15);
      bytes.add((byte)0x10);
      bytes.add((byte)0x17);
      bytes.add((byte)0x2a);
      attributes.put(ZWaveProtocol.ATTR_COMMANDCLASSES, bytes);
      
      
      map = transformer.transformToAttributeMap(attributes);
      Assert.assertEquals(4, map.size());
      
      AttributeKey<?> key = extractKey(ZWaveProtocol.ATTR_MANUFACTURER, map);
      Assert.assertEquals(Integer.valueOf(4), (Integer)map.get(key));
      
      key = extractKey(ZWaveProtocol.ATTR_PRODUCTID, map);
      Assert.assertEquals(Integer.valueOf(8), (Integer)map.get(key));
      
      key = extractKey(ZWaveProtocol.ATTR_PRODUCTTYPE, map);
      Assert.assertEquals(Integer.valueOf(15), (Integer)map.get(key));
      
      key = extractKey(ZWaveProtocol.ATTR_COMMANDCLASSES, map);
      Assert.assertEquals("java.util.Set<java.lang.Byte>", key.getType().toString());
      Set<Byte> byteSet = (Set<Byte>)map.get(key);
      Assert.assertEquals(3, byteSet.size());
      Assert.assertTrue(byteSet.contains((byte)0x10));
      Assert.assertTrue(byteSet.contains((byte)0x17));
      Assert.assertTrue(byteSet.contains((byte)0x2a));
   }
   
   private AttributeKey<?> extractKey(String name, AttributeMap map) {
      Iterator<AttributeKey<?>> it = map.keySet().iterator();
      while (it.hasNext()) {
         AttributeKey<?> key = it.next();
         if (name.equals(key.getName())) {
            return key;
         }
      }
      return null;
   }
   */
}

