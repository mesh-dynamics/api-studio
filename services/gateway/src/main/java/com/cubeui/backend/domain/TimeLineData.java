/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cubeui.backend.domain;

import com.cubeui.backend.Utils;
import com.cubeui.backend.security.Constants;
import com.google.common.util.concurrent.AtomicDouble;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
public class TimeLineData {
  private Map<String, PathResults> pathResults;
  private int currentAllTotal;
  private int currentAllResponseMismatches;
  private double currentAllMismatchFraction;
  private int previousAllTotal;
  private int previousAllResponseMismatches;
  private List<Double> previousMismatches;
  private double previousAverage;
  private double previousAll95CIRespMismatches = 0;
  private int previousCount = 0;

  public TimeLineData() {
    this.pathResults = new HashMap<>();
    this.currentAllTotal = 0;
    this.currentAllResponseMismatches = 0;
    this.currentAllMismatchFraction = 0;
    this.previousAllTotal = 0;
    this.previousAllResponseMismatches = 0;
    this.previousMismatches = new ArrayList<>();
    this.previousAverage = 0;
    this.previousAll95CIRespMismatches = 0;
    this.previousCount = 0;

  }

  public void setCurrentMismatchFraction(int rmm, int total, String path) {
    PathResults pathResult = new PathResults();
    pathResult.setCurrentMismatchFraction(rmm, total);
    this.pathResults.put(path, pathResult);
    this.currentAllTotal += total;
    this.currentAllResponseMismatches += rmm;
  }

  public void addPreviousMismatchFraction(int rmm, int total, String path) {
    PathResults pathResult = Optional.ofNullable(this.pathResults.get(path)).orElse(new PathResults());
    pathResult.addPreviousMisMatchFractions(rmm, total);
    this.pathResults.put(path, pathResult);
    this.previousAllTotal += total;
    this.previousAllResponseMismatches += rmm;
  }

  private void setCurrentAllMismatchFraction() {
    if(this.currentAllTotal != 0) {
      this.currentAllMismatchFraction = (double)this.currentAllResponseMismatches / (double)this.currentAllTotal;
    }
  }

  private void setPreviousAverage() {
    if(this.previousAllTotal != 0) {
      this.previousAverage = (double)this.previousAllResponseMismatches / (double)this.previousAllTotal;
    }
  }

  private void calculate95CIForPrev() {
    this.pathResults.forEach((k,v) -> {
      double pre95 = v.calculate95CI();
      this.previousMismatches.add(pre95);
    });
  }

  public void calculate95CI(int previousCount) {
    this.previousCount = previousCount ;
    this.setPreviousAverage();
    this.setCurrentAllMismatchFraction();
    this.calculate95CIForPrev();
    this.previousAll95CIRespMismatches = Utils.calculate95CI(this.previousMismatches);
  }
}
