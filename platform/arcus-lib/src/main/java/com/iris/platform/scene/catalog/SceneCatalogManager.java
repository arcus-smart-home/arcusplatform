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
package com.iris.platform.scene.catalog;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.core.dao.PopulationDAO;
import com.iris.io.xml.JAXBUtil;
import com.iris.messages.type.Population;
import com.iris.platform.scene.SceneConfig;
import com.iris.platform.scene.catalog.serializer.ActionTemplateType;
import com.iris.platform.scene.catalog.serializer.SceneType;
import com.iris.platform.scene.resolver.ActionResolver;
import com.iris.platform.scene.resolver.CatalogActionTemplateResolver;
import com.iris.resource.Resource;
import com.iris.resource.Resources;

@Singleton
public class SceneCatalogManager {
   private static final Logger LOGGER = LoggerFactory.getLogger(SceneCatalogManager.class);
   
   private final CapabilityRegistry registry;
   private Resource resource; 
   private final AtomicReference<Map<String,SceneCatalog>> catalogRef = new AtomicReference<>();
   private final PopulationDAO populationDao;
   
   private final String catalogPath;

   
   @Inject
   public SceneCatalogManager(SceneConfig config, CapabilityRegistry registry, PopulationDAO populationDao) {
      this.registry = registry;
      this.populationDao = populationDao;
      this.catalogPath = config.getCatalogPath();
   }
   
   @PostConstruct
   public void init(){
      resource = Resources.getResource(catalogPath);
      this.catalogRef.set(loadCatalog()); 
      if(resource.isWatchable()){
         resource.addWatch(() -> {
            catalogRef.set(loadCatalog()); 
         });
      }
   }

   public SceneCatalog getCatalog(String population) {
      if(catalogRef.get() == null) {
         loadCatalog();
      }
      if(StringUtils.isEmpty(population)) {
      	population = Population.NAME_GENERAL;
      }
      return catalogRef.get().get(population);
   }
   
   private Map<String, SceneCatalog> loadCatalog() {
      LOGGER.info("Loading scene catalog {}",resource.getFile());
      Map<String, List<SceneTemplate>> newTemplateMap = initTemplateMap();
      
      com.iris.platform.scene.catalog.serializer.SceneCatalog sc = JAXBUtil.fromXml(resource, com.iris.platform.scene.catalog.serializer.SceneCatalog.class);
      List<ActionResolver> dynamicResolvers = createResolvers(registry, sc);
      List<ActionResolver> resolvers = ImmutableList.<ActionResolver>builder().addAll(dynamicResolvers).build();
      for(SceneType st:sc.getScenes().getScene()){
         SceneTemplate template = createTemplate(
               st.getId(),
               st.getName(),
               st.getDescription(),
               resolvers
         );
         Set<String> populationList = parsePopulations(st.getPopulations());
         populationList.forEach(p -> {            
            List<SceneTemplate> templList = newTemplateMap.get(p);
            if(templList != null) {
               templList.add(template);
            }else{
               LOGGER.warn("{} is not a valid population", p);
            }
         });
         
      }
      Map<String, SceneCatalog> sceneCatalogMap = newTemplateMap.entrySet().stream()
         .collect(Collectors.toMap(Map.Entry::getKey, e -> new SceneCatalog(e.getValue())));
      return sceneCatalogMap;
   }
   
   private Map<String, List<SceneTemplate>> initTemplateMap() {
      List<Population> populationList = populationDao.listPopulations();
      Map<String, List<SceneTemplate>> templateMap = populationList.stream().collect(Collectors.toMap(x -> x.getName(), x -> new ArrayList<SceneTemplate>()));
      return templateMap;
   }

   private Set<String> parsePopulations(String populations)
   {
      if(StringUtils.isNotBlank(populations)) {
         return new HashSet<String>(Splitter.on(",").omitEmptyStrings().splitToList(populations));
      }else{
      	//Make populations required
         throw new IllegalArgumentException("Scene template missing populations");
      }
   }

   private static List<ActionResolver> createResolvers(CapabilityRegistry registry, com.iris.platform.scene.catalog.serializer.SceneCatalog sc) {
      ImmutableList.Builder<ActionResolver> resolvers = ImmutableList.builder();
      for (ActionTemplateType template : sc.getActionTemplates().getActionTemplate()){
         ActionResolver resolver = new CatalogActionTemplateResolver(registry, template);
         resolvers.add(resolver);
      }
      return resolvers.build();
   }

   private static SceneTemplate createTemplate(String id, String name, String description, List<ActionResolver> resolvers) {
      SceneTemplate template = new SceneTemplate();
      template.setId(id);
      template.setName(name);
      template.setDescription(description);
      template.setCreated(new Date());
      template.setModified(new Date());
      template.setActions(resolvers);
      return template;
   }
}

