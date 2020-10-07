package io.md.dao;

import io.md.dao.Event;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DynamicInjectionEventDao {
  private Event requestEvent;
  private Map<String, String> contextMap;
  private String environmentName;
  private String injectionConfigVersion;
}
