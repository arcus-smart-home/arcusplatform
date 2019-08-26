# Use Debian as the base installation
FROM debian:stretch

# Install the latest updates.
ADD sources.list /etc/apt/

# Setup the default locale to be UTF-8
RUN apt-get update && apt-get install -y locales && rm -rf /var/lib/apt/lists/* \
    && localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8

# Install Java and some utilities
RUN \
    apt-get update && \
    apt-get install -y openjdk-8-jdk-headless \
        procps less tcpdump vim locales && \
    rm -rf /var/lib/apt/lists/*

# Define working directory.
WORKDIR /data

# Define commonly used JAVA_HOME variable
ENV LC_ALL en_US.UTF-8
ENV LANG en_US.UTF-8

RUN \
    groupadd -g 999 arcus && \
    useradd -r -u 999 -g arcus arcus

USER arcus

# Define default command.
CMD ["bash"]
