# Use Debian Bookworm with Java 11 for Cassandra 4.x
FROM debian:bookworm

# Setup the default locale to be UTF-8
RUN apt-get update && apt-get install -y locales ca-certificates wget && rm -rf /var/lib/apt/lists/* \
    && localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8

ENV LC_ALL en_US.UTF-8
ENV LANG en_US.UTF-8

# Install Java 11
RUN \
    wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | tee /usr/share/keyrings/adoptium.asc && \
    echo "deb [signed-by=/usr/share/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb bookworm main" > /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && \
    apt-get install -y temurin-11-jre \
        python3 \
        dnsutils \
        unzip \
        procps && \
    useradd -M -U -r -s /bin/false cassandra && \
    apt-get remove -y wget && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Environment variables for configuration
ENV CASSANDRA_VERSION 4.0.15

# Download and install the required version of Apache Cassandra.
RUN \
    apt-get update && apt-get install -y wget && \
    wget https://archive.apache.org/dist/cassandra/${CASSANDRA_VERSION}/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz -O /tmp/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz && \
    tar xfz /tmp/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz -C /opt && \
    ln -s /opt/apache-cassandra-${CASSANDRA_VERSION} /opt/cassandra && \
    rm /tmp/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz && \
    ln -s /opt/cassandra/bin/cqlsh /usr/bin && \
    ln -s /opt/cassandra/bin/nodetool /usr/bin && \
    apt-get remove -y wget && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Add control script
ADD cassandra-cmd /usr/bin/
ADD cassandra-provision /usr/bin/

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
