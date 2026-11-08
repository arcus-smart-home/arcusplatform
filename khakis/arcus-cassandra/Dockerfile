ARG CASSANDRA_VERSION=4.0.21

# Download and unpack Apache Cassandra in a throwaway stage, so curl and its
# libraries never reach the published image.
# archive.apache.org sometimes drops connections, so retry every kind of
# failure (wget gives up on a TLS error). A healthy download runs at several
# MB/s, so anything under 256 KB/s for 90 s counts as a failure and is retried.
# The 90 s windows leave room for the 35-45 s the server can take to answer a
# CI runner. At that floor Kafka's 72 MB tarball, the largest, takes 274 s, so
# each attempt gets 7 minutes and no retry starts after 15, which bounds a
# failing download to about 22 minutes.
# The six 1.12 bundled with cqlsh breaks on Python 3.12+, so drop it in favor of python3-six.
FROM debian:trixie AS download
RUN \
    apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl
# Declared after the apt step so a version bump doesn't rerun it.
ARG CASSANDRA_VERSION
RUN \
    curl -fL --no-progress-meter --retry 5 --retry-all-errors --retry-delay 30 \
        --retry-max-time 900 --connect-timeout 90 \
        --speed-limit 262144 --speed-time 90 --max-time 420 \
        -o /tmp/cassandra.tar.gz \
        https://archive.apache.org/dist/cassandra/${CASSANDRA_VERSION}/apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz && \
    mkdir /dist && \
    tar xfz /tmp/cassandra.tar.gz -C /dist && \
    rm /dist/apache-cassandra-${CASSANDRA_VERSION}/lib/six-*.zip

# Cassandra 4.0 only supports JDK 8/11; use a pinned base image
FROM arcus/java:jdk11

USER root

# Initial system configuration
#  python3 is required to run cqlsh; python3-six and python3-pyasyncore
#    replace modules that Python 3.12+ broke or removed (see above)
#  unzip is required to unzip the modelmanager jar
#  gosu is used to drop privileges at runtime
RUN \
    apt-get update && \
    apt-get install -y --no-install-recommends \
      python3 \
      python3-six \
      python3-pyasyncore \
      dnsutils \
      unzip \
      gosu && \
    rm -rf /var/lib/apt/lists/*

# Environment variables for configuration
ARG CASSANDRA_VERSION
ENV CASSANDRA_VERSION=${CASSANDRA_VERSION}

# Install the required version of Apache Cassandra, owned by arcus so it can
# run as non-root. Copying it in already owned means no later chown -R, which
# would store a second copy of the whole install in its own layer.
COPY --from=download --chown=arcus:arcus /dist/apache-cassandra-${CASSANDRA_VERSION} /opt/apache-cassandra-${CASSANDRA_VERSION}
RUN \
    ln -s /opt/apache-cassandra-${CASSANDRA_VERSION} /opt/cassandra && \
    ln -s /opt/cassandra/bin/cqlsh /usr/bin && \
    ln -s /opt/cassandra/bin/nodetool /usr/bin

# Add control script and logging configuration
ADD cassandra-cmd /usr/bin/
ADD cassandra-provision /usr/bin/
ADD --chown=arcus:arcus logback.xml /opt/apache-cassandra-${CASSANDRA_VERSION}/conf/logback.xml

# Define working directory.
WORKDIR /data
ENV CASSANDRA_HOME /opt/apache-cassandra-${CASSANDRA_VERSION}

# Point logs at a fixed path so compose can mount tmpfs without hardcoding the version
RUN rm -rf /opt/apache-cassandra-${CASSANDRA_VERSION}/logs && \
    mkdir -p /var/log/cassandra && \
    ln -s /var/log/cassandra /opt/apache-cassandra-${CASSANDRA_VERSION}/logs

# Own the log and data dirs so we can run as non-root
RUN chown -R arcus:arcus /var/log/cassandra /data && \
    chown -h arcus:arcus /opt/apache-cassandra-${CASSANDRA_VERSION}/logs

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
