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

the hub authenticates based on the Hub ID (derived from the mac address), which can be derived with the following:

```
class Main {
 	public static long macToLong(String mac) {
		int length = mac.length();
      if (length == 12) {
         return (hexDigitValue(mac.charAt(0)) << 44L) |
                (hexDigitValue(mac.charAt(1)) << 40L) |
                (hexDigitValue(mac.charAt(2)) << 36L) |
                (hexDigitValue(mac.charAt(3)) << 32L) |
                (hexDigitValue(mac.charAt(4)) << 28L) |
                (hexDigitValue(mac.charAt(5)) << 24L) |
                (hexDigitValue(mac.charAt(6)) << 20L) |
                (hexDigitValue(mac.charAt(7)) << 16L) |
                (hexDigitValue(mac.charAt(8)) << 12L) |
                (hexDigitValue(mac.charAt(9)) <<  8L) |
                (hexDigitValue(mac.charAt(10)) << 4L) |
                hexDigitValue(mac.charAt(11));
      }

      if (length == 17) {
         return (hexDigitValue(mac.charAt(0)) << 44L) |
                (hexDigitValue(mac.charAt(1)) << 40L) |
                (hexDigitValue(mac.charAt(3)) << 36L) |
                (hexDigitValue(mac.charAt(4)) << 32L) |
                (hexDigitValue(mac.charAt(6)) << 28L) |
                (hexDigitValue(mac.charAt(7)) << 24L) |
                (hexDigitValue(mac.charAt(9)) << 20L) |
                (hexDigitValue(mac.charAt(10)) << 16L) |
                (hexDigitValue(mac.charAt(12)) << 12L) |
                (hexDigitValue(mac.charAt(13)) <<  8L) |
                (hexDigitValue(mac.charAt(15)) << 4L) |
                hexDigitValue(mac.charAt(16));
      }

      throw new RuntimeException("invalid mac format: " + mac);
   }

    private static long hexDigitValue(char ch) {
       switch (ch) {
       case '0': return 0;
       case '1': return 1;
       case '2': return 2;
       case '3': return 3;
       case '4': return 4;
       case '5': return 5;
       case '6': return 6;
       case '7': return 7;
       case '8': return 8;
       case '9': return 9;
       case 'a': case 'A': return 10;
       case 'b': case 'B': return 11;
       case 'c': case 'C': return 12;
       case 'd': case 'D': return 13;
       case 'e': case 'E': return 14;
       case 'f': case 'F': return 15;
       default: throw new RuntimeException("not a hex digit: " + ch);
       }
    }

    private static final char[] ALLOWED_CHARS = "ABCDEFGHJKLNPQRSTUVWXYZ".toCharArray();
    private static final long ALLOWED_SIZE = ALLOWED_CHARS.length;

    public static String fromMac(String mac) {
       return fromMac(macToLong(mac));
    }

    public static String fromMac(long mac) {
       long macl = mac >> 1;
       long digits = (macl % 10000L) & 0xFFFF;
       long remainder = (macl / 10000L);
       int index;
       index = (int) (remainder % ALLOWED_SIZE);
       char thd = ALLOWED_CHARS[index];
       remainder = remainder / ALLOWED_SIZE;
       index = (int) (remainder % ALLOWED_SIZE);
       char snd = ALLOWED_CHARS[index];
       remainder = remainder / ALLOWED_SIZE;
       index = (int) (remainder % ALLOWED_SIZE);
       char fst = ALLOWED_CHARS[index];

       StringBuilder bld = new StringBuilder(8);
       bld.append(fst);
       bld.append(snd);
       bld.append(thd);
       bld.append('-');

       if (digits < 10) bld.append("000").append(digits);
       else if (digits < 100) bld.append("00").append(digits);
       else if (digits < 1000) bld.append("0").append(digits);
       else bld.append(digits);

       return bld.toString();
    }


  public static void main(String[] args) {
    System.out.println("Hello world!");
    System.out.println(fromMac("<INSERT MAC>"));
  }
}
```

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
