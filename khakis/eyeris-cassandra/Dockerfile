# Use the standard eyeris java image
FROM eyeris/java

# Initial system configuration
#  python is required to run cqlsh
#  unzip is required to unzip the modelmanager jar
RUN \
    apt-get update && \
    apt-get install -y \
      wget \
      python \
      dnsutils \
      unzip && \
    useradd -M -U -r -s /bin/false cassandra && \
    rm -rf /var/lib/apt/lists/*

# Environment variables for configuration
ENV CASSANDRA_VERSION 2.2.9

# Download and install the required version of Apache Cassandra. 
RUN \
    wget --no-check-certificate https://archive.apache.org/dist/cassandra/${CASSANDRA_VERSION}/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz -O /tmp/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz && \
    tar xfz /tmp/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz -C /opt && \
    ln -s /opt/apache-cassandra-${CASSANDRA_VERSION} /opt/cassandra && \
    rm /tmp/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz && \
    ln -s /opt/cassandra/bin/cqlsh /usr/bin

# Add control script
ADD cassandra-cmd /usr/bin/
ADD cassandra-provision /usr/bin/
COPY cassandra-env.sh /opt/cassandra/conf/

# TWCS compiled on 4/6/2017 
# http://thelastpickle.com/blog/2017/01/10/twcs-part2.html
# git clone https://github.com/jeffjirsa/twcs/
# git checkout -t origin/cassandra-2.2
# Commit Hash: 19081e2b53b2bc273292c2820efd3b9d05226c53
COPY TimeWindowCompactionStrategy-2.2.5.jar /opt/cassandra/lib/

# Export useful environment variables

# Define working directory.
WORKDIR /data
ENV CASSANDRA_HOME /opt/apache-cassandra-${CASSANDRA_VERSION}

# Export Apache Kafka environment variables
ENV KAFKA_HOME /opt/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION}

# Expose the Apache Kafka port to the outside
# 7000: intra-node communication
# 7001: TLS intra-node communication
# 7199: JMX
# 9042: CQL
# 9160: thrift service
EXPOSE 7000 7001 7199 9042 9160

# Set the entry point as "cassandra-cmd entry"
ENTRYPOINT ["/usr/bin/cassandra-cmd", "entry"]

# Define default command.
CMD ["/usr/bin/cassandra-cmd", "start"]
