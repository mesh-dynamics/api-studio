package io.md.dao;

import java.util.Optional;

/**
 * @author prasad
 *
 */
public class MatchResultAggregate {


  /**
   * @param path
   * @param service
   * @param app
   * @param replayId
   */
  public MatchResultAggregate(String app, String replayId, Optional<String> service, Optional<String> path) {
    this.app = app;
    this.replayId = replayId;

    this.service = service;
    this.path = path;
  }

  /**
   * This constructor is only for jackson json deserialization
   */
  public MatchResultAggregate() {
    this.app = "";
    this.replayId = "";
  }

  final public String app;
  final public String replayId;

  public Optional<String> service = Optional.empty();
  public Optional<String> path = Optional.empty();

  public int reqmatched = 0; // number of requests exactly matched
  public int reqpartiallymatched = 0; // number of requests partially matched
  public int reqnotmatched = 0; // not matched
  public int respmatched = 0; // resp matched exactly
  public int resppartiallymatched = 0; // resp matched based on template
  public int respnotmatched = 0; // not matched
  public int respmatchexception = 0; // some exception during matching
  public int recReqNotMatched = 0;
  public int mockReqNotMatched = 0;
  public int replayReqNotMatched = 0;
}