# Contributing

## Pre-requisites
- OpenJDK 17

## Releasing a New Version

1. Clone the repo locally and pull in any changes on `main`.
2. Set your accounts.jenkins.io username and password in `./mvn/settings.xml`.
3. Run `mvn release:prepare release:perform --settings .mvn/settings.xml`.

### Experimental Releases

To release experimental versions and configure Jenkins to use it read the documentation. 

- [Publishing Experimental Plugin Releases](https://www.jenkins.io/doc/developer/publishing/releasing-experimental-updates/)

## Testing the plugin
1. Under `.github`, you'll find scripts to build a dockerized jenkins with the Snyk plugin, compiled from the current source, pre-installed.
2. To do that, run the following scripts:

```bash
.github/build.sh
.github/run.sh
```

`run.sh` will output the local URL under which Jenkins can be reached, e.g. `Please connect to IP address http://10.211.55.13:8080`

3. Setup the Snyk CLI in the Jenkins UI: 
- On the Jenkins UI homepage, go to "Manage Jenkins" > "System Configuration" > "Tools" > "Snyk Installations"
- Create a new installation with the preferred details

4. Create a new Snyk API Token:
- On the Jenkins UI homepage, go to "Manage Jenkins" > "Manage Credentials"
- Choose a Store
- Choose a Domain
- Go to "Add Credentials"
- Select "Snyk API Token"
- Configure the Credentials

5. Create a new job and add the Snyk Security task:
- On the Jenkins UI homepage, go to "New Item"
- Select "Freestyle Project"
- Name the project
- Under "Build", select "Add build step" select "Invoke Snyk Security Task"
- Configure as needed. Click the "?" icons for more information about each option.
- Save the job
- Run the job
