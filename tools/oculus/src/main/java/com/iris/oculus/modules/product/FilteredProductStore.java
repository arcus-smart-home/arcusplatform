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
package com.iris.oculus.modules.product;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;

import com.iris.client.event.ClientFuture;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerList;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.ProductModel;
import com.iris.oculus.Oculus;
import com.iris.oculus.util.OperationEvent;
import com.iris.oculus.view.FilteredViewModel;
import com.iris.oculus.view.StoreViewModel;
import com.iris.oculus.view.ViewModel;

public class FilteredProductStore {
   private FilteredViewModel<ProductModel> model;
   private Set<String> products;
   private ProductFilter<String> productsBySearch;
   private ProductFilter<String> productsByBrand;
   private ProductFilter<String> productsByCategory;
   private Optional<Boolean> browseable = Optional.empty();
   private Optional<Boolean> certified = Optional.empty();
   private Optional<Boolean> appRequired = Optional.empty();
   private boolean pendingUpdate = false;
   private ListenerList<OperationEvent<Integer>> listeners = new ListenerList<>();

   public FilteredProductStore(ProductController controller) {
      this.model = new FilteredViewModel<>(new StoreViewModel<>(controller.getProductStore()));
      this.productsBySearch = new ProductFilter<>(controller::search);
      this.productsByBrand = new ProductFilter<>(controller::listByBrand);
      this.productsByCategory = new ProductFilter<>(controller::listByCategory);
   }
   
   public ViewModel<ProductModel> model() {
      return model;
   }
   
   public ListenerRegistration addOperationListener(Listener<OperationEvent<Integer>> listener) {
      return listeners.addListener(listener);
   }
   
   public void search(String term) {
      if(StringUtils.isEmpty(term)) {
         productsBySearch.clear();
      }
      else {
         productsBySearch.update(term);
      }
   }
   
   public void setBrand(Optional<Map<String, Object>> brand) {
      if(brand.isPresent()) {
         productsByBrand.update((String) brand.get().get("name"));
      }
      else {
         productsByBrand.clear();
      }
   }
   
   public void setCategory(Optional<Map<String, Object>> category) {
      if(category.isPresent()) {
         productsByCategory.update((String) category.get().get("name"));
      }
      else {
         productsByCategory.clear();
      }
   }
   
   public void setBrowsable(Optional<Boolean> browseable) {
      this.browseable = browseable;
      deferUpdate();
   }
   
   public void setCertified(Optional<Boolean> certified) {
      this.certified = certified;
      deferUpdate();
   }

   public void setAppRequired(Optional<Boolean> appRequired){
      this.appRequired = appRequired;
      deferUpdate();
   }
   
   private void deferUpdate() {
      if(!pendingUpdate) {
         listeners.fireEvent(OperationEvent.loading());
         pendingUpdate = true;
         SwingUtilities.invokeLater(() -> {
            pendingUpdate = false;
            update();
         });
      }
   }
   
   private void update() {
      products = null;
      products = update(products, productsBySearch);
      products = update(products, productsByBrand);
      products = update(products, productsByCategory);
      
      Predicate<ProductModel> p = (m) -> true;
      if(products != null) {
         p = p.and((m) -> products.contains(m.getAddress()));
      }
      if(browseable.isPresent()) {
         p = p.and((m) -> browseable.get().equals(m.getCanBrowse()));
      }
      if(certified.isPresent()) {
         p = p.and((m) -> certified.get() ? "WORKS".equals(m.getCert()) : !"WORKS".equals(m.getCert()));
      }
      if(appRequired.isPresent()){
         p = p.and((m) ->appRequired.get().equals(m.getAppRequired()));
      }
      model.filterBy(p);
      listeners.fireEvent(OperationEvent.loaded(model.size()));
   }

   private Set<String> update(Set<String> products, ProductFilter<String> filter) {
      if(filter.products == null) {
         return products;
      }
      if(products == null) {
         products = new HashSet<>(filter.products);
      }
      else {
         products.retainAll(filter.products);
      }
      return products;
   }

   private class ProductFilter<V> {
      private Function<V, ClientFuture<? extends Collection<String>>> supplier;
      private Collection<String> products;
      private ClientFuture<? extends Collection<String>> request;
      
      ProductFilter(Function<V, ClientFuture<? extends Collection<String>>> supplier) {
         this.supplier = supplier;
      }
      
      private void cancel() {
         if(request != null) {
            request.cancel(true);
            request = null;
         }
      }

      public void clear() {
         cancel();
         products = null;
         FilteredProductStore.this.deferUpdate();
      }
      
      public ClientFuture<Void> update(V value) {
         request = supplier.apply(value);
            supplier
               .apply(value)
               .onSuccess((values) -> {
                  products = values;
                  FilteredProductStore.this.update();
               })
               .onFailure((e) -> Oculus.error("Unable to filter products", e));
         return request.transform((o) -> null);
      }
      
   }
}

