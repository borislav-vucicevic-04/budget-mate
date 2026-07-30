package com.borislavvucicevic.budgetmate.options;

import androidx.annotation.NonNull;

import com.borislavvucicevic.budgetmate.enums.Quarter;

public class QuarterOption {
  private final String displayName;
  private final Quarter quarter;

  public QuarterOption(String displayName, Quarter quarter) {
    this.displayName = displayName;
    this.quarter = quarter;
  }

  public String getDisplayName() {
    return displayName;
  }

  public Quarter getQuarter() {
    return quarter;
  }

  @NonNull
  @Override
  public String toString() {
    return this.getDisplayName();
  }
}
