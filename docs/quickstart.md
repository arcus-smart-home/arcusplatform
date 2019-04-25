# Quickstar

## Pre-reqs

* Docker
* Java

### Docker
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

I believe this is only used for gradle, so maybe the version doesn't matter too much. I had luck with `java-8-openjdk-amd64` in Ubuntu 18.04.

## General process

```
$ cd # go to your home dir
$ git clone https://github.com/wl-net/arcusplatform.git
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

* eyeris/kafka
* eyeris/zookeeper
* eyeris/cassandra
* eyeris/hub-bridge
* eyeris/client-bridge
* eyeris/driver-services
* eyeris/subsystem-service
* eyeris/rule-service
* eyeris/platform-services
* eyeris/scheduler-service

If any of these aren't able to run and continue to run (e.g. uptime of over a minute), you'll need to troubleshoot further.

### Configuration

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

## Debugging

`docker ps` and `docker ps -a` are your friends. You'll want to look at the logs for any instances that fail to run to determine what the cause is. Most often this will be a depdendency injection issue (e.g. configuration file is missing an option to inject)

