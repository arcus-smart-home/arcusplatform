# Notes about hub

## Certificates

The Arcus hub appears to use mutual TLS with pinned certificates on the hub and a pre-generated certificate on the device. You key filename is based on the device's wired network MAC address, located in /var/volatile/tmp/mfg/keys/. The certificate is also available in /var/volatile/tmp/mfg/certs/ following a similar filename scheme. The certificate is signed by `C=US, ST=NC, L=Mooresville, O=Lowe's Companies, Inc., OU=Iris, CN=Lowe's Iris Hub Signing CA`.

The iris2-system jar file (/data/agent/libs/iris2-system-2.13.22.jar) contains the keystore that the hub uses to determine what it should trust. In theory, putting a new truststore in agent/arcus-system/src/main/resources and placing the respective jar in /data/agent/libs/ would be sufficent to overwrite the system trust store.


