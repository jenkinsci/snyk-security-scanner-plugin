[![Snyk logo](https://snyk.io/style/asset/logo/snyk-print.svg)](https://snyk.io)

***

# Table of Contents
- [Introduction](#introduction)
- [Release Workflow](#release-workflow)
  - [Performing a Plugin Release](#performing-a-plugin-release)
  - [Experimental Plugin Releases](#experimental-plugin-releases)


# Introduction

Snyk Jenkins plugin enables jenkins users to test their open source packages against the [Snyk vulnerability database](https://snyk.io/vuln).


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

To simplify delivery of beta versions of plugins to interested users,  the Jenkins project published an *experimental update center*. It will
include alpha and beta versions of plugins, which are not usually included in the regular update sites.

Releases that contain `alpha` or `beta` in their version number will only show up in the experimental update site, e.g. `2.0.0-alpha-1`.

To configure Jenkins to use experimental update site please follow this [tutorial](https://jenkins.io/doc/developer/publishing/releasing-experimental-updates).
