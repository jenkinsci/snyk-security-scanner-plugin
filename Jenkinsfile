#!/usr/bin/env groovy

/* `buildPlugin` step provided by: https://github.com/jenkins-infra/pipeline-library */
buildPlugin(configurations: [
  [ platform: "linux", jdk: "17" ],
  [ platform: "windows", jdk: "17" ],
  [ platform: "linux", jdk: "17", jenkins: "2.479.3", javaLabel: 17 ],
  [ platform: "windows", jdk: "17", jenkins: "2.479.3", javaLabel: 17 ]
])
