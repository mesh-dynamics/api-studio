package io.md.dao;

import java.util.Optional;

public class MockWithCollection {

  public String replayCollection;
  public String recordCollection;
  public String templateVersion;
  public Optional<String> runId;

  public MockWithCollection(String replayCollection, String recordCollection,
      String templateVersion, Optional<String> runId) {
    this.replayCollection = replayCollection;
    this.recordCollection = recordCollection;
    this.templateVersion = templateVersion;
    this.runId = runId;
  }
}
