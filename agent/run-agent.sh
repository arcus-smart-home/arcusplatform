#!/bin/bash

# This tells the agent code that we aren't a real hub so it should
# expect some hub specific things to be missing (e.g. LEDs, Buzzer, etc.)
export IRIS_AGENT_HUBV2_FAKE=true
export IRIS_AGENT_HUBV2_DATADIR=~/.hub-simulated/data
export IRIS_AGENT_HUBV2_TMPDIR=~/.hub-simulated/tmp

# Set up simulated hub manufacturing files if they don't exist yet.
# These are required by the hub-v2 HAL even in fake mode.
HUB_SIMULATED_DIR="$HOME/.hub-simulated"
MFG_DIR="$HUB_SIMULATED_DIR/tmp/mfg"

if [ ! -f "$MFG_DIR/config/hubID" ]; then
   echo "Setting up simulated hub files in $HUB_SIMULATED_DIR..."
   mkdir -p "$MFG_DIR/config" "$MFG_DIR/certs" "$MFG_DIR/keys"
   mkdir -p "$HUB_SIMULATED_DIR/data/iris/data" "$HUB_SIMULATED_DIR/data/iris/db"

   # Hub identity - override HUB_ID and HUB_MAC before running if desired
   HUB_ID="${HUB_ID:-SIM-0001}"
   HUB_MAC="${HUB_MAC:-00:11:22:33:44:55}"

   printf '%s' "$HUB_ID"       > "$MFG_DIR/config/hubID"
   printf '%s' "$HUB_MAC"      > "$MFG_DIR/config/macAddr1"
   printf '%s' ""               > "$MFG_DIR/config/batchNo"
   printf '%s' "IH200"          > "$MFG_DIR/config/model"
   printf '%s' "Arcus"          > "$MFG_DIR/config/customer"
   printf '%s' "2.0.0"          > "$MFG_DIR/config/hwVer"
   printf '%s' "4000000000"     > "$MFG_DIR/config/hwFlashSize"
   printf '%s' "2.0.0"          > "$HUB_SIMULATED_DIR/tmp/version"

   # Generate a self-signed hub client certificate.
   # The hub-bridge extracts the hub MAC from the CN field (ih200-XX:XX:XX:XX:XX:XX).
   openssl req -x509 -newkey rsa:2048 \
      -keyout "$MFG_DIR/keys/${HUB_MAC}.key" \
      -out "$MFG_DIR/certs/${HUB_MAC}.crt" \
      -days 365 -nodes \
      -subj "/CN=ih200-${HUB_MAC}" 2>/dev/null

   echo "Simulated hub ready: id=$HUB_ID mac=$HUB_MAC"
fi

# By default the hub does not allow the hub bridge to resolve to a
# non-routable IP address. Since we are connecting to a non-routable
# local IP we need to disable that behavior.
export IRIS_AGENT_GATEWAY_ALLOW_LOCAL=true

# Point to hub bridge. Override IRIS_GATEWAY_URI in your environment
# before running this script to connect to a different server, e.g.:
#   export IRIS_GATEWAY_URI=wss://hub.dev.arcus.wl-net.net/hub/1.0
export IRIS_GATEWAY_URI="${IRIS_GATEWAY_URI:-wss://localhost:8082/hub/1.0}"
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
