# Notes about hub

## Certificates

The Arcus hub appears to use mutual TLS with pinned certificates on the hub and a pre-generated certificate on the device. You key filename is based on the device's wired network MAC address, located in /var/volatile/tmp/mfg/keys/. The certificate is also available in /var/volatile/tmp/mfg/certs/ following a similar filename scheme. The certificate is signed by `C=US, ST=NC, L=Mooresville, O=Lowe's Companies, Inc., OU=Iris, CN=Lowe's Iris Hub Signing CA`.

The iris2-system jar file (/data/agent/libs/iris2-system-2.13.22.jar) contains the keystore that the hub uses to determine what it should trust. In theory, putting a new truststore in agent/arcus-system/src/main/resources and placing the respective jar in /data/agent/libs/ would be sufficent to overwrite the system trust store.

To change the system trust store, simply generate a new java keystore with your root certificate(s) of choice, and add it to the jar file. You will need to scp the jar file off of the hub and back onto the hub once complete since the hub does not have the `jar` utility installed.

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

### Unknowns

I'm not sure how the platform actually authenticates the hub yet, since the certificate on the device doesn't directly relate to the hub id that the user claims.

### Hub authentication

the hubs appear to authenticate based on mac address, and the hub id can be derived with the following:

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
