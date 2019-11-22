# Based on https://github.com/kairosdb/kairosdb/issues/288#issuecomment-404411656

FROM arcus/java as builder

USER root

ARG KAIROSDB_TAG="v1.2.2"

RUN apt-get update && \
    apt-get install git -y && \
    git clone --single-branch -b ${KAIROSDB_TAG} https://github.com/kairosdb/kairosdb.git && \
    cd kairosdb && \
    export CLASSPATH=tools/tablesaw-1.2.6.jar && \
    java make package && \
    mkdir -p /build && \
    tar -xzf ./build/kairosdb*.tar.gz -C /build && \
    cd .. && \
    rm -Rf ./kairosdb/ && \
    chown -R arcus:arcus /build/kairosdb

FROM arcus/java

COPY --chown=arcus:arcus --from=0 /build/kairosdb /opt/kairosdb

VOLUME /opt/kairosdb

EXPOSE 4242 8080 2003 2004

COPY kairosdb.properties /opt/kairosdb/conf/kairosdb.properties

WORKDIR /opt/kairosdb/bin

CMD /bin/bash kairosdb.sh run
