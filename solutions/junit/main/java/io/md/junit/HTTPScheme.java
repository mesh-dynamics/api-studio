package io.md.junit;

public enum HTTPScheme {
  HTTP("http"),

  HTTPS("https");

  private String scheme;

  HTTPScheme(String scheme) {
    this.scheme = scheme;
  }

  public String getScheme() {
    return scheme;
  }
}
