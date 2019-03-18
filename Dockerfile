ARG IRITYPE=cliri

FROM iotacafe/maven:3.5.4.oracle8u181.1.webupd8.1.1-1 as local_stage_build
MAINTAINER giorgio@iota.org
ARG IRITYPE
RUN git clone https://github.com/iotaledger/${IRITYPE}.git
WORKDIR /${IRITYPE}
RUN mvn clean package

# execution image
#FROM iotacafe/java:oracle8u181.1.webupd8.1-1
FROM openjdk:jre-slim
ARG IRITYPE
ENV DOCKER_IRI_JAR_PATH=/${IRITYPE}/target/${IRITYPE}*.jar \
    DOCKER_IRI_REMOTE_LIMIT_API="interruptAttachToTangle, attachToTangle, addNeighbors, removeNeighbors, getNeighbors" \
    DOCKER_IRI_MONITORING_API_PORT_DESTINATION=14265 \
    JAVA_OPTIONS="-XX:+UnlockExperimentalVMOptions -XX:+DisableAttachMechanism -XX:InitiatingHeapOccupancyPercent=60 -XX:G1MaxNewSizePercent=75 -XX:MaxGCPauseMillis=10000 -XX:+UseG1GC" \
    JAVA_MIN_MEMORY=2G \
    JAVA_MAX_MEMORY=4G
# Setting this to 1 will have socat exposing 14266 and pointing it on
# localhost. See /entrypoint.sh
# !!! DO NOT DOCKER EXPOSE (-p) 14266 as the remote api settings
#     will not be applied on that port !!!
# You also have to maintain $DOCKER_IRI_MONITORING_API_PORT_DESTINATION
# based on the actual API port exposed via IRI
ENV DOCKER_IRI_MONITORING_API_PORT_ENABLE 0

RUN apt-get update && apt-get install -y jq curl socat --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*
COPY --from=local_stage_build $DOCKER_IRI_JAR_PATH /iri.jar
COPY ./docker/entrypoint.sh /
ENTRYPOINT [ "/entrypoint.sh" ]
