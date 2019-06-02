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
package com.iris.core.dao.cassandra;

import static com.iris.util.TimeZones.getOffsetAsHours;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select.Selection;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.core.dao.metrics.TimingIterator;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.type.Population;
import com.iris.platform.PagedResults;
import com.iris.platform.location.LocationService;
import com.iris.platform.location.PlaceLocation;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.partition.Partitioner;

@Singleton
public class PlaceDAOImpl extends ChangesBaseCassandraCRUDDao<UUID, Place> implements PlaceDAO {
   private static final Logger logger = LoggerFactory.getLogger(PlaceDAOImpl.class);

   private static final Timer streamAllTimer = DaoMetrics.readTimer(PlaceDAO.class, "streamAll");
   private static final Timer streamByPartitionIdTimer = DaoMetrics.readTimer(PlaceDAO.class, "streamByPartitionId");
   private static final Timer streamPlaceAndAccountByPartitionIdTimer = DaoMetrics.readTimer(PlaceDAO.class, "streamPlaceAndAccountByPartitionId");
   private static final Timer streamPlaceAndAccountAndServiceLevelByPartitionIdTimer = DaoMetrics.readTimer(PlaceDAO.class, "streamPlaceAndAccountAndServiceLevelByPartitionId");
   private static final Timer findByPlaceIDInTimer = DaoMetrics.readTimer(PlaceDAO.class, "findByPlaceIDIn");
   private static final Timer setUpdateFlagTimer = DaoMetrics.updateTimer(PlaceDAO.class, "setUpdateFlag");
   private static final Timer getUpdateFlagTimer = DaoMetrics.readTimer(PlaceDAO.class, "getUpdateFlag");
   private static final Timer listPlacesTimer = DaoMetrics.readTimer(PlaceDAO.class, "listPlaces");

   private static final String TABLE = "place";

   private static final String UPDATEFLAG = "updateflag";

   static class PlaceEntityColumns {
      final static String ACCOUNT_ID = "accountId";
      final static String NAME = "name";
      final static String STATE = "state";
      final static String STREET_ADDRESS_1 = "streetAddress1";
      final static String STREET_ADDRESS_2 = "streetAddress2";
      final static String CITY = "city";
      final static String STATE_PROV = "stateProv";
      final static String ZIP_CODE = "zipCode";
      final static String ZIP_PLUS_4 = "zipPlus4";
      final static String TZ_ID = "tzId";
      // TODO drop these from the database and just use tzId
      final static String TZ_NAME = "tzName";
      final static String TZ_OFFSET = "tzOffset";
      final static String TZ_USES_DST = "tzUsesDST";
      final static String COUNTRY = "country";
      final static String ADDR_VALIDATED = "addrValidated";
      final static String ADDR_TYPE = "addrType";
      final static String ADDR_ZIP_TYPE = "addrZipType";
      final static String ADDR_LATITUDE = "addrLatitude";
      final static String ADDR_LONGITUDE = "addrLongitude";
      final static String ADDR_GEO_PRECISION = "addrGeoPrecision";
      final static String ADDR_RDI = "addrRDI";
      final static String ADDR_COUNTY = "addrCounty";
      final static String ADDR_COUNTY_FIPS = "addrCountyFips";

      final static String LAST_SERVICE_LEVEL_CHANGE = "lastservicelevelchange";
      final static String SERVICE_LEVEL = "serviceLevel";
      final static String SERVICE_ADDONS = "serviceAddons";
      final static String POPULATION = "population";
      final static String PRIMARY = "is_primary";
      final static String PARTITION_ID = "partitionId";
   }

   private static final String[] COLUMN_ORDER = {
      PlaceEntityColumns.ACCOUNT_ID,
      PlaceEntityColumns.NAME,
      PlaceEntityColumns.STATE,
      PlaceEntityColumns.STREET_ADDRESS_1,
      PlaceEntityColumns.STREET_ADDRESS_2,
      PlaceEntityColumns.CITY,
      PlaceEntityColumns.STATE_PROV,
      PlaceEntityColumns.ZIP_CODE,
      PlaceEntityColumns.ZIP_PLUS_4,
      PlaceEntityColumns.TZ_ID,
      PlaceEntityColumns.TZ_NAME,
      PlaceEntityColumns.TZ_OFFSET,
      PlaceEntityColumns.TZ_USES_DST,
      PlaceEntityColumns.COUNTRY,
      PlaceEntityColumns.ADDR_VALIDATED,
      PlaceEntityColumns.ADDR_TYPE,
      PlaceEntityColumns.ADDR_ZIP_TYPE,
      PlaceEntityColumns.ADDR_LATITUDE,
      PlaceEntityColumns.ADDR_LONGITUDE,
      PlaceEntityColumns.ADDR_GEO_PRECISION,
      PlaceEntityColumns.ADDR_RDI,
      PlaceEntityColumns.ADDR_COUNTY,
      PlaceEntityColumns.ADDR_COUNTY_FIPS,
      PlaceEntityColumns.LAST_SERVICE_LEVEL_CHANGE,
      PlaceEntityColumns.SERVICE_LEVEL,
      PlaceEntityColumns.SERVICE_ADDONS,
      PlaceEntityColumns.POPULATION,
      PlaceEntityColumns.PRIMARY,
      PlaceEntityColumns.PARTITION_ID,
   };

   private final static ServiceLevelChangeTracker[] changeTrackers = {
   	new ServiceLevelChangeTracker()
   };

   private final PreparedStatement streamAll;
   private final PreparedStatement listPagedNull;
   private final PreparedStatement listPagedContinue;
   private final PreparedStatement streamByPartitionId;
   private final PreparedStatement streamPlaceAndAccountByPartitionId;
   private final PreparedStatement streamPlaceAndAccountAndServiceLevelByPartitionId;
   private final PreparedStatement getAccountById;
   private PreparedStatement setUpdateFlag;
   private PreparedStatement getUpdateFlag;
   private final PreparedStatement getPopulationById;
   private final PreparedStatement getServiceLevelById;

   private final Partitioner partitioner;
   private final LocationService locationService;

   @Inject
   public PlaceDAOImpl(Session session, Partitioner partitioner, LocationService locationService) {
      super(session, TABLE, COLUMN_ORDER, changeTrackers);
      this.partitioner = partitioner;
      this.locationService = locationService;

      setUpdateFlag = CassandraQueryBuilder.update(TABLE)
            .addColumn(UPDATEFLAG)
            .addWhereColumnEquals(BaseEntityColumns.ID)
            .prepare(session);

      getUpdateFlag = CassandraQueryBuilder.select(TABLE)
            .addColumn(UPDATEFLAG)
            .addWhereColumnEquals(BaseEntityColumns.ID)
            .prepare(session);

      streamAll = CassandraQueryBuilder.select(TABLE)
            .addColumns(BASE_COLUMN_ORDER)
            .addColumns(COLUMN_ORDER)
            .prepare(session);

      streamByPartitionId = CassandraQueryBuilder.select(TABLE)
            .addColumns(BASE_COLUMN_ORDER)
            .addColumns(COLUMN_ORDER)
            .addWhereColumnEquals("partitionId")
            .prepare(session);

      streamPlaceAndAccountByPartitionId = CassandraQueryBuilder.select(TABLE)
            .addColumns(BaseEntityColumns.ID)
            .addColumns(PlaceEntityColumns.ACCOUNT_ID)
            .addWhereColumnEquals("partitionId")
            .prepare(session);
      
      streamPlaceAndAccountAndServiceLevelByPartitionId = CassandraQueryBuilder.select(TABLE)
            .addColumns(BaseEntityColumns.ID)
            .addColumns(PlaceEntityColumns.ACCOUNT_ID)
            .addColumns(PlaceEntityColumns.SERVICE_LEVEL)
            .addWhereColumnEquals("partitionId")
            .prepare(session);

      getAccountById = CassandraQueryBuilder.select(TABLE)
            .addColumns(PlaceEntityColumns.ACCOUNT_ID)
            .addWhereColumnEquals(BaseEntityColumns.ID)
            .prepare(session);
      
      getPopulationById = CassandraQueryBuilder.select(TABLE)
            .addColumns(PlaceEntityColumns.POPULATION)
            .addWhereColumnEquals(BaseEntityColumns.ID)
            .prepare(session);
      getServiceLevelById = CassandraQueryBuilder.select(TABLE)
            .addColumns(PlaceEntityColumns.SERVICE_LEVEL)
            .addWhereColumnEquals(BaseEntityColumns.ID)
            .prepare(session);

      listPagedNull = CassandraQueryBuilder
            .select(TABLE)
            .addColumns(BASE_COLUMN_ORDER)
            .addColumns(COLUMN_ORDER)
            .prepare(session);
      listPagedContinue = CassandraQueryBuilder
                 .select(TABLE)
                 .addColumns(BASE_COLUMN_ORDER)
                 .addColumns(COLUMN_ORDER)
                 .where("token(" + BaseEntityColumns.ID + ") >= token(?)")
                 .limit("?")
                 .prepare(session);
   }

   @Override
   protected List<Object> getValues(Place entity) {
      List<Object> values = new LinkedList<Object>();
      values.add(entity.getAccount());
      values.add(entity.getName());
      values.add(entity.getState());
      values.add(entity.getStreetAddress1());
      values.add(entity.getStreetAddress2());
      values.add(entity.getCity());
      values.add(entity.getStateProv());
      values.add(entity.getZipCode());
      values.add(entity.getZipPlus4());
      values.add(entity.getTzId());
      values.add(entity.getTzName());
      values.add(entity.getTzOffset());
      values.add(entity.getTzUsesDST());
      values.add(entity.getCountry());
      values.add(entity.getAddrValidated());
      values.add(entity.getAddrType());
      values.add(entity.getAddrZipType());
      values.add(entity.getAddrLatitude());
      values.add(entity.getAddrLongitude());
      values.add(entity.getAddrGeoPrecision());
      values.add(entity.getAddrRDI());
      values.add(entity.getAddrCounty());
      values.add(entity.getAddrCountyFIPS());
      values.add(entity.getLastServiceLevelChange());
      values.add(entity.getServiceLevel() != null ? entity.getServiceLevel().name() : null);
      values.add(entity.getServiceAddons());
      values.add(entity.getPopulation());
      values.add(entity.isPrimary());
      values.add(partitioner.getPartitionForPlaceId(entity.getId()).getId());
      
      return values;
   }

   @Override
   protected Place createEntity() {
      return new Place();
   }

   @Override
   protected void populateEntity(Row row, Place entity) {
      entity.setCreated(row.getTimestamp(BaseEntityColumns.CREATED));
      entity.setModified(row.getTimestamp(BaseEntityColumns.MODIFIED));
      entity.setAccount(row.getUUID(PlaceEntityColumns.ACCOUNT_ID));
      entity.setName(row.getString(PlaceEntityColumns.NAME));
      entity.setState(row.getString(PlaceEntityColumns.STATE));
      entity.setStreetAddress1(row.getString(PlaceEntityColumns.STREET_ADDRESS_1));
      entity.setStreetAddress2(row.getString(PlaceEntityColumns.STREET_ADDRESS_2));
      entity.setCity(row.getString(PlaceEntityColumns.CITY));
      entity.setStateProv(row.getString(PlaceEntityColumns.STATE_PROV));
      entity.setZipCode(row.getString(PlaceEntityColumns.ZIP_CODE));
      entity.setZipPlus4(row.getString(PlaceEntityColumns.ZIP_PLUS_4));
      entity.setTzId(row.getString(PlaceEntityColumns.TZ_ID));
      entity.setTzName(row.getString(PlaceEntityColumns.TZ_NAME));
      entity.setTzOffset(row.getDouble(PlaceEntityColumns.TZ_OFFSET));
      entity.setTzUsesDST(row.getBool(PlaceEntityColumns.TZ_USES_DST));
      entity.setCountry(row.getString(PlaceEntityColumns.COUNTRY));
      entity.setAddrValidated(row.getBool(PlaceEntityColumns.ADDR_VALIDATED));
      entity.setAddrType(row.getString(PlaceEntityColumns.ADDR_TYPE));
      entity.setAddrZipType(row.getString(PlaceEntityColumns.ADDR_ZIP_TYPE));
      entity.setAddrLatitude(row.getDouble(PlaceEntityColumns.ADDR_LATITUDE));
      entity.setAddrLongitude(row.getDouble(PlaceEntityColumns.ADDR_LONGITUDE));
      entity.setAddrGeoPrecision(row.getString(PlaceEntityColumns.ADDR_GEO_PRECISION));
      entity.setAddrRDI(row.getString(PlaceEntityColumns.ADDR_RDI));
      entity.setAddrCounty(row.getString(PlaceEntityColumns.ADDR_COUNTY));
      entity.setAddrCountyFIPS(row.getString(PlaceEntityColumns.ADDR_COUNTY_FIPS));
      entity.setLastServiceLevelChange(row.getTimestamp(PlaceEntityColumns.LAST_SERVICE_LEVEL_CHANGE));
      String serviceLevel = row.getString(PlaceEntityColumns.SERVICE_LEVEL);
      if(serviceLevel != null) {
         entity.setServiceLevel(ServiceLevel.valueOf(serviceLevel));
      }
      Set<String> addons = row.getSet(PlaceEntityColumns.SERVICE_ADDONS, String.class);
      entity.setServiceAddons(addons == null || addons.isEmpty() ? null : addons);
      String population = row.getString(PlaceEntityColumns.POPULATION);
      entity.setPopulation(StringUtils.isEmpty(population)?Population.NAME_GENERAL:population);
      entity.setPrimary(row.getBool(PlaceEntityColumns.PRIMARY));

      populateMissingLocationData(entity);
   }

   /* (non-Javadoc)
    * @see com.iris.core.dao.cassandra.BaseCassandraCRUDDao#doInsert(java.lang.Object, com.iris.messages.model.BaseEntity)
    */
   @Override
   protected Place doInsert(UUID id, Place entity) {
      Place copy = entity.copy();
      copy.setId(id); // ensure the id is set so that we can determine the partition
      return super.doInsert(id, copy);
   }

   @Override
   public Place create(Place place) {
      UUID id = place.getId();
      if(id == null) {
         id = nextId(place);
      }
      place = doInsert(id, place);
      // do this after the insert so that we don't save information that
      // wasn't explicitly requested by the user (in case we have a bug or the zip code data changes)
      populateMissingLocationData(place);
      return place;
   }

   @Override
   public Place save(Place entity) {
      Place place = super.save(entity);
      // do this after the update so that we don't save information that
      // wasn't explicitly requested by the user (in case we have a bug or the zip code data changes)
      populateMissingLocationData(place);
      return place;
	}

	@Override
   protected UUID getIdFromRow(Row row) {
      return row.getUUID(BaseEntityColumns.ID);
   }

   @Override
   protected UUID nextId(Place place) {
      return UUID.randomUUID();
   }

	/* (non-Javadoc)
    * @see com.iris.core.dao.PlaceDAO#streamAll()
    */
   @Override
   public Stream<Place> streamAll() {
      Context timer = streamAllTimer.time();
      Iterator<Row> rows = session.execute(streamAll.bind()).iterator();
      Iterator<Place> result = TimingIterator.time(
            Iterators.transform(rows, (row) -> buildEntity(row)),
            timer
      );
      Spliterator<Place> stream = Spliterators.spliteratorUnknownSize(result, Spliterator.IMMUTABLE | Spliterator.NONNULL);
      return StreamSupport.stream(stream, false);
   }

   @Override
   public Stream<Place> streamByPartitionId(int partitionId) {
      try(Context ctxt = streamByPartitionIdTimer.time()) {
         BoundStatement bs = streamByPartitionId.bind(partitionId);
         ResultSet rs = session.execute(bs);
         return stream(rs, (row) -> buildEntity(row));
      }
   }

   @Override
   public Stream<Map<UUID,UUID>> streamPlaceAndAccountByPartitionId(int partitionId) {
      try(Context ctxt = streamPlaceAndAccountByPartitionIdTimer.time()) {
         BoundStatement bs = streamPlaceAndAccountByPartitionId.bind(partitionId);
         ResultSet rs = session.execute(bs);
         return stream(rs, (row) -> {
            return ImmutableMap.of(row.getUUID(BaseEntityColumns.ID), row.getUUID(PlaceEntityColumns.ACCOUNT_ID));
         });
      }
   }
   
   @Override
	public Stream<Triple<UUID, UUID, ServiceLevel>> streamPlaceAndAccountAndServiceLevelByPartitionId(int partitionId) {
   	try(Context ctxt = streamPlaceAndAccountAndServiceLevelByPartitionIdTimer.time()) {
         BoundStatement bs = streamPlaceAndAccountAndServiceLevelByPartitionId.bind(partitionId);
         //bs.setConsistencyLevel(ConsistencyLevel.LOCAL_ONE);
         ResultSet rs = session.execute(bs);
         return stream(rs, (row) -> {
         	ServiceLevel s = ServiceLevel.BASIC;
         	String fromDb = row.getString(PlaceEntityColumns.SERVICE_LEVEL);
         	if(StringUtils.isNotBlank(fromDb)) {
         		s = ServiceLevel.valueOf(fromDb);
         	}
            return new ImmutableTriple<>(row.getUUID(BaseEntityColumns.ID), row.getUUID(PlaceEntityColumns.ACCOUNT_ID), s);
         });
      }
	}


   

	@Override
   public UUID getAccountById(UUID placeId) {
      BoundStatement bs = getAccountById.bind(placeId);
      ResultSet rs = session.execute(bs);
      Row row = rs.one();
      return (row != null) ? row.getUUID(PlaceEntityColumns.ACCOUNT_ID) : null;
   }
   
   @Override
   @Nullable
	public String getPopulationById(UUID placeId) {
   	BoundStatement bs = getPopulationById.bind(placeId);
      ResultSet rs = session.execute(bs);
      Row row = rs.one();
      return (row != null) ? row.getString(PlaceEntityColumns.POPULATION) : null;
	}
   
   @Override
   @Nullable
   public ServiceLevel getServiceLevelById(UUID placeId) {
   	BoundStatement bs = getServiceLevelById.bind(placeId);
      ResultSet rs = session.execute(bs);
      Row row = rs.one();
   	String serviceLevel = row.getString(PlaceEntityColumns.SERVICE_LEVEL);
      if(serviceLevel != null) {
         return ServiceLevel.valueOf(serviceLevel);
      }else{
      	return null;
      }
   }

   @Override
	public List<Place> findByPlaceIDIn(Set<UUID> placeIDs) {
		if(placeIDs == null || placeIDs.isEmpty()) {
         return Collections.<Place>emptyList();
      }

      List<Row> rows;
      Selection sel = QueryBuilder.select();
      for(String c : BASE_COLUMN_ORDER) { sel.column(c); }
      for(String c : COLUMN_ORDER) { sel.column(c); }
    	try(Context ctxt = findByPlaceIDInTimer.time()) {
    		rows = session.execute(
	    						sel.from(TABLE)
	    						.where(QueryBuilder.in(BaseEntityColumns.ID, placeIDs.toArray()))
    						).all();
    	}

    	// Return empty List here?
      if(rows == null || rows.isEmpty()) {
      	return Collections.<Place>emptyList();
      }

      List<Place> places = new ArrayList<Place>();
      for (Row row : rows) {
      	Place place = createEntity();
      	populateBaseEntity(row, place);
      	populateEntity(row, place);
      	places.add(place);
      }

      return places;
	}

   @Override
   public void setUpdateFlag(UUID placeId, boolean updateFlag) {
      Preconditions.checkArgument(placeId != null, "The place id cannot be null");
      BoundStatement statement = new BoundStatement(setUpdateFlag);
      try(Context ctxt = setUpdateFlagTimer.time()) {
         session.execute(statement.bind(updateFlag, placeId));
      }
   }

   @Override
   public boolean getUpdateFlag(UUID placeId) {
      Preconditions.checkArgument(placeId != null, "The place id cannot be null");
      BoundStatement statement = new BoundStatement(getUpdateFlag);
      ResultSet resultSet;
      try(Context ctxt = getUpdateFlagTimer.time()) {
         resultSet = session.execute(statement.bind(placeId));
      }
      Row row = resultSet.one();
      return row.getBool(UPDATEFLAG);
   }

   @Override
   public ModelEntity findPlaceModelById(UUID placeId) {
      Place place = findById(placeId);
      if(place == null) {
         return null;
      }
      ModelEntity entity = new ModelEntity(toAttributes(place));
      entity.setCreated(place.getCreated());
      entity.setModified(place.getModified());
      return entity;
   }

   private Map<String,Object> toAttributes(Place place) {
      Map<String,Object> attrs = new HashMap<>();
      attrs.put(PlaceCapability.ATTR_ACCOUNT, place.getAccount().toString());
      setIf(PlaceCapability.ATTR_ADDRCOUNTY, place.getAddrCounty(), attrs);
      setIf(PlaceCapability.ATTR_ADDRCOUNTYFIPS, place.getAddrCountyFIPS(), attrs);
      setIf(PlaceCapability.ATTR_ADDRGEOPRECISION, place.getAddrGeoPrecision(), attrs);
      setIf(PlaceCapability.ATTR_ADDRLATITUDE, place.getAddrLatitude(), attrs);
      setIf(PlaceCapability.ATTR_ADDRLONGITUDE, place.getAddrLongitude(), attrs);
      setIf(PlaceCapability.ATTR_ADDRRDI, place.getAddrRDI(), attrs);
      setIf(PlaceCapability.ATTR_ADDRTYPE, place.getAddrType(), attrs);
      setIf(PlaceCapability.ATTR_ADDRVALIDATED, place.getAddrValidated(), attrs);
      setIf(PlaceCapability.ATTR_ADDRZIPTYPE, place.getAddrZipType(), attrs);
      setIf(PlaceCapability.ATTR_CITY, place.getCity(), attrs);
      setIf(PlaceCapability.ATTR_COUNTRY, place.getCountry(), attrs);
      setIf(PlaceCapability.ATTR_NAME, place.getName(), attrs);

      if(place.getPopulation() != null) {
         attrs.put(PlaceCapability.ATTR_POPULATION, place.getPopulation().toString());
      }
      setIf(PlaceCapability.ATTR_SERVICEADDONS, place.getServiceAddons(), attrs);
      setIf(PlaceCapability.ATTR_LASTSERVICELEVELCHANGE, place.getLastServiceLevelChange(), attrs);
      setIf(PlaceCapability.ATTR_SERVICELEVEL, place.getServiceLevel(), attrs);
      setIf(PlaceCapability.ATTR_STATE, place.getState(), attrs);
      setIf(PlaceCapability.ATTR_STATEPROV, place.getStateProv(), attrs);
      setIf(PlaceCapability.ATTR_STREETADDRESS1, place.getStreetAddress1(), attrs);
      setIf(PlaceCapability.ATTR_STREETADDRESS2, place.getStreetAddress2(), attrs);
      setIf(PlaceCapability.ATTR_TZNAME, place.getTzName(), attrs);
      setIf(PlaceCapability.ATTR_TZID, place.getTzId(), attrs);
      setIf(PlaceCapability.ATTR_TZOFFSET, place.getTzOffset(), attrs);
      setIf(PlaceCapability.ATTR_TZUSESDST, place.getTzUsesDST(), attrs);
      setIf(PlaceCapability.ATTR_ZIPCODE, place.getZipCode(), attrs);
      setIf(PlaceCapability.ATTR_ZIPPLUS4, place.getZipPlus4(), attrs);
      setIf(PlaceCapability.ATTR_CREATED, place.getCreated(), attrs);
      setIf(PlaceCapability.ATTR_MODIFIED, place.getModified(), attrs);
      setIf(Capability.ATTR_ADDRESS, place.getAddress(), attrs);
      setIf(Capability.ATTR_CAPS, place.getCaps(), attrs);
      attrs.put(Capability.ATTR_ID, place.getId().toString());
      setIf(Capability.ATTR_TAGS, place.getTags(), attrs);
      setIf(Capability.ATTR_TYPE, place.getType(), attrs);
      return attrs;
   }

   private void setIf(String key, Object value, Map<String,Object> map) {
      if(value != null) {
         map.put(key, value);
      }
   }

   private void populateMissingLocationData(Place entity) {
      boolean isEmpty = StringUtils.isEmpty(entity.getAddrGeoPrecision());
      boolean isZed = isZed(entity.getAddrLatitude(), entity.getAddrLongitude());
      Optional<PlaceLocation> location = locationService.getForPlace(entity);
      if(isEmpty || isZed) {
         if(location.isPresent()) {
            logger.debug("Setting gelocation from zip [{}]", location.get().getCode());
            entity.setAddrCounty(location.get().getCounty());

            entity.setAddrLatitude(location.get().getGeoLocation().getLatitude());
            entity.setAddrLongitude(location.get().getGeoLocation().getLongitude());
            entity.setAddrGeoPrecision(location.get().getGeoPrecision());
         }
         else {
            logger.debug("Unable to determine missing geolocation information for place [{}]", entity.getId());
            entity.setAddrLatitude(0.0);
            entity.setAddrLongitude(0.0);
            entity.setAddrGeoPrecision(Place.GEOPRECISION_NONE);
         }
      }
      if(StringUtils.isEmpty(entity.getTzId())) {
         if(location.isPresent()) {
            TimeZone timeZone = location.get().getTimeZone();
            logger.debug("Populating missing timezone from zip [{}] as [{}]", location.get().getCode(), timeZone.getID());
            entity.setTzId(timeZone.getID());
            entity.setTzName(timeZone.getDisplayName());
            entity.setTzOffset(getOffsetAsHours(timeZone.getRawOffset()));
            entity.setTzUsesDST(timeZone.observesDaylightTime());
         }
         else {
            logger.debug("Unable to determine missing timezone information for place [{}]", entity.getId());
         }
      }
      if(StringUtils.isEmpty(entity.getState())) {
         if(location.isPresent()) {
            logger.debug("Populating missing state from zip [{}] as [{}]", location.get().getCode(), location.get().getState());
            entity.setState(location.get().getState());
         }
         else {
         	logger.debug("Unable to determine missing state for place [{}]", entity.getId());
         }
      }
      if(StringUtils.isEmpty(entity.getCity())) {
         if(location.isPresent()) {
            logger.debug("Populating missing city from zip [{}] as [{}]", location.get().getCode(), location.get().getPrimaryCity());
            entity.setCity(location.get().getPrimaryCity());
         }
         else {
            logger.debug("Unable to determine missing city for place [{}]", entity.getId());
         }
      }
   }

	private boolean isZed(Double addrLatitude, Double addrLongitude) {
		if(addrLatitude == null || addrLongitude == null) {
			return true;
		}
		else if(
				(-0.01 > addrLatitude && addrLatitude < 0.01) &&
				(-0.01 > addrLongitude && addrLongitude < 0.01)
		) {
			return true;
		}
		return false;
	}

	@Override
	public PagedResults<Place> listPlaces(PlaceQuery query) {
		   BoundStatement bs = null;
		   if (query.getToken() != null) {
			   bs = listPagedContinue.bind(UUID.fromString(query.getToken()), query.getLimit() + 1);
		   } else {
			   bs = listPagedNull.bind( );
		   }
		   try(Context ctxt = listPlacesTimer.time()) {
            return doList(bs, query.getLimit());
         }
	}

	private static class ServiceLevelChangeTracker implements ChangeTracker<Place> {

		@Override
      public Place checkForChange(Place original, Place current) {
	      if (!Objects.equals(original.getServiceLevel(), current.getServiceLevel())) {
	      	Place newPlace = current.copy();
	      	newPlace.setLastServiceLevelChange(new Date());
	      	return newPlace;
	      }
	      return current;
      }

	}

	
	

}

