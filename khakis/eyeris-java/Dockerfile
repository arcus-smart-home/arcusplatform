# Use Debian Jessie as the base installation
FROM debian:stretch

# Install the latest updates.
ADD sources.list /etc/apt/

# Install Java and some utilities
RUN \
    apt-get update && apt-get install -y gnupg && \
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
    echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee /etc/apt/sources.list.d/webupd8team-java.list && \
    echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886 && \

    apt-get update && \
    apt-get install -y openjdk-8-jdk-headless \
        procps less tcpdump vim locales && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /var/cache/oracle-jdk8-installer

# Setup the default locale to be UTF-8
RUN \
    DEBIAN_FRONTEND=noninteractive dpkg-reconfigure locales && \
    echo "en_US.UTF-8 UTF-8" >/etc/locale.gen && \
    locale-gen && \
    /usr/sbin/update-locale LANG=en_US.UTF-8

# Define working directory.
WORKDIR /data

# Define commonly used JAVA_HOME variable
ENV LC_ALL en_US.UTF-8

# Define default command.
CMD ["bash"]
