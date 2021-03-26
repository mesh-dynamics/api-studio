package com.cubeui.backend.domain;

import com.cubeui.backend.Utils;
import com.cubeui.backend.security.Constants;
import com.google.common.util.concurrent.AtomicDouble;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PathResults {

  /**
   * Current refers to the path replayId we are generating the test Report
   */
  int currentResponseMismatches;
  int currentTotal;
  double currentMismatchFraction;
  /**
   * Previous refers to the same path for other replayIds of the same golden
   */
  int previousResponseMismatches;
  int previousTotal;
  double previousMismatchFraction;
  List<Double> previousMismatchFractions;
  int previous95CIRespMismatches;

  public PathResults() {
    this.currentResponseMismatches = 0;
    this.currentTotal = 0;
    this.currentMismatchFraction = 0;
    this.previousResponseMismatches = 0;
    this.previousTotal = 0;
    this.previousMismatchFraction = 0;
    this.previousMismatchFractions = new ArrayList<>();
    this.previous95CIRespMismatches = 0;
  }

  public void setCurrentMismatchFraction(int rmm, int total) {
    this.currentResponseMismatches = rmm;
    this.currentTotal = total;
    if(this.currentTotal != 0) {
      this.currentMismatchFraction = (double)this.currentResponseMismatches / (double)this.currentTotal;
    }
  }

  public void addPreviousMisMatchFractions(int rmm, int total) {
    this.previousResponseMismatches += rmm;
    this.previousTotal += total;
    if(total !=0) {
      double val = (double)rmm/(double)total;
      this.previousMismatchFractions.add(val);
    } else {
      this.previousMismatchFractions.add(0.0);
    }
    if(this.previousTotal != 0) {
      this.previousMismatchFraction = (double)this.previousResponseMismatches / (double)this.previousTotal;
    }
  }

  public int calculate95CI() {
    this.previous95CIRespMismatches = Utils.calculate95CI(this.previousMismatchFractions);
    return this.previous95CIRespMismatches;
  }
}
