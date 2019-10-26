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
package com.iris.agent.zwave.code;

import java.lang.annotation.Annotation;

import com.iris.agent.util.ByteUtils;
import com.iris.agent.zwave.code.anno.Id;
import com.iris.agent.zwave.code.anno.Name;

public abstract class AbstractByteCmd implements ByteCommand {
   private final byte id;
   private final String name;
   
   public AbstractByteCmd() {
      this.id = (byte)(getAnnotation(Id.class).value());
      this.name = getAnnotation(Name.class).value();
   }
      
   @Override
   public byte[] bytes() {
      return ByteUtils.toByteArray(id);
   }

   @Override
   public String name() {
      return name;
   }
   
   @Override
   public int intId() {
      return 0x00FF & id;
   }
   
   @Override
   public byte byteId() {
      return id;
   }
   
   private <A extends Annotation> A getAnnotation(Class<A> annotationType) {
      return this.getClass().getAnnotationsByType(annotationType)[0];
   }
}
