package com.cubeui.backend.domain;

import java.util.Objects;

public class GraphEdge {

  String from;
  String to;

  public GraphEdge(String from, String to) {
    this.from = from;
    this.to = to;
  }

  public String getFrom() {
    return from;
  }

  public String getTo() {
    return to;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GraphEdge graphEdge = (GraphEdge) o;
    return Objects.equals(from, graphEdge.from) &&
        Objects.equals(to, graphEdge.to);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to);
  }
}
