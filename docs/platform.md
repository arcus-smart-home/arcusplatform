# Notes

## Sending requests to subsystem-service

It's relatively straightforward to send messages to another service via PlatformBus, but you need to be very careful that you set isRequest when sending the messsage, or it might not be handled appropriately.
