ARG ZOOKEEPER_VERSION=3.9.6

# Download and unpack Apache Zookeeper in a throwaway stage, so curl and its
# libraries never reach the published image.
# archive.apache.org sometimes drops connections, so retry every kind of
# failure (wget gives up on a TLS error). A healthy download runs at several
# MB/s, so anything under 256 KB/s for 90 s counts as a failure and is retried.
# The 90 s windows leave room for the 35-45 s the server can take to answer a
# CI runner, and the 10 minute cap is what a 70 MB file needs at that floor.
FROM debian:trixie AS download
ARG ZOOKEEPER_VERSION
RUN \
    apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl
RUN \
    curl -fL --no-progress-meter --retry 5 --retry-all-errors --retry-delay 30 \
        --connect-timeout 90 --speed-limit 262144 --speed-time 90 --max-time 600 \
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
