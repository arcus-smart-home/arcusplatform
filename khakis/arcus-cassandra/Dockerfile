# Cassandra 4.0 only supports JDK 8/11; use a pinned base image
FROM arcus/java:jdk11

USER root

# Initial system configuration
#  python3 is required to run cqlsh; python3-six and python3-pyasyncore
#    replace modules that Python 3.12+ broke or removed (see below)
#  unzip is required to unzip the modelmanager jar
#  gosu is used to drop privileges at runtime
RUN \
    apt-get update && \
    apt-get install -y --no-install-recommends \
      wget \
      python3 \
      python3-six \
      python3-pyasyncore \
      dnsutils \
      unzip \
      gosu && \
    rm -rf /var/lib/apt/lists/*

# Environment variables for configuration
ENV CASSANDRA_VERSION 4.0.21

# Download and install the required version of Apache Cassandra.
# The six 1.12 bundled with cqlsh breaks on Python 3.12+, so drop it in favor of python3-six.
RUN \
    wget https://archive.apache.org/dist/cassandra/${CASSANDRA_VERSION}/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz -O /tmp/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz && \
    tar xfz /tmp/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz -C /opt && \
    ln -s /opt/apache-cassandra-${CASSANDRA_VERSION} /opt/cassandra && \
    rm /tmp/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz && \
    ln -s /opt/cassandra/bin/cqlsh /usr/bin && \
    ln -s /opt/cassandra/bin/nodetool /usr/bin && \
    rm /opt/cassandra/lib/six-*.zip

# Add control script and logging configuration
ADD cassandra-cmd /usr/bin/
ADD cassandra-provision /usr/bin/
ADD logback.xml /opt/apache-cassandra-${CASSANDRA_VERSION}/conf/logback.xml

# Define working directory.
WORKDIR /data
ENV CASSANDRA_HOME /opt/apache-cassandra-${CASSANDRA_VERSION}

# Point logs at a fixed path so compose can mount tmpfs without hardcoding the version
RUN rm -rf /opt/apache-cassandra-${CASSANDRA_VERSION}/logs && \
    mkdir -p /var/log/cassandra && \
    ln -s /var/log/cassandra /opt/apache-cassandra-${CASSANDRA_VERSION}/logs

# Own the install and data dirs so we can run as non-root
RUN chown -R arcus:arcus /opt/apache-cassandra-${CASSANDRA_VERSION} /var/log/cassandra /data

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
