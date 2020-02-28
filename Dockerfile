FROM ubuntu:18.04

RUN apt-get update
RUN apt-get install -y wget software-properties-common
RUN wget -qO - https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public | apt-key add -
RUN add-apt-repository --yes https://adoptopenjdk.jfrog.io/adoptopenjdk/deb/
RUN apt-get install -y adoptopenjdk-8-hotspot
RUN apt-get install -y maven

COPY . /src
WORKDIR /src

RUN mvn -version
RUN java -version

RUN mvn -B -C -q clean test
RUN mvn -B -C -fae -Dskip.unit.tests=true verify -Pintegration-tests
RUN mvn -B -C -fae site -Psite-all-reports
