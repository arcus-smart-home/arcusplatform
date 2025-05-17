# Use Debian as the base installation
FROM debian:bullseye

# Install the latest updates.
ADD sources.list /etc/apt/

# Setup the default locale to be UTF-8
RUN apt-get update && apt-get install -y locales ca-certificates wget gnupg && rm -rf /var/lib/apt/lists/* \
    && localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8

# Install Java and some utilities
RUN \
    wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | apt-key add - && \
    echo "deb https://packages.adoptium.net/artifactory/deb bullseye main " >> /etc/apt/sources.list && \
    apt-get update && \
    apt-get install -y temurin-8-jre \
        procps less tcpdump vim locales && \
    apt-get remove -y gnupg wget && \
    apt-get clean && \
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
