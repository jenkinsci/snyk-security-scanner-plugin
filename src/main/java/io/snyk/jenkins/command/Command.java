package io.snyk.jenkins.command;
import java.util.stream.Stream;

public enum Command {

  TEST("test"),
  MONITOR("monitor"),
  SBOM("sbom"),
  SBOM_TEST("sbom test");

  private final String command;

  Command(String command) {
    this.command = command;
  }

  public String getCommand() {
    return command;
  }

  public static Command getIfPresent(String command) {
    if (command == null) {
      return null;
    }

    return Stream.of(Command.values())
                 .filter(entry -> entry.getCommand().equalsIgnoreCase(command))
                 .findFirst().orElse(null);
  }
}
