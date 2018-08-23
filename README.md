[![Snyk logo](https://snyk.io/style/asset/logo/snyk-print.svg)](https://snyk.io)

***

# snyk/jenkins-Plugin

Snyk Jenkins plugin enables jenkins users to test their open source packages against the [Snyk vulnerability database](https://snyk.io/vuln)

## Installation

1. This plugin requires Docker installation on the machine in order to scan your dependencies.
2. Pull Snyk docker image by running the following command: `docker pull snyk/snyk-cli`
3. Add Jenkins user to the docker group: `sudo usermod -aG docker jenkins-user` and verify that the Jenkins user can
run docker commands without a sudo.


## Release

0. Set up your local maven env to allow releases of the jenkins plugin (chat with people who have done this before).
1. Create a branch off of master, push changes, open a PR and get it merged to master.
2. Pull master locally and run `mvn release:prepare release:perform -X -B`. This pushes the release and adds two commits to master.
3. Push master branch to the repo to allow for the next release to happen in the future.

