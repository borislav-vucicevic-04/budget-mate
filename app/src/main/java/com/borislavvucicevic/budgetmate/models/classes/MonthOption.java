package com.borislavvucicevic.budgetmate.models.classes;

import androidx.annotation.NonNull;

import com.borislavvucicevic.budgetmate.models.enums.Month;
import com.borislavvucicevic.budgetmate.models.enums.Quarter;

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