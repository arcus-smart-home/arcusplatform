# Troubleshooting

## Common Sources of Issues

A number of common Arcus issues step back to a few common underlying causes:

### Gradle Cache

If services try to start without a name or other strange behavior, try clearing the gradle cache. This is especially important after a gradle update.

`./gradlew clean`

### Entropy

Arcus will have difficulty if your system does not have enough entropy. Despite best efforts to use /dev/urandom instead of /dev/random, there are still dependencies on /dev/random. Fortunately, an easy solution is to install `haveged`, an entropy daemon.

### CORS

CORS is generally intended for web browsers, however there are some cases in Arcus where it may apply outside of a browser. If you experience issues getting arcusios or oculus to connect to Arcus Platform where a 4XX status code is returned when correct credentials are provided, ensure that cors.origins includes the full hostname of client-bridge (e.g. `https://client.arcussmarthome.com` and `https://client.arcussmarthome.com:443`).