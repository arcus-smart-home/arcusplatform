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
package com.iris.oculus.util;

import java.util.Optional;

import com.google.common.base.Supplier;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Capability;
import com.iris.client.capability.Device;
import com.iris.client.capability.Product;
import com.iris.client.capability.Rule;
import com.iris.client.capability.Scene;
import com.iris.client.capability.Subsystem;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.model.DeviceModel;
import com.iris.client.model.Model;
import com.iris.client.model.ProductModel;
import com.iris.client.model.RuleModel;
import com.iris.client.model.SceneModel;
import com.iris.client.model.SubsystemModel;
import com.iris.oculus.Oculus;

public class Models {

   public static String nameOf(Model m) {
      try {
         switch(m.getType()) {
         case Device.NAMESPACE:
           return _nameOf((DeviceModel) m);
         case Product.NAMESPACE:
           return _nameOf((ProductModel) m);
         case Rule.NAMESPACE:
            return _nameOf((RuleModel) m);
         case Scene.NAMESPACE:
            return _nameOf((SceneModel) m);
         case Subsystem.NAMESPACE:
            return _nameOf((SubsystemModel) m);
         default:
           return m.getAddress();
         }
      }
      catch(Exception e) {
         Oculus.warn("Error applying label", e);
         return m != null && m.getAddress() != null ? m.getAddress() : "<unknown>";
      }
   }
   
   private static String _nameOf(DeviceModel m) {
      return m.getName();
   }
   
   private static String _nameOf(ProductModel m) {
      return m.getShortName();
   }
   
   private static String _nameOf(RuleModel m) {
      return String.format("%s: %s", m.getTemplate(), m.getName());
   }
   
   private static String _nameOf(SceneModel m) {
      return String.format("%s: %s", m.getTemplate(), m.getName());
   }
   
   private static String _nameOf(SubsystemModel m) {
      return String.format("%s v%s", m.getName(), m.getVersion());
   }
   
   public static Optional<Model> getIfLoaded(String address) {
      return getIfLoaded(address, Model.class);
   }

   public static String nameOf(String address) {
      Optional<Model> target = Models.getIfLoaded(address);
      if(target.isPresent()) {
         return Models.typeOf(target.get()) + " " + Models.nameOf(target.get());
      }
      else {
         return address + " (missing)";
      }
   }
   
   public static String typeOf(Model m) {
      switch(m.getType()) {
      case Device.NAMESPACE:
        return "Device";
      case Rule.NAMESPACE:
         return "Rule";
      case Scene.NAMESPACE:
         return "Scene";
      case Subsystem.NAMESPACE:
         return "Subsystem";
      default:
        return m.getType();
      }
   }

   public static <M extends Model> Optional<M> getIfLoaded(String address, Class<M> type) {
      return Optional.ofNullable((M) IrisClientFactory.getModelCache().get(address));
   }

   public static <M extends Model> ClientFuture<M> getOrLoad(String address, Class<M> type) {
   	return getOrLoad(address, () -> {
   		ClientRequest request = new ClientRequest();
   		request.setAddress(address);
   		request.setCommand(Capability.CMD_GET_ATTRIBUTES);
   		
   		return
	   		IrisClientFactory
	   			.getClient()
	   			.request(request)
	   			.transform((response) -> (M) IrisClientFactory.getModelCache().addOrUpdate(response.getAttributes()));
   	});
   }
   
   public static <M extends Model> ClientFuture<M> getOrLoad(String address, Supplier<ClientFuture<M>> loader) {
   	Model model =
	   	IrisClientFactory
	   		.getModelCache()
	   		.get(address);
   	if(model != null) {
   		return Futures.succeededFuture((M) model);
   	}
   	
   	return loader.get();
   }

}

