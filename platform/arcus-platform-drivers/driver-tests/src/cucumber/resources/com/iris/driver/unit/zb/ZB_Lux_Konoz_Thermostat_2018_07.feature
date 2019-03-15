@Zigbee @Lux @Konoz
Feature: ZigBee LUX KONOz Thermostat Driver Test

These scenarios test the functionality of the Zigbee LUX KONOz Thermostat driver

    Background:
    Given the ZB_Lux_Konoz_Thermostat_2018_07.driver has been initialized
        And the device has endpoint 1

    @basic
    Scenario: Driver reports capabilities to platform. 
    When a base:GetAttributes command is placed on the platform bus
    Then the driver should place a base:GetAttributesResponse message on the platform bus
        And the message's base:caps attribute list should be ['base', 'dev', 'devadv', 'devpow', 'devconn', 'devota', 'ident', 'temp', 'therm']
        And the message's dev:devtypehint attribute should be Thermostat
        And the message's devadv:drivername attribute should be ZBLuxKonozThermostat 
        And the message's devadv:driverversion attribute should be 2018.07
        And the message's therm:minsetpoint attribute should be 7.2
        And the message's therm:maxsetpoint attribute should be 35.0
        And the message's therm:setpointseparation attribute should be 1.67
    Then both busses should be empty



############################################################
# Generic Driver Tests
############################################################

    @basic @added
    Scenario: Make sure any "time of change" attributes are defaulted when the device is first Added
        When the device is added
        Then the capability devpow:sourcechanged should be recent

    @basic @name
    Scenario Outline: Make sure driver allows device name to be set 
        When a base:SetAttributes command with the value of dev:name <value> is placed on the platform bus
        Then the platform attribute dev:name should change to <value>

        Examples:
          | value                    |
          | Device                   |
          | "My Device"              |
          | "Tom's Device"           |
          | "Bob & Sue's Device"     |



############################################################
# Generic ZigBee Driver Tests
############################################################

    @basic
    Scenario: Make sure driver has implemented onIdentify.Identify method for Identify capability
        When the capability method ident:Identify
            And send to driver
        Then the driver should send identify identifyCmd    

    #
    # NOTE: For ZigBee, Battery Level is reported in tenths of volts (100mV)
    #  Minimum Voltage is 3.8 Volts (38 tenths)
    #  Nominal Voltage is 4.8 Volts (48 tenths) - 3 AA Batteries
    #
    @basic @battery
    Scenario Outline: Device reports battery level with power cluster response
        Given the capability devpow:battery is 80
        When the device response with power <message-type>
            And with parameter ATTR_BATTERY_VOLTAGE <voltage-level>
            And send to driver 
        Then the capability devpow:battery should be <battery-attr>
            And the driver should place a base:ValueChange message on the platform bus

        Examples:
          | message-type              | voltage-level | battery-attr | remarks                                                  |
          | zclreadattributesresponse | 45            |   0.0        | 46 tenths or less should be treated as 0%                |
          | zclreadattributesresponse | 46            |   0.0        | 46 tenths or less should be treated as 0%                |
          | zclreadattributesresponse | 53            |  50.0        |                                                          |
          | zclreadattributesresponse | 60            | 100.0        | 60 tenths or more should be treated as 100%              |
          | zclreadattributesresponse | 61            | 100.0        | 60 tenths or more should be treated as 100%              |

          | zclreportattributes       | 45            |   0.0        | 46 tenths or less should be treated as 0%                |
          | zclreportattributes       | 46            |   0.0        | 46 tenths or less should be treated as 0%                |
          | zclreportattributes       | 53            |  50.0        |                                                          |
          | zclreportattributes       | 60            | 100.0        | 60 tenths or more should be treated as 100%              |
          | zclreportattributes       | 61            | 100.0        | 60 tenths or more should be treated as 100%              |


############################################################
# Generic Thermostat Driver Tests
############################################################

    Scenario Outline: Make sure driver processes user writable Thermostat attributes 
        When a base:SetAttributes command with the value of <type> <value> is placed on the platform bus
        Then the platform attribute <type> should change to <value>

        Examples:
          | type                        | value                  |
          | therm:filtertype            | Size:16x25x1           |
          | therm:filterlifespanruntime | 720                    |
          | therm:filterlifespandays    | 180                    |

    Scenario: Make sure driver has implemented onThermostat.changeFilter for Thermsotat capability
        When a therm:changeFilter command is placed on the platform bus
        Then the platform attribute therm:dayssincefilterchange should change to 0
        Then the platform attribute therm:runtimesincefilterchange should change to 0
            And the driver should place a therm:changeFilterResponse message on the platform bus
        Then protocol bus should be empty

