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
package com.iris.ipcd.bridge;

import java.awt.Toolkit;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.ipcd.adapter.aosmith.AosConstants;
import com.iris.protocol.ipcd.message.model.ParameterInfo;

public class FakeAOSmithWaterHeater implements Runnable {   
   public final static String INTERVAL = "10";
   public final static int FAST_POLL = 1;
   
	// public final static String SERIAL_NUMBER = "F40BC0C0";
   public final static String SERIAL_NUMBER = "123456789ABC";
	public final static String PASSWORD = "r7gGgG4HzA";
	// public final static String DEFAULT_URI = "https://devices.irissmarttest.com/aosmith/v2.php";
	public final static String DEFAULT_URI = "http://localhost:443/aosmith/v1";
	// public final static String DEFAULT_URI = "https://devices.irissmarthome.com/aosmith/v2.php";
	
	private final Map<String,String> state = new ConcurrentHashMap<>();
	
	private final BlockingQueue<String> commands;
	private final AtomicBoolean mute = new AtomicBoolean(false);
	private final SSLContext sslContext = FakeSSLContextFactory.getContext();
	
	private String uri = DEFAULT_URI;
	private Gson gson = new Gson();
	private AtomicReference<Timer> timerRef = new AtomicReference<Timer>();
	
	public FakeAOSmithWaterHeater(BlockingQueue<String> commands) {
	   this.commands = commands;
	   
	   Toolkit.getDefaultToolkit().beep();
      System.out.println("####\n#### Poll Server " + DateFormat.getTimeInstance().format(new Date()) + "\n####");
	   
		state.put("DeviceText", SERIAL_NUMBER);
		state.put("Password", PASSWORD);
		state.put("ModuleApi", "1.4");
		state.put("ModFwVer", "1.5");
		state.put("MasterFwVer", "07.03");
		state.put("MasterModelId", "B1.00");
		state.put("DisplayFwVer", "04.03");
		state.put("WifiFwVer", "C2.2.A09");
		state.put("UpdateRate", INTERVAL);
		state.put("Mode", "Standard");
		state.put("AvailableModes", "Standard,Vacation,EnergySmart");
		state.put("SetPoint", "120");
		state.put("Units", "F");
		state.put("LeakDetect", "NotDetected");
		state.put("Grid", "Disabled");
		state.put("MaxSetPoint", "120");
		state.put("HotWaterVol", "Low");
		state.put("SystemInHeating", "True");
		state.put("Leak", "None");
		state.put("DryFire", "None");
		state.put("ElementFail", "None");
		state.put("TankSensorFail", "None");
		state.put("EcoError", "False");
		state.put("MasterDispFail", "None");
		state.put("SignalStrength", "-41");
	}
	
	public void run() {
	   try {
	      timerRef.set(new Timer());
	      timerRef.get().schedule(new PollServerTask(), FAST_POLL * 1000);
	      while(true) {
	         String cmd = commands.take();
	         if (cmd == null) {
	            break;
	         }
	         
	         String[] parts = cmd.split(" ");
	         if ("bye".equalsIgnoreCase(parts[0])) {
	            System.out.println("Exiting...");
	            System.exit(0);
	         } else if ("poll".equalsIgnoreCase(parts[0])) {
	            pollServer();
	         } else if ("report".equalsIgnoreCase(parts[0])) {
	            showState(true);
	         } else if ("status".equalsIgnoreCase(parts[0])) {
	            showState(false);
	         } else if ("set".equalsIgnoreCase(parts[0])) {
	            setParameterValues(parts);
	         } else if ("mute".equalsIgnoreCase(parts[0])) {
	            this.mute.set(true);
	         } else if ("unmute".equalsIgnoreCase(parts[0])) {
	            this.mute.set(false);
	         } else if ("help".equalsIgnoreCase(parts[0])) {
	            showHelp();
	         }
	         else {
	            System.out.println("!!! Invalid Command: " + cmd);
	         }
	         
	      }
	   }
	   catch (Exception ex) {
	      System.out.println("Boom!!");
	      ex.printStackTrace();
	      System.exit(0);
	   }
	}
	
	private void setParameterValues(String[] parts) {
	   if (parts.length < 3) {
	      System.out.println("Invalid parameter setting. Must be in form 'set Param Value'");
	      return;
	   }
	   String param = parts[1];
	   if (state.get(param) == null) {
	      System.out.println("Invalid parameter specified: " + param);
	      showValidParams();
	      return;
	   }
	   String ipcdName = AosConstants.aosToIpcd.get(param);
	   if (ipcdName == null) {
	      System.out.println("Internal error: " + param + " should be a valid parameter");
	      return;
	   }
	   
	   //if (!IpcdProtocol.isWriteable(AosConstants.parameterInfoMap.get(ipcdName))) {
	   //   System.out.println(param + " is not a writeable parameter");
	   //   showValidParams();
	   //   return;
	   //}
	   state.put(param, parts[2]);
	}
	
	private void showValidParams() {
	   System.out.println("   Valid Parameters Are:");
	   for (String param : state.keySet()) {
	      String ipcdName = AosConstants.aosToIpcd.get(param);
	      ParameterInfo paramInfo = AosConstants.parameterInfoMap.get(ipcdName);
	      if (IpcdProtocol.isWriteable(paramInfo)) {
	         System.out.println("      " + param);
	      }
	   }
	}
	
	private void showState(boolean all) {
	   if (all) {
	      System.out.println("Current State of All Params: ");
	   } else {
	      System.out.println("Current State of Writeable Params: ");
	   }
	   for (String param : state.keySet()) {
	      ParameterInfo parameterInfo = AosConstants.parameterInfoMap.get(AosConstants.aosToIpcd.get(param));
	      if (parameterInfo == null) {
	         System.out.println("!!! No ParameterInfo for " + param);
	      }
	      if (all || IpcdProtocol.isWriteable(parameterInfo)) {
	         System.out.println(String.format("   %3s %s -> %s", parameterInfo.getAttrib(), param, state.get(param)));
	      }
	   }
	}
	
	private void showHelp() {
	   System.out.println("Commands:");
      System.out.println("   bye - exit simulator");
      System.out.println("   report - show current state of heater");
      System.out.println("   status - show state of writeable params");
      System.out.println("   set - set a parameter value [ set SetPoint 110 ]");
      System.out.println("   mute - stop polling server");
      System.out.println("   unmute - start polling server");
      System.out.println("   help - show this");
      System.out.println(" ");
   }
	/*
	 * DeviceText=F40BC0C0&Password=r7gGgG4HzA&ModuleApi=1.4&ModFwVer=1.5&MasterFwVer=07.03&MasterModelId=B1.00&DisplayFwVer=04.03&WifiFwVer=C2.2.AO9&UpdateRate=10&Mode=Standard&SetPoint=120&Units=F&LeakDetect=NotDetected&MaxSetPoint=120&Grid=Disabled&AvailableModes=Standard,Vacation,EnergySmart,&HotWaterVol=Low&Leak=None&DryFire=None&ElementFail=None&TankSensorFail=None&EcoError=False&MasterDispFail=None &SignalStrength=-41
	 */
	
	private String buildPayload() {
		StringBuffer sb = new StringBuffer();
		for (Map.Entry<String,String> entry : state.entrySet()) {
			if (sb.length() > 0) {
				sb.append('&');
			}
			sb.append(entry.getKey()).append('=').append(entry.getValue());
		}
		return sb.toString();
	}
	
	private void pollServer() {	   
	   Timer oldTimer = timerRef.getAndSet(null);
	   if (oldTimer != null) {
	      oldTimer.cancel();
	   }
	   HttpClientBuilder builder = HttpClientBuilder.create();
	   builder.setSslcontext(sslContext);
		CloseableHttpClient httpClient = builder.build();
		
		boolean fastPoll = false;
		try {
			StringEntity entity = new StringEntity(buildPayload(), ContentType.APPLICATION_FORM_URLENCODED);
			HttpUriRequest pollingMsg = RequestBuilder.post()
					.setUri(uri)
					.setEntity(entity)
					.build();
			CloseableHttpResponse response = httpClient.execute(pollingMsg);
			try {
				HttpEntity stuff = response.getEntity();
				String thing = EntityUtils.toString(stuff);
				System.out.println("####\n#### RESPONSE " + response.getStatusLine());
				System.out.println("####\n    Response Message: " + thing + "\n####");
				
				fastPoll = applyResults(thing);
			}
			finally {
				response.close();
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   timerRef.set(new Timer());
			if (fastPoll) {
			   System.out.println("####\n#### Schedule Fast Poll: " + FAST_POLL + "s" + "\n####");
			   timerRef.get().schedule(new PollServerTask(), FAST_POLL * 1000);
			}
			else {
			   int pollingInterval = Integer.valueOf(state.get(AosConstants.AOS_PARAM_UPDATERATE));
			   System.out.println("####\n#### Schedule Standard Poll: " + pollingInterval + "s" + "\n####");
			   timerRef.get().schedule(new PollServerTask(), pollingInterval * 1000);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
   private boolean applyResults(String json) {
	   boolean didSomething = false;
	   Map<String,Object> map = new HashMap<String,Object>();
	   map = (Map<String,Object>)gson.fromJson(json, map.getClass());
	   
	   if (map.containsKey("error")) {
	      System.out.println("!!! ERROR " + map.get("error"));
	      return didSomething;
	   }
	   
	   for (String param : map.keySet()) {
	      if (param.equalsIgnoreCase("Success")) {
            System.out.println("Message Reply was Successful!");
            continue;
         }
	      ParameterInfo paramInfo = AosConstants.parameterInfoMap.get(AosConstants.aosToIpcd.get(param));
	      if (paramInfo == null) {
	         System.out.println("!!! Invalid Parameter " + param + " - value:" + map.get(param));
	         continue;
	      }
	      System.out.println("####\n#### " + param + " has attrib:" + paramInfo.getAttrib() + "\n####");
	      if (!IpcdProtocol.isWriteable(paramInfo)) {
	         System.out.println("!!! Tried to write to read-only parameter " + param + " - value:" + map.get(param));
	         continue;
	      }
	      String value = String.valueOf(map.get(param));
	      if (param.equals(AosConstants.AOS_PARAM_SETPOINT)) {
	         if (value.contains(".")) {
	            System.out.println("!!! SetPoint must be set to a whole number not " + value);
	            continue;
	         }
	      }
         didSomething = true;
         state.put(param, value);
         System.out.println("Setting param: " + param + "  - value:" + value);
	   }
	   return didSomething;
	}
	
	private class PollServerTask extends TimerTask {

      @Override
      public void run() {
         pollServer();
      }
	   
	}
}

