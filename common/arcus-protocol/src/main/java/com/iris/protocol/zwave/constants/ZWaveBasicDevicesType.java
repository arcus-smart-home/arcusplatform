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
package com.iris.protocol.zwave.constants;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class ZWaveBasicDevicesType {

   /************ Basic Device Class identifiers **************/
   public final static byte                   CONTROLLER                       = (byte) 0x01;                    /*
                                                                                                                  * Node
                                                                                                                  * is
                                                                                                                  * a
                                                                                                                  * portable
                                                                                                                  * controller
                                                                                                                  */
   public final static byte                   ROUTING_SLAVE                    = (byte) 0x04;                    /*
                                                                                                                  * Node
                                                                                                                  * is
                                                                                                                  * a
                                                                                                                  * slave
                                                                                                                  * with
                                                                                                                  * routing
                                                                                                                  * capabilities
                                                                                                                  */
   public final static byte                   SLAVE                            = (byte) 0x03;                    /*
                                                                                                                  * Node
                                                                                                                  * is
                                                                                                                  * a
                                                                                                                  * slave
                                                                                                                  */
   public final static byte                   BASIC_STATIC_CONTROLLER          = (byte) 0x02;                    /*
                                                                                                                  * Node
                                                                                                                  * is
                                                                                                                  * a
                                                                                                                  * static
                                                                                                                  * controller
                                                                                                                  */

   /***** Generic and Specific Device Class identifiers ******/
   /* Device class Av Control Point */
   public final static byte                   AV_CONTROL_POINT                 = (byte) 0x03;                    /*
                                                                                                                  * AV
                                                                                                                  * Control
                                                                                                                  * Point
                                                                                                                  */
   public final static byte                   NOT_USED                         = (byte) 0x00;                    /*
                                                                                                                  * Specific
                                                                                                                  * Device
                                                                                                                  * Class
                                                                                                                  * Not
                                                                                                                  * Used
                                                                                                                  */
   public final static byte                   DOORBELL                         = (byte) 0x12;
   public final static byte                   SATELLITE_RECEIVER               = (byte) 0x04;                    /*
                                                                                                                  * Satellite
                                                                                                                  * Receiver
                                                                                                                  */
   public final static byte                   SATELLITE_RECEIVER_V2            = (byte) 0x11;                    /*
                                                                                                                  * Satellite
                                                                                                                  * Receiver
                                                                                                                  * V2
                                                                                                                  */

   /* Device class Display */
   public final static byte                   DISPLAY                          = (byte) 0x04;
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   SIMPLE_DISPLAY                   = (byte) 0x01;                    /*
                                                                                                                  * Display
                                                                                                                  * (simple)
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */

   /* Device class Entry Control */
   public final static byte                   ENTRY_CONTROL                    = (byte) 0x40;                    /*
                                                                                                                  * Entry
                                                                                                                  * Control
                                                                                                                  */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   DOOR_LOCK                        = (byte) 0x01;                    /*
                                                                                                                  * Door
                                                                                                                  * Lock
                                                                                                                  */
   public final static byte                   ADVANCED_DOOR_LOCK               = (byte) 0x02;                    /*
                                                                                                                  * Advanced
                                                                                                                  * Door
                                                                                                                  * Lock
                                                                                                                  */
   public final static byte                   SECURE_KEYPAD_DOOR_LOCK          = (byte) 0x03;                    /*
                                                                                                                  * Door
                                                                                                                  * Lock
                                                                                                                  * keypad
                                                                                                                  * lever
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   SECURE_KEYPAD_DOOR_LOCK_DEADBOLT = (byte) 0x04;                    /*
                                                                                                                  * Door
                                                                                                                  * Lock
                                                                                                                  * keypad
                                                                                                                  * deadbolt
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   SECURE_DOOR                      = (byte) 0x05;                    /*
                                                                                                                  * Barrier
                                                                                                                  * Operator
                                                                                                                  * Specific
                                                                                                                  * Device
                                                                                                                  * Class
                                                                                                                  */
   public final static byte                   SECURE_GATE                      = (byte) 0x06;                    /*
                                                                                                                  * Barrier
                                                                                                                  * Operator
                                                                                                                  * Specific
                                                                                                                  * Device
                                                                                                                  * Class
                                                                                                                  */
   public final static byte                   SECURE_BARRIER_ADDON             = (byte) 0x07;                    /*
                                                                                                                  * Barrier
                                                                                                                  * Operator
                                                                                                                  * Specific
                                                                                                                  * Device
                                                                                                                  * Class
                                                                                                                  */
   public final static byte                   SECURE_BARRIER_OPEN_ONLY         = (byte) 0x08;                    /*
                                                                                                                  * Barrier
                                                                                                                  * Operator
                                                                                                                  * Specific
                                                                                                                  * Device
                                                                                                                  * Class
                                                                                                                  */
   public final static byte                   SECURE_BARRIER_CLOSE_ONLY        = (byte) 0x09;                    /*
                                                                                                                  * Barrier
                                                                                                                  * Operator
                                                                                                                  * Specific
                                                                                                                  * Device
                                                                                                                  * Class
                                                                                                                  */
   public final static byte                   SECURE_LOCKBOX                   = (byte) 0x0A;                    /* SDS12724 */

   /* Device class Generic Controller */
   public final static byte                   GENERIC_CONTROLLER               = (byte) 0x01;                    /*
                                                                                                                  * Remote
                                                                                                                  * Controller
                                                                                                                  */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   PORTABLE_REMOTE_CONTROLLER       = (byte) 0x01;                    /*
                                                                                                                  * Remote
                                                                                                                  * Control
                                                                                                                  * (Multi
                                                                                                                  * Purpose)
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   PORTABLE_SCENE_CONTROLLER        = (byte) 0x02;                    /*
                                                                                                                  * Portable
                                                                                                                  * Scene
                                                                                                                  * Controller
                                                                                                                  */
   public final static byte                   PORTABLE_INSTALLER_TOOL          = (byte) 0x03;
   public final static byte                   REMOTE_CONTROL_AV                = (byte) 0x04;                    /*
                                                                                                                  * Remote
                                                                                                                  * Control
                                                                                                                  * (AV)
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   REMOTE_CONTROL_SIMPLE            = (byte) 0x06;                    /*
                                                                                                                  * Remote
                                                                                                                  * Control
                                                                                                                  * (Simple)
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */

   /* Device class Meter */
   public final static byte                   METER                            = (byte) 0x31;                    /* Meter */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   SIMPLE_METER                     = (byte) 0x01;                    /*
                                                                                                                  * Sub
                                                                                                                  * Energy
                                                                                                                  * Meter
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   ADV_ENERGY_CONTROL               = (byte) 0x02;                    /*
                                                                                                                  * Whole
                                                                                                                  * Home
                                                                                                                  * Energy
                                                                                                                  * Meter
                                                                                                                  * (Advanced)
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   WHOLE_HOME_METER_SIMPLE          = (byte) 0x03;                    /*
                                                                                                                  * Whole
                                                                                                                  * Home
                                                                                                                  * Meter
                                                                                                                  * (Simple)
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */

   /* Device class Meter Pulse */
   public final static byte                   METER_PULSE                      = (byte) 0x30;                    /*
                                                                                                                  * Pulse
                                                                                                                  * Meter
                                                                                                                  */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */

   /* Device class Non Interoperable */
   public final static byte                   NON_INTEROPERABLE                = (byte) 0xFF;                    /*
                                                                                                                  * Non
                                                                                                                  * interoperable
                                                                                                                  */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */

   /* Device class Repeater Slave */
   public final static byte                   GENERIC_REPEATER_SLAVE           = (byte) 0x0F;                    /*
                                                                                                                  * Repeater
                                                                                                                  * Slave
                                                                                                                  */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   SPECIFIC_REPEATER_SLAVE          = (byte) 0x01;                    /*
                                                                                                                  * Basic
                                                                                                                  * Repeater
                                                                                                                  * Slave
                                                                                                                  */

   /* Device class Security Panel */
   public final static byte                   SECURITY_PANEL                   = (byte) 0x17;
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   ZONED_SECURITY_PANEL             = (byte) 0x01;

   /* Device class Semi Interoperable */
   public final static byte                   SEMI_INTEROPERABLE               = (byte) 0x50;                    /*
                                                                                                                  * Semi
                                                                                                                  * Interoperable
                                                                                                                  */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   ENERGY_PRODUCTION                = (byte) 0x01;                    /*
                                                                                                                  * Energy
                                                                                                                  * Production
                                                                                                                  */

   ; /* Device class Sensor Alarm */
   public final static byte                   SENSOR_ALARM                     = (byte) 0xA1;
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   ADV_ZENSOR_NET_ALARM_SENSOR      = (byte) 0x05;
   public final static byte                   ADV_ZENSOR_NET_SMOKE_SENSOR      = (byte) 0x0A;
   public final static byte                   BASIC_ROUTING_ALARM_SENSOR       = (byte) 0x01;
   public final static byte                   BASIC_ROUTING_SMOKE_SENSOR       = (byte) 0x06;
   public final static byte                   BASIC_ZENSOR_NET_ALARM_SENSOR    = (byte) 0x03;
   public final static byte                   BASIC_ZENSOR_NET_SMOKE_SENSOR    = (byte) 0x08;
   public final static byte                   ROUTING_ALARM_SENSOR             = (byte) 0x02;
   public final static byte                   ROUTING_SMOKE_SENSOR             = (byte) 0x07;
   public final static byte                   ZENSOR_NET_ALARM_SENSOR          = (byte) 0x04;
   public final static byte                   ZENSOR_NET_SMOKE_SENSOR          = (byte) 0x09;
   public final static byte                   ALARM_SENSOR                     = (byte) 0x0B;                    /*
                                                                                                                  * Sensor
                                                                                                                  * (Alarm)
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */

   ; /* Device class Sensor Binary */
   public final static byte                   SENSOR_BINARY                    = (byte) 0x20;                    /*
                                                                                                                  * Binary
                                                                                                                  * Sensor
                                                                                                                  */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   ROUTING_SENSOR_BINARY            = (byte) 0x01;                    /*
                                                                                                                  * Routing
                                                                                                                  * Binary
                                                                                                                  * Sensor
                                                                                                                  */

   ; /* Device class Sensor Multilevel */
   public final static byte                   SENSOR_MULTILEVEL                = (byte) 0x21;                    /*
                                                                                                                  * Multilevel
                                                                                                                  * Sensor
                                                                                                                  */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   ROUTING_SENSOR_MULTILEVEL        = (byte) 0x01;                    /*
                                                                                                                  * Sensor
                                                                                                                  * (Multilevel)
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   CHIMNEY_FAN                      = (byte) 0x02;

   ; /* Device class Static Controller */
   public final static byte                   GENERIC_STATIC_CONTROLLER        = (byte) 0x02;                    /*
                                                                                                                  * Static
                                                                                                                  * Controller
                                                                                                                  */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   PC_CONTROLLER                    = (byte) 0x01;                    /*
                                                                                                                  * Central
                                                                                                                  * Controller
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   SCENE_CONTROLLER                 = (byte) 0x02;                    /*
                                                                                                                  * Scene
                                                                                                                  * Controller
                                                                                                                  */
   public final static byte                   STATIC_INSTALLER_TOOL            = (byte) 0x03;
   public final static byte                   SET_TOP_BOX                      = (byte) 0x04;                    /*
                                                                                                                  * Set
                                                                                                                  * Top
                                                                                                                  * Box
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   SUB_SYSTEM_CONTROLLER            = (byte) 0x05;                    /*
                                                                                                                  * Sub
                                                                                                                  * System
                                                                                                                  * Controller
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   TV                               = (byte) 0x06;                    /*
                                                                                                                  * TV
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   GATEWAY                          = (byte) 0x07;                    /*
                                                                                                                  * Gateway
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */

   ; /* Device class Switch Binary */
   public final static byte                   SWITCH_BINARY                    = (byte) 0x10;                    /*
                                                                                                                  * Binary
                                                                                                                  * Switch
                                                                                                                  */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   POWER_SWITCH_BINARY              = (byte) 0x01;                    /*
                                                                                                                  * On
                                                                                                                  * /
                                                                                                                  * Off
                                                                                                                  * Power
                                                                                                                  * Switch
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   SCENE_SWITCH_BINARY              = (byte) 0x03;                    /*
                                                                                                                  * Binary
                                                                                                                  * Scene
                                                                                                                  * Switch
                                                                                                                  */
   public final static byte                   POWER_STRIP                      = (byte) 0x04;                    /*
                                                                                                                  * Power
                                                                                                                  * Strip
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   SIREN                            = (byte) 0x05;                    /*
                                                                                                                  * Siren
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   VALVE_OPEN_CLOSE                 = (byte) 0x06;                    /*
                                                                                                                  * Valve
                                                                                                                  * (open
                                                                                                                  * /
                                                                                                                  * close)
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   COLOR_TUNABLE_BINARY             = (byte) 0x02;

   ; /* Device class Switch Multilevel */
   public final static byte                   SWITCH_MULTILEVEL                = (byte) 0x11;                    /*
                                                                                                                  * Multilevel
                                                                                                                  * Switch
                                                                                                                  */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   CLASS_A_MOTOR_CONTROL            = (byte) 0x05;                    /*
                                                                                                                  * Window
                                                                                                                  * Covering
                                                                                                                  * No
                                                                                                                  * Position
                                                                                                                  * /
                                                                                                                  * Endpoint
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   CLASS_B_MOTOR_CONTROL            = (byte) 0x06;                    /*
                                                                                                                  * Window
                                                                                                                  * Covering
                                                                                                                  * Endpoint
                                                                                                                  * Aware
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   CLASS_C_MOTOR_CONTROL            = (byte) 0x07;                    /*
                                                                                                                  * Window
                                                                                                                  * Covering
                                                                                                                  * Position
                                                                                                                  * /
                                                                                                                  * Endpoint
                                                                                                                  * Aware
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   MOTOR_MULTIPOSITION              = (byte) 0x03;                    /*
                                                                                                                  * Multiposition
                                                                                                                  * Motor
                                                                                                                  */
   public final static byte                   POWER_SWITCH_MULTILEVEL          = (byte) 0x01;                    /*
                                                                                                                  * Light
                                                                                                                  * Dimmer
                                                                                                                  * Switch
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   SCENE_SWITCH_MULTILEVEL          = (byte) 0x04;                    /*
                                                                                                                  * Multilevel
                                                                                                                  * Scene
                                                                                                                  * Switch
                                                                                                                  */
   public final static byte                   FAN_SWITCH                       = (byte) 0x08;                    /*
                                                                                                                  * Fan
                                                                                                                  * Switch
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   COLOR_TUNABLE_MULTILEVEL         = (byte) 0x02;

   ; /* Device class Switch Remote */
   public final static byte                   SWITCH_REMOTE                    = (byte) 0x12;                    /*
                                                                                                                  * Remote
                                                                                                                  * Switch
                                                                                                                  */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   SWITCH_REMOTE_BINARY             = (byte) 0x01;                    /*
                                                                                                                  * Binary
                                                                                                                  * Remote
                                                                                                                  * Switch
                                                                                                                  */
   public final static byte                   SWITCH_REMOTE_MULTILEVEL         = (byte) 0x02;                    /*
                                                                                                                  * Multilevel
                                                                                                                  * Remote
                                                                                                                  * Switch
                                                                                                                  */
   public final static byte                   SWITCH_REMOTE_TOGGLE_BINARY      = (byte) 0x03;                    /*
                                                                                                                  * Binary
                                                                                                                  * Toggle
                                                                                                                  * Remote
                                                                                                                  * Switch
                                                                                                                  */
   public final static byte                   SWITCH_REMOTE_TOGGLE_MULTILEVEL  = (byte) 0x04;                    /*
                                                                                                                  * Multilevel
                                                                                                                  * Toggle
                                                                                                                  * Remote
                                                                                                                  * Switch
                                                                                                                  */

   ; /* Device class Switch Toggle */
   public final static byte                   SWITCH_TOGGLE                    = (byte) 0x13;                    /*
                                                                                                                  * Toggle
                                                                                                                  * Switch
                                                                                                                  */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   SWITCH_TOGGLE_BINARY             = (byte) 0x01;                    /*
                                                                                                                  * Binary
                                                                                                                  * Toggle
                                                                                                                  * Switch
                                                                                                                  */
   public final static byte                   SWITCH_TOGGLE_MULTILEVEL         = (byte) 0x02;                    /*
                                                                                                                  * Multilevel
                                                                                                                  * Toggle
                                                                                                                  * Switch
                                                                                                                  */

   ; /* Device class Thermostat */
   public final static byte                   THERMOSTAT                       = (byte) 0x08;                    /* Thermostat */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   SETBACK_SCHEDULE_THERMOSTAT      = (byte) 0x03;                    /*
                                                                                                                  * Setback
                                                                                                                  * Schedule
                                                                                                                  * Thermostat
                                                                                                                  */
   public final static byte                   SETBACK_THERMOSTAT               = (byte) 0x05;                    /*
                                                                                                                  * Thermostat
                                                                                                                  * (Setback)
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   SETPOINT_THERMOSTAT              = (byte) 0x04;
   public final static byte                   THERMOSTAT_GENERAL               = (byte) 0x02;                    /*
                                                                                                                  * Thermostat
                                                                                                                  * General
                                                                                                                  */
   public final static byte                   THERMOSTAT_GENERAL_V2            = (byte) 0x06;                    /*
                                                                                                                  * Thermostat
                                                                                                                  * (HVAC)
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */
   public final static byte                   THERMOSTAT_HEATING               = (byte) 0x01;                    /*
                                                                                                                  * Thermostat
                                                                                                                  * Heating
                                                                                                                  */

   ; /* Device class Ventilation */
   public final static byte                   VENTILATION                      = (byte) 0x16;
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   RESIDENTIAL_HRV                  = (byte) 0x01;

   ; /* Device class Window Covering */
   public final static byte                   WINDOW_COVERING                  = (byte) 0x09;                    /*
                                                                                                                  * Window
                                                                                                                  * Covering
                                                                                                                  */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   SIMPLE_WINDOW_COVERING           = (byte) 0x01;                    /*
                                                                                                                  * Simple
                                                                                                                  * Window
                                                                                                                  * Covering
                                                                                                                  * Control
                                                                                                                  */

   ; /* Device class Zip Node */
   public final static byte                   ZIP_NODE                         = (byte) 0x15;
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   ZIP_ADV_NODE                     = (byte) 0x02;
   public final static byte                   ZIP_TUN_NODE                     = (byte) 0x01;

   ; /* Device class Wall Controller */
   public final static byte                   WALL_CONTROLLER                  = (byte) 0x18;
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   BASIC_WALL_CONTROLLER            = (byte) 0x01;                    /*
                                                                                                                  * Wall
                                                                                                                  * Controller
                                                                                                                  * Device
                                                                                                                  * Type
                                                                                                                  */

   ; /* Device class Network Extender */
   public final static byte                   NETWORK_EXTENDER                 = (byte) 0x05;                    /*
                                                                                                                  * Network
                                                                                                                  * Extender
                                                                                                                  * Generic
                                                                                                                  * Device
                                                                                                                  * Class
                                                                                                                  */
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   SECURE_EXTENDER                  = (byte) 0x01;                    /*
                                                                                                                  * Specific
                                                                                                                  * Device
                                                                                                                  * Secure
                                                                                                                  * Extender
                                                                                                                  */

   ; /* Device class Appliance */
   public final static byte                   APPLIANCE                        = (byte) 0x06;
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * Not Used
    */
   public final static byte                   GENERAL_APPLIANCE                = (byte) 0x01;
   public final static byte                   KITCHEN_APPLIANCE                = (byte) 0x02;
   public final static byte                   LAUNDRY_APPLIANCE                = (byte) 0x03;

   ; /* Device class Sensor Notification */
   public final static byte                   SENSOR_NOTIFICATION              = (byte) 0x07;
   /*
    * public final static byte NOT_USED = (byte) 0x00 ; /*Specific Device Class
    * not used
    */
   public final static byte                   NOTIFICATION_SENSOR              = (byte) 0x01;

   public final static BiMap<String, Byte>    genericNames                     = createGenericNamesBiMap();

   // Uses a combined generic and specific byte to look up the specific values.
   public final static Map<Integer, String> specificZWaveNames               = createSpecificZWaveNamesBiMap();
   public final static Map<Integer, String> specificIrisNames                = createSpecificIrisNamesBiMap();

   public static String getNameOf(Byte genericId, Byte specificId) {
      String name;
      int mergedId = merge(genericId, specificId);

      if (specificIrisNames.containsKey(mergedId)) {
         name = specificIrisNames.get(mergedId);
      } else {
         name = "Unknown Device [ " + genericId + ", " + specificId + " ]";
      }
      return name;
   }

   private static BiMap<String, Byte> createGenericNamesBiMap() {
      BiMap<String, Byte> bimap = HashBiMap.create();

      bimap.put("AV Control Point", AV_CONTROL_POINT);
      bimap.put("Display", DISPLAY);
      bimap.put("Entry Control", ENTRY_CONTROL);
      bimap.put("Generic Controller", GENERIC_CONTROLLER);
      bimap.put("Meter", METER);
      bimap.put("Meter Pulse", METER_PULSE);
      bimap.put("Non-Interoperable", NON_INTEROPERABLE);
      bimap.put("Generic Repeater Slave", GENERIC_REPEATER_SLAVE);
      bimap.put("Security Panel", SECURITY_PANEL);
      bimap.put("Semi-Interoperable", SEMI_INTEROPERABLE);
      bimap.put("Sensor Alarm", SENSOR_ALARM);
      bimap.put("Binary Sensor", SENSOR_BINARY);
      bimap.put("Sensor Multilevel", SENSOR_MULTILEVEL);
      bimap.put("Static Controller", GENERIC_STATIC_CONTROLLER);
      bimap.put("Binary Switch", SWITCH_BINARY);
      bimap.put("Multilevel Switch", SWITCH_MULTILEVEL);
      bimap.put("Remote Switch", SWITCH_REMOTE);
      bimap.put("Toggle Switch", SWITCH_TOGGLE);
      bimap.put("Thermostat", THERMOSTAT);
      bimap.put("Ventilation", VENTILATION);
      bimap.put("Window Covering", WINDOW_COVERING);
      bimap.put("ZIP Node", ZIP_NODE);
      bimap.put("Wall Controller", WALL_CONTROLLER);
      bimap.put("Network Extender", NETWORK_EXTENDER);
      bimap.put("Appliance", APPLIANCE);
      bimap.put("Sensor Notification", SENSOR_NOTIFICATION);

      return bimap;
   }

   private static Map<Integer, String> createSpecificZWaveNamesBiMap() {
      Map<Integer, String> bimap = new HashMap<Integer,String>();

      bimap.put( merge(AV_CONTROL_POINT, NOT_USED), "Specific Device Class Not Used" );
      bimap.put( merge(AV_CONTROL_POINT, DOORBELL), "Doorbell" );
      bimap.put(merge(AV_CONTROL_POINT, SATELLITE_RECEIVER),"Satellite Receiver - Version 1" );
      bimap.put(merge(AV_CONTROL_POINT, SATELLITE_RECEIVER_V2),"Satellite Receiver - Version 2");

      bimap.put(merge(DISPLAY, NOT_USED),"Not Used");
      bimap.put( merge(DISPLAY, SIMPLE_DISPLAY), "Simple");

      bimap.put(merge(ENTRY_CONTROL, NOT_USED), "Not Used");
      bimap.put(merge(ENTRY_CONTROL, DOOR_LOCK), "Door Lock");
      bimap.put(merge(ENTRY_CONTROL, ADVANCED_DOOR_LOCK), "Advanced Door Lock");
      bimap.put(merge(ENTRY_CONTROL, SECURE_KEYPAD_DOOR_LOCK), "Door Lock (keypad - lever) Device Type");
      bimap.put(merge(ENTRY_CONTROL, SECURE_KEYPAD_DOOR_LOCK_DEADBOLT), "Door Lock (keypad - deadbolt) Device Type");
      bimap.put(merge(ENTRY_CONTROL, SECURE_DOOR), "Secured Barrier - Door");
      bimap.put(merge(ENTRY_CONTROL, SECURE_GATE), "Secured Barrier - Gate");
      bimap.put(merge(ENTRY_CONTROL, SECURE_BARRIER_ADDON), "Secured Barrier - Addon");
      bimap.put(merge(ENTRY_CONTROL, SECURE_BARRIER_OPEN_ONLY), "Secured Barrier - Open Only");
      bimap.put(merge(ENTRY_CONTROL, SECURE_BARRIER_CLOSE_ONLY), "Secured Barrier - Close Only");
      bimap.put(merge(ENTRY_CONTROL, SECURE_LOCKBOX), "Lock Box");

      bimap.put(merge(GENERIC_CONTROLLER, NOT_USED), "Not Used");
      bimap.put(merge(GENERIC_CONTROLLER, PORTABLE_REMOTE_CONTROLLER), "Remote Control (Multi Purpose) Device Type");
      bimap.put(merge(GENERIC_CONTROLLER, PORTABLE_SCENE_CONTROLLER), "Portable Scene Controller");
      bimap.put(merge(GENERIC_CONTROLLER, PORTABLE_INSTALLER_TOOL), "Portable Installer Tool");
      bimap.put(merge(GENERIC_CONTROLLER, REMOTE_CONTROL_AV), "Remote Control (AV) Device Type");
      bimap.put(merge(GENERIC_CONTROLLER, REMOTE_CONTROL_SIMPLE), "Remote Control (Simple) Device Type");

      bimap.put(merge(METER, NOT_USED), "Not Used");
      bimap.put(merge(METER, SIMPLE_METER), "Simple Meter");
      bimap.put(merge(METER, ADV_ENERGY_CONTROL), "Whole Home Energy Meter (Advanced) Device");
      bimap.put(merge(METER, WHOLE_HOME_METER_SIMPLE), "Whole Home Meter (Simple) Device");

      bimap.put(merge(METER_PULSE, NOT_USED), "Not Used");

      bimap.put(merge(NON_INTEROPERABLE, NOT_USED), "Not Used");

      bimap.put(merge(GENERIC_REPEATER_SLAVE, NOT_USED), "Not Used");
      bimap.put(merge(GENERIC_REPEATER_SLAVE, SPECIFIC_REPEATER_SLAVE), "Basic Repeater Slave");

      bimap.put(merge(SECURITY_PANEL, NOT_USED), "Not Used");
      bimap.put(merge(SECURITY_PANEL, ZONED_SECURITY_PANEL), "Zoned Security Panel");

      bimap.put(merge(SEMI_INTEROPERABLE, NOT_USED), "Not Used");
      bimap.put(merge(SEMI_INTEROPERABLE, ENERGY_PRODUCTION), "Energy Production");

      bimap.put(merge(SENSOR_ALARM, NOT_USED), "Not Used");
      bimap.put(merge(SENSOR_ALARM, ADV_ZENSOR_NET_ALARM_SENSOR), "Advanced Zensor Net Alarm Sensor");
      bimap.put(merge(SENSOR_ALARM, ADV_ZENSOR_NET_SMOKE_SENSOR), "Advanced Zensor Net Smoke Sensor");
      bimap.put(merge(SENSOR_ALARM, BASIC_ROUTING_ALARM_SENSOR), "Basic Routing Alarm Sensor");
      bimap.put(merge(SENSOR_ALARM, BASIC_ROUTING_SMOKE_SENSOR), "Basic Routing Smoke Sensor");
      bimap.put(merge(SENSOR_ALARM, BASIC_ZENSOR_NET_ALARM_SENSOR), "Basic Zensor Net Alarm Sensor");
      bimap.put(merge(SENSOR_ALARM, BASIC_ZENSOR_NET_SMOKE_SENSOR), "Basic Zensor Net Smoke Sensor");
      bimap.put(merge(SENSOR_ALARM, ROUTING_ALARM_SENSOR), "Routing Alarm Sensor");
      bimap.put(merge(SENSOR_ALARM, ROUTING_SMOKE_SENSOR), "Routing Smoke Sensor");
      bimap.put(merge(SENSOR_ALARM, ZENSOR_NET_ALARM_SENSOR), "Zensor Net Alarm Sensor");
      bimap.put(merge(SENSOR_ALARM, ZENSOR_NET_SMOKE_SENSOR), "Zensor Net Smoke Sensor");
      bimap.put(merge(SENSOR_ALARM, ALARM_SENSOR), "Sensor (Alarm) Device Type");

      bimap.put(merge(SENSOR_BINARY, NOT_USED), "Not Used");
      bimap.put(merge(SENSOR_BINARY, ROUTING_SENSOR_BINARY), "Routing Sensor Binary");

      bimap.put(merge(SENSOR_MULTILEVEL, NOT_USED), "Not Used");
      bimap.put(merge(SENSOR_MULTILEVEL, ROUTING_SENSOR_MULTILEVEL), "Sensor (Multilevel) Device");
      bimap.put(merge(SENSOR_MULTILEVEL, CHIMNEY_FAN), "Chimney Fan");

      bimap.put(merge(GENERIC_STATIC_CONTROLLER, NOT_USED), "Not Used");
      bimap.put(merge(GENERIC_STATIC_CONTROLLER, PC_CONTROLLER), "Central Controller Device");
      bimap.put(merge(GENERIC_STATIC_CONTROLLER, SCENE_CONTROLLER), "Scene Controller");
      bimap.put(merge(GENERIC_STATIC_CONTROLLER, STATIC_INSTALLER_TOOL), "Static Installer Tool");
      bimap.put(merge(GENERIC_STATIC_CONTROLLER, SET_TOP_BOX), "Set Top Box Device");
      bimap.put(merge(GENERIC_STATIC_CONTROLLER, SUB_SYSTEM_CONTROLLER), "Sub System Controller Device");
      bimap.put(merge(GENERIC_STATIC_CONTROLLER, TV), "TV Device");
      bimap.put(merge(GENERIC_STATIC_CONTROLLER, GATEWAY), "Gateway Device");

      bimap.put(merge(SWITCH_BINARY, NOT_USED), "Not Used");
      bimap.put(merge(SWITCH_BINARY, POWER_SWITCH_BINARY), "On/Off Power Switch Device");
      bimap.put(merge(SWITCH_BINARY, SCENE_SWITCH_BINARY), "Binary Scene Switch");
      bimap.put(merge(SWITCH_BINARY, POWER_STRIP), "Power Strip Device");
      bimap.put(merge(SWITCH_BINARY, SIREN), "Siren Device");
      bimap.put(merge(SWITCH_BINARY, VALVE_OPEN_CLOSE), "Valve (open/close) Device");
      bimap.put(merge(SWITCH_BINARY, COLOR_TUNABLE_BINARY), "Binary Tunable Color Light");

      bimap.put(merge(SWITCH_MULTILEVEL, NOT_USED), "Not Used");
      bimap.put(merge(SWITCH_MULTILEVEL, CLASS_A_MOTOR_CONTROL), "Window Covering No Position/Endpoint Device");
      bimap.put(merge(SWITCH_MULTILEVEL, CLASS_B_MOTOR_CONTROL), "Window Covering Endpoint Aware Device");
      bimap.put(merge(SWITCH_MULTILEVEL, CLASS_C_MOTOR_CONTROL), "Window Covering Position/Endpoint Aware Device");
      bimap.put(merge(SWITCH_MULTILEVEL, MOTOR_MULTIPOSITION), "Multiposition Motor");
      bimap.put(merge(SWITCH_MULTILEVEL, POWER_SWITCH_MULTILEVEL), "Light Dimmer Switch Device Type");
      bimap.put(merge(SWITCH_MULTILEVEL, SCENE_SWITCH_MULTILEVEL), "Multilevel Scene Switch");
      bimap.put(merge(SWITCH_MULTILEVEL, FAN_SWITCH), "Fan Switch Device Type");
      bimap.put(merge(SWITCH_MULTILEVEL, COLOR_TUNABLE_MULTILEVEL), "Multilevel Tunable Color Light");

      bimap.put(merge(SWITCH_REMOTE, NOT_USED), "Not Used");
      bimap.put(merge(SWITCH_REMOTE, SWITCH_REMOTE_BINARY), "Binary Remote Switch");
      bimap.put(merge(SWITCH_REMOTE, SWITCH_REMOTE_MULTILEVEL), "Multilevel Remote Switch");
      bimap.put(merge(SWITCH_REMOTE, SWITCH_REMOTE_TOGGLE_BINARY), "Binary Toggle Remote Switch");
      bimap.put(merge(SWITCH_REMOTE, SWITCH_REMOTE_TOGGLE_MULTILEVEL), "Multilevel Toggle Remote Switch");

      bimap.put(merge(SWITCH_TOGGLE, NOT_USED), "Not Used");
      bimap.put(merge(SWITCH_TOGGLE, SWITCH_TOGGLE_BINARY), "Binary Toggle Switch");
      bimap.put(merge(SWITCH_TOGGLE, SWITCH_TOGGLE_MULTILEVEL), "Multilevel Toggle Switch");

      bimap.put(merge(THERMOSTAT, NOT_USED), "Not Used");
      bimap.put(merge(THERMOSTAT, SETBACK_SCHEDULE_THERMOSTAT), "Setback Schedule Thermostat");
      bimap.put(merge(THERMOSTAT, SETBACK_THERMOSTAT), "Thermostat (Setback) Device Type");
      bimap.put(merge(THERMOSTAT, SETPOINT_THERMOSTAT), "Setpoint Thermostat");
      bimap.put(merge(THERMOSTAT, THERMOSTAT_GENERAL), "Thermostat General");
      bimap.put(merge(THERMOSTAT, THERMOSTAT_GENERAL_V2), "Thermostat (HVAC) Device");
      bimap.put(merge(THERMOSTAT, THERMOSTAT_HEATING), "Thermostat Heating");

      bimap.put(merge(VENTILATION, NOT_USED), "Not Used");
      bimap.put(merge(VENTILATION, RESIDENTIAL_HRV), "Residential HRV");

      bimap.put(merge(WINDOW_COVERING, NOT_USED), "Not Used");
      bimap.put(merge(WINDOW_COVERING, SIMPLE_WINDOW_COVERING), "Simple Window Covering");

      bimap.put(merge(ZIP_NODE, NOT_USED), "Not Used");
      bimap.put(merge(ZIP_NODE, ZIP_ADV_NODE), "ZIP Advanced Node");
      bimap.put(merge(ZIP_NODE, ZIP_TUN_NODE), "ZIP Tun Node");

      bimap.put(merge(WALL_CONTROLLER, NOT_USED), "Not Used");
      bimap.put(merge(WALL_CONTROLLER, BASIC_WALL_CONTROLLER), "Basic Wall Controller");

      bimap.put(merge(NETWORK_EXTENDER, NOT_USED), "Not Used");
      bimap.put(merge(NETWORK_EXTENDER, SECURE_EXTENDER), "Secure Extender");

      bimap.put(merge(APPLIANCE, NOT_USED), "Not Used");
      bimap.put(merge(APPLIANCE, GENERAL_APPLIANCE), "General Appliance");
      bimap.put(merge(APPLIANCE, KITCHEN_APPLIANCE), "Kitchen Appliance");
      bimap.put(merge(APPLIANCE, LAUNDRY_APPLIANCE), "Laundry Appliance");

      bimap.put(merge(SENSOR_NOTIFICATION, NOT_USED), "Not Used");
      bimap.put(merge(SENSOR_NOTIFICATION, NOTIFICATION_SENSOR), "Notification Sensor");

      return bimap;
   }

   private static Map<Integer, String> createSpecificIrisNamesBiMap() {
      Map<Integer, String > bimap = new HashMap<Integer, String>();

      bimap.put(merge(AV_CONTROL_POINT, NOT_USED), "AV Controller");
      bimap.put(merge(AV_CONTROL_POINT, DOORBELL), "AV Controller");
      bimap.put(merge(AV_CONTROL_POINT, SATELLITE_RECEIVER), "AV Controller");
      bimap.put(merge(AV_CONTROL_POINT, SATELLITE_RECEIVER_V2), "AV Controller");

      bimap.put(merge(DISPLAY, NOT_USED), "Display");
      bimap.put(merge(DISPLAY, SIMPLE_DISPLAY), "Display");

      bimap.put(merge(ENTRY_CONTROL, NOT_USED), "Entry Control");
      bimap.put(merge(ENTRY_CONTROL, DOOR_LOCK), "Door Lock");
      bimap.put(merge(ENTRY_CONTROL, ADVANCED_DOOR_LOCK), "Door Lock");
      bimap.put(merge(ENTRY_CONTROL, SECURE_KEYPAD_DOOR_LOCK), "Lever Lock");
      bimap.put(merge(ENTRY_CONTROL, SECURE_KEYPAD_DOOR_LOCK_DEADBOLT), "Keypad Lock");
      bimap.put(merge(ENTRY_CONTROL, SECURE_DOOR), "Garage Door Controller");
      bimap.put(merge(ENTRY_CONTROL, SECURE_GATE), "Gate Controller");
      bimap.put(merge(ENTRY_CONTROL, SECURE_BARRIER_ADDON), "Garage Door Controller");
      bimap.put(merge(ENTRY_CONTROL, SECURE_BARRIER_OPEN_ONLY), "Garage Door Controller");
      bimap.put(merge(ENTRY_CONTROL, SECURE_BARRIER_CLOSE_ONLY), "Garage Door Controller");
      bimap.put(merge(ENTRY_CONTROL, SECURE_LOCKBOX), "Lock Box");

      bimap.put(merge(GENERIC_CONTROLLER, NOT_USED), "Generic Controller");
      bimap.put(merge(GENERIC_CONTROLLER, PORTABLE_REMOTE_CONTROLLER), "Generic Controller");
      bimap.put(merge(GENERIC_CONTROLLER, PORTABLE_SCENE_CONTROLLER), "Generic Controller");
      bimap.put(merge(GENERIC_CONTROLLER, PORTABLE_INSTALLER_TOOL), "Generic Controller");
      bimap.put(merge(GENERIC_CONTROLLER, REMOTE_CONTROL_AV), "Generic Controller");
      bimap.put(merge(GENERIC_CONTROLLER, REMOTE_CONTROL_SIMPLE), "Generic Controller");

      bimap.put(merge(METER, NOT_USED), "Meter");
      bimap.put(merge(METER, SIMPLE_METER), "Energy Meter");
      bimap.put(merge(METER, ADV_ENERGY_CONTROL), "Energy Meter");
      bimap.put(merge(METER, WHOLE_HOME_METER_SIMPLE), "Energy Meter");

      bimap.put(merge(METER_PULSE, NOT_USED), "Meter Pulse");

      bimap.put(merge(NON_INTEROPERABLE, NOT_USED), "Non Interoperable");

      bimap.put(merge(GENERIC_REPEATER_SLAVE, NOT_USED), "Repeater Slave");
      bimap.put(merge(GENERIC_REPEATER_SLAVE, SPECIFIC_REPEATER_SLAVE), "Repeater Slave");

      bimap.put(merge(SECURITY_PANEL, NOT_USED), "Security Panel");
      bimap.put(merge(SECURITY_PANEL, ZONED_SECURITY_PANEL), "Security Panel");

      bimap.put(merge(SEMI_INTEROPERABLE, NOT_USED), "Semi Interoperable");
      bimap.put(merge(SEMI_INTEROPERABLE, ENERGY_PRODUCTION), "Energy Device");

      bimap.put(merge(SENSOR_ALARM, NOT_USED), "Sensor Alarm");
      bimap.put(merge(SENSOR_ALARM, ADV_ZENSOR_NET_ALARM_SENSOR), "Sensor Alarm");
      bimap.put(merge(SENSOR_ALARM, ADV_ZENSOR_NET_SMOKE_SENSOR), "Sensor Alarm");
      bimap.put(merge(SENSOR_ALARM, BASIC_ROUTING_ALARM_SENSOR), "Sensor Alarm");
      bimap.put(merge(SENSOR_ALARM, BASIC_ROUTING_SMOKE_SENSOR), "Sensor Alarm");
      bimap.put(merge(SENSOR_ALARM, BASIC_ZENSOR_NET_ALARM_SENSOR), "Sensor Alarm");
      bimap.put(merge(SENSOR_ALARM, BASIC_ZENSOR_NET_SMOKE_SENSOR), "Sensor Alarm");
      bimap.put(merge(SENSOR_ALARM, ROUTING_ALARM_SENSOR), "Sensor Alarm");
      bimap.put(merge(SENSOR_ALARM, ROUTING_SMOKE_SENSOR), "Sensor Alarm");
      bimap.put(merge(SENSOR_ALARM, ZENSOR_NET_ALARM_SENSOR), "Sensor Alarm");
      bimap.put(merge(SENSOR_ALARM, ZENSOR_NET_SMOKE_SENSOR), "Sensor Alarm");
      bimap.put(merge(SENSOR_ALARM, ALARM_SENSOR), "Sensor Alarm");

      bimap.put(merge(SENSOR_BINARY, NOT_USED), "Binary Sensor");
      bimap.put(merge(SENSOR_BINARY, ROUTING_SENSOR_BINARY), "Binary Sensor");

      bimap.put(merge(SENSOR_MULTILEVEL, NOT_USED), "Multilevel Sensor");
      bimap.put(merge(SENSOR_MULTILEVEL, ROUTING_SENSOR_MULTILEVEL), "Multilevel Sensor");
      bimap.put(merge(SENSOR_MULTILEVEL, CHIMNEY_FAN), "Multilevel Sensor");

      bimap.put(merge(GENERIC_STATIC_CONTROLLER, NOT_USED), "Static Controller");
      bimap.put(merge(GENERIC_STATIC_CONTROLLER, PC_CONTROLLER), "Static Controller");
      bimap.put(merge(GENERIC_STATIC_CONTROLLER, SCENE_CONTROLLER), "Static Controller");
      bimap.put(merge(GENERIC_STATIC_CONTROLLER, STATIC_INSTALLER_TOOL), "Static Controller");
      bimap.put(merge(GENERIC_STATIC_CONTROLLER, SET_TOP_BOX), "Static Controller");
      bimap.put(merge(GENERIC_STATIC_CONTROLLER, SUB_SYSTEM_CONTROLLER), "Static Controller");
      bimap.put(merge(GENERIC_STATIC_CONTROLLER, TV), "Static Controller");
      bimap.put(merge(GENERIC_STATIC_CONTROLLER, GATEWAY), "Static Controller");

      bimap.put(merge(SWITCH_BINARY, NOT_USED), "Not Used");
      bimap.put(merge(SWITCH_BINARY, POWER_SWITCH_BINARY), "Switch");
      bimap.put(merge(SWITCH_BINARY, SCENE_SWITCH_BINARY), "Switch");
      bimap.put(merge(SWITCH_BINARY, POWER_STRIP), "Power Strip");
      bimap.put(merge(SWITCH_BINARY, SIREN), "Siren");
      bimap.put(merge(SWITCH_BINARY, VALVE_OPEN_CLOSE), "Valve");
      bimap.put(merge(SWITCH_BINARY, COLOR_TUNABLE_BINARY), "Light");

      bimap.put(merge(SWITCH_MULTILEVEL, NOT_USED), "Multilevel Switch");
      bimap.put(merge(SWITCH_MULTILEVEL, CLASS_A_MOTOR_CONTROL), "Window Blind");
      bimap.put(merge(SWITCH_MULTILEVEL, CLASS_B_MOTOR_CONTROL), "Window Blind");
      bimap.put(merge(SWITCH_MULTILEVEL, CLASS_C_MOTOR_CONTROL), "Window Blind");
      bimap.put(merge(SWITCH_MULTILEVEL, MOTOR_MULTIPOSITION), "Motor");
      bimap.put(merge(SWITCH_MULTILEVEL, POWER_SWITCH_MULTILEVEL), "Dimmer Switch");
      bimap.put(merge(SWITCH_MULTILEVEL, SCENE_SWITCH_MULTILEVEL), "Switch");
      bimap.put(merge(SWITCH_MULTILEVEL, FAN_SWITCH), "Fan Switch");
      bimap.put(merge(SWITCH_MULTILEVEL, COLOR_TUNABLE_MULTILEVEL), "Colored Light");

      bimap.put(merge(SWITCH_REMOTE, NOT_USED), "Remote Switch");
      bimap.put(merge(SWITCH_REMOTE, SWITCH_REMOTE_BINARY), "Switch");
      bimap.put(merge(SWITCH_REMOTE, SWITCH_REMOTE_MULTILEVEL), "Switch");
      bimap.put(merge(SWITCH_REMOTE, SWITCH_REMOTE_TOGGLE_BINARY), "Toggle Switch");
      bimap.put(merge(SWITCH_REMOTE, SWITCH_REMOTE_TOGGLE_MULTILEVEL), "Toggle Switch");

      bimap.put(merge(SWITCH_TOGGLE, NOT_USED), "Toggle Switch");
      bimap.put(merge(SWITCH_TOGGLE, SWITCH_TOGGLE_BINARY), "Toggle Switch");
      bimap.put(merge(SWITCH_TOGGLE, SWITCH_TOGGLE_MULTILEVEL), "Toggle Switch");

      bimap.put(merge(THERMOSTAT, NOT_USED), "Thermostat");
      bimap.put(merge(THERMOSTAT, SETBACK_SCHEDULE_THERMOSTAT), "Thermostat");
      bimap.put(merge(THERMOSTAT, SETBACK_THERMOSTAT), "Thermostat");
      bimap.put(merge(THERMOSTAT, SETPOINT_THERMOSTAT), "Thermostat");
      bimap.put(merge(THERMOSTAT, THERMOSTAT_GENERAL), "Thermostat");
      bimap.put(merge(THERMOSTAT, THERMOSTAT_GENERAL_V2), "Thermostat");
      bimap.put(merge(THERMOSTAT, THERMOSTAT_HEATING), "Thermostat");

      bimap.put(merge(VENTILATION, NOT_USED), "Ventilation");
      bimap.put(merge(VENTILATION, RESIDENTIAL_HRV), "Heat Recovery Ventilator");

      bimap.put(merge(WINDOW_COVERING, NOT_USED), "Window Blind");
      bimap.put(merge(WINDOW_COVERING, SIMPLE_WINDOW_COVERING), "Window Blind");

      bimap.put(merge(ZIP_NODE, NOT_USED), "ZIP Node");
      bimap.put(merge(ZIP_NODE, ZIP_ADV_NODE), "ZIP Node");
      bimap.put(merge(ZIP_NODE, ZIP_TUN_NODE), "ZIP Node");

      bimap.put(merge(WALL_CONTROLLER, NOT_USED), "Wall Controller");
      bimap.put(merge(WALL_CONTROLLER, BASIC_WALL_CONTROLLER), "Wall Controller");

      bimap.put(merge(NETWORK_EXTENDER, NOT_USED), "Range Extender");
      bimap.put(merge(NETWORK_EXTENDER, SECURE_EXTENDER), "Range Extender");

      bimap.put(merge(APPLIANCE, NOT_USED), "Applicance");
      bimap.put(merge(APPLIANCE, GENERAL_APPLIANCE), "Appliance");
      bimap.put(merge(APPLIANCE, KITCHEN_APPLIANCE), "Kitchen Appliance");
      bimap.put(merge(APPLIANCE, LAUNDRY_APPLIANCE), "Laundry Appliance");

      bimap.put(merge(SENSOR_NOTIFICATION, NOT_USED), "Notification Sensor");
      bimap.put(merge(SENSOR_NOTIFICATION, NOTIFICATION_SENSOR), "Notification Sensor");

      return bimap;
   }

   // This really should be in some binary helpers in java main somewhere.
   public static int merge(byte b1, byte b2) {
      int value = (((int) b1) & 0xFF);
      value <<= 8;
      value += (((int) b2) & 0xFF);
      return value;
   }

   public static String generateTable() {
      String tbr;
      String name;
      int mid;

      tbr = "ZWaveDeviceNames =[";
      for (int gid = 0; gid < 0xFF; gid++) {
         for (int sid = 0; sid < 0xFF; sid++) {
            if (genericNames.inverse().containsKey((byte) gid)) {
               mid = merge((byte) gid, (byte) sid);
               if (specificIrisNames.containsKey(mid)) {
                  name = ZWaveBasicDevicesType.getNameOf((byte) gid, (byte) sid);
                  tbr += "(" + gid + "," + sid + "," + name + ")\n";
               }
            }
         }
      }
      tbr += "]";
      return tbr;
   }

   public static void main(String[] args) {
      System.out.println(ZWaveBasicDevicesType.generateTable());
   }

}

