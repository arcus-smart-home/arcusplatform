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

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public class ZWaveManufactures {
	public final static BiMap<String,Integer>	names = createNamesBiMap();

	/************* Manufacturer ID identifiers **************/
	public final static int NOT_DEFINED_OR_UNDEFINED                            = 0xFFFF   ;  //Not defined or un-defined
	public final static int _2B_ELECTRONICS                                     = 0x0028   ;  //2B Electronics
	public final static int _2GIG_TECHNOLOGIES_INC                              = 0x009B   ;  //2gig Technologies Inc.
	public final static int _3E_TECHNOLOGIES                                    = 0x002A   ;  //3e Technologies
	public final static int A1_COMPONENTS                                       = 0x0022   ;  //A-1 Components
	public final static int ABILIA                                              = 0x0117   ;  //Abilia
	public final static int ACT_ADVANCED_CONTROL_TECHNOLOGIES                   = 0x0001   ;  //ACT - Advanced Control Technologies
	public final static int AEON_LABS                                           = 0x0086   ;  //AEON Labs
	public final static int AIRLINE_MECHANICAL_CO_LTD                           = 0x0111   ;  //Airline Mechanical Co., Ltd.
	public final static int ALARMCOM                                            = 0x0094   ;  //Alarm.com
	public final static int ASIA_HEADING                                        = 0x0029   ;  //Asia Heading
	public final static int ATECH                                               = 0x002B   ;  //Atech
	public final static int BALBOA_INSTRUMENTS                                  = 0x0018   ;  //Balboa Instruments
	public final static int BENEXT                                              = 0x008A   ;  //BeNext
	public final static int BESAFER                                             = 0x002C   ;  //BeSafer
	public final static int BFT_SPA                                             = 0x014B   ;  //BFT S.p.A.
	public final static int BOCA_DEVICES                                        = 0x0023   ;  //Boca Devices
	public final static int BROADBAND_ENERGY_NETWORKS_INC                       = 0x002D   ;  //Broadband Energy Networks Inc.
	public final static int BULOGICS                                            = 0x0026   ;  //BuLogics
	public final static int CAMEO_COMMUNICATIONS_INC                            = 0x009C   ;  //Cameo Communications Inc.
	public final static int CARRIER                                             = 0x002E   ;  //Carrier
	public final static int CASAWORKS                                           = 0x000B   ;  //CasaWorks
	public final static int CHECKIT_SOLUTIONS_INC                               = 0x014E   ;  //Check-It Solutions Inc.
	public final static int CHROMAGIC_TECHNOLOGIES_CORPORATION                  = 0x0116   ;  //Chromagic Technologies Corporation
	public final static int COLOR_KINETICS_INCORPORATED                         = 0x002F   ;  //Color Kinetics Incorporated
	public final static int COMPUTIME                                           = 0x0140   ;  //Computime
	public final static int CONNECTED_OBJECT                                    = 0x011B   ;  //Connected Object
	public final static int CONTROLTHINK_LC                                     = 0x0019   ;  //ControlThink LC
	public final static int CONVERGEX_LTD                                       = 0x000F   ;  //ConvergeX Ltd.
	public final static int COOPER_LIGHTING                                     = 0x0079   ;  //Cooper Lighting
	public final static int COOPER_WIRING_DEVICES                               = 0x001A   ;  //Cooper Wiring Devices
//	public final static int CORNUCOPIA_CORP                                     = 0x012D   ;  //Cornucopia Corp
	public final static int COVENTIVE_TECHNOLOGIES_INC                          = 0x009D   ;  //Coventive Technologies Inc.
	public final static int CYBERHOUSE                                          = 0x0014   ;  //Cyberhouse
	public final static int CYBERTAN_TECHNOLOGY_INC                             = 0x0067   ;  //CyberTAN Technology, Inc.
	public final static int CYTECH_TECHNOLOGY_PRE_LTD                           = 0x0030   ;  //Cytech Technology Pre Ltd.
	public final static int DANFOSS                                             = 0x0002   ;  //Danfoss
	public final static int DEFACONTROLS_BV                                     = 0x013F   ;  //Defacontrols BV
	public final static int DESTINY_NETWORKS                                    = 0x0031   ;  //Destiny Networks
	public final static int DIEHL_AKO                                           = 0x0103   ;  //Diehl AKO
	public final static int DIGITAL_5_INC                                       = 0x0032   ;  //Digital 5, Inc.
	public final static int DYNAQUIP_CONTROLS                                   = 0x0132   ;  //DynaQuip Controls
	public final static int ECOLINK                                             = 0x014A   ;  //Ecolink
	public final static int EKA_SYSTEMS                                         = 0x0087   ;  //Eka Systems
	public final static int ELECTRONIC_SOLUTIONS                                = 0x0033   ;  //Electronic Solutions
	public final static int ELGEV_ELECTRONICS_LTD                               = 0x0034   ;  //El-Gev Electronics LTD
	public final static int ELK_PRODUCTS_INC                                    = 0x001B   ;  //ELK Products, Inc.
	public final static int EMBEDIT_AS                                          = 0x0035   ;  //Embedit A/S
	public final static int ENBLINK_CO_LTD                                      = 0x014D   ;  //Enblink Co. Ltd
	public final static int EUROTRONICS                                         = 0x0148   ;  //Eurotronics
	public final static int EVERSPRING                                          = 0x0060   ;  //Everspring
	public final static int EVOLVE                                              = 0x0113   ;  //Evolve
	public final static int EXCEPTIONAL_INNOVATIONS                             = 0x0036   ;  //Exceptional Innovations
	public final static int EXHAUSTO                                            = 0x0004   ;  //Exhausto
	public final static int EXIGENT_SENSORS                                     = 0x009F   ;  //Exigent Sensors
	public final static int EXPRESS_CONTROLS                                    = 0x001E   ;  //Express Controls (former Ryherd Ventures)
	public final static int FAKRO                                               = 0x0085   ;  //Fakro
	public final static int FIBARGROUP                                          = 0x010F   ;  //Fibargroup
	public final static int FOARD_SYSTEMS                                       = 0x0037   ;  //Foard Systems
	public final static int FOLLOWGOOD_TECHNOLOGY_COMPANY_LTD                   = 0x0137   ;  //FollowGood Technology Company Ltd.
	public final static int FORTREZZ_LLC                                        = 0x0084   ;  //FortrezZ LLC
	public final static int FOXCONN                                             = 0x011D   ;  //Foxconn
	public final static int FROSTDALE                                           = 0x0110   ;  //Frostdale
	public final static int GOOD_WAY_TECHNOLOGY_CO_LTD                          = 0x0068   ;  //Good Way Technology Co., Ltd
	public final static int GREENWAVE_REALITY_INC                               = 0x0099   ;  //GreenWave Reality Inc.
	public final static int HITECH_AUTOMATION                                   = 0x0017   ;  //HiTech Automation
	public final static int HOLTEC_ELECTRONICS_BV                               = 0x013E   ;  //Holtec Electronics BV
	public final static int HOME_AUTOMATED_INC                                  = 0x005B   ;  //Home Automated Inc.
	public final static int HOME_AUTOMATED_LIVING                               = 0x000D   ;  //Home Automated Living
	public final static int HOME_AUTOMATION_EUROPE                              = 0x009A   ;  //Home Automation Europe
	public final static int HOME_DIRECTOR                                       = 0x0038   ;  //Home Director
	public final static int HOMEMANAGEABLES_INC                                 = 0x0070   ;  //Homemanageables, Inc.
	public final static int HOMEPRO                                             = 0x0050   ;  //Homepro
	public final static int HOMESCENARIO                                        = 0x0162   ;  //HomeScenario
	public final static int HOMESEER_TECHNOLOGIES                               = 0x000C   ;  //HomeSeer Technologies
	public final static int HONEYWELL                                           = 0x0039   ;  //Honeywell
	public final static int HORSTMANN_CONTROLS_LIMITED                          = 0x0059   ;  //Horstmann Controls Limited
	public final static int ICOM_TECHNOLOGY_BV                                  = 0x0011   ;  //iCOM Technology b.v.
	public final static int INGERSOLL_RAND_SCHLAGE                              = 0x006C   ;  //Ingersoll Rand (Schlage)
	public final static int INGERSOLL_RAND_ECOLINK                              = 0x011F   ;  //Ingersoll Rand (Former Ecolink)
	public final static int INLON_SRL                                           = 0x003A   ;  //Inlon Srl
	public final static int INNOBAND_TECHNOLOGIES_INC                           = 0x0141   ;  //Innoband Technologies, Inc
	public final static int INNOVUS                                             = 0x0077   ;  //INNOVUS
	public final static int INTEL                                               = 0x0006   ;  //Intel
	public final static int INTELLICON                                          = 0x001C   ;  //IntelliCon
	public final static int INTERMATIC                                          = 0x0005   ;  //Intermatic
	public final static int INTERNET_DOM                                        = 0x0013   ;  //Internet Dom
	public final static int IR_SEC_SAFETY                                       = 0x003B   ;  //IR Sec. & Safety
	public final static int IWATSU                                              = 0x0123   ;  //IWATSU
	public final static int JASCO_PRODUCTS                                      = 0x0063   ;  //Jasco Products
	public final static int KAMSTRUP_AS                                         = 0x0091   ;  //Kamstrup A/S
	public final static int LAGOTEK_CORPORATION                                 = 0x0051   ;  //Lagotek Corporation
	public final static int LEVITON                                             = 0x001D   ;  //Leviton
	public final static int LIFESTYLE_NETWORKS                                  = 0x003C   ;  //Lifestyle Networks
	public final static int LINEAR_CORP                                         = 0x014F   ;  //Linear Corp
	public final static int LIVING_STYLE_ENTERPRISES_LTD                        = 0x013A   ;  //Living Style Enterprises, Ltd.
	public final static int LOGITECH                                            = 0x007F   ;  //Logitech
	public final static int LOUDWATER_TECHNOLOGIES_LLC                          = 0x0025   ;  //Loudwater Technologies, LLC
	public final static int LS_CONTROL                                          = 0x0071   ;  //LS Control
	public final static int MARMITEK_BV                                         = 0x003D   ;  //Marmitek BV
	public final static int MARTEC_ACCESS_PRODUCTS                              = 0x003E   ;  //Martec Access Products
	public final static int MB_TURN_KEY_DESIGN                                  = 0x008F   ;  //MB Turn Key Design
	public final static int MERTEN                                              = 0x007A   ;  //Merten
	public final static int MITSUMI                                             = 0x0112   ;  //MITSUMI
	public final static int MONSTER_CABLE                                       = 0x007E   ;  //Monster Cable
	public final static int MOTOROLA                                            = 0x003F   ;  //Motorola
	public final static int MTC_MAINTRONIC_GERMANY                              = 0x0083   ;  //MTC Maintronic Germany
	public final static int NAPCO_SECURITY_TECHNOLOGIES_INC                     = 0x0121   ;  //Napco Security Technologies, Inc.
	public final static int NORTHQ                                              = 0x0096   ;  //NorthQ
	public final static int NOVAR_ELECTRICAL_DEVICES_AND_SYSTEMS_EDS            = 0x0040   ;  //Novar Electrical Devices and Systems (EDS)
	public final static int OMNIMA_LIMITED                                      = 0x0119   ;  //Omnima Limited
	public final static int ONSITE_PRO                                          = 0x014C   ;  //OnSite Pro
	public final static int OPENPEAK_INC                                        = 0x0041   ;  //OpenPeak Inc.
	public final static int PHILIO_TECHNOLOGY_CORP                              = 0x013C   ;  //Philio Technology Corp
	public final static int POLYCONTROL                                         = 0x010E   ;  //Poly-control
	public final static int POWERLYNX                                           = 0x0016   ;  //PowerLynx
	public final static int PRAGMATIC_CONSULTING_INC                            = 0x0042   ;  //Pragmatic Consulting Inc.
	public final static int PULSE_TECHNOLOGIES_ASPALIS                          = 0x005D   ;  //Pulse Technologies (Aspalis)
	public final static int QEES                                                = 0x0095   ;  //Qees
	public final static int QUBY                                                = 0x0130   ;  //Quby
	public final static int RADIO_THERMOSTAT_COMPANY_OF_AMERICA_RTC             = 0x0098   ;  //Radio Thermostat Company of America (RTC)
	public final static int RARITAN                                             = 0x008E   ;  //Raritan
	public final static int REITZGROUPDE                                        = 0x0064   ;  //Reitz-Group.de
	public final static int REMOTEC_TECHNOLOGY_LTD                              = 0x5254   ;  //Remotec Technology Ltd
	public final static int RESIDENTIAL_CONTROL_SYSTEMS_INC_RCS                 = 0x0010   ;  //Residential Control Systems, Inc. (RCS)
	public final static int RIMPORT_LTD                                         = 0x0147   ;  //R-import Ltd.
	public final static int RS_SCENE_AUTOMATION                                 = 0x0065   ;  //RS Scene Automation
	public final static int SAECO                                               = 0x0139   ;  //Saeco
	public final static int SAN_SHIH_ELECTRICAL_ENTERPRISE_CO_LTD               = 0x0093   ;  //San Shih Electrical Enterprise Co., Ltd.
	public final static int SANAV                                               = 0x012C   ;  //SANAV
	public final static int SCIENTIA_TECHNOLOGIES_INC                           = 0x001F   ;  //Scientia Technologies, Inc.
	public final static int SECURE_WIRELESS                                     = 0x011E   ;  //Secure Wireless
	public final static int SELUXIT                                             = 0x0069   ;  //Seluxit
	public final static int SENMATIC_AS                                         = 0x0043   ;  //Senmatic A/S
	public final static int SEQUOIA_TECHNOLOGY_LTD                              = 0x0044   ;  //Sequoia Technology LTD
	public final static int SIGMA_DESIGNS                                       = 0x0000   ;  //Sigma Designs
	public final static int SINE_WIRELESS                                       = 0x0045   ;  //Sine Wireless
	public final static int SMART_PRODUCTS_INC                                  = 0x0046   ;  //Smart Products, Inc.
	public final static int SMK_MANUFACTURING_INC                               = 0x0102   ;  //SMK Manufacturing Inc.
	public final static int SOMFY                                               = 0x0047   ;  //Somfy
	public final static int SYLVANIA                                            = 0x0009   ;  //Sylvania
	public final static int SYSTECH_CORPORATION                                 = 0x0136   ;  //Systech Corporation
	public final static int TEAM_PRECISION_PCL                                  = 0x0089   ;  //Team Precision PCL
	public final static int TECHNIKU                                            = 0x000A   ;  //Techniku
	public final static int TELL_IT_ONLINE                                      = 0x0012   ;  //Tell It Online
	public final static int TELSEY                                              = 0x0048   ;  //Telsey
	public final static int THERE_CORPORATION                                   = 0x010C   ;  //There Corporation
	public final static int TKB_HOME                                            = 0x0118   ;  //TKB Home
	public final static int TKH_GROUP_EMINENT                                   = 0x011C   ;  //TKH Group / Eminent
	public final static int TRANE_CORPORATION                                   = 0x008B   ;  //Trane Corporation
	public final static int TRICKLESTAR                                         = 0x0066   ;  //TrickleStar
	public final static int TRICKLESTAR_LTD_EMPOWER_CONTROLS_LTD                = 0x006B   ;  //Tricklestar Ltd. (former Empower Controls Ltd.)
	public final static int TRIDIUM                                             = 0x0055   ;  //Tridium
	public final static int TWISTHINK                                           = 0x0049   ;  //Twisthink
	public final static int UNIVERSAL_ELECTRONICS_INC                           = 0x0020   ;  //Universal Electronics Inc.
	public final static int VDA                                                 = 0x010A   ;  //VDA
	public final static int VERO_DUCO                                           = 0x0080   ;  //Vero Duco
	public final static int VIEWSONIC_CORPORATION                               = 0x005E   ;  //ViewSonic Corporation
	public final static int VIMAR_CRS                                           = 0x0007   ;  //Vimar CRS
	public final static int VISION_SECURITY                                     = 0x0109   ;  //Vision Security
	public final static int VISUALIZE                                           = 0x004A   ;  //Visualize
	public final static int WATT_STOPPER                                        = 0x004B   ;  //Watt Stopper
	public final static int WAYNE_DALTON                                        = 0x0008   ;  //Wayne Dalton
	public final static int WENZHOU_MTLC_ELECTRIC_APPLIANCES_COLTD              = 0x011A   ;  //Wenzhou MTLC Electric Appliances Co.,Ltd.
	public final static int WIDOM                                               = 0x0149   ;  //wiDom
	public final static int WILSHINE_HOLDING_CO_LTD                             = 0x012D   ;  //Wilshine Holding Co., Ltd
	public final static int WINTOP                                              = 0x0097   ;  //Wintop
	public final static int WOODWARD_LABS                                       = 0x004C   ;  //Woodward Labs
	public final static int WRAP                                                = 0x0003   ;  //Wrap
	public final static int WUHAN_NWD_TECHNOLOGY_CO_LTD                         = 0x012E   ;  //Wuhan NWD Technology Co., Ltd.
	public final static int XANBOO                                              = 0x004D   ;  //Xanboo
	public final static int ZDATA_LLC                                           = 0x004E   ;  //Zdata, LLC.
	public final static int ZIPATO                                              = 0x0131   ;  //Zipato
	public final static int ZONOFF                                              = 0x0120   ;  //Zonoff
	public final static int ZWAVE_TECHNOLOGIA                                   = 0x004F   ;  //Z-Wave Technologia
	public final static int ZWAVEME                                             = 0x0115   ;  //Z-Wave.Me
	public final static int ZYKRONIX                                            = 0x0021   ;  //Zykronix
	public final static int ZYXEL                                               = 0x0135   ;  //ZyXEL


	public static BiMap<String,Integer> createNamesBiMap() {
		return ImmutableBiMap.<Integer,String>builder()
				.put(0xFFFF	, "Not defined or un-defined")
				.put(0x0028	, "2B Electronics")
				.put(0x009B	, "2gig	Technologies	Inc.")
				.put(0x002A	, "3e Technologies")
				.put(0x0022	, "A-1 Components")
				.put(0x0117	, "Abilia")
				.put(0x0001	, "ACT	-	Advanced	Control	Technologies")
				.put(0x0086	, "AEON	Labs")
				.put(0x0111	, "Airline	Mechanical Co.,	Ltd.")
				.put(0x0094	, "Alarm.com")
				.put(0x0029	, "Asia	Heading	")
				.put(0x002B	, "Atech")
				.put(0x0018	, "Balboa	Instruments")
				.put(0x008A	, "BeNext")
				.put(0x002C	, "BeSafer")
				.put(0x014B	, "BFT	S.p.A.")
				.put(0x0023	, "Boca	Devices	")
				.put(0x002D	, "Broadband	Energy	Networks	Inc.")
				.put(0x0026	, "BuLogics")
				.put(0x009C	, "Cameo Communications	Inc.")
				.put(0x002E	, "Carrier")
				.put(0x000B	, "CasaWorks")
				.put(0x014E	, "Check-It	Solutions	Inc.")
				.put(0x0116	, "Chromagic	Technologies	Corporation")
				.put(0x002F	, "Color	Kinetics	Incorporated")
				.put(0x0140	, "Computime")
				.put(0x011B	, "Connected	Object")
				.put(0x0019	, "ControlThink	LC")
				.put(0x000F	, "ConvergeX	Ltd.")
				.put(0x0079	, "Cooper	Lighting")
				.put(0x001A	, "Cooper	Wiring	Devices")
//				.put(0x012D	, "Cornucopia	Corp")				// Duplicate
				.put(0x009D	, "Coventive	Technologies	Inc.")
				.put(0x0014	, "Cyberhouse")
				.put(0x0067	, "CyberTAN	Technology,	Inc.")
				.put(0x0030	, "Cytech	Technology	Pre	Ltd.")
				.put(0x0002	, "Danfoss")
				.put(0x013F	, "Defacontrols	BV")
				.put(0x0031	, "Destiny	Networks")
				.put(0x0103	, "Diehl	AKO	")
				.put(0x0032	, "Digital	5,	Inc.")
				.put(0x0132	, "DynaQuip	Controls")
				.put(0x014A	, "Ecolink")
				.put(0x0087	, "Eka	Systems	")
				.put(0x0033	, "Electronic	Solutions")
				.put(0x0034	, "El-Gev	Electronics	LTD")
				.put(0x001B	, "ELK	Products	Inc.")
				.put(0x0035	, "Embedit	A/S")
				.put(0x014D	, "Enblink	Co.	Ltd")
				.put(0x0148	, "Eurotronics")
				.put(0x0060	, "Everspring")
				.put(0x0113	, "Evolve")
				.put(0x0036	, "Exceptional	Innovations	")
				.put(0x0004	, "Exhausto")
				.put(0x009F	, "Exigent	Sensors")
				.put(0x001E	, "Express	Controls")
				.put(0x0085	, "Fakro")
				.put(0x010F	, "Fibargroup")
				.put(0x0037	, "Foard	Systems")
				.put(0x0137	, "FollowGood	Technology	Company	Ltd.")
				.put(0x0084	, "FortrezZ	LLC")
				.put(0x011D	, "Foxconn")
				.put(0x0110	, "Frostdale")
				.put(0x0068	, "Good	Way	TechnologyCo.,	Ltd")
				.put(0x0099	, "GreenWave	Reality	Inc.")
				.put(0x0017	, "HiTech	Automation")
				.put(0x013E	, "Holtec	Electronics	BV")
				.put(0x005B	, "Home	Automated	Inc.")
				.put(0x000D	, "Home	Automated	Living")
				.put(0x009A	, "Home	Automation	Europe")
				.put(0x0038	, "Home	Director")
				.put(0x0070	, "Homemanageables, Inc.")
				.put(0x0050	, "Homepro")
				.put(0x0162	, "HomeScenario")
				.put(0x000C	, "HomeSeer	Technologies")
				.put(0x0039	, "Honeywell")
				.put(0x0059	, "Horstmann	Controls	Limited")
				.put(0x0011	, "iCOM	Technology	b.v.")
				.put(0x006C	, "Ingersoll	Rand	(Schlage)")
				.put(0x011F	, "Ingersoll	Rand	(Former	Ecolink)")
				.put(0x003A	, "Inlon	Srl")
				.put(0x0141	, "Innoband	Technologies,	Inc")
				.put(0x0077	, "INNOVUS")
				.put(0x0006	, "Intel")
				.put(0x001C	, "IntelliCon")
				.put(0x0005	, "Intermatic")
				.put(0x0013	, "Internet	Dom")
				.put(0x003B	, "IR	Sec.	&	Safety")
				.put(0x0123	, "IWATSU")
				.put(0x0063	, "Jasco Products")
				.put(0x0091	, "Kamstrup	A/S")
				.put(0x0051	, "Lagotek	Corporation")
				.put(0x001D	, "Leviton")
				.put(0x003C	, "Lifestyle	Networks")
				.put(0x014F	, "Linear	Corp")
				.put(0x013A	, "Living	Style	Enterprises,	Ltd.")
				.put(0x007F	, "Logitech")
				.put(0x0025	, "Loudwater	Technologies,	LLC")
				.put(0x0071	, "LS	Control")
				.put(0x003D	, "Marmitek	BV")
				.put(0x003E	, "Martec	Access	Products")
				.put(0x008F	, "MB	Turn	Key	Design")
				.put(0x007A	, "Merten")
				.put(0x0112	, "MITSUMI")
				.put(0x007E	, "Monster	Cable")
				.put(0x003F	, "Motorola")
				.put(0x0083	, "MTC	Maintronic	Germany")
				.put(0x0121	, "Napco Security Technologies,	Inc.")
				.put(0x0096	, "NorthQ")
				.put(0x0040	, "Novar	Electrical	Devices	and	Systems	(EDS)")
				.put(0x0119	, "Omnima	Limited")
				.put(0x014C	, "OnSite	Pro")
				.put(0x0041	, "OpenPeak	Inc.")
				.put(0x013C	, "Philio	Technology	Corp")
				.put(0x010E	, "Poly-control")
				.put(0x0016	, "PowerLynx")
				.put(0x0042	, "Pragmatic	Consulting	Inc.")
				.put(0x005D	, "Pulse	Technologies	(Aspalis)")
				.put(0x0095	, "Qees")
				.put(0x0130	, "Quby")
				.put(0x0098	, "Radio	Thermostat	Company	of	America	(RTC)")
				.put(0x008E	, "Raritan")
				.put(0x0064	, "Reitz-Group.de")
				.put(0x5254	, "Remotec	Technology	Ltd")
				.put(0x0010	, "Residential	Control	Systems, Inc. (RCS)")
				.put(0x0147	, "R-import	Ltd.")
				.put(0x0065	, "RS	Scene	Automation")
				.put(0x0139	, "Saeco")
				.put(0x0093	, "San	Shih	Electrical	Enterprise Co., Ltd.")
				.put(0x012C	, "SANAV")
				.put(0x001F	, "Scientia Technologies, Inc.")
				.put(0x011E	, "Secure	Wireless")
				.put(0x0069	, "Seluxit")
				.put(0x0043	, "Senmatic	A/S")
				.put(0x0044	, "Sequoia	Technology	LTD")
				.put(0x0000	, "Sigma	Designs")
				.put(0x0045	, "Sine	Wireless")
				.put(0x0046	, "Smart	Products,	Inc.")
				.put(0x0102	, "SMK	Manufacturing	Inc.")
				.put(0x0047	, "Somfy")
				.put(0x0009	, "Sylvania")
				.put(0x0136	, "Systech	Corporation")
				.put(0x0089	, "Team	Precision	PCL")
				.put(0x000A	, "Techniku")
				.put(0x0012	, "Tell	It	Online")
				.put(0x0048	, "Telsey")
				.put(0x010C	, "There	Corporation")
				.put(0x0118	, "TKB	Home")
				.put(0x011C	, "TKH	Group	/	Eminent")
				.put(0x008B	, "Trane	Corporation")
				.put(0x0066	, "TrickleStar")
				.put(0x006B	, "Tricklestar	Ltd.	(former	Empower	Controls	Ltd.)")
				.put(0x0055	, "Tridium")
				.put(0x0049	, "Twisthink")
				.put(0x0020	, "Universal	Electronics	Inc.")
				.put(0x010A	, "VDA")
				.put(0x0080	, "Vero	Duco")
				.put(0x005E	, "ViewSonic	Corporation")
				.put(0x0007	, "Vimar	CRS")
				.put(0x0109	, "Vision	Security")
				.put(0x004A	, "Visualize")
				.put(0x004B	, "Watt	Stopper")
				.put(0x0008	, "Wayne	Dalton")
				.put(0x011A	, "Wenzhou	MTLC	Electric	Appliances	Co.,Ltd.")
				.put(0x0149	, "wiDom")
				.put(0x012D	, "Wilshine	Holding	Co.,Ltd")		// Dumplicate ont the 0x012D
				.put(0x0097	, "Wintop")
				.put(0x004C	, "Woodward	Labs")
				.put(0x0003	, "Wrap")
				.put(0x012E	, "Wuhan NWD Technology Co., Ltd.")
				.put(0x004D	, "Xanboo")
				.put(0x004E	, "Zdata LLC.")
				.put(0x0131	, "Zipato")
				.put(0x0120	, "Zonoff")
				.put(0x004F	, "Z-Wave Technologia")
				.put(0x0115	, "Z-Wave.Me")
				.put(0x0021	, "Zykronix")
				.put(0x0135	, "ZyXEL")
				.build().inverse();
	}


	public static String get(int id) {
		return names.inverse().get(id);
	}

	public static Integer get(String name) {
		return names.get(name);
	}

}

