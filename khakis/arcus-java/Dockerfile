# Use Debian as the base installation
FROM debian:bookworm
ARG JAVA_VERSION=21

# Install the latest updates.
ADD sources.list /etc/apt/

# Setup the default locale to be UTF-8
RUN apt-get update && apt-get install -y --no-install-recommends locales ca-certificates wget && rm -rf /var/lib/apt/lists/* \
    && localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8

# Install Java and some utilities
RUN \
    mkdir -p /etc/apt/keyrings && \
    wget -qO /etc/apt/keyrings/adoptium.asc https://packages.adoptium.net/artifactory/api/gpg/key/public && \
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb bookworm main" > /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && \
    apt-get install -y --no-install-recommends temurin-${JAVA_VERSION}-jre \
        procps less locales && \
    apt-get remove -y wget && \
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
