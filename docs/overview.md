# Quickstart

This page outlines some of the high-level compatibility for Arcus and how to get started with setting it up.

## What's supported

The following classes of devices are supported:

Some examples of what works:

* Iris first generation devices, including Contact Sensors, Motion Sensors, Range Extenders, and Smart Plugs
* Iris second generation devices, including Keyfobs, Contact Sensors, Motion Sensors, and Smart Plugs
* Iris third generation devices, including Contact Sensors, Motion Sensors, and Smart Plugs
* Schlage door locks
* Utilitech and Iris water leak sensors
* Utilitech glass break sensors
* Utilitech sirens
* GE/Jasco Z-Wave Switches

### Known to not work

* Honeywell wifi thermostats
* Phillips Hue
* Nest
* Sercomm cameras

### Needs confirmation

* Alexa
* Google Assistant

## Pre-reqs

* Docker
* Java
* 10GB of RAM, and 25GB of disk space.

### Overall summary of services you need to get running

#### Platform (khakis)
Start by deploying the following:
* arcus/zookeeper - this is required by kafka.
* arcus/kafka - for getting messages between the bridges (e.g. hub-bridge and client-bridge) to the respective message processing services (e.g. subsystem-service).
* arcus/cassandra - used for persistent storage.


#### Platform (services)
The following services are critical to basic user functionality:

* ui-service - shows the arcus web ui. Alternatively, you can run the nodejs server outside of a container.
* client-bridge - hosts the main websocket server that users connect to (via web or mobile client)
* platform-services - used to handle account registration and and other shared tasks
* subsystem-service - used for a lot of functionality, including alarms, door locks, light switches, etc.
* notification-services - (optional) used for sending notifications via APNS, GCM, Twilio, or SendGrid.
* ivr-callback-server (optional) - used for saying a message to the user over the phone. If not configured, you'll get a phone call where the computer will say "an error occurred", and hang up.
* driver-services - handles zigbee/zwave/etc between the bridge and other arcus systems.
* rule-service - provides rules
* scheduler-service - provides the ability to schedule services.

If you do not deploy all of these services, the web ui will not be able to load since it ways to responses from all of these services.

### Docker

If you are not familiar with Docker, you should take some time to look up Docker basics first, even if it's just a 15 minute introduction or crash course.

It is recommended that you setup docker in direct lvm mode, see https://docs.docker.com/storage/storagedriver/device-mapper-driver/#configure-direct-lvm-mode-for-production

generally this is just a matter of creating a block device / partition for the system in question that is at least 10-20GB in size

then point /etc/docker/daemon.json at the block device:

```
$ cat /etc/docker/daemon.json 
{
  "storage-driver": "devicemapper",
  "storage-opts": [
    "dm.directlvm_device=/dev/xvdc",
    "dm.thinp_percent=95",
    "dm.thinp_metapercent=1",
    "dm.thinp_autoextend_threshold=80",
    "dm.thinp_autoextend_percent=20",
    "dm.directlvm_device_force=false"
  ]
}
```

Make sure you can run basic commands like `docker ps` before continuing.

### Java

You'll need to install Java (e.g. OpenJDK or AdoptOpenJDK) 8. Versions above 8 are currently not supported. If you choose to install multiple versions of the JDK, you'll need to tell gradle to use the correct version. Please note that when running Arcus in production you'll use the version of java included with the arcus/java container (OpenJDK 8 aka `openjdk-8-jdk-headless` in debian).

Example of working Java version:
```
java -version
openjdk version "1.8.0_232"
OpenJDK Runtime Environment (build 1.8.0_232-8u232-b09-1~deb9u1-b09)
OpenJDK 64-Bit Server VM (build 25.232-b09, mixed mode)
```

Using gradle with a custom version of Java (needed on each gradle command):
```
./gradlew -Dorg.gradle.java.home=/usr/lib/jvm/java-8-openjdk-amd64/
```

## General process

```
$ cd # go to your home dir
$ git clone https://github.com/arcus-smart-home/arcusplatform.git
$ cd arcusplatform
$ ./gradlew :platform:arcus-khakis:startPlatform
$ ./gradlew startService
```

Any known issues are documented as GitHub issues on https://github.com/arcus-smart-home/arcusplatform/issues

Other gradle jobs can be listed with

```
$ ./gradlew jobs
```

You'll need to get at least the following services up to reach a usable system:

* arcus/kafka
* arcus/zookeeper
* arcus/cassandra
* arcus/hub-bridge
* arcus/client-bridge
* arcus/driver-services
* arcus/subsystem-service
* arcus/rule-service
* arcus/platform-services
* arcus/scheduler-service

If any of these aren't able to run and continue to run (e.g. uptime of over a minute), you'll need to troubleshoot further.

### Configuration

You can either set the configuration through environment variables (e.g. foo.bar= becomes FOO_BAR=), or directly in the properties files (discouraged). You will need to start the Docker container with the appropriate environment variables, ideally through some management software, like Kubernetes.

### Generating an AES key

Arcus expects AES keys to be in base64. the following command can be used to generate credentials:

`openssl rand -base64 32`

#### Setup CORS for arcusweb

CORS (Cross Origin Resource Sharing) relaxes the Same Origin Policy to allow cross-origin requests with special headers (e.g. as needed for WebSockets), and retrieval of files cross-origin. You should not set CORS to "\*", but rather set it to the sites that need to have access to the client-bridge, e.g. your webui

This is enforced by platform/bridge-common/src/main/java/com/iris/netty/server/netty/IrisNettyCorsConfig.java

Set "cors.origins" in ./platform/arcus-containers/client-bridge/src/dist/conf/client-bridge.properties



### Does it work?

You can use oculus (`./gradlew :tools:oculus:run`) to login, assuming that client-bridge and the other critical services are running. From there you can setup your account and look around at the system.

### Pairing a hub

This is more difficult than it sounds, because of mtls. You need to get a certificate issued for your site and add the root ca for your certificate into the java keystore on the hub.
Otherwise, you may need to disable TLS and hard code your hub into platform/arcus-hubsession/src/main/java/com/iris/hubcom/server/session/HubClient.java:30 in order to "authenticate" the hub. The latter of these approaches is obviously not recommended.

If you decide to use LetsEncrypt as your CA, then you can easily replace the trust store on your device with the one in this jar file, which will trust LetEncrypt (both the DST and ISRG roots). Execute the following on your hub (a

```
# md5sum  /data/agent/libs/iris2-hub-controller-2.13.22.jar
276ed29f2ff79eb99f6bab0072621f7b  /data/agent/libs/iris2-hub-controller-2.13.22.jar
```
If you see a different version, you will need to update your hub first. Instructions on how to do so are TBD.

```
# curl https://tools.arcus.wl-net.net/files/iris2-system-2.13.22.jar --output /data/agent/libs/iris2-system-2.13.22.jar 
```

Now follow the instructions in tools/hubdebug and set `IRIS_GATEWAY_URI` to your server. Assuming you did this correctly, your hub should start blinking a green light, instead of green & red lights.

## Debugging

`docker ps` and `docker ps -a` are your friends. You'll want to look at the logs for any instances that fail to run to determine what the cause is. Most often this will be a depdendency injection issue (e.g. configuration file is missing an option to inject)

