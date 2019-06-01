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
package com.iris.platform.rule.cassandra;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.messages.model.Model;
import com.iris.messages.model.PersistentModel;
import com.iris.messages.model.SimpleModel;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.rule.cassandra.RuleEnvironmentTable.ActionColumn;
import com.iris.platform.rule.cassandra.RuleEnvironmentTable.Column;
import com.iris.platform.rule.cassandra.RuleEnvironmentTable.SceneColumn;
import com.iris.platform.scene.SceneDao;
import com.iris.platform.scene.SceneDefinition;

@Singleton
public class SceneDaoImpl extends BaseRuleEnvironmentDaoImpl<SceneDefinition> implements SceneDao {
   static final String TYPE = "scene";
   
   private static final String [] UPSERT_COLUMNS = new String [] {
      Column.CREATED.columnName(),
      Column.MODIFIED.columnName(),
      Column.NAME.columnName(),
      Column.DESCRIPTION.columnName(),
      Column.TAGS.columnName(),
      SceneColumn.TEMPLATE.columnName(),
      SceneColumn.SATISFIABLE.columnName(),
      SceneColumn.NOTIFICATION.columnName(),
      SceneColumn.ACTION.columnName(),
      SceneColumn.LAST_FIRE_TIME.columnName(),
      SceneColumn.LAST_FIRE_STATE.columnName(),
      SceneColumn.ENABLED.columnName()
   };
   
   private final PreparedStatement upsert;
   private final BeanAttributesTransformer<SceneDefinition> transformer;

   @Inject
   public SceneDaoImpl(Session session, BeanAttributesTransformer<SceneDefinition> transformer) {
      super(session, TYPE);
      
      this.transformer = transformer;
      this.upsert =
               CassandraQueryBuilder
                  .update(RuleEnvironmentTable.NAME)
                  .addColumns(UPSERT_COLUMNS)
                  .where(whereIdEq(TYPE))
                  .prepare(session);
   }
   
   protected SceneDefinition buildEntity(Row row) {
      SceneDefinition sd = new SceneDefinition();
      sd.setPlaceId(row.getUUID(Column.PLACE_ID.columnName()));
      sd.setSequenceId(row.getInt(Column.ID.columnName()));
      sd.setCreated(row.getTimestamp(Column.CREATED.columnName()));
      sd.setModified(row.getTimestamp(Column.MODIFIED.columnName()));
      sd.setName(row.getString(Column.NAME.columnName()));
      sd.setDescription(row.getString(Column.DESCRIPTION.columnName()));
      sd.setTags(row.getSet(Column.TAGS.columnName(), String.class));
      sd.setLastFireState(row.getString(SceneColumn.LAST_FIRE_STATE.columnName()));
      sd.setLastFireTime(row.getTimestamp(SceneColumn.LAST_FIRE_TIME.columnName()));
      sd.setSatisfiable(row.getBool(SceneColumn.SATISFIABLE.columnName()));
      sd.setNotification(row.getBool(SceneColumn.NOTIFICATION.columnName()));
      sd.setTemplate(row.getString(SceneColumn.TEMPLATE.columnName()));
      sd.setEnabled(row.getBool(SceneColumn.ENABLED.columnName()));
      ByteBuffer action = row.getBytes(SceneColumn.ACTION.columnName());
      if(action != null) {
         byte [] array = new byte[action.remaining()];
         action.get(array);
         sd.setAction(array);
      }
      return sd;
   }

   protected Statement prepareUpsert(SceneDefinition sd, Date ts) {
      BoundStatement bs = upsert.bind();
      bs.setUUID(Column.PLACE_ID.columnName(), sd.getPlaceId());
      bs.setInt(Column.ID.columnName(), sd.getSequenceId());
      bs.setTimestamp(Column.CREATED.columnName(), sd.getCreated());
      bs.setTimestamp(Column.MODIFIED.columnName(), sd.getModified());
      bs.setString(Column.NAME.columnName(), sd.getName());
      bs.setString(Column.DESCRIPTION.columnName(), sd.getDescription());
      bs.setSet(Column.TAGS.columnName(), sd.getTags());
      bs.setString(SceneColumn.TEMPLATE.columnName(), sd.getTemplate());
      bs.setBool(SceneColumn.SATISFIABLE.columnName(), sd.isSatisfiable());
      bs.setBool(SceneColumn.NOTIFICATION.columnName(), sd.isNotification());
      bs.setTimestamp(SceneColumn.LAST_FIRE_TIME.columnName(), sd.getLastFireTime());
      bs.setString(SceneColumn.LAST_FIRE_STATE.columnName(), sd.getLastFireState());
      bs.setBool(SceneColumn.ENABLED.columnName(),sd.isEnabled());

      if(sd.getAction() != null) {
         bs.setBytes(ActionColumn.ACTION.columnName(), ByteBuffer.wrap(sd.getAction()));
      }
      else {
         bs.setBytes(ActionColumn.ACTION.columnName(), ByteBuffer.wrap(new byte [] {}));
      }
      return bs;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scene.SceneDao#listModelsByPlace(java.util.UUID)
    */
   @Override
   public List<Model> listModelsByPlace(UUID placeId) {
      return listByPlace(placeId)
               .stream()
               .map((sd) -> new SimpleModel(transformer.transform(sd)))
               .collect(Collectors.toList());
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.SceneDao#save(com.iris.messages.model.PersistentModel)
    */
   @Override
   public PersistentModel save(PersistentModel model) {
      SceneDefinition sd = transformer.transform(model.toMap());
      sd.setCreated(model.getCreated());
      sd.setModified(sd.getModified());
      
      save(sd);
      
      ModelEntity entity = new ModelEntity(transformer.transform(sd));
      entity.setId(sd.getId().getRepresentation());
      entity.setCreated(sd.getCreated());
      entity.setModified(sd.getModified());
      return entity;
   }
}

