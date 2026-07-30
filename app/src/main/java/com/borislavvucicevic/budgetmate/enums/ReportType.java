package com.borislavvucicevic.budgetmate.enums;

/**
 * Defines the available time periods for generating financial reports.
 *
 * <p>A report type determines the date range used when retrieving and
 * aggregating budget-related data.</p>
 */
public enum ReportType {

  /**
   * A report covering a single day.
   */
  DAILY,

  /**
   * A report covering a single month.
   */
  MONTHLY,

  /**
   * A report covering a three-month period.
   */
  QUARTERLY,

  /**
   * A report covering a single year.
   */
  YEARLY,

  /**
   * A report covering a user-defined date range.
   */
  CUSTOM
}