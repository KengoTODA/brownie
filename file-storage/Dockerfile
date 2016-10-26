FROM java:8-jre
MAINTAINER Kengo TODA skypencil@gmail.com

# Install brownie

RUN mkdir -p /etc/brownie
COPY target/file-storage-*.jar /etc/brownie/brownie.jar
COPY application-conf.json /etc/brownie/application-conf.json

# EXPOSE HTTP (8080) and Hazelcast ports
# https://github.com/vert-x3/vertx-hazelcast/blob/3.1.0/src/main/resources/default-cluster.xml
EXPOSE 5701 8080 54327/udp

CMD ["/usr/bin/java", "-jar", "/etc/brownie/brownie.jar", "-cluster", "-conf", "/etc/brownie/application-conf.json"]
