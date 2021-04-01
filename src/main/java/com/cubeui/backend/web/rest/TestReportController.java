package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.PathResults;
import com.cubeui.backend.domain.TimeLineData;
import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import com.cubeui.backend.service.MailTemplateService;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.md.dao.MatchResultAggregate;
import io.md.dao.Replay;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class TestReportController {

  @Autowired
  private CubeServerService cubeServerService;
  @Autowired
  private Validation validation;
  @Autowired
  private MailTemplateService mailTemplateService;
  @Autowired
  private ObjectMapper jsonMapper;

  @GetMapping("/sendTestReport/{replayId}")
  public ResponseEntity sendTestReport(@PathVariable("replayId") String replayId, HttpServletRequest request,
      Authentication authentication, @RequestParam List<String> emails) throws IOException {
    try {
      Pair<Replay, TimeLineData> response = getTimeLineData(replayId, authentication);
      mailTemplateService.sendTestReportTemplate(response.getFirst(),response.getSecond(),emails);
      return ResponseEntity.ok(Map.of("Message","The report has been send to the emails"));
    } catch (Exception e) {
      log.info(String.format("Error, message=", e.getMessage()));
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("Error", e.getMessage()));
    }
  }

  @GetMapping("/generateTestReport/{replayId}")
  public ResponseEntity generateTestResponse(HttpServletRequest request, @PathVariable String replayId,
      Authentication authentication) {
    try {
      Pair<Replay, TimeLineData> response = getTimeLineData(replayId, authentication);
      return ResponseEntity.ok(Map.of("replay", response.getFirst(), "timeLineData", response.getSecond()));
    } catch (Exception e) {
      log.info(String.format("Error, message=", e.getMessage()));
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("Error", e.getMessage()));
    }
  }

  private Pair<Replay, TimeLineData> getTimeLineData(String replayId, Authentication authentication) throws Exception {
    Optional<Replay> optionalReplay = cubeServerService.getReplay(replayId);
    Replay replay = optionalReplay
        .orElseThrow(() -> new RecordNotFoundException("No Replay found for replayId=" + replayId));
    validation.validateCustomerName(authentication, replay.customerId);
    ResponseEntity<byte[]> responseForTimeliners = cubeServerService.getTimeLinersResult(replay);
    if(responseForTimeliners.getStatusCode() == HttpStatus.OK) {
      String body = new String(responseForTimeliners.getBody());
      JsonNode json = jsonMapper.readTree(body);
      ArrayNode timelineResults = (ArrayNode) json.get("timelineResults");
      TimeLineData timeLineData = new TimeLineData();
      timelineResults.forEach(tr -> {
        String resultsBody = tr.get("results").toString();
        try {
          List<MatchResultAggregate> results = jsonMapper
              .readValue(resultsBody, new TypeReference<List<MatchResultAggregate>>() {
              });
          results = results.stream()
              .filter(r -> r.path.isPresent() && replay.paths.contains(r.path.get()))
              .collect(Collectors.toList());
          results.forEach(result -> {
            PathResults pathResult;
            int rmm = result.respnotmatched + result.respmatchexception;
            int total = result.respmatched + result.respnotmatched + result.resppartiallymatched
                + result.respmatchexception;
            if (result.replayId.equalsIgnoreCase(replay.replayId)) {
              timeLineData.setCurrentMismatchFraction(rmm, total, result.path.get());
            } else {
              timeLineData.addPreviousMismatchFraction(rmm, total, result.path.get());
            }
          });
        } catch (IOException e) {
          log.info(String.format("Error while reading response, message=", e.getMessage()));
        }
      });
      timeLineData.calculate95CI(timelineResults.size() - 1);
      return  Pair.of(replay, timeLineData);
    }
    throw new Exception("Error while fetching data from timeliners");
  }
}
