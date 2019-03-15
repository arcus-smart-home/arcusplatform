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
package com.iris.common.subsystem.security;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubSoundsCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SecurityAlarmModeModel;

/**
 * Refer to https://eyeris.atlassian.net/wiki/display/I2D/Keypad+issue+summary 
 * section "REQUIREMENTS for Silent Alarm"
 * @author daniellep
 *
 */
public class TestSecuritySubsystem_SilentAlarm extends
		SecuritySubsystemTestCase {
	
	protected Model hub;
	@Before
	public void init() {
		hub = addModel(SecurityFixtures.createHubAttributes());		
		
	}
	   

	@Test
	public void testArmingSilentFalseSoundTrue() {
		testArming(false, true);
	}
	
	@Test
	public void testArmingSilentTrueSoundTrue() {
		testArming(true, true);
	    
	}
	
	@Test
	public void testArmingSilentFalseSoundFalse() {
	    testArming(false, false);	    
	}
	
	@Test
	public void testArmingSilentTrueSoundFalse() {
	    testArming(true, false);	    
	}
	
	
	
	@Test
	public void testSoakingAlertingSilentFalseSoundTrue() {
		testSoakingAndAlert(false, true, true, true);
	}
	
	@Test
	public void testSoakingAlertingSilentTrueSoundTrue() {
		testSoakingAndAlert(true, true, false, false);
	}	
	
	@Test
	public void testSoakingAlertingSilentFalseSoundFalse() {
		testSoakingAndAlert(false, false, false, true);
	}
	
	@Test
	public void testSoakingAlertingSilentTrueSoundFalse() {
		testSoakingAndAlert(true, false, false, false);
	}
	
	protected void testSoakingAndAlert(boolean silentAlarm, boolean soundOn, boolean expectedKeypadAlarmSounderForSoaking, boolean expectedKeypadAlarmSounderForAlert) {
      Set<String> expectedSounds = getExpectedSounds(silentAlarm, soundOn);
      
		model.setAlarmState(SecuritySubsystemCapability.ALARMSTATE_DISARMED);
		model.setAlarmMode(SecuritySubsystemCapability.ALARMMODE_OFF);
		SecurityAlarmModeModel.setSilent(SecuritySubsystemCapability.ALARMMODE_ON, model, silentAlarm);
		SecurityAlarmModeModel.setSoundsEnabled(SecuritySubsystemCapability.ALARMMODE_ON, model, soundOn);
		startSubsystem();
		armOn();
		timeout(); // advance through arming
		faultDevice(glassbreakSensor);
		assertEquals(SecuritySubsystemCapability.ALARMSTATE_SOAKING, model.getAlarmState());
		assertContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String,Object>of(KeyPadCapability.ATTR_ENABLEDSOUNDS, expectedSounds));
		if(expectedKeypadAlarmSounderForSoaking) {
			//check hub
			containsSendMessageWithAttrs(HubSoundsCapability.PlayToneRequest.NAME, ImmutableMap.<String, Object>of(HubSoundsCapability.PlayToneRequest.ATTR_TONE,HubSoundsCapability.PlayToneRequest.TONE_ARMING));
		}else{
			containsSendMessageWithAttrs(HubSoundsCapability.PlayToneRequest.NAME, ImmutableMap.<String, Object>of(HubSoundsCapability.PlayToneRequest.ATTR_TONE,NULL_VALUE));
			
		}
		requests.reset();
		sends.reset();
		timeout(); // advance through soaking
		assertEquals(SecuritySubsystemCapability.ALARMSTATE_ALERT, model.getAlarmState());
		if(expectedKeypadAlarmSounderForAlert) {
			//check hub
			containsSendMessageWithAttrs(HubSoundsCapability.PlayToneRequest.NAME, ImmutableMap.<String, Object>of(HubSoundsCapability.PlayToneRequest.ATTR_TONE,HubSoundsCapability.PlayToneRequest.TONE_INTRUDER));
		}else{
			containsSendMessageWithAttrs(HubSoundsCapability.PlayToneRequest.NAME, ImmutableMap.<String, Object>of(HubSoundsCapability.PlayToneRequest.ATTR_TONE,NULL_VALUE));
			
		}
	}
	
	protected void testArming(boolean silentAlarm, boolean soundOn) {
      Set<String> expectedSounds = getExpectedSounds(silentAlarm, soundOn);
      
		model.setAlarmState(SecuritySubsystemCapability.ALARMSTATE_DISARMED);
		model.setAlarmMode(SecuritySubsystemCapability.ALARMMODE_OFF);
		SecurityAlarmModeModel.setSilent(SecuritySubsystemCapability.ALARMMODE_ON, model, silentAlarm);
		SecurityAlarmModeModel.setSoundsEnabled(SecuritySubsystemCapability.ALARMMODE_ON, model, soundOn);		
		
		startSubsystem();	  
		armOn();
		assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMING,model.getAlarmState());
		assertContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String,Object>of(KeyPadCapability.ATTR_ENABLEDSOUNDS, expectedSounds) );
		if(soundOn) {
			//check hub
			containsSendMessageWithAttrs(HubSoundsCapability.PlayToneRequest.NAME, ImmutableMap.<String, Object>of(HubSoundsCapability.PlayToneRequest.ATTR_TONE,HubSoundsCapability.PlayToneRequest.TONE_ARMING));
		}else{
			containsSendMessageWithAttrs(HubSoundsCapability.PlayToneRequest.NAME, ImmutableMap.<String, Object>of(HubSoundsCapability.PlayToneRequest.ATTR_TONE,NULL_VALUE));
			
		}
		requests.reset();
		timeout();	      
		assertEquals(SecuritySubsystemCapability.ALARMSTATE_ARMED,model.getAlarmState());
		containsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String,Object>of(KeyPadCapability.ATTR_ALARMSOUNDER, NULL_VALUE) );
		requests.reset();
		
	}
	
	private Set<String> getExpectedSounds(boolean silentAlarm, boolean soundOn) {
      if(silentAlarm && !soundOn) {
         return KeypadState.SOUNDS_OFF;
      }
      else if(silentAlarm && soundOn) {
         return KeypadState.SOUNDS_KEYPAD_ONLY;
      }
      else if(!silentAlarm && !soundOn) {
         return KeypadState.SOUNDS_ALARM_ONLY;
      }
      else {
         return KeypadState.SOUNDS_ON;
      }
	}
}

