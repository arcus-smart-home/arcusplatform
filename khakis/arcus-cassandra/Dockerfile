# Use the standard arcus java image
FROM arcus/java

USER root

# Initial system configuration
#  python3 is required to run cqlsh
#  unzip is required to unzip the modelmanager jar
RUN \
    apt-get update && \
    apt-get install -y \
      wget \
      python3 \
      dnsutils \
      unzip && \
    rm -rf /var/lib/apt/lists/*

# Environment variables for configuration
ENV CASSANDRA_VERSION 4.0.15

# Download and install the required version of Apache Cassandra.
RUN \
    wget https://archive.apache.org/dist/cassandra/${CASSANDRA_VERSION}/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz -O /tmp/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz && \
    tar xfz /tmp/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz -C /opt && \
    ln -s /opt/apache-cassandra-${CASSANDRA_VERSION} /opt/cassandra && \
    rm /tmp/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz && \
    ln -s /opt/cassandra/bin/cqlsh /usr/bin && \
    ln -s /opt/cassandra/bin/nodetool /usr/bin

# Add control script and logging configuration
ADD cassandra-cmd /usr/bin/
ADD cassandra-provision /usr/bin/
ADD logback.xml /opt/apache-cassandra-${CASSANDRA_VERSION}/conf/logback.xml

# Define working directory.
WORKDIR /data
ENV CASSANDRA_HOME /opt/apache-cassandra-${CASSANDRA_VERSION}

# Expose Cassandra ports
# 7000: intra-node communication
# 7001: TLS intra-node communication
# 7199: JMX
# 9042: CQL
EXPOSE 7000 7001 7199 9042

# Set the entry point as "cassandra-cmd entry"
ENTRYPOINT ["/usr/bin/cassandra-cmd", "entry"]

# Define default command.
CMD ["/usr/bin/cassandra-cmd", "start"]
