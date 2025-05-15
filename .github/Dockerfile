FROM adoptopenjdk/openjdk11 as build
# use local mvn repo to speedup build
COPY .m2 /root/.m2
WORKDIR /app
# run mvn just to satisfy dependencies - this enables dependency caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw verify -q -s .mvn/settings.xml --fail-never

# only now copy the src and do the real build based on the layer above that caches deps
COPY . .
RUN ./mvnw clean verify -s .mvn/settings.xml

FROM jenkins/jenkins:2.450-jdk11

USER root
RUN apt-get update && DEBIAN_FRONTEND=noninteractive \
    apt-get install -qq --no-install-recommends git python3 python3-pip python3-virtualenv sudo

USER jenkins

ENV JENKINS_USER admin
ENV JENKINS_PASS admin

ENV JAVA_OPTS -Djenkins.install.runSetupWizard=false

RUN jenkins-plugin-cli --plugins workflow-step-api
RUN jenkins-plugin-cli --plugins credentials
RUN jenkins-plugin-cli --plugins git
RUN jenkins-plugin-cli --plugins configuration-as-code

COPY --from=build /app/target/snyk-security-scanner.* /usr/share/jenkins/ref/plugins/

EXPOSE 8080
EXPOSE 50000
