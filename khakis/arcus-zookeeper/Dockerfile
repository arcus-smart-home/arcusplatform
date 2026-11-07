ARG ZOOKEEPER_VERSION=3.9.6

# Download and unpack Apache Zookeeper in a throwaway stage, so curl and its
# libraries never reach the published image.
# archive.apache.org is slow and sometimes drops connections, so retry every
# kind of failure (wget gives up on a TLS error), abandon a stalled transfer
# and cap each attempt at 20 minutes.
FROM debian:trixie AS download
ARG ZOOKEEPER_VERSION
RUN \
    apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl && \
    rm -rf /var/lib/apt/lists/*
RUN \
    curl -fL --no-progress-meter --retry 5 --retry-all-errors --retry-delay 30 \
        --connect-timeout 60 --speed-limit 1024 --speed-time 120 --max-time 1200 \
        -o /tmp/zookeeper.tar.gz \
        https://archive.apache.org/dist/zookeeper/zookeeper-${ZOOKEEPER_VERSION}/apache-zookeeper-${ZOOKEEPER_VERSION}-bin.tar.gz && \
    mkdir /dist && \
    tar xfz /tmp/zookeeper.tar.gz -C /dist && \
    mv /dist/apache-zookeeper-${ZOOKEEPER_VERSION}-bin /dist/zookeeper-${ZOOKEEPER_VERSION}

# Use the standard arcus java image
FROM arcus/java
ARG ZOOKEEPER_VERSION

USER root

# Initial system configuration
RUN useradd -M -U -r -s /bin/false zookeeper

# Environment variables for configuration
ENV ZOOKEEPER_VERSION=${ZOOKEEPER_VERSION}

# Install the required version of Apache Zookeeper.
COPY --from=download /dist/zookeeper-${ZOOKEEPER_VERSION} /opt/zookeeper-${ZOOKEEPER_VERSION}
RUN ln -s /opt/zookeeper-${ZOOKEEPER_VERSION} /opt/zookeeper

# Add Apache Zookeeper control script
ADD zookeeper-cmd /usr/bin/

# Export useful environment variables
ENV ZOOKEEPER_HOME /opt/zookeeper-${ZOOKEEPER_VERSION}

# Define working directory.
WORKDIR /data

# Expose the service ports
EXPOSE 2181 2888 3888

# Set the entry point as "zookeeper-cmd init"
ENTRYPOINT ["/usr/bin/zookeeper-cmd", "entry"]

# Define default command.
CMD ["/usr/bin/zookeeper-cmd", "start"]
