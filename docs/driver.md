# Writing your own drivers in Arcus

# Tips

* First work on figuring out how your device identifies. What manufacture, device, and other identifying information is available?
* Check the driver-services log to see how the device identifies to the platform.
* Make sure you know how to quickly pair/reset your device, as you will need to pair the device in order to test device matching.
* Use logging to see what messages the device is sending.

# IPCD Specific Notes

IPCD Drivers also need to be registered manually in addition to dropping the .driver file. This can be done in `IpcdDeviceTypeRegistry`.

# Testing

In order to test your changes, you will need to deploy the following services:

* client-bridge (to pick up new product catalog)
* subsystem-service (in order to start the pairing process, otherwise you may see empty prompts in the UI)
* driver-services (to actually pair/use the device)
* ipcd-bridge (if you are writing an IPCD driver)

If you experience any issues pairing, check subsystem-service and and driver-services. Once you have the device paired, you can generally just look at the driver-services log.

For local development, if you run driver-services outside of docker you the resulting driver will be reloaded. This can greatly improve the effectiveness of development time.