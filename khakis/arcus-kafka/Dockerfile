ARG KAFKA_SCALA_VERSION=2.12
ARG KAFKA_VERSION=2.8.2

# Download and unpack Apache Kafka in a throwaway stage, so curl and its
# libraries never reach the published image.
# archive.apache.org is slow and sometimes drops connections, so retry every
# kind of failure (wget gives up on a TLS error), abandon a stalled transfer
# and cap each attempt at 20 minutes.
FROM debian:trixie AS download
ARG KAFKA_SCALA_VERSION
ARG KAFKA_VERSION
RUN \
    apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl && \
    rm -rf /var/lib/apt/lists/*
RUN \
    curl -fL --no-progress-meter --retry 5 --retry-all-errors --retry-delay 30 \
        --connect-timeout 60 --speed-limit 1024 --speed-time 120 --max-time 1200 \
        -o /tmp/kafka.tgz \
        https://archive.apache.org/dist/kafka/${KAFKA_VERSION}/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION}.tgz && \
    mkdir /dist && \
    tar xfz /tmp/kafka.tgz -C /dist

# Kafka 2.8 only supports JDK 8/11; use a pinned base image
FROM arcus/java:jdk11
ARG KAFKA_SCALA_VERSION
ARG KAFKA_VERSION

USER root

# Initial system configuration
RUN useradd -M -U -r -s /bin/false kafka

# Environment variables for configuration
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
