[![Snyk logo](https://snyk.io/style/asset/logo/snyk-print.svg)](https://snyk.io)

***

# snyk/jenkins-Plugin

Snyk Jenkins plugin enables jenkins users to test their open source packages against the [Snyk vulnerability database](https://snyk.io/vuln)

## Installation

1. This plugin requires Docker installation on the machine in order to scan your dependencies.
2. Pull Snyk docker image by running the following command: `docker pull snyk/snyk`
3. Add Jenkins user to the docker group: `sudo usermod -aG docker jenkins-user` and verify that the Jenkins user can
run docker commands without a sudo.



