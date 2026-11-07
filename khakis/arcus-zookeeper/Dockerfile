# Use the standard arcus java image
FROM arcus/java

USER root

# Initial system configuration
RUN \
    apt-get update && \
    apt-get install -y curl && \
    useradd -M -U -r -s /bin/false zookeeper && \
    rm -rf /var/lib/apt/lists/*

# Environment variables for configuration
ENV ZOOKEEPER_VERSION 3.9.6

# Download and install the required version of Apache Zookeeper.
# archive.apache.org is slow and sometimes drops connections, so retry every
# kind of failure (wget treats a TLS error as final) and give up on a stall.
RUN \
    curl -fL --no-progress-meter --retry 5 --retry-all-errors --retry-delay 30 \
        --connect-timeout 60 --speed-limit 1024 --speed-time 120 \
        -o /tmp/zookeeper-${ZOOKEEPER_VERSION}.tar.gz \
        https://archive.apache.org/dist/zookeeper/zookeeper-${ZOOKEEPER_VERSION}/apache-zookeeper-${ZOOKEEPER_VERSION}-bin.tar.gz && \
    tar xfz /tmp/zookeeper-${ZOOKEEPER_VERSION}.tar.gz -C /opt && \
    mv /opt/apache-zookeeper-${ZOOKEEPER_VERSION}-bin /opt/zookeeper-${ZOOKEEPER_VERSION} && \
    ln -s /opt/zookeeper-${ZOOKEEPER_VERSION} /opt/zookeeper && \
    rm /tmp/zookeeper-${ZOOKEEPER_VERSION}.tar.gz

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
