#!/usr/bin/env groovy

/* `buildPlugin` step provided by: https://github.com/jenkins-infra/pipeline-library */
buildPlugin(configurations: [
  [ platform: "linux", jdk: "8" ],
  [ platform: "windows", jdk: "8" ],
  [ platform: "linux", jdk: "8", jenkins: "2.289.3", javaLabel: 8 ],
  [ platform: "windows", jdk: "8", jenkins: "2.289.3", javaLabel: 8 ]
])
