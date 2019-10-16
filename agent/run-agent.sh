#!/bin/bash
 
# This tells the agent code that we aren't a real hub so it should
# expect some hub specific things to be missing (e.g. LEDs, Buzzer, etc.)
export IRIS_AGENT_HUBV2_FAKE=true
export IRIS_AGENT_HUBV2_DATADIR=~/.hub-simulated/data
export IRIS_AGENT_HUBV2_TMPDIR=~/.hub-simulated/tmp
 
# By default the hub does not allow the hub bridge to resolve to a
# non-routable IP address. Since we are connecting to a non-routable
# local IP we need to disable that behavior.
export IRIS_AGENT_GATEWAY_ALLOW_LOCAL=true
 
# Point to a locally running hub bridge and enable higher levels of logging
export IRIS_GATEWAY_URI=wss://localhost:8082/hub/1.0
export IRIS_AGENT_LOGTYPE=dev
 
# Disable Z-Wave support. If you need to run Z-Wave support in a
# local manner then get a Z-Wave controller USB dongle and update
# ZWAVE_PORT to point at the correct USB modem port.
export ZWAVE_DISABLE=true
export ZWAVE_PORT=/dev/ttyACM0
 
# Disable Zigbee support. If you need to run Zigbee support in a
# local manner then get a Zigbee development adapter and update ZIGBEE_PORT
# to the IP address assigned to the debug adapter.
export ZIGBEE_DISABLE=true
export ZIGBEE_PORT=tcp://192.168.2.218
 
# Disable Sercomm camera support. If you re-enable Sercomm camera
# support but still aren't able to pair your Sercomm device then
# check the IRIS_AGENT_UPNP_IFACES setting below.
# export SERCOMM_DISABLE=true
 
# Disable 4G backup support. If you need to run 4G backup support in a
# local mannger then get a 4G backup dongle from the hub technical lead
# and ask for assistance in setting up the backup settings.
export FOURG_DISABLE=true
 
# This controls the network interfaces that the
# hub will use for UPNP device discovery. If you
# have a Sercomm device or Hue device you will need
# to ensure that this points to the same interface
# that the Sercomm or Hue device is connected to.
export IRIS_AGENT_UPNP_IFACES=enp38s0
 
../gradlew --no-daemon run

