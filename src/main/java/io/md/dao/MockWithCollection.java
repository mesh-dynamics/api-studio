package io.md.dao;

public class MockWithCollection {

  public String replayCollection;
  public String recordCollection;
  public String templateVersion;

  public MockWithCollection(String replayCollection, String recordCollection,
      String templateVersion) {
    this.replayCollection = replayCollection;
    this.recordCollection = recordCollection;
    this.templateVersion = templateVersion;
  }
}
