#!/usr/bin/env groovy
def minimumSupportedJenkinsVersion = "2.504.1"

/* `buildPlugin` step provided by: https://github.com/jenkins-infra/pipeline-library */
buildPlugin(configurations: [
  // Test against which ever is the newest Jenkins version
  [ platform: "linux", jdk: "17" ],
  [ platform: "windows", jdk: "17" ],
  [ platform: "linux", jdk: "21" ],
  [ platform: "windows", jdk: "21" ],


  [ platform: "linux", jdk: "17", jenkins: "${minimumSupportedJenkinsVersion}"],
  [ platform: "windows", jdk: "17", jenkins: "${minimumSupportedJenkinsVersion}"],
  [ platform: "linux", jdk: "21", jenkins: "${minimumSupportedJenkinsVersion}"],
  [ platform: "windows", jdk: "21", jenkins: "${minimumSupportedJenkinsVersion}"],
])
