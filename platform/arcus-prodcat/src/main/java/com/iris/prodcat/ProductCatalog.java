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
package com.iris.prodcat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.iris.model.Version;
import com.iris.prodcat.search.ProductIndex;

public class ProductCatalog {

	private static final String IRIS_BRAND = "Iris";
	private Metadata metadata;
	private List<ProductCatalogEntry> allProducts = new ArrayList<ProductCatalogEntry>();
	private List<ProductCatalogEntry> browseableProductsCache = new ArrayList<ProductCatalogEntry>();
	private Map<String,ProductCatalogEntry> allProductsByIdMapCache = new LinkedHashMap<String,ProductCatalogEntry>();
	private Map<String,ProductCatalogEntry> browseableProductsByIdMapCache = new LinkedHashMap<String,ProductCatalogEntry>();
	private Map<String,List<ProductCatalogEntry>> productsByCategoryMapCache = new LinkedHashMap<String,List<ProductCatalogEntry>>();
	private Map<String,List<ProductCatalogEntry>> productsByBrandMapCache = new LinkedHashMap<String,List<ProductCatalogEntry>>();
	private List<Category> categoriesCache = new ArrayList<>();
	private List<Brand> brandsCache = new ArrayList<>();
	private ProductIndex productIndex = null;
	private Version newestHubFirmwareValue;   //The newest minHubFirmware value in products
	private final ProductComparator productComparator = new ProductComparator();
	private final BrandComparator brandComparator = new BrandComparator();
	
	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public int getProductCount() {
		// products contains all products declared, including those marked canbrowse=false so 
		// return the count of objects in the filtered browseableProductsCache which does not include
		// products that should not be seen.
		return browseableProductsCache.size();
	}

	public List<ProductCatalogEntry> getProducts() {
		// return only those products which have canbrowse=true
		return Collections.unmodifiableList(browseableProductsCache);
	}
	
	public List<ProductCatalogEntry> getProducts(Version firmwareVersion) {
      // return only those products which have canbrowse=true and meets hub firmware version requirement
	   if(isNoNeedToFilterByFirmwareVersion(firmwareVersion)) {
	      return getProducts();
	   }else{
	      return filterProductsByFirmwareVersion(firmwareVersion, browseableProductsByIdMapCache.values());	      
	   }
   }
	
	public List<ProductCatalogEntry> getProducts(Version firmwareVersion, Boolean hubRequired) {
	      // return only those products which have the required value of hubRequired
		  if(isNoNeedToFilterByFirmwareVersion(firmwareVersion)) {
		      return filterProductsByHubRequired(hubRequired, getProducts());
		   }else{
		      return filterProductsByHubRequired(hubRequired, filterProductsByFirmwareVersion(firmwareVersion, browseableProductsByIdMapCache.values()));	      
		   }
	   }
	
   public List<ProductCatalogEntry> getAllProducts() {
		// returns all products, even those with canbrowse=false
		return Collections.unmodifiableList(allProducts);
	}
	
	public List<ProductCatalogEntry> getAllProducts(Version firmwareVersion) {
      // returns all products, even those with canbrowse=false and meets hub firmware version requirement
      if(isNoNeedToFilterByFirmwareVersion(firmwareVersion)) {
         return getAllProducts();
      }else{
         return filterProductsByFirmwareVersion(firmwareVersion, allProducts);       
      }
   }
	
	public List<ProductCatalogEntry> getAllProducts(Version firmwareVersion, Boolean hubRequired) {
	      // returns all products, even those with canbrowse=false and meets hub firmware version requirement
	      if(isNoNeedToFilterByFirmwareVersion(firmwareVersion)) {
	         return filterProductsByHubRequired(hubRequired, getAllProducts());
	      }else{
	         return filterProductsByFirmwareVersion(firmwareVersion, allProducts);       
	      }
	   }
	
	
	
	public void setData(Collection<Brand> allBrands, Collection<Category> allCategories, List<ProductCatalogEntry> products) {
		Collections.sort(products, productComparator);
		this.allProducts = Collections.unmodifiableList(products);
		refreshCaches(allBrands, allCategories);
		try {
         productIndex = new ProductIndex(this);
      } catch (IOException e) {
         throw new RuntimeException("Exception while indexing product catalog.", e);
      }
	}

	public int getCategoryCount() {
	   return categoriesCache.size();
	}

	public List<Category> getCategories() {
		return Collections.unmodifiableList(categoriesCache);
	}

	public Map<String,Integer> getProductCountByCategory() {
	   Map<String,Integer> counts = new HashMap<>();
	   productsByCategoryMapCache.forEach((k, v) -> counts.put(k, v.size()));
	   return counts;
	}
	
	public Map<String,Integer> getProductCountByCategory(Version firmwareVersion) {
	   if(isNoNeedToFilterByFirmwareVersion(firmwareVersion)) {
	      return getProductCountByCategory();
	   }else{
         Map<String,Integer> counts = new HashMap<>();
         productsByCategoryMapCache.forEach((k, v) -> counts.put(k, filterProductsByFirmwareVersion(firmwareVersion, v).size()));
         return counts;
	   }
   }

	public int getBrandCount() {
	   return brandsCache.size();
	}

	public List<Brand> getBrands() {
		return Collections.unmodifiableList(brandsCache);
	}

	public Map<String,Integer> getProductCountByBrand() {
	   Map<String,Integer> counts = new HashMap<>();
	   productsByBrandMapCache.forEach((k,v) -> counts.put(k, v.size()));
	   return counts;
	}
	
	public Map<String,Integer> getProductCountByBrand(Version firmwareVersion) {
	   if(isNoNeedToFilterByFirmwareVersion(firmwareVersion)) {
         return getProductCountByBrand();
      }else{
         Map<String,Integer> counts = new HashMap<>();
         productsByBrandMapCache.forEach((k,v) -> counts.put(k, filterProductsByFirmwareVersion(firmwareVersion, v).size()));
         return counts;
      }
   }

	public List<ProductCatalogEntry> getProductsByCategory(String category) {
		List<ProductCatalogEntry> ps = productsByCategoryMapCache.get(category);
		if (ps == null) {
			return null;
		}
		return Collections.unmodifiableList(ps);
	}
	
	public List<ProductCatalogEntry> getProductsByCategory(String category, Version firmwareVersion) {
	   if(isNoNeedToFilterByFirmwareVersion(firmwareVersion)) {
	      return getProductsByCategory(category);
	   }else{
	      List<ProductCatalogEntry> ps = productsByCategoryMapCache.get(category);
	      if (ps == null) {
	         return null;
	      }
	      return filterProductsByFirmwareVersion(firmwareVersion, ps);
	   }
      
   }

	public List<ProductCatalogEntry> getProductsByBrand(String brand) {
		List<ProductCatalogEntry> ps = productsByBrandMapCache.get(brand);
		if (ps == null) {
			return null;
		}
		return Collections.unmodifiableList(ps);
	}
	
	public List<ProductCatalogEntry> getProductsByBrand(String brand, Version firmwareVersion) {
	   if(isNoNeedToFilterByFirmwareVersion(firmwareVersion)) {
         return getProductsByBrand(brand);
      }else{
         List<ProductCatalogEntry> ps = productsByBrandMapCache.get(brand);
         if (ps == null) {
            return null;
         }
         return filterProductsByFirmwareVersion(firmwareVersion, ps);
      }
   }
	
	public List<ProductCatalogEntry> getProductsByBrand(String brand, Version firmwareVersion, Boolean hubRequired) {
		
		if(isNoNeedToFilterByFirmwareVersion(firmwareVersion)) {
	         return filterProductsByHubRequired(hubRequired, getProductsByBrand(brand));
	      }else{
	         List<ProductCatalogEntry> ps = productsByBrandMapCache.get(brand);
	         if (ps == null) {
	            return null;
	         }
	         return filterProductsByHubRequired(hubRequired, filterProductsByFirmwareVersion(firmwareVersion, ps));
	      }
	}

	public ProductCatalogEntry getProductById(String id) {
		return allProductsByIdMapCache.get(id);
	}

	public List<ProductCatalogEntry> findProducts(String search) {
	   if (productIndex == null) {
	      return null;
	   }
	   try {
         return productIndex.search(search);
      } catch (Exception e) {
         throw new RuntimeException("Exception while searching product catalog", e);
      }
	}

	private void refreshCaches(Collection<Brand> allBrands, Collection<Category> allCategories) {
		allProductsByIdMapCache.clear();
		browseableProductsCache.clear();
		categoriesCache.clear();
		brandsCache.clear();		

		Set<String> categoryNames = new HashSet<String>();
		Set<String> brandNames = new HashSet<String>();

		for (ProductCatalogEntry p : allProducts) {
			allProductsByIdMapCache.put(p.getId(), p);
		   if(p.getMinHubFirmware() != null) {
		      if(this.newestHubFirmwareValue == null || p.getMinHubFirmware().compareTo(newestHubFirmwareValue) < 0) {
		         newestHubFirmwareValue = p.getMinHubFirmware();
		      }
		   }
			// only include products in cache, categories, and brands if 
			// the product is marked able to be browsed, otherwise suppress
			if (p.getCanBrowse()) {
				// add product to list of products that allow browsing
				browseableProductsCache.add(p);

				// add product to product map cache
				browseableProductsByIdMapCache.put(p.getId(), p);

				// add name to category name cache
				categoryNames.addAll(p.getCategories());

				// add product to category cache
				for (String category: p.getCategories()) {
					List<ProductCatalogEntry> prodForCat = productsByCategoryMapCache.get(category);
					if (prodForCat == null) {
						prodForCat = new ArrayList<ProductCatalogEntry>();
						productsByCategoryMapCache.put(category, prodForCat);
					}
					prodForCat.add(p);
				}

				// add name to brand name cache
				brandNames.add(p.getVendor());

				// add product to brand cache
				List<ProductCatalogEntry> prodForBrand = productsByBrandMapCache.get(p.getVendor());
				if (prodForBrand == null) {
					prodForBrand = new ArrayList<ProductCatalogEntry>();
					productsByBrandMapCache.put(p.getVendor(), prodForBrand);
				}
				prodForBrand.add(p);
			}
		}

		brandsCache = allBrands.stream().filter((b) -> brandNames.contains(b.getName())).collect(Collectors.toList());
		Collections.sort(brandsCache, brandComparator);
		
		categoriesCache = allCategories.stream().filter((c) -> categoryNames.contains(c.getName())).collect(Collectors.toList());
		Collections.sort(categoriesCache);

		// sort the list of products for each category, alpha by name
		for (String category : productsByCategoryMapCache.keySet()) {
			List<ProductCatalogEntry> prodForCat = productsByCategoryMapCache.get(category);
			Collections.sort(prodForCat, productComparator);
		}

		// sort the list of products for each brand
		for (String category : productsByBrandMapCache.keySet()) {
			List<ProductCatalogEntry> prodForCat = productsByBrandMapCache.get(category);
			Collections.sort(prodForCat, productComparator);
		}

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
		result = prime * result + ((allProducts == null) ? 0 : allProducts.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProductCatalog other = (ProductCatalog) obj;
		if (metadata == null) {
			if (other.metadata != null)
				return false;
		} else if (!metadata.equals(other.metadata))
			return false;
		if (allProducts == null) {
			if (other.allProducts != null)
				return false;
		} else if (!allProducts.equals(other.allProducts))
			return false;
		return true;
	}

	
	private List<ProductCatalogEntry> filterProductsByFirmwareVersion(Version firmwareVersion, Collection<ProductCatalogEntry> originalProductList) {
	   
	   return Collections.unmodifiableList(originalProductList.stream()
	      .filter(p -> p.getMinHubFirmware() == null || firmwareVersion.compareTo(p.getMinHubFirmware()) <=0)
	      .collect(Collectors.toList()));
   }
	
	private List<ProductCatalogEntry> filterProductsByHubRequired(Boolean hubRequired, Collection<ProductCatalogEntry> originalProductList) {
		
		if(hubRequired == null) {
			return Collections.unmodifiableList(originalProductList.stream().collect(Collectors.toList()));
		}
		else {
			return Collections.unmodifiableList(originalProductList.stream()
				      .filter(p -> p.getHubRequired() == hubRequired)
				      .collect(Collectors.toList()));
		}
	}
	
	/**
	 * There are 3 conditions that no firmware version filtering is required:
	 * 1. The given firmwareVersion is null, meaning the client did not specify a firmware version to be filtered
	 * 2. this.newestHubFirmwareValue is null, meaning there is no minHubFirmware value specified in any product in this ProductCatalog.
	 * 3. The given firmwareVersion is same or newer than the newest minHubFirmware value specified in product_catalog.xml.
	 * @param firmwareVersion
	 * @return
	 */
	private boolean isNoNeedToFilterByFirmwareVersion(Version firmwareVersion)
   {
	   if(firmwareVersion == null || newestHubFirmwareValue == null || firmwareVersion.compareTo(newestHubFirmwareValue) <= 0) {
	      return true;
	   }else{
	      return false;
	   }
   }
	
	/**
	 * Sort the ProductCatalogEntry.  The rule is:
	 * 1. Order Iris as the first brand (vendor attribute)
	 * 2. Within Iris brand, non-first generation products come first in alphabetical order, 
	 * followed by the 1st generation products in alphabetical order.
	 *
	 */
	private static final class ProductComparator implements Comparator<ProductCatalogEntry> {
		
		private static final String IRIS_1ST_GEN = "1st Gen";
		
		@Override
		public int compare(ProductCatalogEntry p1, ProductCatalogEntry p2) {
			if(isIrisBrand(p1)) {
				if(isIrisBrand(p2)) {
					return compareTwoIrisProducts(p1, p2);
				}else{
					return -1;  //p1 first since Iris brand
				}
			}else{
				//p1 not Iris brand
				if(isIrisBrand(p2)) {
					return 1;	//p2 first since Iris brand
				}else{
					//neither Iris brand
					int brandComparison = p1.getVendor().compareTo(p2.getVendor());
					if(brandComparison != 0) {
						return brandComparison;
					}else{
						//same brand, compare name
						return p1.getName().compareTo(p2.getName());
					}
				}
			}
			
		}
		
		
		private boolean isIrisBrand(ProductCatalogEntry p1) {
			return IRIS_BRAND.equalsIgnoreCase(p1.getVendor());
		}
		
		private int compareTwoIrisProducts(ProductCatalogEntry p1, ProductCatalogEntry p2) {
			if(is1stGen(p1)) {
				if(is1stGen(p2)) {
					return p1.getName().compareTo(p2.getName()) ;  //alphabetically by product name
				}else{
					return 1;  //p2 first since non 1st gen Iris					
				}
			}else{
				//p1 is non 1st gen
				if(is1stGen(p2)) {
					return -1;	//p1 first non 1st gen Iris		
				}else{
					//both non 1st gen
					return p1.getName().compareTo(p2.getName()) ;  //alphabetically by product name
				}
			}
		}
		
		private boolean is1stGen(ProductCatalogEntry p1) {
			return p1.getName().startsWith(IRIS_1ST_GEN);
		}
	}
	
	private static final class BrandComparator implements Comparator<Brand> {

		@Override
		public int compare(Brand b1, Brand b2) {
			if(b1.getName().equals(b2.getName())) {
				return 0;
			}else{
				if(IRIS_BRAND.equals(b1.getName())) {
					return -1;	//b1 first because of Iris brand
				}else if(IRIS_BRAND.equals(b2.getName())) {
					return 1;	//b2 first because of Iris brand
				}else{
					return b1.getName().compareTo(b2.getName());  //neither Iris brand, compare alphabetically
				}
			}
		}
		
	}
	


}

