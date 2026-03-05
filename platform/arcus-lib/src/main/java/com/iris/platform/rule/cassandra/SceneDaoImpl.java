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

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.Statement;
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
   public SceneDaoImpl(CqlSession session, BeanAttributesTransformer<SceneDefinition> transformer) {
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
      sd.setPlaceId(row.getUuid(Column.PLACE_ID.columnName()));
      sd.setSequenceId(row.getInt(Column.ID.columnName()));
      sd.setCreated(row.isNull(Column.CREATED.columnName()) ? null : Date.from(row.getInstant(Column.CREATED.columnName())));
      sd.setModified(row.isNull(Column.MODIFIED.columnName()) ? null : Date.from(row.getInstant(Column.MODIFIED.columnName())));
      sd.setName(row.getString(Column.NAME.columnName()));
      sd.setDescription(row.getString(Column.DESCRIPTION.columnName()));
      sd.setTags(row.getSet(Column.TAGS.columnName(), String.class));
      sd.setLastFireState(row.getString(SceneColumn.LAST_FIRE_STATE.columnName()));
      sd.setLastFireTime(row.isNull(SceneColumn.LAST_FIRE_TIME.columnName()) ? null : Date.from(row.getInstant(SceneColumn.LAST_FIRE_TIME.columnName())));
      sd.setSatisfiable(row.getBoolean(SceneColumn.SATISFIABLE.columnName()));
      sd.setNotification(row.getBoolean(SceneColumn.NOTIFICATION.columnName()));
      sd.setTemplate(row.getString(SceneColumn.TEMPLATE.columnName()));
      sd.setEnabled(row.getBoolean(SceneColumn.ENABLED.columnName()));
      ByteBuffer action = row.isNull(SceneColumn.ACTION.columnName()) ? null : row.getByteBuffer(SceneColumn.ACTION.columnName());
      if(action != null) {
         byte [] array = new byte[action.remaining()];
         action.get(array);
         sd.setAction(array);
      }
      return sd;
   }

   protected Statement<?> prepareUpsert(SceneDefinition sd, Date ts) {
      BoundStatement bs = upsert.bind();
      bs = bs.setUuid(Column.PLACE_ID.columnName(), sd.getPlaceId());
      bs = bs.setInt(Column.ID.columnName(), sd.getSequenceId());
      bs = bs.setInstant(Column.CREATED.columnName(), sd.getCreated() == null ? null : sd.getCreated().toInstant());
      bs = bs.setInstant(Column.MODIFIED.columnName(), sd.getModified() == null ? null : sd.getModified().toInstant());
      bs = bs.setString(Column.NAME.columnName(), sd.getName());
      bs = bs.setString(Column.DESCRIPTION.columnName(), sd.getDescription());
      bs = bs.setSet(Column.TAGS.columnName(), sd.getTags(), String.class);
      bs = bs.setString(SceneColumn.TEMPLATE.columnName(), sd.getTemplate());
      bs = bs.setBoolean(SceneColumn.SATISFIABLE.columnName(), sd.isSatisfiable());
      bs = bs.setBoolean(SceneColumn.NOTIFICATION.columnName(), sd.isNotification());
      bs = bs.setInstant(SceneColumn.LAST_FIRE_TIME.columnName(), sd.getLastFireTime() == null ? null : sd.getLastFireTime().toInstant());
      bs = bs.setString(SceneColumn.LAST_FIRE_STATE.columnName(), sd.getLastFireState());
      bs = bs.setBoolean(SceneColumn.ENABLED.columnName(),sd.isEnabled());

      if(sd.getAction() != null) {
         bs = bs.setByteBuffer(ActionColumn.ACTION.columnName(), ByteBuffer.wrap(sd.getAction()));
      }
      else {
         bs = bs.setByteBuffer(ActionColumn.ACTION.columnName(), ByteBuffer.wrap(new byte [] {}));
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
