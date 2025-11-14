FROM spring-scs-docker-virtual.usw1.packages.broadcom.com/ci/concourse-release-scripts:0.4.2-SNAPSHOT

RUN apt-get update && apt-get install --no-install-recommends -y net-tools git
RUN rm -rf /var/lib/apt/lists/*

ADD https://raw.githubusercontent.com/spring-io/concourse-java-scripts/v0.0.4/concourse-java.sh /opt/

