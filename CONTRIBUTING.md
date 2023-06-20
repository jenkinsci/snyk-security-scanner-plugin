# Contributing

## Releasing a New Version

1. Clone the repo locally and pull in any changes on `master`.
2. Set your accounts.jenkins.io username and password in `./mvn/settings.xml`.
3. Run `mvn release:prepare release:perform --settings .mvn/settings.xml`.

### Experimental Releases

To release experimental versions and configure Jenkins to use it read the documentation.

- [Publishing Experimental Plugin Releases](https://www.jenkins.io/doc/developer/publishing/releasing-experimental-updates/)

## Testing the plugin

1. Under `.github`, you'll find scripts to build a dockerized jenkins with the W3Security plugin, compiled from the current source, pre-installed.
2. To do that, run the following scripts:

```bash
.github/build.sh
.github/run.sh
```

`run.sh` will output the local URL under which Jenkins can be reached, e.g. `Please connect to IP address http://10.211.55.13:8080`
