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

Once complete, reboot the hub or restart the hub agent, and it should use the updated trust store.
