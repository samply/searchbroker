# Searchbroker

To make samples easily accessible for researcher to help [making new treatments possible.](http://www.bbmri-eric.eu/)

The Searchbroker connects [Searchbroker-UI](https://code.mitro.dkfz.de/projects/SHAR/repos/samply.sample-locator.ui) and [Connector](https://code.mitro.dkfz.de/projects/SHAR/repos/samply.share.client.v2) as part of the [sample-locator-deployment](https://github.com/samply/sample-locator-deployment) or [open-telekom-cloud](https://github.com/samply/open-telekom-cloud).

For APIs see the instance https://samplelocator.test.bbmri.de/broker

## Build

Requirements:

- [Java 8](#java)
- [Database](#database)
- Maven

```
git clone https://code.mitro.dkfz.de/projects/SHAR/repos/samply.share.broker.rest
cd searchbroker
mvn install -Psamply
```



## Run ([Docker](#docker) or [Manual](#manual))

### Docker

    docker network create gba
    
    docker run \
        --rm \
        --name pg-searchbroker \
        --network=gba \
        -e POSTGRES_USER=samply \
        -e POSTGRES_DB=samply.searchbroker \
        -e POSTGRES_PASSWORD=samply \
        -p 5432:5432 \
    postgres:9.6
    
    
    mvn clean install -Psamply
    
    docker build . -t searchbroker:latest
    
    docker run \
        --rm \
        --name=searchbroker \
        --network=gba \
        -p 8080:8080 \
        -e TOMCAT_USERNAME='admin' \
        -e TOMCAT_PASSWORD='ChangeMe' \
        -e POSTGRES_HOST='pg-searchbroker' \
        -e POSTGRES_DB='samply.searchbroker' \
        -e POSTGRES_USER='samply' \
        -e POSTGRES_PASS='samply' \
        -e MAIL_HOST='DUMMY' \
        -e MAIL_FROM_ADDRESS='DUMMY' \
        -e MAIL_FROM_NAME='DUMMY' \
        -e STATISTICS_MAILS='itc@germanbiobanknode.de\\nitb@germanbiobanknode.de' \
        -e AUTH_HOST='https://auth.germanbiobanknode.de' \
        -e AUTH_PUBLIC_KEY='MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA/D51sGPXYNn/2cYYxCB5bP0JMRrWXhn3qmCIwrJCO8VgmsT9DiRJz/EmiP6qOrC3NMYSaELBGgaQeb690mqIMxY/5qkfIbNJYuV7F2gcPQFWxY2neO24XnFXQqsA0PcdlF5lB0HPCYKoqjV1hVtBl9IS8/v8mJ1FMa4oifGD8AqrLEQkItkEK+yg53rbs0sxlEFYp1U4gogmW6MdQ1ZDfCLiL6eWBFWRpHZAzXxfkauoxcccReH6hv7DPkI3ngxxARx8ivcLS+psJOe8RL2LrlS49flbazOWBmG/f3DFdoEcXYcraSnFc9lx7SJK4xsL6mBv6Tc1Qtf0nuAG+3bLICe9M0pE62z9wSVebe4F7htfElSr7MS2EMXX5iW0whe1RrsPojPY12ZEKOL7WGvJTyDOnA2Nzp22p5Ii/wru1uNaD/7xsw4OcMxHaYFi87dJSbsfx1OEXP3Co+zWZ2B1WdV83bvlx7NNHsATYeQuKG7IeBco+oYoXAjOk7IBlc0M6WqOpuXuBNXOGpvPR4aRd0COYXIZd+DqoK3ZLCr7gEYHHeCUx6Y8cKLK4sxbhHjGqusjVEPYdM46txSawNNIhp0LtfDilWWwecYX3N0WIPFElfKL43tIrjVrzsfL7nECsapVByhqBGFZX+mY2gEprBnqDCrVeUELmKiwm+ioQtkCAwEAAQ==' \
        -e AUTH_CLIENT_ID='productive-searchbroker-ui' \
        -e CATALINA_OPTS='"-Xmx2g"' \
    searchbroker:latest

| Envionment variable | Meaning | Default |
|---|---|---|
|POSTGRES_HOST|URL of the Postgres host|
|POSTGRES_PORT|Postgres port|5432
|POSTGRES_DB|Postgres Database|
|POSTGRES_USER|Postgres user|
|POSTGRES_PASS|Postgres password|
|MAIL_HOST|Mail server|
|MAIL_PORT|Port of the mail server|
|MAIL_PROTOCOL|Mail server protocol|smtp
|MAIL_FROM_ADDRESS|Default FROM mail address|
|MAIL_FROM_NAME|Default name for mail address|
|AUTH_HOST|Authentication server url|
|AUTH_PUBLIC_KEY|public key of the authentication server||
|AUTH_CLIENT_ID|client id for the authentication server||
|PROXY_HOST|Proxy url||
|PROXY_PORT|Proxy port||
|BROKER_NAME|name of the searchbroker||
|MDR_URL|MDR url||
|ICINGA_HOST|url of the monitoring server||
|ICINGA_PATH|rest api path of the monitoring server||
|ICINGA_USERNAME|username for the monitoring api authentication||
|ICINGA_PASSWORD|password for the monitoring api authentication||
|ICINGA_SITE_SUFFIX|suffix for grouping the banks in one group||
|ICINGA_PROJECT|under which project the searchbroker runs||
|FEATURE_CREATE_NEW_SITE|if the connector can create new sites|false|
|LOG_LEVEL|tomcat log level|info|
|CATALINA_OPTS|configuration for tomcat||



### Manual

Requirements:

- [Database](#database)
- [Tomcat](#tomcat)
- The Searchbroker webapp as .war file: [build yourselve](#build) or download from release tab of Github



Steps:

- Delete folder ${tomcat.home}/webapps/ROOT.
- Rename .war file to ROOT.war
- Copy ROOT.war to ${tomcat.home}/webapps/ 

Start tomcat by executing ${tomcat.home}/bin/startup.sh (Windows: startup.bat) or by running the tomcat-service if you [created one.](#tomcat-service-for-autostart)



## Environment

### Database

The Open-Source database Postresql 9.6 is used. The database connection uses the connection pool of Tomcat. 

This webapp needs schema '**samply**' in the database '**samply.searchbroker**' under user '**samply**' and password '**samply**' under port `5432`. 

To change these settings during build, search for these values in the **src/pom.xml** and adapt to your needs.
During run, see context.xml (described under [Configurations](#Configurations)).



- Follow installation for port **5432**

  - Windows: https://www.enterprisedb.com/downloads/postgres-postgresql-downloads
  - Linux Mint:

  ```
  sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt/ xenial-pgdg main" > /etc/apt/sources.list.d/postgresql.list'
  
  wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
  
  sudo apt-get install postgresql-9.6
  ```

  ​	Other Linux:

  ```
  sudo add-apt-repository "deb http://apt.postgresql.org/pub/repos/apt/ $(lsb_release -sc)-pgdg main"
  
  wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
  
  sudo apt-get install postgresql-9.6
  ```

- Create database and user:

  - pgAdmin installed: Having Server opened, under "Databases": rightclick on "Login/Group Roles". Select "Create"?"Login/Group Role". Tab Generel: Enter Name. Tab Definition: Enter Password. Tab Privileges: enable "Can Login?" and "Superuser". By creating new Databases, select this user as "Owner"*

  - command line: 

    ```
    (sudo su postgres)
    psql
    CREATE DATABASE "samply.searchbroker";
    CREATE USER samply WITH PASSWORD 'samply';
    GRANT ALL PRIVILEGES ON DATABASE "samply.searchbroker" to samply;
    ```



### Tomcat

Requirements:

- [Java 8](#java)

  

1. Download and unzip: http://mirror.funkfreundelandshut.de/apache/tomcat/tomcat-8/v8.5.38/bin/apache-tomcat-8.5.38.zip (eg. to /opt/tomcat-searchbroker)

2. Change ports: Every webapp has its own tomcat, so change ports for Store-Tomcat in ${tomcat.base}/conf/server.xml:

   ```
   ...
   ...<connector port="8083" protocol="HTTP/1.1" connectionTimeout="20000" redirectPort="8103" />...
   ...
   ...<connector port="8003" protocol="AJP/1.3" redirectPort="8103" /> ...
   ...
   ...<Server port="8203" shutdown="SHUTDOWN">...
   ...
   ```



### Java

Is a dependency of tomcat,

if you install different jre versions on this machine, set jre 8 for tomcat by creating a so called "setenv.sh".

Linux: [OpenJDK](https://openjdk.java.net/install/)

Windows: [Oracle](https://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html) 



### Configurations

These configuration files are used:

```
src/main/java/webapp/WEB-INF/conf/
(log4j2.xml, mailSending.xml, samply.share.broker.conf, samply_common_config.xml, OAuth2Client.xml <- same as UI)

src/main/java/webapp/META-INF/
(context.xml)
```

The context.xml will be auto-copied by tomcat at startup to ${tomcat.base}/conf/Catalina/localhost/ROOT.xml.
This file will not be overwritten by updating the WAR file due to tomcat settings.

All files under WEB-INF/conf will always be found from FileFinder as ultimate fallback.

If you want to save your configurations, copy all files under WEB-INF/conf (tomcat or code source) to ${tomcat.base}/conf.

According to the predefinded log4j2.xml, all logs can be found in ${tomcat.base}/logs/searchbroker.

**IntelliJ** creates a *tomcat.base* directory for every startup of the application. So save your configuration files to *tomcat.home* and it will copy these files and logs every time to *tomcat.base*. You will see the paths at startup in the first lines of the console output.

To use a **proxy**, set your url in file **samply_common_config.xml**.


### Productive Settings

#### Tomcat service for autostart

​	Linux:

​		Remember path of output:

```
sudo update-java-alternatives -l

```

​		Create new service file:

```
sudo nano /etc/systemd/system/tomcat-searchbroker.service

```

​		Copy the remembered path to JAVA_HOME and add `/jre` to the end of this path, also check 		tomcat path:

```
[Unit]
Description=Apache Tomcat Web Application Container
After=network.target

[Service]
Type=forking

Environment=JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64/jre
Environment=CATALINA_PID=/opt/tomcat-searchbroker/temp/tomcat.pid
Environment=CATALINA_HOME=/opt/tomcat-searchbroker
Environment=CATALINA_BASE=/opt/tomcat-searchbroker
Environment='CATALINA_OPTS=-Xms512M -Xmx1024M -server -XX:+UseParallelGC'
Environment='JAVA_OPTS=-Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom'

ExecStart=/opt/tomcat-searchbroker/bin/startup.sh
ExecStop=/opt/tomcat-searchbroker/bin/shutdown.sh

User=tomcat
Group=tomcat
UMask=0007
RestartSec=10
Restart=always

[Install]
WantedBy=multi-user.target

```



​	Windows: 

​		Follow installer: http://ftp.fau.de/apache/tomcat/tomcat-8/v8.5.38/bin/apache-tomcat-8.5.38.exe

​		And check service (one per app/tomcat): http://www.ansoncheunghk.info/article/5-steps-install-multiple-apache-tomcat-instance-windows


## License
        
 Copyright 2020 The Samply Development Community
        
 Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
        
 http://www.apache.org/licenses/LICENSE-2.0
        
 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
