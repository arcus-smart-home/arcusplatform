# Use the standard eyeris java image
FROM eyeris/java

# Initial system configuration
RUN \
    apt-get update && \
    apt-get install -y wget && \
    useradd -M -U -r -s /bin/false kafka && \
    rm -rf /var/lib/apt/lists/*

# Environment variables for configuration
ENV KAFKA_SCALA_VERSION 2.11
ENV KAFKA_VERSION 0.10.0.1

# Download and install the required version of Apache Kafka. 
RUN \
    wget https://archive.apache.org/dist/kafka/${KAFKA_VERSION}/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION}.tgz -O /tmp/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION}.tgz && \
    tar xfz /tmp/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION}.tgz -C /opt && \
    ln -s /opt/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION} /opt/kafka && \
    rm /tmp/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION}.tgz

# Add Apache Kafka control script and debug utilities
ADD kafka-cmd /usr/bin/kafka-cmd 
ADD kafka-provision /usr/bin/kafka-provision
ADD kafka-operations-provision /usr/bin/kafka-operations-provision
ADD kafka-console-consumer /usr/bin/
ADD kafka-console-producer /usr/bin/

# Define working directory.
WORKDIR /data

# Export Apache Kafka environment variables
ENV KAFKA_HOME /opt/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION}

# Expose the Apache Kafka port to the outside
EXPOSE 9092

# Set the entry point as "kafka-cmd init"
ENTRYPOINT ["/usr/bin/kafka-cmd", "entry"]

# Define default command.
CMD ["/usr/bin/kafka-cmd", "start"]
