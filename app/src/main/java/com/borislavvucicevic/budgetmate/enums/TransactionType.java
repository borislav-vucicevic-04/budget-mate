package com.borislavvucicevic.budgetmate.enums;

/**
 * Represents the type of financial transaction in the BudgetMate
 * application.
 *
 * <p>A transaction can either represent money received by the user or
 * money spent by the user. This enum is used throughout the application
 * to distinguish between income and expense transactions.</p>
 *
 * <p>The transaction type may also be used when determining how transaction
 * amounts are displayed, categorized, and processed in financial reports.</p>
 */
public enum TransactionType {

  /**
   * Represents an incoming financial transaction that increases the user's
   * available funds.
   */
  INCOME,

  /**
   * Represents an outgoing financial transaction that decreases the user's
   * available funds.
   */
  EXPENSE
}
