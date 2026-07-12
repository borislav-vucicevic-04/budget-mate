package com.borislavvucicevic.budgetmate.models;

import androidx.annotation.NonNull;

public class CurrencyOption {
  private final String displayName;
  private final CurrencyCode code;

  public CurrencyOption(String displayName, CurrencyCode code) {
    this.displayName = displayName;
    this.code = code;
  }

  public String getDisplayName() {
    return displayName;
  }

  public CurrencyCode getCode() {
    return code;
  }

  @NonNull
  @Override
  public String toString() {
    return code.toString();
  }
}
