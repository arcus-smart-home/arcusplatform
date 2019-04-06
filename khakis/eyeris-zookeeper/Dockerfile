# Use the standard eyeris java image
FROM eyeris/java

# Initial system configuration
RUN \
    apt-get update && \
    apt-get install -y wget && \
    useradd -M -U -r -s /bin/false zookeeper && \
    rm -rf /var/lib/apt/lists/*

# Environment variables for configuration
ENV ZOOKEEPER_VERSION 3.4.14

# Download and install the required version of Apache Zookeeper.
RUN \
    wget http://mirrors.ibiblio.org/apache/zookeeper/zookeeper-${ZOOKEEPER_VERSION}/zookeeper-${ZOOKEEPER_VERSION}.tar.gz -O /tmp/zookeeper-${ZOOKEEPER_VERSION}.tar.gz && \
    tar xfz /tmp/zookeeper-${ZOOKEEPER_VERSION}.tar.gz -C /opt && \
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
