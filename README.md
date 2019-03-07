[![Snyk logo](https://snyk.io/style/asset/logo/snyk-print.svg)](https://snyk.io)

***

# Table of Contents
- [Introduction](#introduction)
- [Configuration](#configuration)
  - [Global Configuration](#global-configuration)
  - [Project Configuration](#project-configuration)
    - [Freestyle Jobs](#freestyle-jobs)
    - [Pipeline Jobs](#pipeline-jobs)
- [Migration from v1](#migration-from-v1)
- [Release Workflow](#release-workflow)
  - [Performing a Plugin Release](#performing-a-plugin-release)
  - [Experimental Plugin Releases](#experimental-plugin-releases)


# Introduction

Snyk Jenkins plugin enables jenkins users to test their open source packages against the [Snyk vulnerability database](https://snyk.io/vuln).


# Configuration

## Global Configuration

Before using snyk plugin on a project, you must configure some global settings.

First, it is necessary to define the Snyk CLI version to make available on Jenkins. From the main page click on **Manage Jenkins**, then click
on **Global Tool Configuration** to goto the Jenkins tool page. Then add a Snyk installation version and this version will be automatically
installed on the Jenkins during builds.

![Snyk Installer](docs/snyk_configuration_installation_v2.png)

> We recommend to set `latest` version, so you will get the actual version of Snyk CLI.

Second, provide the Snyk API Token to Jenkins so CLI will be able to access snyk.io. From the Jenkins home page click **Credentials > System**.
In the **ID** field, specify a meaningful credential ID value - for example, `my-snyk-api-token`.

![Snyk API Token](docs/snyk_configuration_token_v2.png)


## Project Configuration

### Freestyle Jobs

For a project to use the Snyk Security plugin, you need to enable it in the project configuration page. To add the task to the job, select
**Build > Add build step > Invoke Snyk Security Task**.

#### Basic Configuration

![Basic configuration](docs/snyk_buildstep_basic_v2.png)

- **When issues are found** - This specifies if builds should be failed or continued based on issues found by Snyk.
- **Monitor project on build** - Take a snapshot of its current dependencies on Snyk.io.
- **Snyk token** - The ID for the API token from the Credentials plugin to be used to authenticate to Snyk (credential type must be "Snyk API token").
- **Target file** - The path to the manifest file to be used by Snyk.
- **Organisation** - The Snyk organisation in which this project should be tested and monitored.
- **Project name** - A custom name for the Snyk project created for this Jenkins project on every build.

#### Advanced Configuration

To see the advanced configuration for the plugin, click the "Advanced" button. This section allows you to specify Snyk installation and
additional arguments to Snyk CLI and can be used by power users.

![Advanced configuration](docs/snyk_buildstep_advanced_v2.png)

- **Snyk installation** - Snyk installation configured in "Global Tool Configuration".
- **Additional arguments** - _TODO: good description about arguments passed to CLI_

### Pipeline Jobs

Snyk pipeline integration expose `snyk` function to scan your dependencies as part of your pipeline script. We recommend to use "Snippet Generator"
to generate needed step statement you may copy into your Jenkinsfile.

This `snyk` function accepts the following parameters:

- **additionalArguments** - _TODO: good description about arguments passed to CLI_
- **failOnIssues** - This specifies if builds should be failed or continued based on issues found by Snyk.
- **organisation** - The Snyk organisation in which this project should be tested and monitored.
- **projectName** - A custom name for the Snyk project created for this Jenkins project on every build.
- **severity** - Only report vulnerabilities of provided level or higher (low/medium/high).
- **snykInstallation** - Snyk installation configured in "Global Tool Configuration".
- **snykTokenId** - The ID for the API token from the Credentials plugin to be used to authenticate to Snyk.
- **targetFile** - The path to the manifest file to be used by Snyk.


# Migration from v1

**Note:** the new v2 of the plugin contains incompatible changes to v1 and require you to adapt your Jenkins jobs. You have to perform
global configuration described in [here](#global-configuration).

- The plugin does not requires Docker installation on master or worker nodes. Add a Snyk installer in "Global Tool Configuration" page.
- You don't need to pass Snyk API token as `SNYK_TOKEN` environment variable to the job. Add a credential of type "Snyk API token".
- Parameters "Runtime Arguments", "Docker Image", "HTTP Proxy", "HTTPS Proxy" are obsolete and don't needed anymore.
- Pipeline syntax was changed, see [Pipeline jobs](#pipeline-jobs) section for documentation.


# Release Workflow

We're using Travis CI to automatically build releases. First make sure the following variables are defined in Travis
[repository settings](https://docs.travis-ci.com/user/environment-variables#defining-variables-in-repository-settings):
- `JENKINS_USERNAME`
- `JENKINS_PASSWORD`

> Note! Currently releases are possible only from `master` and `2.0.0-dev` branches.

## Performing a Plugin Release

1. Create a tag on commit you want to release (form is `x.y.z`). This `x.y.z` will be used as artifact version when deploying to jenkinsci
repository.
2. Check that "Release" stage on Travis was successful.
3. The new version of the plugin should show up in the update center within eight hours.

## Experimental Plugin Releases

To simplify delivery of beta versions of plugins to interested users, the Jenkins project published an *experimental update center*. It will
include alpha and beta versions of plugins, which are not usually included in the regular update sites.

Releases that contain `alpha` or `beta` in their version number will only show up in the experimental update site, e.g. `2.0.0-alpha-1`.

To configure Jenkins to use experimental update site please follow this [tutorial](https://jenkins.io/doc/developer/publishing/releasing-experimental-updates).
