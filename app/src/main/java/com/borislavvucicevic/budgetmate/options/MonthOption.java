package com.borislavvucicevic.budgetmate.options;

import androidx.annotation.NonNull;

import com.borislavvucicevic.budgetmate.enums.Month;

public class MonthOption {
  private final String displayName;
  private final Month month;

  public MonthOption(String displayName, Month quarter) {
    this.displayName = displayName;
    this.month = quarter;
  }

  public String getDisplayName() {
    return displayName;
  }

  public Month getMonth() {
    return month;
  }

  @NonNull
  @Override
  public String toString() {
    return this.getDisplayName();
  }
}