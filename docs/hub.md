# Notes on the IRIS Hub

# Hubs

There are several generations of Hubs

* AlertMe Hub (unsupported) - this had minimal resources and is not capable of running Arcus.
* Iris v2 hub (IH200) / Centralite - this is the most common hub, and was shipped with the Iris Pro Monitoring Kit
* Iris v3 hub (IH300) / GreatStar - this was the final hub, shipped with the final "Safe and Secure" kit.

## Certificates

The Arcus hub uses mutual TLS with pinned certificates on the hub and a pre-generated certificate on the device. The key filename is based on the device's wired network MAC address, located in /var/volatile/tmp/mfg/keys/. The certificate is also available in /var/volatile/tmp/mfg/certs/ following a similar filename scheme. The certificate is signed by `C=US, ST=NC, L=Mooresville, O=Lowe's Companies, Inc., OU=Iris, CN=Lowe's Iris Hub Signing CA`.

The iris2-system jar file (/data/agent/libs/iris2-system-2.13.22.jar) contains the keystore that the hub uses to determine what it should trust. Arcus ships with a truststore that supports LetsEncrypt via the ISRG and DST roots. The trust store can be changed by updating truststore.jks in agent/arcus-system/src/main/resources and placing the respective jar in /data/agent/libs/.

To change the system trust store, simply generate a new java keystore with your root certificate(s) of choice, and add it to the jar file. You will need to scp the jar file off of the hub and back onto the hub once complete since the hub does not have the `jar` utility installed.

### Copying the new trust store

Either replace the existing iris2-system jar, or delete it and upload arcus-system-x.x.x.jar
```
scp agent/arcus-system/build/libs/arcus-system-*.jar root@172.16.1.128:/data/agent/libs/iris2-system-2.13.22.jar
```

### Editing the existing trust store (not recommended)

Copying the jar file from the Hub to your local system:
```
scp root@172.16.1.121:/data/agent/libs/iris2-system-2.13.22.jar .
```

Updating the trust store in the jar file:
```
$ jar uf iris2-system-2.13.22.jar truststore.jks
```

Replacing the jar file on the hub:
```
$ scp iris2-system-2.13.22.jar  root@172.16.1.121:/data/agent/libs
```

Once complete, reboot the hub or restart the hub agent, and it should use the updated trust store. Please note that if you factory reset the hub, the keystore will be blown away.

### Hub authentication

The hub authenticates using a Hub ID derived from its wired network MAC address. The hub reads its MAC from hardware (in `IrisHalImpl`) and converts it to a hub ID at startup.

#### Hub ID format

Hub IDs follow the pattern `AAA-DDDD` (3 alpha characters, a dash, 4 decimal digits), e.g. `LWW-1107`.

#### MAC to Hub ID algorithm

Implemented in `common/arcus-common/src/main/java/com/iris/util/HubID.java` (with MAC parsing in `MACAddress.java`):

1. Parse the 6-byte MAC address into a 48-bit long (accepts `00:16:A2:05:FE:06` or `0016A205FE06` formats)
2. Right-shift by 1 bit (`mac >> 1`)
3. Extract the 4-digit numeric suffix: `shifted % 10000`, zero-padded
4. Divide the remainder by 10000, then extract 3 alpha characters by repeatedly taking `remainder % 23` and indexing into the alphabet `ABCDEFGHJKLNPQRSTUVWXYZ` (23 characters — `I`, `M`, `O` are excluded to avoid visual confusion with `1`, `N`, `0`)

#### Examples

| MAC Address          | Hub ID     |
|----------------------|------------|
| `00:16:A2:05:FE:06`  | `LWW-1107` |
| `A0:19:B2:B0:00:00`  | `HFH-2672` |

#### Notes

- The mapping is one-way and deterministic but **not reversible** — the right-shift discards 1 bit, so two adjacent MAC addresses can produce the same hub ID.
- The key filename for hub certificates (in `/var/volatile/tmp/mfg/keys/`) is also based on the MAC address.

## Iris Hub specific things

There are several tty devices in /dev/ttyO\*. The mapping is as follows:

ttyO0: console
ttyO1: ZWave
ttyO2: ZigBee
ttyO3: ???

```
44e09000.serial: ttyO0 at MMIO 0x44e09000 (irq = 158, base_baud = 3000000) is a OMAP UART0
console [ttyO0] enabled
48022000.serial: ttyO1 at MMIO 0x48022000 (irq = 159, base_baud = 3000000) is a OMAP UART1
48024000.serial: ttyO2 at MMIO 0x48024000 (irq = 160, base_baud = 3000000) is a OMAP UART2
481a8000.serial: ttyO4 at MMIO 0x481a8000 (irq = 161, base_baud = 3000000) is a OMAP UART4
```

## Firmware Update Log

```
/usr/bin/update -f 'file:///data/iris/data/tmp/hubOS.bin'
Downloading file...done
Firmware version is: v2.2.0.009
Firmware model is: IH200
Firmware customer is: ALL
Decrypting firmware file...Done.

Firmware image validation passed!
Unpacking firmware update archive...
Verifying file checksums...
MLO-beaglebone: OK
ble-firmware-hwflow.bin: OK
ble-firmware.bin: OK
core-image-minimal-iris-beaglebone.squashfs: OK
u-boot-beaglebone.img: OK
uImage-am335x-boneblack.dtb: OK
uImage-beaglebone.bin: OK
zigbee-firmware-hwflow.bin: OK
zigbee-firmware.bin: OK
zwave-firmware.bin: OK
Mounting kernel partitions...
Bootindex1 = 2
Bootindex2 = 1
Installing to second update partition.
Installing u-boot files...
Installing root filesystem...
Verifying root filesystem...
Zigbee radio hardware supports hardware flow control
diff: can't stat '/data/firmware/zigbee-firmware-hwflow.bin': No such file or directory
Installing Zigbee firmware...
Running test image with CLI...
Resetting Zigbee
Waiting for receiver ping... done.
Sending zigbee-firmware-hwflow.bin 
  25 / 1137 [   2% ] chunks sent, file is 145536 bytesBLE radio hardware supports hardware flow control

Firmware update was successful - please reboot to run latest firmware.
```
