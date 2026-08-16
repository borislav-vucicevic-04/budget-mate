package com.borislavvucicevic.budgetmate.schemas;

import androidx.annotation.NonNull;

public fina Collections {
  USERS,
  TRANSACTIONS,
  CATEGORIES;

  @NonNull
  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }
}