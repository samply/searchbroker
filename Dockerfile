FROM alpine:latest as extract
RUN apk add --no-cache unzip
ADD target/searchbroker*.war /searchbroker/searchbroker.war
RUN mkdir -p /searchbroker/extracted && \
       unzip /searchbroker/searchbroker.war -d /searchbroker/extracted/

ARG COMPONENT="searchbroker"
FROM samply/tomcat-common:0.4
MAINTAINER t.brenner@dkfz-heidelberg.de
LABEL mainzelliste.version=$SOURCE_BRANCH mainzelliste.commit=$SOURCE_COMMIT
### Values for docker.common that shouldn't be modified
ENV MANDATORY_VARIABLES="" COMPONENT="searchbroker"
### Component specific default values
ENV SEARCHBROKER_POSTGRES_PORT="5432" SEARCHBROKER_MAIL_PORT="25" SEARCHBROKER_MAIL_PROTOCOL="smtp" \
    SEARCHBROKER_FEATURE_CREATE_NEW_SITE="false" LOG_LEVEL="info"

# TODO: Not working, why is this needed?
# RUN apt-get update && apt-get install -y ttf-dejavu && \
#     rm -rf /var/lib/apt/lists/*

ENV JMX_EXPORTER_VERSION=0.3.1 CATALINA_OPTS="${CATALINA_OPTS} -javaagent:/samply/jmx_prometheus_javaagent-0.3.1.jar=9100:/samply/jmx-exporter.yml"
COPY --chown=searchbroker src/docker/jmx-exporter.yml                /samply/jmx-exporter.yml
ADD --chown=searchbroker https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/$JMX_EXPORTER_VERSION/jmx_prometheus_javaagent-$JMX_EXPORTER_VERSION.jar /samply/
