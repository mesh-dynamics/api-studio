package com.cube.core;

import com.cube.dao.ReqRespStore;
import com.cube.dao.Result;
import com.cube.utils.ScheduledCompletable;
import io.md.dao.Recording;
import io.md.dao.agent.config.AgentConfigTagInfo;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TagConfig {

  private static Logger LOGGER = LogManager.getLogger(TagConfig.class);

  ReqRespStore rrstore;
  private final ScheduledExecutorService scheduler;
  static private final long tagApplyDelay = 30;

  public TagConfig(ReqRespStore rrstore) {
    this.rrstore = rrstore;
    // use single thread for all waiting requirements so as to not block the main threads
    scheduler = Executors.newSingleThreadScheduledExecutor();;
  }

  public CompletableFuture<Void> setTag(Recording recording, String instanceId, String tag) {
    AtomicBoolean changed = new AtomicBoolean(false);
    Result<AgentConfigTagInfo> response = rrstore.getAgentConfigTagInfoResults(
        recording.customerId, recording.app, Optional.empty(), instanceId);
    response.getObjects().forEach(agentconfig -> {
      if(!agentconfig.tag.equals(tag)){
        AgentConfigTagInfo agentConfigTagInfo = new AgentConfigTagInfo(
            agentconfig.customerId, agentconfig.app, agentconfig.service, agentconfig.instanceId, tag);
        changed.set(true);
        rrstore.updateAgentConfigTag(agentConfigTagInfo);
      }
    });
    LOGGER.info("Waiting for 30s to set Tag: " + tag + ", current time" + Instant.now());
    if(changed.get()) {
      // waiting is done in a different thread and a future is returned
      return ScheduledCompletable.schedule(
          scheduler, () -> {
            LOGGER.info("Finished waiting for set Tag: " + tag + ", current time" + Instant.now());
            return null;
          }, tagApplyDelay, TimeUnit.SECONDS);
    }
    return CompletableFuture.completedFuture(null);
  }
}
