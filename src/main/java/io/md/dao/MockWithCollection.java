package io.md.dao;

import java.util.Optional;

public class MockWithCollection {

  public String replayCollection;
  public String recordCollection;
  public String templateVersion;
  public String runId;

  public MockWithCollection(String replayCollection, String recordCollection,
      String templateVersion, String runId) {
    this.replayCollection = replayCollection;
    this.recordCollection = recordCollection;
    this.templateVersion = templateVersion;
    this.runId = runId;
  }
}
