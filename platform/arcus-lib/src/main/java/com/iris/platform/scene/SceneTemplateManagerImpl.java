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
package com.iris.platform.scene;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Place;
import com.iris.platform.scene.catalog.SceneCatalog;
import com.iris.platform.scene.catalog.SceneCatalogManager;
import com.iris.platform.scene.catalog.SceneTemplate;

/**
 * 
 */
@Singleton
public class SceneTemplateManagerImpl implements SceneTemplateManager {
   private final SceneDao sceneDao;
   private final SceneCatalogManager manager;
   private final PlaceDAO placeDao;
   private final PopulationDAO populationDao;

   @Inject
   public SceneTemplateManagerImpl(
         SceneDao sceneDao,
         SceneCatalogManager manager,
         PlaceDAO placeDao,
         PopulationDAO populationDao
   ) {
      this.sceneDao = sceneDao;
      this.manager = manager;
      this.placeDao = placeDao;
      this.populationDao = populationDao;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scene.SceneTemplateDao#listByPlaceId(java.util.UUID)
    */
   @Override
   public List<SceneTemplateEntity> listByPlaceId(UUID placeId) {
      Set<String> usedTemplates = sceneDao.getTemplateIds(placeId);
      SceneCatalog catalog = getCatalogForPlace(placeId);
      return 
            catalog
               .getTemplates()
               .stream()
               .map((template) -> build(template, usedTemplates))
               .collect(Collectors.toList());
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scene.SceneTemplateManager#findByPlaceAndId(java.util.UUID, java.lang.String)
    */
   @Override
   public SceneTemplate findByPlaceAndId(UUID placeId, String templateId) {
      SceneCatalog catalog = getCatalogForPlace(placeId);
      return catalog.getById(templateId);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scene.SceneTemplateDao#findByPlaceAndId(java.util.UUID, java.lang.String)
    */
   @Override
   public SceneTemplateEntity findEntityByPlaceAndId(UUID placeId, String templateId) {
      Set<String> usedTemplates = sceneDao.getTemplateIds(placeId);
      SceneTemplate template = findByPlaceAndId(placeId, templateId);
      if(template == null) {
         return null;
      }
      
      return build(template, usedTemplates);
   }
   
   private static SceneTemplateEntity build(SceneTemplate template, Set<String> usedTemplates) {
      SceneTemplateEntity entity = new SceneTemplateEntity(template);
      if(SceneTemplate.CUSTOM_TEMPLATE.equals(template.getId())) {
         entity.setCustom(true);
         entity.setAvailable(true);
      }
      else {
         entity.setCustom(false);
         entity.setAvailable(!usedTemplates.contains(entity.getId()));
      }
      return entity;
   }

   private SceneCatalog getCatalogForPlace(UUID placeId) {
      String population = placeDao.getPopulationById(placeId);
      return getCatalogForPlace(population);
   }
   
   private SceneCatalog getCatalogForPlace(String population) {
      return manager.getCatalog(population);
   }
   
}

