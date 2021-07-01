# Contributing

## Releasing a New Version

1. Clone the repo locally and pull in any changes on `master`.
2. Set your accounts.jenkins.io username and password in `./mvn/settings.xml`.
3. Run `mvn release:prepare release:perform --settings .mvn/settings.xml`.

### Experimental Releases

To release experimental versions and configure Jenkins to use it read the documentation. 

- [Publishing Experimental Plugin Releases](https://www.jenkins.io/doc/developer/publishing/releasing-experimental-updates/)
