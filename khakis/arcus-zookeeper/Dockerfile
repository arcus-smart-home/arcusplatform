ARG ZOOKEEPER_VERSION=3.9.6

# Download and unpack Apache Zookeeper in a throwaway stage, so curl and its
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
ARG ZOOKEEPER_VERSION
RUN \
    curl -fL --no-progress-meter --retry 5 --retry-all-errors --retry-delay 30 \
        --retry-max-time 900 --connect-timeout 90 \
        --speed-limit 262144 --speed-time 90 --max-time 420 \
        -o /tmp/zookeeper.tar.gz \
        https://archive.apache.org/dist/zookeeper/zookeeper-${ZOOKEEPER_VERSION}/apache-zookeeper-${ZOOKEEPER_VERSION}-bin.tar.gz && \
    mkdir /dist && \
    tar xfz /tmp/zookeeper.tar.gz -C /dist && \
    mv /dist/apache-zookeeper-${ZOOKEEPER_VERSION}-bin /dist/zookeeper-${ZOOKEEPER_VERSION}

# Use the standard arcus java image
FROM arcus/java

USER root

# Initial system configuration
RUN useradd -M -U -r -s /bin/false zookeeper

# Environment variables for configuration
ARG ZOOKEEPER_VERSION
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
