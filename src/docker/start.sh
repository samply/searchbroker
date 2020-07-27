#!/usr/bin/env bash

set -e

sed -i "s%{postgres-host}%${POSTGRES_HOST}%"            /usr/local/tomcat/conf/Catalina/localhost/broker.xml
sed -i "s/{postgres-port}/${POSTGRES_PORT:-5432}/"      /usr/local/tomcat/conf/Catalina/localhost/broker.xml
sed -i "s/{postgres-db}/${POSTGRES_DB}/"                /usr/local/tomcat/conf/Catalina/localhost/broker.xml
sed -i "s/{postgres-user}/${POSTGRES_USER}/"            /usr/local/tomcat/conf/Catalina/localhost/broker.xml
sed -i "s/{postgres-pass}/${POSTGRES_PASS}/"            /usr/local/tomcat/conf/Catalina/localhost/broker.xml

sed -i "s%{mail-host}%${MAIL_HOST}%"                    /usr/local/tomcat/conf/mailSending.xml
sed -i "s%{mail-port}%${MAIL_PORT:-25}%"                /usr/local/tomcat/conf/mailSending.xml
sed -i "s%{mail-protocol}%${MAIL_PROTOCOL:-smtp}%"      /usr/local/tomcat/conf/mailSending.xml
sed -i "s%{mail-from-address}%${MAIL_FROM_ADDRESS}%"    /usr/local/tomcat/conf/mailSending.xml
sed -i "s%{mail-from-name}%${MAIL_FROM_NAME}%"          /usr/local/tomcat/conf/mailSending.xml

sed -i "s%{statistics-mails}%${STATISTICS_MAILS}%"      /usr/local/tomcat/conf/statistic_notification.txt

sed -i "s%{auth-host}%${AUTH_HOST}%"                    /usr/local/tomcat/conf/OAuth2Client.xml
sed -i "s%{auth-public-key}%${AUTH_PUBLIC_KEY}%"        /usr/local/tomcat/conf/OAuth2Client.xml
sed -i "s%{auth-client-id}%${AUTH_CLIENT_ID}%"          /usr/local/tomcat/conf/OAuth2Client.xml

sed -i "s/{proxy-host}/${PROXY_HOST}/"                  /usr/local/tomcat/conf/samply_common_config.xml
sed -i "s/{proxy-port}/${PROXY_PORT}/"                  /usr/local/tomcat/conf/samply_common_config.xml

sed -i "s/{icinga-host}/${ICINGA_HOST}/"                /usr/local/tomcat/conf/samply.share.broker.conf
sed -i "s/{icinga-path}/${ICINGA_PATH}/"                /usr/local/tomcat/conf/samply.share.broker.conf
sed -i "s/{icinga-username}/${ICINGA_USERNAME}/"        /usr/local/tomcat/conf/samply.share.broker.conf
sed -i "s/{icinga-password}/${ICINGA_PASSWORD}/"        /usr/local/tomcat/conf/samply.share.broker.conf
sed -i "s/{icinga-site-suffix}/${ICINGA_SITE_SUFFIX}/"  /usr/local/tomcat/conf/samply.share.broker.conf

export CATALINA_OPTS="${CATALINA_OPTS} -javaagent:/samply/jmx_prometheus_javaagent-0.3.1.jar=9100:/samply/jmx-exporter.yml"

# Replace start.sh with catalina.sh
exec /usr/local/tomcat/bin/catalina.sh run
