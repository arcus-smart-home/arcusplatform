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
import java.util.concurrent.Future;

import com.google.common.util.concurrent.ListenableFuture;
import com.iris.client.event.handler.ClientEventHandler;
import com.iris.messages.MessageBody;

public interface ClientPlatformService extends ClientBaseService {
	/**
     * 
     * This requests a connection to the platform using {@code username} and {@code password}
     * which is used in {@link #httpLogin(String, String, String)} if an authToken (cookie)
     * is not present.
     * 
     * Before any connection is attempted, a check to see if we are currently connected using 
     * the same {@code uri} is made.  Since the username and password are not stored in this class,
     * if the user is logging out and then back in as another user, a call to {@link #disconnect()}
     * should be made first to avoid errors on connecting.
     * 
     * @param uri Url of the host - should include ws:// or http://
     * @param username username to login with
     * @param password password for username
     * @return Future that can be used to check when the client is connected, or has errored out.
     */
	public Future<Boolean> connect(String uri, String username, String password);
	
	/**
     * 
     * Disconnects from the platform if a connection has currently been established.  If a
     * connection is currently in the process of connecting, this waits for that to finish
     * for up to 3 seconds and then continues to shutdown if that process has completed.
     * 
     * This also removes, and attempts to cancel, any pending requests that have been made
     * to the platform.
     * 
     * Note: despite setting runtimeShared(false) on the wasync options builder, this seems
     * to (randomly?) get set back to true.  This will also check for an async http client
     * created by wasync and close that connection if it is open.
     * 
     */
	public void disconnect();
	
	/**
     * 
     * Check if the client has finished any attempts to connect to the platform
     * and is currently connected.
     * 
     * @return true if the client is connected to the platform
     */
	public boolean connected();
	
	/**
     * 
     * Check if the client is disconnected from the platform.
     * 
     * @return true if the client is not connected to the platform.
     */
	public boolean disconnected();
	
	/**
     * 
     * Checks if an error has occurred during connection.
     * 
     * @return true if there was an error connecting to the platform.
     */
    public boolean connectionError();
	
	/**
     * 
     * Register a new {@code handler} for {@code eventType} replacing any existing
     * entries for {@code eventType}.  If you use {@link #request(String, MessageBody)} this
     * handler will not be called, pending there is a matching correlation ID in the pool.
     * If there is not a matching correlation ID, the matching handler will be called.
     * 
     * @param eventType Event type
     * @param handler Handler for this event
     */
	public <H extends ClientEventHandler<?>> void registerEventHandler(Class<? extends MessageBody> eventType, H handler);
	
    /**
     * Send a fire-and-forget request to the platform.  Ad-Hoc handlers will capture
     * these events if they end up returning a response.  For example: if you get an error
     * event from the platform and you have an ErrorEvent Handler registered.
     * 
     * @param destination
     * @param message
     * 
     * @throws IOException
     */
    public void send(String destination, MessageBody message) throws IOException;
    
    /**
     * Send a request to the platform where you desire a response.  This does
     * not guarantee a response, but if the request you are sending should return
     * one, you can use this method to wait for a response on the provided future.
     * 
     * Ad-Hoc handlers will not capture these events.
     * 
     * @param destination
     * @param message
     * 
     * @return Future representing the MessageBody of the 
     * 
     * @throws IOException
     */
    public ListenableFuture<MessageBody> request(String destination, MessageBody message) throws IOException;
}

