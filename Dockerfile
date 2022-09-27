FROM tomcat:8-jre8-temurin-focal

RUN ["rm", "-fr", "/usr/local/tomcat/webapps"]
ADD target/searchbroker.war                     /usr/local/tomcat/webapps/broker.war

ADD src/docker/context.xml                      /usr/local/tomcat/conf/Catalina/localhost/broker.xml

ADD src/docker/OAuth2Client.xml                 /usr/local/tomcat/conf/
ADD src/docker/samply_common_config.xml         /usr/local/tomcat/conf/
ADD src/docker/mailSending.xml                  /usr/local/tomcat/conf/
ADD src/docker/log4j2.xml                       /usr/local/tomcat/conf/
ADD src/docker/statistic_notification.txt       /usr/local/tomcat/conf/statistic_notification.txt
ADD src/docker/samply.share.broker.conf         /usr/local/tomcat/conf/samply.share.broker.conf
ADD src/docker/features.properties              /usr/local/tomcat/conf/features.properties

ADD src/docker/start.sh                         /samply/
RUN chmod +x                                    /samply/start.sh

RUN chown -R 1001:1001 /samply/ /usr/local/tomcat/

RUN apt-get update && apt-get upgrade -y && \
    apt-get install -y fonts-dejavu && \
    apt-get autoremove -y && apt-get clean \

USER 1001
CMD ["/samply/start.sh"]
