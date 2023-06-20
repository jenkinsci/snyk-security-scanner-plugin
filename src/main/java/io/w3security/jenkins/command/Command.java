package io.w3security.jenkins.command;

public enum Command {

  TEST("test"),
  MONITOR("monitor");

  private final String commandName;

  Command(String commandName) {
    this.commandName = commandName;
  }

  public String commandName() {
    return commandName;
  }

}
