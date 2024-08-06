# Snyk Security

[![Homepage](https://img.shields.io/jenkins/plugin/v/snyk-security-scanner.svg)](https://plugins.jenkins.io/snyk-security-scanner)
[![Changelog](https://img.shields.io/github/release/jenkinsci/snyk-security-scanner-plugin.svg?label=changelog)](https://github.com/jenkinsci/snyk-security-scanner-plugin/releases)
[![Installs](https://img.shields.io/jenkins/plugin/i/snyk-security-scanner.svg)](https://plugins.jenkins.io/snyk-security-scanner)
[![Vulnerabilities](https://snyk.io/test/github/jenkinsci/snyk-security-scanner-plugin/badge.svg)](https://snyk.io/test/github/jenkinsci/snyk-security-scanner-plugin)

[![Snyk](https://snyk.io/style/asset/logo/snyk-print.svg)](https://snyk.io)

Test and monitor your projects for vulnerabilities with Jenkins. Officially maintained by [Snyk](https://snyk.io).

## Usage

To use the plugin up you will need to take the following steps in order:

1. [Install the Snyk Security Plugin](#1-install-the-snyk-security-plugin)
2. [Configure a Snyk Installation](#2-configure-a-snyk-installation)
3. [Configure a Snyk API Token Credential](#3-configure-a-snyk-api-token-credential)
4. [Add Snyk Security to your Project](#4-add-snyk-security-to-your-project)
5. [Run a Build and View Your Snyk Report](#5-view-your-snyk-security-report)

## 1. Install the Snyk Security Plugin

- Go to "Manage Jenkins" > "Manage Plugins" > "Available".
- Search for "Snyk Security".
- Install the plugin.

## 2. Configure a Snyk Installation

- Go to "Manage Jenkins" > "Global Tool Configuration"
- Add a "Snyk Installation"
- Configure the Installation
- Remember the "Name" as you'll need it when configuring the build step.

### Automatic Installations

The plugin can download the latest version of Snyk's binaries and keep them up-to-date for you.

<blockquote>
<details>
<summary>📷 Show Preview</summary>

![Snyk Installer Auto Update](docs/snyk_configuration_installation_auto-update_v2.png)

</details>
</blockquote>

### Manual Installations

- Download the following binaries. Choose the binary suitable for your agent's operating system:
  - [Snyk CLI](https://github.com/snyk/snyk/releases/latest)
  - [snyk-to-html](https://github.com/snyk/snyk-to-html/releases/latest)
- Place the binaries in a single directory on your agent.
  - Do not change the filename of the binaries.
  - Make sure you have the correct permissions to execute the binaries.
- Provide the absolute path to the directory under "Installation
directory".

<blockquote>
<details>
<summary>📷 Show Preview</summary>

![Snyk Installer Manual](docs/snyk_configuration_installation_manual_v2.png)

</details>
</blockquote>

### Custom API Endpoints

By default, Snyk uses the https://snyk.io/api endpoint. 
It is possible to configure Snyk to use a different endpoint by changing the `SNYK_API` environment variable:

- Go to "Manage Jenkins" > "Configure System"
- Under "Global Properties" check the "Environment variables" option
- Click "Add"
- Set the name to `SNYK_API` and the value to the custom endpoint

Refer to the [Snyk documentation](https://docs.snyk.io/snyk-cli/configure-the-snyk-cli#configuration-to-connect-to-the-snyk-api) for more information about API configuration.

## 3. Configure a Snyk API Token Credential

- [Get your Snyk API Token](https://support.snyk.io/hc/en-us/articles/360004037537-Authentication-for-third-party-tools)
- Go to "Manage Jenkins" > "Manage Credentials"
- Choose a Store
- Choose a Domain
- Go to "Add Credentials"
- Select "Snyk API Token"
- Configure the Credentials
- Remember the "ID" as you'll need it when configuring the build step.

<blockquote>
<details>
<summary>📷 Show Preview</summary>

![Snyk API Token](docs/snyk_configuration_token_v2.png)

</details>
</blockquote>

## 4. Add Snyk Security to your Project

This step will depend on if you're using Freestyle Projects or Pipeline Projects.

### Freestyle Projects

- Select a project
- Go to "Configure"
- Under "Build", select "Add build step" select "Invoke Snyk Security Task"
- Configure as needed. Click the "?" icons for more information about each option.

<blockquote>
<details>
<summary>📷 Show Preview</summary>

![Basic configuration](docs/snyk_buildstep.png)

</details>
</blockquote>

### Pipeline Projects

Use the `snykSecurity` step as part of your pipeline script. You can use the "Snippet Generator" to generate the code
from a web form and copy it into your pipeline.

<blockquote>
<details>
<summary>📷 Show Example</summary>

```groovy
pipeline {
  agent any

  stages {
    stage('Build') {
      steps {
        echo 'Building...'
      }
    }
    stage('Test') {
      steps {
        echo 'Testing...'
        snykSecurity(
          snykInstallation: '<Your Snyk Installation Name>',
          snykTokenId: '<Your Snyk API Token ID>',
          // place other optional parameters here, for example:
          additionalArguments: '--all-projects --detection-depth=<DEPTH>'
        )
      }
    }
    stage('Deploy') {
      steps {
        echo 'Deploying...'
      }
    }
  }
}
```

</details>
</blockquote>

You can pass the following parameters to your `snykSecurity` step.

#### `snykInstallation` (required)

Snyk Installation Name. As configured in "[2. Configure a Snyk Installation](#2-configure-a-snyk-installation)".

#### `snykTokenId` (optional, default: *none*)

Snyk API Token Credential ID. As configured in "[3. Configure a Snyk API Token Credential](#3-configure-a-snyk-api-token-credential)".

If you prefer to provide the Snyk API Token another way, such using alternative credential bindings, you'll need to
provide a "SNYK_TOKEN" build environment variable.

#### `failOnIssues` (optional, default: `true`)

Whether the step should fail if issues and vulnerabilities are found.

#### `failOnError` (optional, default: `true`)

Whether the step should fail if Snyk fails to scan the project due to an error. Errors include scenarios like: failing
to download Snyk's binaries, improper Jenkins setup, bad configuration and server errors.

#### `organisation` (optional, default: *automatic*)

The Snyk Organisation in which this project should be tested and monitored. See `--org`
under [Snyk CLI docs](https://snyk.io/docs/using-snyk/) for default behaviour.

#### `projectName` (optional, default: *automatic*)

A custom name for the Snyk project created for this Jenkins project on every build. See `--project-name`
under [Snyk CLI docs](https://snyk.io/docs/using-snyk/) for default behaviour.

#### `targetFile` (optional, default: *automatic*)

The path to the manifest file to be used by Snyk. See `--file` under [Snyk CLI docs](https://snyk.io/docs/using-snyk/)
for default behaviour.

#### `severity` (optional, default: *automatic*)

The minimum severity to detect. Can be one of the following: `low`, `medium`, `high`
, `critical`. See `--severity-threshold` under [Snyk CLI docs](https://snyk.io/docs/using-snyk/) for default behaviour.

#### `additionalArguments` (optional, default: *none*)

See [Snyk CLI docs](https://snyk.io/docs/using-snyk/) for information on additional arguments.

## 5. View your Snyk Security Report

- Complete a new build of your project.
- Go to the build's page.
- Click on "Snyk Security Report" in the sidebar to see the results.

<blockquote>
<details>
<summary>📷 Show Preview</summary>

![Snyk Build Report](docs/snyk_build_report.png)

</details>
</blockquote>

If there are any errors you may not see the report. See [Troubleshooting](#troubleshooting).

## Troubleshooting

### Increase Logging

To see more information on your steps, you can increase logging and re-run your steps.

- View the "Console Output" for a specific build.
- Add a logger to capture all `io.snyk.jenkins` logs.
  Follow [this article](https://support.cloudbees.com/hc/en-us/articles/204880580-How-do-I-create-a-logger-in-Jenkins-for-troubleshooting-and-diagnostic-information-)
  .
- Add `--debug` to "Additional Arguments" to capture all Snyk CLI logs. Debug output is available under "Console Output"
  for your build.

### Failed Installations

By default, Snyk Installations will download Snyk's binaries over the network from `downloads.snyk.io` and use `static.snyk.io` as a fallback. If this fails there
may be a network or proxy issue. If you cannot fix the issue, you can use a [Manual Installation](#2-configure-a-snyk-installation) instead.

---

Made with 💜 by Snyk
