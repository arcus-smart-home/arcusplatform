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
package com.iris.platform.hub.registration;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.HubRegistrationDAO;
import com.iris.firmware.FirmwareResult;
import com.iris.firmware.FirmwareURLBuilder;
import com.iris.firmware.FirmwareUpdateResolver;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.model.Hub;
import com.iris.messages.model.HubRegistration;
import com.iris.messages.model.HubRegistration.RegistrationState;
import com.iris.messages.model.HubRegistrationErrors;
import com.iris.util.ThreadPoolBuilder;
import com.netflix.governator.annotations.WarmUp;

@Singleton
public class HubRegistrationRegistry  {
	private static final Logger logger = LoggerFactory.getLogger(HubRegistrationRegistry.class);	

	private final HubRegistrationDAO hubRegistrationDao;
	private final ScheduledExecutorService executor;
	private final ConcurrentMap<String, HubRegistration> hubsInRegistration;
	private final long timeoutIntervalMs;
	private final long upgradeTimeoutMs;
	private final FirmwareUpdateResolver resolver;
	private final FirmwareURLBuilder<Hub> urlBuilder;
	private final Hub hubTemplate;
	
	@Inject
	public HubRegistrationRegistry(HubRegistrationDAO hubRegistrationDao, HubRegistrationConfig config,
			FirmwareUpdateResolver resolver, FirmwareURLBuilder<Hub> urlBuilder) {
		this.hubRegistrationDao = hubRegistrationDao;
		this.resolver = resolver;
		this.urlBuilder = urlBuilder;
		this.executor = ThreadPoolBuilder.newSingleThreadedScheduler("hub-registraion-watchdog");
		this.timeoutIntervalMs = TimeUnit.SECONDS.toMillis(config.getTimeoutIntervalSec());
		this.upgradeTimeoutMs = TimeUnit.MINUTES.toMillis(config.getUpgradeTimeoutMin());
		this.hubsInRegistration = new ConcurrentHashMap<>();
		this.hubTemplate = new Hub();
		hubTemplate.setModel("IH200");
	}

	@WarmUp
	public void start() {
		// 1. Load hub_registration into cache
		initHubRegistrationCache();

		// 2. Setup executor
		executor.scheduleWithFixedDelay(() -> timeout(), timeoutIntervalMs, timeoutIntervalMs, TimeUnit.MILLISECONDS);
	}

	@PreDestroy
	public void stop() {
		executor.shutdownNow();
	}

	public void online(String hubId) {
		HubRegistration curHubReg = hubsInRegistration.computeIfAbsent(hubId,
				(ha) -> this.loadOrCreateNewHubRegistration(hubId));
		curHubReg.setLastConnected(new Date());
		if (RegistrationState.OFFLINE.equals(curHubReg.getState())) {
			curHubReg.setState(RegistrationState.ONLINE);
		}
		curHubReg = hubRegistrationDao.save(curHubReg);
		logger.debug("hubId [{}] is online, hub_registration created at [{}], state is [{}]", curHubReg.getId(),
				curHubReg.getCreated(), curHubReg.getState());
	}

	public void offline(String hubId) {
		HubRegistration curHubReg = hubsInRegistration.get(hubId);
		if (curHubReg != null) {
			if(RegistrationState.DOWNLOADING.equals(curHubReg.getState())) {
				curHubReg.setState(RegistrationState.APPLYING);
			}else{
				curHubReg.setState(RegistrationState.OFFLINE);
			}
			curHubReg = hubRegistrationDao.save(curHubReg);
			logger.debug("hubId [{}] is offline, hub_registration created at [{}], state is [{}]", curHubReg.getId(),
					curHubReg.getCreated(), curHubReg.getState());
		}
	}
	
	public FirmwareResult getTargetFirmware(String model, String currentFirmwareVersion, String population) {
	   if (StringUtils.isNotEmpty(population)) {
	      return resolver.getTargetForVersion(model, currentFirmwareVersion, population);
	   } else {
	      return resolver.getTargetForVersion(model, currentFirmwareVersion);
	   }
	}
	
	@Nullable
	public MessageBody upgrade(Hub hub, String currentFirmwareVersion, String targetFirmware, String priority, boolean showLed) {

      Instant now = Instant.now();
      HubRegistration curHubReg = findHubRegistration(hub.getId());

      if (curHubReg != null && RegistrationState.APPLYING.equals(curHubReg.getState()) && targetFirmware != null) {
         // state is APPLYING, but firmwareVersion still did not reach
         // targetFirmware version.  Will log a warning, but do not update the error in hub_registration
         logger.warn(
               "Hub [{}] is at firmware version [{}], but registration state is APPLYING which should not happen.  Will try sending upgrade request again.",
               hub.getId(), currentFirmwareVersion);
      }
      

      // Send Upgrade request to Hub
      if (curHubReg != null) {
         curHubReg.setFirmwareVersion(currentFirmwareVersion);
         curHubReg.setTargetVersion(targetFirmware);
         curHubReg.setUpgradeRequestTime(new Date(now.toEpochMilli()));
         curHubReg.setState(RegistrationState.DOWNLOADING);   //We are setting it here to ensure the state change even if we don't get any "hubadv:FirmwareUpgradeProcess" event.
         curHubReg.setDownloadProgress(0);
         hubRegistrationDao.save(curHubReg);
      }
      String firmwareURL = urlBuilder.buildURL(hub, targetFirmware);

      logger.debug("Sending firmware update request with url: {} to hub: {}", firmwareURL, hub.getId());
      
      MessageBody upgrade = HubAdvancedCapability.FirmwareUpdateRequest.builder()
    		.withUrl(firmwareURL)
            .withPriority(priority)
            .withType(HubAdvancedCapability.FirmwareUpdateRequest.TYPE_FIRMWARE)
            .withShowLed(showLed)
            .build();
      return upgrade;
	}

	@Nullable
	public MessageBody upgradeIfNeeded(Hub hub, String currentFirmwareVersion) {
	   return upgradeIfNeeded(hub, currentFirmwareVersion, null);
	}

	@Nullable
	public MessageBody upgradeIfNeeded(Hub hub, String currentFirmwareVersion, String population) {
		FirmwareResult targetFirmwareResult = getTargetFirmware(hub.getModel(), currentFirmwareVersion, population);

		HubRegistration curHubReg = findHubRegistration(hub.getId());
		
		if (targetFirmwareResult.getResult() != FirmwareResult.Result.UPGRADE_NEEDED) {
			if (targetFirmwareResult.getResult() == FirmwareResult.Result.UPGRADE_NOT_POSSIBLE) {
				logger.info("Hub [{}] is at firmware version [{}] which can't be upgraded", hub.getId(), currentFirmwareVersion);
				firmwareUpgradeFailed(curHubReg, HubRegistrationErrors.VERSION_CAN_NOT_UPGRADE.getCode(),
						HubRegistrationErrors.VERSION_CAN_NOT_UPGRADE.getMessage(currentFirmwareVersion));
			}
			return null;
		}

		return upgrade(hub, currentFirmwareVersion, targetFirmwareResult.getTarget(), HubAdvancedCapability.FirmwareUpdateRequest.PRIORITY_URGENT, true);
	}

	public boolean updateFirmwareUpgradeProcess(String hubId, String upgradeStatus, Double percentDone) {
		HubRegistration hubReg = findHubRegistration(hubId);
		if (hubReg != null) {
			if (HubRegistration.RegistrationState.DOWNLOADING.name().equalsIgnoreCase(upgradeStatus)) {
				hubReg.setState(HubRegistration.RegistrationState.DOWNLOADING);
				if (percentDone != null) {
					int percentInt = Math.min(100,
							Math.max(0, ((int) Math.round(percentDone.doubleValue() * 10)) * 10));
					hubReg.setDownloadProgress(percentInt);
				}
			}
			hubReg.setLastConnected(new Date());
			hubRegistrationDao.save(hubReg);
			return true;
		} else {
			logger.warn("The hub with id [{}] is already registered but sending firmware update messages to mothership",
					hubId);
			return false;
		}
	}

	public void firmwareUpgradeFailed(String hubId, String code, String msg) {
		HubRegistration hubReg = findHubRegistration(hubId);
		firmwareUpgradeFailed(hubReg, code, msg);
	}

	protected void timeout() {
		logger.debug("HubRegistrationRegistry.timeout is called");
		long currentTimeMs = System.currentTimeMillis();
		for (HubRegistration hubReg : hubsInRegistration.values()) {		   
			long stalenessMs = currentTimeMs - hubReg.getLastConnected().getTime();
			logger.trace("Inspecting hub [{}] - [{}] - remove?-[{}]", hubReg.getId(), hubReg.getState(), stalenessMs > this.upgradeTimeoutMs);
			if (stalenessMs > this.upgradeTimeoutMs) {
				HubRegistration fromDb = hubRegistrationDao.findById(hubReg.getId());
				if (fromDb != null && HubRegistration.RegistrationState.REGISTERED.equals(fromDb.getState())) {
					hubsInRegistration.remove(hubReg.getId()); // hub registered, so succeed
					logger.debug("Remove hub with id [{}] from cache because state is REGISTERED", hubReg.getId());
				} else {
					hubsInRegistration.remove(hubReg.getId()); // hub registration timed out
					logger.warn(
							"Hub with id [{}] has reached upgrade timeout limit, remove from cache.  Current state is [{}]",
							hubReg.getId(), fromDb.getState());
				}
			}
		}
	}

	@Nullable
	protected HubRegistration findHubRegistration(String hubId) {
		return hubsInRegistration.get(hubId);
	}

	private void initHubRegistrationCache() {
		Stream<HubRegistration> allHubRegFromDb = hubRegistrationDao.streamAll();
		if (allHubRegFromDb != null) {
			allHubRegFromDb.forEach((curHubReg) -> {
				if (!RegistrationState.REGISTERED.equals(curHubReg.getState())) {
					hubsInRegistration.putIfAbsent(curHubReg.getId(), curHubReg);
				}
			});
		}
	}

	private HubRegistration loadOrCreateNewHubRegistration(String hubId) {
		HubRegistration curHubReg = hubRegistrationDao.findById(hubId);
		if (curHubReg == null) {
			curHubReg = new HubRegistration();
			curHubReg.setId(hubId);
			curHubReg.setState(RegistrationState.ONLINE);
		}

		return curHubReg;

	}

	private void firmwareUpgradeFailed(HubRegistration hubReg, String code, String msg) {
		firmwareUpgradeFailed(hubReg, code, msg, false);
	}
	
	private void firmwareUpgradeFailed(HubRegistration hubReg, String code, String msg, boolean retry) {
		if (hubReg != null) {
			hubReg.setUpgradeErrorCode(code);
			hubReg.setUpgradeErrorMessage(msg);
			hubReg.setUpgradeErrorTime(new Date());
			hubReg.setLastConnected(new Date());			
			hubRegistrationDao.save(hubReg);
			logger.warn("Hub with id [{}] firmware upgrade failed, reason - [{}] - [{}]. {}", hubReg.getId(), code, msg, retry?"Will retry.":"Will NOT retry.");
			if(!retry) {
				hubsInRegistration.remove(hubReg.getId());
			}
		}
	}

}

