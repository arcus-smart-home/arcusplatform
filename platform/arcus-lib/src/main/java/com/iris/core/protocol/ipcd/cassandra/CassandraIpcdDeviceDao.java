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
package com.iris.core.protocol.ipcd.cassandra;

import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.core.protocol.ipcd.exceptions.DeviceNotFoundException;
import com.iris.core.protocol.ipcd.exceptions.IpcdDaoException;
import com.iris.core.protocol.ipcd.exceptions.PlaceMismatchException;
import com.iris.messages.address.Address;
import com.iris.platform.partition.Partitioner;
import com.iris.protocol.ipcd.IpcdDevice;
import com.iris.protocol.ipcd.IpcdDevice.ConnState;
import com.iris.protocol.ipcd.IpcdDevice.RegistrationState;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.ipcd.message.model.Device;

@Singleton
public class CassandraIpcdDeviceDao implements IpcdDeviceDao {
   private final Session session;
   private final PreparedStatement findById;
   private final PreparedStatement update;
   private final PreparedStatement insert;
   private final PreparedStatement deleteById;
   private final PreparedStatement streamByPartition;
   private final PreparedStatement claim;
   private final PreparedStatement completeRegistration;
   private final PreparedStatement unregister;
   private final PreparedStatement forceRegistration;
   private final PreparedStatement delete;
   private final PreparedStatement offline;
   private final Partitioner partitioner;

   @Inject
   public CassandraIpcdDeviceDao(Session session, Partitioner partitioner) {
      this.session = session;

      this.findById = CassandraQueryBuilder.select(IpcdDeviceTable.NAME)
                           .addColumns(IpcdDeviceTable.INSERT_COLUMNS)
                           .addWhereColumnEquals(IpcdDeviceTable.Columns.PROTOCOL_ADDRESS)
                           .prepare(session);
      this.update = CassandraQueryBuilder.update(IpcdDeviceTable.NAME)
                           .addColumns(
                                 IpcdDeviceTable.UPDATE_COLUMNS
                           )
                           .addWhereColumnEquals(IpcdDeviceTable.Columns.PROTOCOL_ADDRESS)
                           .prepare(session);
      this.insert = CassandraQueryBuilder.insert(IpcdDeviceTable.NAME)
                           .addColumns(
                                 IpcdDeviceTable.INSERT_COLUMNS
                            )
                            .prepare(session);
      this.deleteById = CassandraQueryBuilder.delete(IpcdDeviceTable.NAME)
                           .addWhereColumnEquals(IpcdDeviceTable.Columns.PROTOCOL_ADDRESS)
                           .prepare(session);

      streamByPartition =
            CassandraQueryBuilder
               .select(IpcdDeviceTable.NAME)
               .addColumns(IpcdDeviceTable.INSERT_COLUMNS)
               .addWhereColumnEquals("partitionId")
               .prepare(session);

      claim = CassandraQueryBuilder.update(IpcdDeviceTable.NAME)
            .addColumns(IpcdDeviceTable.Columns.ACCOUNT_ID, IpcdDeviceTable.Columns.PLACE_ID, IpcdDeviceTable.Columns.PARTITION_ID, IpcdDeviceTable.Columns.REGISTRATION_STATE)
            .addWhereColumnEquals(IpcdDeviceTable.Columns.PROTOCOL_ADDRESS)
            .ifClause(IpcdDeviceTable.Columns.PLACE_ID  + " = null AND " + IpcdDeviceTable.Columns.REGISTRATION_STATE + " = ? AND " + IpcdDeviceTable.Columns.CONN_STATE + " = ?")
            .prepare(session);

      completeRegistration = CassandraQueryBuilder.update(IpcdDeviceTable.NAME)
            .addColumns(IpcdDeviceTable.Columns.DRIVER_ADDRESS, IpcdDeviceTable.Columns.REGISTRATION_STATE)
            .addWhereColumnEquals(IpcdDeviceTable.Columns.PROTOCOL_ADDRESS)
            .ifClause(IpcdDeviceTable.Columns.PLACE_ID  + " = ?")
            .prepare(session);

      unregister = CassandraQueryBuilder.update(IpcdDeviceTable.NAME)
            .addColumns(IpcdDeviceTable.Columns.ACCOUNT_ID, IpcdDeviceTable.Columns.PLACE_ID, IpcdDeviceTable.Columns.PARTITION_ID, IpcdDeviceTable.Columns.DRIVER_ADDRESS, IpcdDeviceTable.Columns.REGISTRATION_STATE)
            .addWhereColumnEquals(IpcdDeviceTable.Columns.PROTOCOL_ADDRESS)
            .ifClause(IpcdDeviceTable.Columns.PLACE_ID  + " = ?")
            .prepare(session);

      forceRegistration = CassandraQueryBuilder.update(IpcdDeviceTable.NAME)
            .addColumns(IpcdDeviceTable.Columns.ACCOUNT_ID, IpcdDeviceTable.Columns.PLACE_ID, IpcdDeviceTable.Columns.PARTITION_ID, IpcdDeviceTable.Columns.DRIVER_ADDRESS, IpcdDeviceTable.Columns.REGISTRATION_STATE)
            .addWhereColumnEquals(IpcdDeviceTable.Columns.PROTOCOL_ADDRESS)
            .ifExists()
            .prepare(session);

      delete = CassandraQueryBuilder.delete(IpcdDeviceTable.NAME)
            .addWhereColumnEquals(IpcdDeviceTable.Columns.PROTOCOL_ADDRESS)
            .ifClause(IpcdDeviceTable.Columns.PLACE_ID + " = ?")
            .prepare(session);

      offline = CassandraQueryBuilder.update(IpcdDeviceTable.NAME)
            .addColumns(IpcdDeviceTable.Columns.CONN_STATE)
            .addWhereColumnEquals(IpcdDeviceTable.Columns.PROTOCOL_ADDRESS)
            .ifExists()
            .prepare(session);

      this.partitioner = partitioner;
   }

   @Override
   public IpcdDevice findByProtocolAddress(String address) {
      ResultSet rs = session.execute(findById.bind(address));
      Row row = rs.one();
      return row != null ? toIpcdDevice(row) : null;
   }

   @Override
   public IpcdDevice findByProtocolAddress(Address address) {
      String id = address.getRepresentation();
      return findByProtocolAddress(id);
   }

   @Override
   public IpcdDevice save(IpcdDevice ipcdDevice) {
      Date now = new Date();
      IpcdDevice updatedDevice = ipcdDevice.copy();
      updatedDevice.setModified(now);
      if (ipcdDevice.getCreated() == null) {
         updatedDevice.setCreated(now);
         session.execute(insert.bind(getInsertColumns(ipcdDevice)));
      }
      else {
         session.execute(update.bind(getUpdateColumnsPlusKey(ipcdDevice)));
      }
      return updatedDevice;
   }

   @Override
   public void delete(IpcdDevice ipcdDevice) {
      session.execute(deleteById.bind(ipcdDevice.getProtocolAddress()));
   }

   @Override
   public Stream<IpcdDevice> streamByPartitionId(int partitionId) {
      BoundStatement bs = streamByPartition.bind(partitionId);
      ResultSet rs = session.execute(bs);
      return StreamSupport.stream(rs.spliterator(), false).map((r) -> toIpcdDevice(r));
   }

   private Object[] getInsertColumns(IpcdDevice device) {
      return new Object[] {
            device.getProtocolAddress(),
            device.getAccountId(),
            device.getPlaceId(),
            device.getDriverAddress(),
            device.getCreated(),
            device.getModified(),
            device.getLastConnected(),
            device.getVendor(),
            device.getModel(),
            device.getSn(),
            device.getIpcdver(),
            device.getFirmware(),
            device.getConnection(),
            device.getActions(),
            device.getCommands(),
            device.getV1DeviceId(),
            (device.getPlaceId() != null ? partitioner.getPartitionForPlaceId(device.getPlaceId()).getId() : 0),
            String.valueOf(device.getConnState()),
            String.valueOf(device.getRegistrationState())
      };
   }

   private Object[] getUpdateColumnsPlusKey(IpcdDevice device) {
      return new Object[] {
            device.getAccountId(),
            device.getPlaceId(),
            device.getDriverAddress(),
            device.getCreated(),
            device.getModified(),
            device.getLastConnected(),
            device.getVendor(),
            device.getModel(),
            device.getSn(),
            device.getIpcdver(),
            device.getFirmware(),
            device.getConnection(),
            device.getActions(),
            device.getCommands(),
            device.getV1DeviceId(),
            (device.getPlaceId() != null ? partitioner.getPartitionForPlaceId(device.getPlaceId()).getId() : 0),
            String.valueOf(device.getConnState()),
            String.valueOf(device.getRegistrationState()),
            device.getProtocolAddress()
      };
   }

   private IpcdDevice toIpcdDevice(Row row) {
      IpcdDevice ipcdDevice = new IpcdDevice();
      ipcdDevice.setProtocolAddress(row.getString(IpcdDeviceTable.Columns.PROTOCOL_ADDRESS));
      ipcdDevice.setAccountId(row.getUUID(IpcdDeviceTable.Columns.ACCOUNT_ID));
      ipcdDevice.setPlaceId(row.getUUID(IpcdDeviceTable.Columns.PLACE_ID));
      ipcdDevice.setDriverAddress(row.getString(IpcdDeviceTable.Columns.DRIVER_ADDRESS));
      ipcdDevice.setCreated(row.getDate(IpcdDeviceTable.Columns.CREATED));
      ipcdDevice.setModified(row.getDate(IpcdDeviceTable.Columns.MODIFIED));
      ipcdDevice.setLastConnected(row.getDate(IpcdDeviceTable.Columns.LAST_CONNECTED));
      ipcdDevice.setVendor(row.getString(IpcdDeviceTable.Columns.VENDOR));
      ipcdDevice.setModel(row.getString(IpcdDeviceTable.Columns.MODEL));
      ipcdDevice.setSn(row.getString(IpcdDeviceTable.Columns.SN));
      ipcdDevice.setIpcdver(row.getString(IpcdDeviceTable.Columns.IPCD_VER));
      ipcdDevice.setFirmware(row.getString(IpcdDeviceTable.Columns.FIRMWARE));
      ipcdDevice.setConnection(row.getString(IpcdDeviceTable.Columns.CONNECTION));
      ipcdDevice.setActions(row.getSet(IpcdDeviceTable.Columns.ACTIONS, String.class));
      ipcdDevice.setCommands(row.getSet(IpcdDeviceTable.Columns.COMMANDS, String.class));
      ipcdDevice.setV1DeviceId(row.getString(IpcdDeviceTable.Columns.V1DEVICEID));

      String connState = row.getString(IpcdDeviceTable.Columns.CONN_STATE);
      if(!StringUtils.isBlank(connState)) {
         ipcdDevice.setConnState(ConnState.valueOf(connState.toUpperCase()));
      }

      String regState = row.getString(IpcdDeviceTable.Columns.REGISTRATION_STATE);
      if(!StringUtils.isBlank(regState)) {
         ipcdDevice.setRegistrationState(RegistrationState.valueOf(regState));
      }

      return ipcdDevice;
   }

   @Override
   public String claimAndGetProtocolAddress(Device d, UUID accountId, UUID placeId) throws IpcdDaoException {
      String protocolAddress = IpcdProtocol.ipcdAddress(d).getRepresentation();
      try {
         claim(protocolAddress, accountId, placeId);
         return protocolAddress;
      } catch(DeviceNotFoundException dnfe) {
         d.setSn(StringUtils.lowerCase(d.getSn()));
         protocolAddress = IpcdProtocol.ipcdAddress(d).getRepresentation();
         claim(protocolAddress, accountId, placeId);
         return protocolAddress;
      }
   }

   private void claim(String protocolAddress, UUID accountId, UUID placeId) throws IpcdDaoException {
      BoundStatement stmt = new BoundStatement(claim);
      stmt.bind(
            accountId,
            placeId,
            partitioner.getPartitionForPlaceId(placeId).getId(),
            IpcdDevice.RegistrationState.PENDING_DRIVER.name(),
            protocolAddress,
            IpcdDevice.RegistrationState.UNREGISTERED.name(),
            IpcdDevice.ConnState.ONLINE.name()
      );
      ResultSet rs = session.execute(stmt);
      parseOptimisticResult(rs, protocolAddress, null);
   }

   @Override
   public void completeRegistration(String protocolAddress, UUID placeId, String driverAddress) throws IpcdDaoException {
      BoundStatement stmt = new BoundStatement(completeRegistration);
      stmt.bind(
            driverAddress,
            IpcdDevice.RegistrationState.REGISTERED.name(),
            protocolAddress,
            placeId
      );

      ResultSet rs = session.execute(stmt);
      parseOptimisticResult(rs, protocolAddress, placeId);
   }

   @Override
   public void clearRegistration(String protocolAddress, UUID placeId) throws IpcdDaoException {
      BoundStatement stmt = new BoundStatement(unregister);
      stmt.bind(
            null,
            null,
            0,
            null,
            IpcdDevice.RegistrationState.UNREGISTERED.name(),
            protocolAddress,
            placeId
      );

      ResultSet rs = session.execute(stmt);
      parseOptimisticResult(rs, protocolAddress, placeId);
   }

   @Override
   public void forceRegistration(String protocolAddress, UUID accountId, UUID placeId, String driverAddress) {
      BoundStatement stmt = new BoundStatement(forceRegistration);
      stmt.bind(
            accountId,
            placeId,
            partitioner.getPartitionForPlaceId(placeId).getId(),
            driverAddress,
            IpcdDevice.RegistrationState.REGISTERED.name(),
            protocolAddress
      );

      session.execute(stmt);
   }

   @Override
   public void delete(String protocolAddress, UUID placeId) throws IpcdDaoException {
      BoundStatement stmt = new BoundStatement(delete);
      stmt.bind(
            protocolAddress,
            placeId
      );

      ResultSet rs = session.execute(stmt);
      parseOptimisticResult(rs, protocolAddress, placeId);
   }

   @Override
   public void offline(String protocolAddress) {
      BoundStatement stmt = new BoundStatement(offline);
      stmt.bind(IpcdDevice.ConnState.OFFLINE.name(), protocolAddress);
      session.execute(stmt);
   }

   private void parseOptimisticResult(ResultSet rs, String protocolAddress, UUID requiredPlace) throws IpcdDaoException {
      // not applied could imply that the device doesn't exist or the place didn't match
      if(!rs.wasApplied()) {
         Row r = rs.one();
         ColumnDefinitions colDef = r.getColumnDefinitions();

         if(colDef.contains(IpcdDeviceTable.Columns.PLACE_ID)) {
            // if the returned row contains a place id, the place id didn't match
            UUID actualPlaceId = r.getUUID(IpcdDeviceTable.Columns.PLACE_ID);
            if(!Objects.equal(requiredPlace, actualPlaceId)) {
               throw new PlaceMismatchException(requiredPlace, actualPlaceId);
            }
         }
         // special case for claiming device that is offline
         if(colDef.contains(IpcdDeviceTable.Columns.CONN_STATE) && IpcdDevice.ConnState.OFFLINE.name().equals(r.getString(IpcdDeviceTable.Columns.CONN_STATE))) {
            throw new DeviceNotFoundException(protocolAddress);
         }
         throw new DeviceNotFoundException(protocolAddress);
      }
   }
}

