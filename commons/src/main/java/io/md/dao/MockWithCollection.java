package io.md.dao;

import java.util.Optional;

public class MockWithCollection {

  public String replayCollection;
  public String recordCollection;
  public String templateVersion;
  public String runId;
  public Optional<String> dynamicInjectionConfigVersion;
  public boolean isDevtool;
  public Optional<Replay> replay;


  public MockWithCollection(String replayCollection, String recordCollection,
      String templateVersion, String runId, Optional<String> dynamicInjectionConfigVersion, boolean isDevtool , Optional<Replay> replay) {
    this.replayCollection = replayCollection;
    this.recordCollection = recordCollection;
    this.templateVersion = templateVersion;
    this.runId = runId;
    this.dynamicInjectionConfigVersion = dynamicInjectionConfigVersion;
    this.isDevtool = isDevtool;
    this.replay = replay;
  }
}