package io.md.dao;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME,
    property = "type")
@JsonSubTypes({@Type(value = AgentConfig.class)})
public interface StoreConfig<T> {
  T getConfig();
  String getType();
}
