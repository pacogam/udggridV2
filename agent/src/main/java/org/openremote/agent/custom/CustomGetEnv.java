package org.openremote.agent.custom;


public class CustomGetEnv {

  public static String get(String name) {
    String value = System.getenv(name);

    if (value == null || value.isEmpty()) {
      throw new RuntimeException("Missing environment variable: " + name);
    }

    return value;
  }
}
