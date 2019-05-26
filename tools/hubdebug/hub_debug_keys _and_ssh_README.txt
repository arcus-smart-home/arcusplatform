How to use a hub dongle to configure and SSH into an Iris hub
-------------------------------------------------------------

Iris hubs can be accessed via SSH to enable full root level access to your local hub environment for development.

Debug access is enabled by attaching a specially configured USB dongle to the hub's USB port and then rebooting the hub.

============================================
Section 1: Creating and configuring a Dongle
============================================

The USB port on the back of the v2 hub can be used to connect a USB memory stick (formatted as FAT32).

This dongle has three main purposes:

Enable SSH access into the hub regardless of where it is pointed.

Optionally enable the hub to be pointed at a non-production environment (i.e. a locally running platform)

Optionally enable the installation of custom firmware images

To set up a dongle, you will need at a minimum the debug key (see below)

All files are all placed on the root level of the dongle


File Number 1:

Required
(hubID).dbg
Example filename "LWD-2226.dbg"

This is an encrypted debug key. There is a separate key for every hub. With the open sourcing of the Arcus project, hub debug keys for all existing Iris hubs are included in  zip files in this project. You can unzip this archive and find the key(s) for your hub(s)

For information on SSH, see the second section of this document


File Number 2:

Optional if you just want to use SSH but needed if you wish to do development
(hubID).cfg
Example filename "LWD-2226.cfg"

This is a plain text file that contains the information about setting the log level and the pointing to the correct hub bridge. Here are some of the parameters that can be in this file along with comments:


IRIS_GATEWAY_URI = wss://myhubbridge.com:443/hub/1.0
The URL for the hub bridge (local or remote) that you are connecting to, along with the port the hub bridge is running on.

IRIS_AGENT_LOGTYPE = dev
Turns on useful hub local logging

IRIS_AGENT_GATEWAY_ALLOW_LOCAL = true
This is needed if you are connection to a local development environment as opposed to say a cloud platform. NOTE: if this is enabled, you MUST NOT include the IRIS_AGENT_LOGTYPE line above

IRIS_LOGGING_STREAMEND = 1556472271000
IRIS_LOGGING_STREAMLVL = DEBUG
These two together turn on hub log streaming to the platform and sets an end date (in Unix epoch time). These are optional and if you are just exploring your hub, there's no point in including them on until you have a full infrastructure going. 

IRIS_AGENT_REFLEX_LOGGING = y
This turns on extra logging for the reflex drivers running locally on the hub (i.e. contact sensors opening and closing, etc)

Here's a simple example block of configuration for a hub that you might want to use for local development:

IRIS_GATEWAY_URI = wss://10.0.0.4:8082/hub/1.0
IRIS_AGENT_GATEWAY_ALLOW_LOCAL = true
IRIS_AGENT_REFLEX_LOGGING = y


File Number 3:

Optional (used only if you need to install new FW)
Example filename "hubOS.bin"

The third file on the dongle is optional, and that is a firmware image. If a firmware image is present, the hub will use it.
Locate the firmware image you wish to install, copy it to the dongle, and rename it "hubOS.bin"
Plug in the dongle, and reboot the hub to install the firmware, the hub may reboot a second time after installation.

WARNING: installing an invalid FW image may corrupt the hub. There is a recovery partition but this doesn't always work so use at your own risk.



===================================
Section 2: SSHing into the Iris hub
===================================

To enable SSH access into the Iris hub, you just need the debug file on the USB stick. 

1. Find the correct debug key for your Hub ID, and copy it onto the USB stick
2. (optional) add a .cfg file with the configuration options set as desired
3. Attach the dongle to the USB port ont he hub and reboot the hub
4. Wait a minute or so
5. Determine the hub's IP address by looking at the DHCP table for your local router
6. Using any SSH client such as a Mac or Linux command line, type

ssh root@10.0.0.4

replacing "10.0.0.4" with the hub's actual IP

Enter the correct password...

v2 hubs:
kz58!~Eb.RZ?+bqb

v3 hubs:
zm{[*f6gB5X($]R9

Most configuration data is stored in the /data directory, including the hub's local sqlite3 database at /data/iris/db/iris.db

A running agent can be killed with the commands
killall -9 irisagentd
killall -9 java

and restarted with 
/etc/init.d/irisagent











