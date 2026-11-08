ARG KAFKA_SCALA_VERSION=2.12
ARG KAFKA_VERSION=2.8.2

# Download and unpack Apache Kafka in a throwaway stage, so curl and its
# libraries never reach the published image.
# archive.apache.org sometimes drops connections, so retry every kind of
# failure (wget gives up on a TLS error). A healthy download runs at several
# MB/s, so anything under 256 KB/s for 90 s counts as a failure and is retried.
# The 90 s windows leave room for the 35-45 s the server can take to answer a
# CI runner. At that floor Kafka's 72 MB tarball, the largest, takes 274 s, so
# each attempt gets 7 minutes and no retry starts after 15, which bounds a
# failing download to about 22 minutes.
FROM debian:trixie AS download
RUN \
    apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl
# Declared after the apt step so a version bump doesn't rerun it.
ARG KAFKA_SCALA_VERSION
ARG KAFKA_VERSION
RUN \
    curl -fL --no-progress-meter --retry 5 --retry-all-errors --retry-delay 30 \
        --retry-max-time 900 --connect-timeout 90 \
        --speed-limit 262144 --speed-time 90 --max-time 420 \
        -o /tmp/kafka.tgz \
        https://archive.apache.org/dist/kafka/${KAFKA_VERSION}/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION}.tgz && \
    mkdir /dist && \
    tar xfz /tmp/kafka.tgz -C /dist

# Kafka 2.8 only supports JDK 8/11; use a pinned base image
FROM arcus/java:jdk11

USER root

# Initial system configuration
RUN useradd -M -U -r -s /bin/false kafka

# Environment variables for configuration
ARG KAFKA_SCALA_VERSION
ARG KAFKA_VERSION
ENV KAFKA_SCALA_VERSION=${KAFKA_SCALA_VERSION}
ENV KAFKA_VERSION=${KAFKA_VERSION}

# Install the required version of Apache Kafka.
COPY --from=download --chown=kafka:kafka /dist/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION} /opt/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION}
RUN ln -s /opt/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION} /opt/kafka

# Add Apache Kafka control script and debug utilities
ADD kafka-cmd /usr/bin/kafka-cmd
ADD kafka-provision /usr/bin/kafka-provision
ADD kafka-operations-provision /usr/bin/kafka-operations-provision
ADD kafka-console-consumer /usr/bin/
ADD kafka-console-producer /usr/bin/

# Define working directory.
WORKDIR /data
RUN \
    chown -R kafka:kafka /data

# Export Apache Kafka environment variables
ENV KAFKA_HOME /opt/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION}

# Expose the Apache Kafka port to the outside
EXPOSE 9092

USER kafka

# Set the entry point as "kafka-cmd init"
ENTRYPOINT ["/usr/bin/kafka-cmd", "entry"]

# Define default command.
CMD ["/usr/bin/kafka-cmd", "start"]
