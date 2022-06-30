FROM adoptopenjdk/openjdk8 as build

COPY .m2 /root/.m2

WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw verify --fail-never

COPY . .
RUN ./mvnw clean verify -s .mvn/settings.xml

FROM jenkins/jenkins:2.346.1-lts-jdk8

ENV JENKINS_USER admin
ENV JENKINS_PASS admin

ENV JAVA_OPTS -Djenkins.install.runSetupWizard=false

RUN /usr/local/bin/install-plugins.sh workflow-step-api
RUN /usr/local/bin/install-plugins.sh credentials
RUN /usr/local/bin/install-plugins.sh git
RUN /usr/local/bin/install-plugins.sh github

COPY --from=build /app/target/snyk-security-scanner.* /usr/share/jenkins/ref/plugins/

EXPOSE 8080
EXPOSE 50000


