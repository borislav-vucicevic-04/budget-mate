package com.borislavvucicevic.budgetmate.options;

import androidx.annotation.NonNull;

import com.borislavvucicevic.budgetmate.enums.ReportType;

public class ReportTypeOption {
  private final String displayName;
  private final ReportType type;

  public ReportTypeOption(String displayName, ReportType type) {
    this.displayName = displayName;
    this.type = type;
  }

  public String getDisplayName() {
    return displayName;
  }

  public ReportType getType() {
    return type;
  }

  @NonNull
  @Override
  public String toString() { return  this.getDisplayName(); }
}
