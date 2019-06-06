# How to use eye-kat

First, ensure that kafka.eyeris is in /etc/hosts.

`./gradlew :tools:eye-kat:jar`

`./tools/eye-kat/build/scripts/eye-kat -t platform`

You should then see messages from the platform topic
