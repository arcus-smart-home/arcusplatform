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

### Java

I believe only Java 1.8 works. I had luck with java-8-openjdk-amd64.

## General process

```
$ cd # go to your home dir
$ https://github.com/wl-net/arcusplatform.git
$ cd arcusplatform
$ ./gradlew runDocker
$ ./gradlew startService
```

Other jobs can be listed with

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

### Does it work?

You can use oculus (`./gradlew :tools:oculus:run`) to login, assuming that client-bridge and the other critical services are running. From there you can setup your account and look around at the system.

### Pairing a hub

This is more difficult than it sounds, because of mtls. You may need to disable TLS and hard code your hub into platform/arcus-hubsession/src/main/java/com/iris/hubcom/server/session/HubClient.java:30 in order to "authenticate" the hub.

## Debugging

`docker ps` and `docker ps -a` are your friends. You'll want to look at the logs for any instances that fail to run to determine what the cause is. Most often this will be a depdendency injection issue (e.g. configuration file is missing an option to inject)

