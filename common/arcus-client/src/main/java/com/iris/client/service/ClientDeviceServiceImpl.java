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
package com.iris.client.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.iris.client.model.device.ClientDeviceModel;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;

// TODO:  jettison due to code generation?  if not need to fix
public class ClientDeviceServiceImpl implements ClientBaseService {
    private static final Logger logger = LoggerFactory.getLogger(ClientDeviceServiceImpl.class);
    private final ClientPlatformService clientPlatformService;
    private final ClientCachingService clientCachingService;

    private int retryNumber = 0;
    private Timer hubRegisterTaskTimer;
    private SettableFuture<Boolean> completedSuccessfully;

    public ClientDeviceServiceImpl(ClientPlatformService platformService, ClientCachingService cacheService) {
        this.clientPlatformService = platformService;
        this.clientCachingService = cacheService;
    }

    /**
     *
     * Loads devices from the platform if devices from the cache are not present. If
     * {@code usingAdHocHandlers} is true, then an immediate future will be returned with
     * any devices from the cache that were found and any registered handlers for
     * ListDevicesResponse will be used to handle the message.
     *
     * If false, then you should .get() the future and handle yourself since the message
     * will not be passed to any handlers.
     *
     * If useCacheAndRefreshDeviceList is true and devices were found in the cache, a
     * request will be sent to the platform to retreive the latest list of devices as well.
     *
     * @param usingAdHocHandlers
     * @param useCacheAndRefreshDeviceList
     * @return
     * @throws IOException
     */
    public final ListenableFuture<List<ClientDeviceModel>> loadDevices(boolean usingAdHocHandlers, boolean useCacheAndRefreshDeviceList) throws IOException {
       return Futures.immediateFuture(Collections.<ClientDeviceModel>emptyList());
//		Collection<ClientDeviceModel> devices = clientCachingService.loadDeviceCache();
//		List<ClientDeviceModel> devicesToReturn = new LinkedList<ClientDeviceModel>(devices);
//
//		if (devices.size() > 0) {
//			if (useCacheAndRefreshDeviceList) {
//				clientPlatformService.send(DEVICES_DESTINATION, new ListDevicesRequest(ACCOUNT_ID));
//			}
//			return Futures.immediateFuture(devicesToReturn);
//		}
//
//		if (usingAdHocHandlers) {
//			clientPlatformService.send(DEVICES_DESTINATION, new ListDevicesRequest(ACCOUNT_ID));
//			return Futures.immediateFuture(devicesToReturn);
//		} else {
//			return(Futures.transform(clientPlatformService.request(DEVICES_DESTINATION, new ListDevicesRequest(ACCOUNT_ID)),
//	    			new Function<MessageBody, List<ClientDeviceModel>>() {
//	    				@Override
//	    				public List<ClientDeviceModel> apply(MessageBody input) {
//	    					if (!(input instanceof ListDevicesResponse)) {
//	    						logger.warn("Unexpected response type {}, when expecting {}.  Returning no devices!", input.getClass(), ListDevicesResponse.class);
//	    						return(Collections.emptyList());
//	    					}
//
//	    					List<ClientDeviceModel> devices = new LinkedList<ClientDeviceModel>();
//	    					for (Device d : ((ListDevicesResponse) input).getDevices()) {
//	    						ClientDeviceModel clientDevice = ClientDeviceModel.fromPlatformDevice(d);
//	    			        	devices.add(clientDevice);
//	    			        }
//
//	    					if (clientCachingService != null) {
//	    						for (ClientDeviceModel clientDevice : devices) {
//	    							clientCachingService.saveToCache(clientDevice);
//	    						}
//    			        	} else {
//	    						logger.warn("Cannot cache devices.  Caching service is null. Please register caching service by calling setCachingService()");
//	    					}
//
//	    					return(devices);
//	    				}
//	    		})
//	    	);
//		}
    }

    /**
     *
     * Shortcut for calling {@code #loadDevices(boolean, boolean)} as true, false.
     * IE: Don't refresh the device list if we found any devices in the cache.
     *
     * @param usingAdHocHandlers
     * @return
     * @throws IOException
     */
    public final ListenableFuture<List<ClientDeviceModel>> loadDevices(boolean usingAdHocHandlers) throws IOException {
    	return loadDevices(usingAdHocHandlers, false);
    }

    /**
     *
     * Signal the platform we want to go into pairing mode.  Returns a future so that the
     * client can use to cancel the request if the user is done adding devices before the timeout.
     *
     * @param hubID
     * @throws IOException
     * @throws RuntimeException if an {@link ErrorEvent} is received during the pairing request.
     */
    public final ListenableFuture<Boolean> startPairing(String hubID, long timeoutInMs) throws IOException {
        Preconditions.checkNotNull(hubID, "HubID Cannot be null");
        return Futures.immediateFuture(Boolean.FALSE);
//
//        String destination = PAIRING_DESTINATION.replace("_HUBID_", hubID);
//
//        return(
//            Futures.transform(
//            		clientPlatformService.request(destination, new PairingRequest(PairingActionType.START_PAIRING, timeoutInMs)),
//
//                    new Function<MessageBody, Boolean>() {
//                        @Override
//                        public Boolean apply(MessageBody input) {
//
//                            if (input instanceof ErrorEvent) {
//                            	logger.trace("Pairing Request Response. Received Error {}.", ((ErrorEvent)input).getMessage());
//                                throw new RuntimeException(((ErrorEvent) input).getMessage());
//                            }
//
//                            return(true);
//                        }
//                    }
//            )
//        );
    }

    public final void stopPairing(String hubID) {
        Preconditions.checkNotNull(hubID, "HubID Cannot be null");

//        String destination = PAIRING_DESTINATION.replace("_HUBID_", hubID);
//
//        try {
//        	clientPlatformService.send(destination, new PairingRequest(PairingActionType.STOP_PAIRING, 0));
//        } catch (IOException ex) {
//        	logger.trace("Received exception while sending stopPairing Request: " + ex.getLocalizedMessage());
//        }
    }

    /**
     *
     * Get a devices attributes. If usingAdHocHandlers is true, then any registered handlers
     * for GetAttributesResponse messages will be handled by the registered handlers and a
     * copy of the device will be returned immediately.
     *
     * @param device
     * @param names
     * @param usingAdHocHandlers
     *
     * @return
     *
     * @throws IllegalStateException if unexpected results for GetAttributes are encountered.
     * @throws IOException if the platform service is not connected.
     */
    public ListenableFuture<ClientDeviceModel> getAttributes(final ClientDeviceModel device, Set<String> names, boolean usingAdHocHandlers) throws IOException {
       return Futures.immediateFuture(null);
//        Map<String,Object> attributes = new HashMap<>();
//        names = names == null ? Collections.<String>emptySet() : names;
//        attributes.put(ClientDeviceModel.DeviceBase.ARG_GET_ATTRIBUTES_NAMES, names);
//
//        if (usingAdHocHandlers) {
//        	send(device, ClientDeviceModel.DeviceBase.NAMESPACE, ClientDeviceModel.DeviceBase.NAME_GET_ATTRIBUTES, attributes);
//        	return(Futures.immediateFuture(device));
//        } else {
//        	return Futures.transform(request(device, ClientDeviceModel.DeviceBase.NAMESPACE, ClientDeviceModel.DeviceBase.NAME_GET_ATTRIBUTES, attributes),
//                new Function<MessageBody, ClientDeviceModel>() {
//                    @SuppressWarnings("unchecked")
//                    @Override
//                    public ClientDeviceModel apply(MessageBody input) {
//                        if(input instanceof DeviceEvent) {
//                            DeviceEvent event = (DeviceEvent) input;
//                            if(event.getEvent().equals(ClientDeviceModel.DeviceBase.EVENT_GET_ATTRIBUTES_RESPONSE)) {
//                                GetAttributesEvent result = GetAttributesEvent.fromResultMap((Map<String,Object>) event.getAttributes().get(ClientDeviceModel.DeviceBase.RET_GET_ATTRIBUTES));
//                                device.putAttributes(result.getAttributes());
//
//                                return device;
//                            }
//                            throw new IllegalStateException("Unexpected result for GetAttributes: " + event.getEvent());
//                        }
//                        throw new IllegalStateException("Unexpected result for GetAttributes: " + input.getMessageType());
//                    }
//                });
//        }
    }

    /**
     *
     * Send a hub registration request.  This method sets up a Timer task that is scheduled at
     * {@code pollingIntervalInMS} and runs for {@code maxRetries} attempts before giving up and
     * returning a future set to false.
     *
     * If any errors, except a "Hub not found" error, will result in setting an exception on the
     * future.  Since the "Hub not found" error could be caused by the customer not plugging the hub
     * in yet, or is waiting for the hub to finish it's synchronization with the Hub Bridge, this
     * will continue to retry.
     *
     * @param hubID Clients HubID
     * @param maxRetries Number of attempts before giving up
     * @param timeoutPerRequestInMillis Time to wait for each individual request to come in
     * @param pollingIntervalInMS Time in between requests
     * @param useAdHocHandler If you want to use a registered handler for the HubAddedEvent or not.
     *
     * @return
     */
    public ListenableFuture<Boolean> registerHub(String hubID, int maxRetries, long timeoutPerRequestInMillis, long pollingIntervalInMS, boolean useAdHocHandler) {
    	hubRegisterTaskTimer = new Timer();
    	completedSuccessfully = SettableFuture.create();
    	retryNumber = 0;

    	if (maxRetries < 1) {
    		logger.trace("Max retries was set to: {}, require at least 1.", maxRetries);
    		return(Futures.immediateFuture(false));
    	}

    	if (pollingIntervalInMS < 500) {
    		logger.trace("Setting polling interval to at least 500 ms.  Was requested at: {}", pollingIntervalInMS);
    		pollingIntervalInMS = 500;
    	}

		hubRegisterTaskTimer.schedule(new RegisterHubRequestTask(hubID, timeoutPerRequestInMillis, maxRetries, useAdHocHandler), 0, pollingIntervalInMS);
		return(completedSuccessfully);
    }

    private final void send(ClientDeviceModel device, String namespace, String command, Map<String,Object> attributes) throws IOException {
//        DeviceCommand payload = new DeviceCommand(Utils.namespace(namespace, command), attributes);
//        clientPlatformService.send(device.getDriverAddress(), payload);
    }

    private final ListenableFuture<? extends MessageBody> request(ClientDeviceModel device, String namespace, String command, Map<String,Object> attributes) throws IOException {
       return null;
//        DeviceCommand payload = new DeviceCommand(Utils.namespace(namespace, command), attributes);
//        return clientPlatformService.request(device.getDriverAddress(), payload);
    }



    private class RegisterHubRequestTask extends TimerTask {
    	private boolean useAdHocHandler;
    	private int maxRetries;
    	private long eachRequestTimeout;
    	private String hubID;

    	public RegisterHubRequestTask(String hubID, long timeoutPerAttemptInMS, int maxRetries, boolean useAdHocHandler) {
    		this.useAdHocHandler = useAdHocHandler;
    		this.eachRequestTimeout = timeoutPerAttemptInMS;
    		this.hubID = hubID;
    		this.maxRetries = maxRetries;
    	}

    	@Override
    	public void run() {
    	  hubRegisterTaskTimer.cancel();
        completedSuccessfully.set(false);
//    		retryNumber++;
//    		try {
//    			Map<String,Object> attributes = new HashMap<>();
//    			attributes.put(ClientDeviceModel.Base.ATTR_ID, this.hubID);
//    			attributes.put(ClientDeviceModel.HubBase.ATTR_ACCOUNT, ACCOUNT_ID.toString());
//    			attributes.put(ClientDeviceModel.HubBase.ATTR_PLACE, PLACE_ID.toString());
//
//    			if (useAdHocHandler) {
//    				clientPlatformService.send(ADD_HUB_DESTINATION, new RegisterHubRequest(attributes));
//    			} else {
//					MessageBody body = clientPlatformService.request(ADD_HUB_DESTINATION, new RegisterHubRequest(attributes))
//							.get(eachRequestTimeout, TimeUnit.MILLISECONDS);
//
//					if (body.getClass().equals(HubAddedEvent.class)) {
//						if (clientCachingService != null) {
//							clientCachingService.saveToCache(ClientDeviceModel.fromHubAddedEvent((HubAddedEvent) body));
//						}
//
//						hubRegisterTaskTimer.cancel();
//						completedSuccessfully.set(true);
//					} else if (body.getClass().equals(ErrorEvent.class)) {
//						ErrorEvent errorMessage = (ErrorEvent) body;
//
//						// This is expected if the client hasn't plugged in the hub yet which they could be in the process of doing
//						// so we'll continue to try
//						if (!errorMessage.getCode().equals(ClientErrorCodes.HubRegistration.NOT_FOUND) ) {
//
//							// Other error events aren't recoverable.
//							hubRegisterTaskTimer.cancel();
//							completedSuccessfully.setException(new Error(errorMessage.getCode()));
//						}
//					}
//    			}
//			} catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
//				hubRegisterTaskTimer.cancel();
//				completedSuccessfully.setException(e);
//			}
//
//    		if (retryNumber == maxRetries) {
//    			hubRegisterTaskTimer.cancel();
//    			completedSuccessfully.set(false);
//    		}
    	}
    }
}

